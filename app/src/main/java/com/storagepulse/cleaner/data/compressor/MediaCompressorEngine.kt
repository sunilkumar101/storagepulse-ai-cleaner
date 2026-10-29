package com.storagepulse.cleaner.data.compressor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodecList
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.storagepulse.cleaner.data.media.VideoDetails
import com.storagepulse.cleaner.data.media.VideoItem
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import com.storagepulse.cleaner.data.trash.TrashMoveResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

enum class CompressionTier { HIGH_FIDELITY, BALANCED_1080, COMPACT_720 }

/** What a tier will produce for one video. */
data class TierPlan(
    val tier: CompressionTier,
    val shortSide: Int,
    val videoBitrate: Int,
    val mimeType: String,
    val estimatedBytes: Long,
    /** False when the result would not be meaningfully smaller than the original. */
    val worthIt: Boolean,
)

data class EncoderInfo(val mimeType: String, val name: String, val hardware: Boolean)

/** Real preview: a 2-second clip encoded with the chosen tier and compared with the original. */
data class PreviewResult(
    val original: Bitmap,
    val compressed: Bitmap,
    /** Luma PSNR between the two frames (≥ 40 dB is visually transparent for most people). */
    val psnrDb: Double,
    /** Full-video size projected from the encoded clip. */
    val projectedBytes: Long,
    /** Encoding speed vs real time (2 s clip / wall time). */
    val speedX: Double,
    val encoderName: String?,
)

sealed interface CompressOutcome {
    data class Done(val output: File, val originalBytes: Long, val outputBytes: Long, val originalInTrash: Boolean) : CompressOutcome
    data class Skipped(val reason: String) : CompressOutcome
    data class Failed(val reason: String) : CompressOutcome
}

