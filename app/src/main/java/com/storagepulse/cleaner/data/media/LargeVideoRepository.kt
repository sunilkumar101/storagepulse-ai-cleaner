package com.storagepulse.cleaner.data.media

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class VideoItem(
    val uri: Uri,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
) {
    val folder: String get() = path.substringBeforeLast('/').substringAfter("/storage/emulated/0") + "/"
}

/** Details read from the file itself (cheap: one file, on demand). */
data class VideoDetails(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val fps: Int?,
    val bitrateBps: Long?,
    /** e.g. "video/hevc", "video/avc". */
    val codecMime: String?,
)

@Singleton
class LargeVideoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Videos at or above [minBytes], largest first (works in Full and Scoped Mode). */
    suspend fun largeVideos(minBytes: Long = MIN_BYTES): List<VideoItem> = query(
        "${MediaStore.Video.Media.SIZE} >= ?", arrayOf(minBytes.toString()),
    )

    /** Videos inside [folder] (not sub-folders' hidden dirs), largest first. */
    suspend fun videosIn(folder: String, minBytes: Long = 5_000_000L): List<VideoItem> = query(
        "${MediaStore.Video.Media.DATA} LIKE ? AND ${MediaStore.Video.Media.SIZE} >= ?",
        arrayOf("${folder.trimEnd('/')}/%", minBytes.toString()),
    )

    /** One video by path (MediaStore row, or a plain file if it is not indexed). */
    suspend fun videoAt(path: String): VideoItem? = query(
        "${MediaStore.Video.Media.DATA} = ?", arrayOf(path),
    ).firstOrNull() ?: withContext(Dispatchers.IO) {
        File(path).takeIf { it.isFile }?.let { f ->
            VideoItem(Uri.fromFile(f), f.path, f.name, f.length(), f.lastModified(), 0, 0, 0)
        }
    }

    private suspend fun query(selection: String, args: Array<String>): List<VideoItem> = withContext(Dispatchers.IO) {
        val base = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )
        val out = ArrayList<VideoItem>()
        context.contentResolver.query(
            base, projection,
            selection, args,
            "${MediaStore.Video.Media.SIZE} DESC",
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val data = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val name = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val size = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val taken = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val mod = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val dur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val w = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val h = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            while (c.moveToNext()) {
                val path = c.getString(data) ?: continue
                if (path.contains("/.") || !File(path).exists()) continue
                out += VideoItem(
                    uri = ContentUris.withAppendedId(base, c.getLong(id)),
                    path = path,
                    name = c.getString(name) ?: File(path).name,
                    sizeBytes = c.getLong(size),
                    dateTaken = c.getLong(taken).takeIf { it > 0 } ?: (c.getLong(mod) * 1000),
                    durationMs = c.getLong(dur),
                    width = c.getInt(w),
                    height = c.getInt(h),
                )
            }
        }
        out
    }

    suspend fun details(item: VideoItem): VideoDetails = withContext(Dispatchers.IO) {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, item.uri)
            fun meta(key: Int) = mmr.extractMetadata(key)
            val rotation = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            var w = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: item.width
            var h = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: item.height
            if (rotation == 90 || rotation == 270) w = h.also { h = w }
            val duration = meta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: item.durationMs
            val frames = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLongOrNull()
            } else {
                null
            }
            val captureFps = meta(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            val fps = when {
                captureFps != null && captureFps > 0 -> captureFps.roundToInt()
                frames != null && duration > 0 -> (frames * 1000.0 / duration).roundToInt()
                else -> null
            }
            VideoDetails(
                width = w,
                height = h,
                durationMs = duration,
                fps = fps,
                bitrateBps = meta(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull(),
                codecMime = videoCodec(item.uri),
            )
        } catch (e: Exception) {
            VideoDetails(item.width, item.height, item.durationMs, null, null, null)
        } finally {
            runCatching { mmr.release() }
        }
    }

    private fun videoCodec(uri: Uri): String? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) }
                .firstOrNull { it?.startsWith("video/") == true }
        } catch (e: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    companion object {
        const val MIN_BYTES = 100_000_000L

        /**
         * Rough H.265 target size for a "visually lossless" re-encode at this resolution.
         * Only an estimate for the button label; the compressor measures the real result.
         */
        fun estimateCompressedBytes(details: VideoDetails): Long {
            val shortSide = minOf(details.width, details.height)
            val videoBps = when {
                shortSide >= 2160 -> 16_000_000L
                shortSide >= 1440 -> 9_000_000L
                shortSide >= 1080 -> 5_000_000L
                shortSide >= 720 -> 2_800_000L
                else -> 1_400_000L
            }
            val fpsFactor = if ((details.fps ?: 30) > 45) 1.4 else 1.0
            val totalBps = (videoBps * fpsFactor).toLong() + 128_000L
            return totalBps / 8 * details.durationMs / 1000
        }
    }
}