/**
 * On-device video compressor (Media3 Transformer, hardware H.265 with H.264 fallback).
 * Never deletes an original: it either goes to the 30-Day Safe Trash (the compressed file takes
 * its name) or stays untouched next to a "_compressed" copy.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Singleton
class MediaCompressorEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeTrash: SafeTrashRepository,
    private val scanner: StorageScannerRepository,
) {
    val encoder: EncoderInfo by lazy { pickEncoder() }

    fun plan(item: VideoItem, details: VideoDetails, tier: CompressionTier): TierPlan {
        val srcShort = min(details.width, details.height).takeIf { it > 0 } ?: 1080
        val durationS = (details.durationMs.coerceAtLeast(1)) / 1000.0
        val srcBps = (details.bitrateBps ?: (item.sizeBytes * 8 / durationS).toLong()).coerceAtLeast(500_000)
        val highFps = (details.fps ?: 30) > 45
        val hevc = encoder.mimeType == MimeTypes.VIDEO_H265

        val (short, baseBps) = when (tier) {
            CompressionTier.HIGH_FIDELITY -> srcShort to (srcBps * 0.4)
            CompressionTier.BALANCED_1080 -> min(srcShort, 1080) to bitrateFor(min(srcShort, 1080))
            CompressionTier.COMPACT_720 -> min(srcShort, 720) to bitrateFor(min(srcShort, 720)) * 0.75
        }
        var bps = baseBps * (if (highFps && tier != CompressionTier.HIGH_FIDELITY) 1.4 else 1.0)
        if (!hevc) bps *= 1.5 // H.264 needs ~50% more bits for the same quality
        bps = min(bps, srcBps * 0.85)
        val audioBps = 128_000
        val estimate = ((bps + audioBps) / 8 * durationS).toLong()
        return TierPlan(
            tier = tier,
            shortSide = short,
            videoBitrate = bps.toInt().coerceAtLeast(300_000),
            mimeType = encoder.mimeType,
            estimatedBytes = estimate,
            worthIt = estimate < item.sizeBytes * 0.85,
        )
    }

    /** HEVC targets for visually clean phone video at each size (30 fps). */
    private fun bitrateFor(shortSide: Int): Double = when {
        shortSide >= 2160 -> 16_000_000.0
        shortSide >= 1440 -> 9_000_000.0
        shortSide >= 1080 -> 4_500_000.0
        shortSide >= 720 -> 2_600_000.0
        else -> 1_400_000.0
    }

    /** Encodes 2 s from the middle of the video and measures quality, size and speed. */
    suspend fun preview(item: VideoItem, details: VideoDetails, tier: CompressionTier): PreviewResult {
        val plan = plan(item, details, tier)
        val duration = details.durationMs.coerceAtLeast(1)
        val clipMs = min(PREVIEW_CLIP_MS, duration)
        val startMs = max(0, duration / 2 - clipMs / 2)
        val out = File(context.cacheDir, "compress_preview_${tier.name}.mp4").apply { delete() }

        val media = MediaItem.Builder()
            .setUri(item.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(startMs + clipMs)
                    .build(),
            )
            .build()
        val started = System.currentTimeMillis()
        val result = export(media, plan, out.path) {}
        val wallMs = (System.currentTimeMillis() - started).coerceAtLeast(1)

        return withContext(Dispatchers.IO) {
            val midUs = (startMs + clipMs / 2) * 1000
            val original = frameAt(item.uri.toString(), midUs, fromUri = true)
            val compressed = frameAt(out.path, (clipMs / 2) * 1000, fromUri = false)
            val psnr = if (original != null && compressed != null) psnr(original, compressed) else 0.0
            PreviewResult(
                original = original ?: blank(),
                compressed = compressed ?: blank(),
                psnrDb = psnr,
                projectedBytes = (out.length().toDouble() / clipMs * duration).toLong(),
                speedX = clipMs.toDouble() / wallMs,
                encoderName = result.videoEncoderName,
            ).also { out.delete() }
        }
    }

    /**
     * Compresses one video. The new file is written next to the original (same volume) and
     * only replaces it after it exists, is smaller, and has the same duration.
     */
    suspend fun compress(
        item: VideoItem,
        details: VideoDetails,
        tier: CompressionTier,
        keepOriginalInSafeTrash: Boolean,
        onProgress: (Float) -> Unit,
    ): CompressOutcome {
        val original = File(item.path)
        if (!original.isFile) return CompressOutcome.Skipped("File not found")
        val plan = plan(item, details, tier)
        if (!plan.worthIt) return CompressOutcome.Skipped("Already efficient")

        val parent = original.parentFile ?: return CompressOutcome.Failed("No folder")
        val temp = File(parent, ".sp_tmp_${original.nameWithoutExtension}.mp4").apply { delete() }
        try {
            export(MediaItem.fromUri(item.uri), plan, temp.path, onProgress)
        } catch (e: Exception) {
            temp.delete()
            return CompressOutcome.Failed(e.message ?: "Encoding failed")
        }

        return withContext(Dispatchers.IO) {
            val outBytes = temp.length()
            val outDuration = durationMs(temp.path)
            when {
                outBytes <= 0 -> {
                    temp.delete(); CompressOutcome.Failed("Empty output")
                }
                outBytes >= original.length() -> {
                    temp.delete(); CompressOutcome.Skipped("Not smaller than the original")
                }
                outDuration != null && kotlin.math.abs(outDuration - details.durationMs) > DURATION_TOLERANCE_MS -> {
                    temp.delete(); CompressOutcome.Failed("Duration mismatch")
                }
                else -> finish(original, temp, keepOriginalInSafeTrash)
            }
        }
    }

    private suspend fun finish(original: File, temp: File, keepInTrash: Boolean): CompressOutcome {
        val originalBytes = original.length()
        val originalModified = original.lastModified()
        val base = original.nameWithoutExtension
        var inTrash = false
        val target = if (keepInTrash) {
            val moved = safeTrash.moveToSafeTrash(listOf(original)).single()
            if (moved is TrashMoveResult.Moved) {
                inTrash = true
                scanner.applyRemoval(mapOf(original.path to originalBytes))
                uniqueFile(original.parentFile!!, "$base.mp4")
            } else {
                uniqueFile(original.parentFile!!, "${base}_compressed.mp4")
            }
        } else {
            uniqueFile(original.parentFile!!, "${base}_compressed.mp4")
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            return CompressOutcome.Failed("Could not save the compressed file")
        }
        target.setLastModified(originalModified) // keeps the gallery's date order
        MediaScannerConnection.scanFile(context, arrayOf(target.path, original.path), null, null)
        return CompressOutcome.Done(target, originalBytes, target.length(), inTrash)
    }

    // ------------------------------------------------------------------ Transformer

    /** Runs one Transformer export on the main thread (Transformer's required thread). */
    private suspend fun export(
        media: MediaItem,
        plan: TierPlan,
        outputPath: String,
        onProgress: (Float) -> Unit,
    ): ExportResult = withContext(Dispatchers.Main) {
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(plan.videoBitrate).build())
            .setEnableFallback(true)
            .build()
        val edited = EditedMediaItem.Builder(media)
            .setEffects(Effects(emptyList(), listOf(Presentation.createForShortSide(plan.shortSide))))
            .build()

        var transformer: Transformer? = null
        val poller = launch {
            val holder = ProgressHolder()
            while (isActive) {
                val t = transformer
                if (t != null && t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress / 100f)
                }
                delay(250)
            }
        }
        try {
            suspendCancellableCoroutine { cont ->
                val t = Transformer.Builder(context)
                    .setVideoMimeType(plan.mimeType)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoderFactory)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(exportResult)
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            if (cont.isActive) cont.resumeWithException(exportException)
                        }
                    })
                    .build()
                transformer = t
                cont.invokeOnCancellation {
                    t.cancel()
                    File(outputPath).delete()
                }
                t.start(edited, outputPath)
            }
        } finally {
            poller.cancel()
            onProgress(1f)
        }
    }

    private fun pickEncoder(): EncoderInfo {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder }
        fun find(mime: String) = codecs.filter { c -> c.supportedTypes.any { it.equals(mime, ignoreCase = true) } }
            .sortedByDescending { c -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) c.isHardwareAccelerated else !c.name.startsWith("OMX.google") && !c.name.startsWith("c2.android") }
            .firstOrNull()
        val hevc = find(MimeTypes.VIDEO_H265)
        val chosen = hevc ?: find(MimeTypes.VIDEO_H264)
        val mime = if (hevc != null) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264
        val hardware = chosen != null && if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) chosen.isHardwareAccelerated else true
        return EncoderInfo(mime, chosen?.name ?: "default", hardware)
    }

    // ------------------------------------------------------------------ frames & quality

    private fun frameAt(source: String, timeUs: Long, fromUri: Boolean): Bitmap? {
        val mmr = MediaMetadataRetriever()
        return try {
            if (fromUri) mmr.setDataSource(context, android.net.Uri.parse(source)) else mmr.setDataSource(source)
            val frame = mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
            val scale = FRAME_HEIGHT.toFloat() / frame.height
            Bitmap.createScaledBitmap(frame, (frame.width * scale).toInt().coerceAtLeast(1), FRAME_HEIGHT, true)
                .also { if (it !== frame) frame.recycle() }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { mmr.release() }
        }
    }

    private fun durationMs(path: String): Long? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(path)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            runCatching { mmr.release() }
        }
    }

    /** Luma PSNR over the overlapping area of two frames scaled to the same height. */
    private fun psnr(a: Bitmap, b: Bitmap): Double {
        val w = min(a.width, b.width)
        val h = min(a.height, b.height)
        if (w == 0 || h == 0) return 0.0
        val pa = IntArray(w * h)
        val pb = IntArray(w * h)
        a.getPixels(pa, 0, w, 0, 0, w, h)
        b.getPixels(pb, 0, w, 0, 0, w, h)
        var mse = 0.0
        for (i in pa.indices) {
            val d = (luma(pa[i]) - luma(pb[i])).toDouble()
            mse += d * d
        }
        mse /= pa.size
        return if (mse <= 1e-9) 99.0 else 10 * log10(255.0 * 255.0 / mse)
    }

    private fun luma(c: Int) = ((c shr 16 and 0xFF) * 299 + (c shr 8 and 0xFF) * 587 + (c and 0xFF) * 114) / 1000

    private fun blank(): Bitmap = Bitmap.createBitmap(16, 9, Bitmap.Config.RGB_565)

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        var n = 2
        while (f.exists()) {
            f = File(dir, name.substringBeforeLast('.') + " ($n)." + name.substringAfterLast('.'))
            n++
        }
        return f
    }

    private companion object {
        const val PREVIEW_CLIP_MS = 2_000L
        const val FRAME_HEIGHT = 540
        const val DURATION_TOLERANCE_MS = 1_500L
    }
}
