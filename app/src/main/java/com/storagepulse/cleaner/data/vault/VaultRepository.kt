package com.storagepulse.cleaner.data.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.webkit.MimeTypeMap
import com.storagepulse.cleaner.data.storage.FileClassifier
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.storage.scanAndWait
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class VaultLockResult(val locked: Int, val failed: Int, val lockedPaths: Set<String>)

/** Unlock window shared by the gallery hero card and the vault screen. */
@Singleton
class VaultSession @Inject constructor() {
    private var unlockedUntil = 0L
    fun unlock() { unlockedUntil = System.currentTimeMillis() + WINDOW_MS }
    fun lock() { unlockedUntil = 0 }
    val isUnlocked: Boolean get() = System.currentTimeMillis() < unlockedUntil
    private companion object { const val WINDOW_MS = 3 * 60_000L }
}

/**
 * Private vault in app-private storage (filesDir/private_vault). Each file is AES-256-GCM
 * encrypted with a Keystore key, verified by decrypting it again (SHA-256), and only then is
 * the public original removed. NOTE: app-private files are deleted if the app is uninstalled —
 * the UI tells users to restore first.
 */
@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: VaultDao,
    private val scanner: StorageScannerRepository,
) {
    private val dir: File get() = File(context.filesDir, "private_vault").apply { mkdirs() }

    fun observe(): Flow<List<VaultItemEntity>> = dao.observeAll()
    fun photoCount(): Flow<Int> = dao.photoCount()
    fun videoCount(): Flow<Int> = dao.videoCount()

    val isHardwareBacked: Boolean by lazy { VaultCrypto.isHardwareBacked() }

    suspend fun lock(paths: List<String>): VaultLockResult = withContext(Dispatchers.IO) {
        var failed = 0
        val locked = HashSet<String>()
        for (path in paths) {
            if (lockOne(File(path))) locked += path else failed++
        }
        if (locked.isNotEmpty()) {
            scanner.applyRemoval(locked.associateWith { 0L })
            scanAndWait(context, locked)
        }
        VaultLockResult(locked.size, failed, locked)
    }

    private suspend fun lockOne(src: File): Boolean {
        if (!src.isFile) return false
        val id = UUID.randomUUID().toString()
        val enc = File(dir, "$id.spv")
        val thumb = File(dir, "$id.thumb")
        var rowId = 0L
        return try {
            // Encrypt while hashing the plaintext.
            val md = MessageDigest.getInstance("SHA-256")
            DigestInputStream(src.inputStream().buffered(), md).use { input ->
                enc.outputStream().buffered().use { out -> VaultCrypto.encrypt(input, out) }
            }
            val sha = md.digest().toHex()
            // Verify by decrypting everything again before touching the original.
            val verify = MessageDigest.getInstance("SHA-256")
            enc.inputStream().buffered().use { input -> VaultCrypto.decrypt(input, DigestSink(verify)) }
            if (verify.digest().toHex() != sha) throw IllegalStateException("Verification failed")

            val ext = FileClassifier.extensionOf(src.name)
            val isVideo = FileClassifier.isVideo(ext)
            makeThumbnail(src, isVideo)?.let { bytes ->
                thumb.outputStream().buffered().use { VaultCrypto.encrypt(ByteArrayInputStream(bytes), it) }
            }
            rowId = dao.insert(
                VaultItemEntity(
                    originalPath = src.path,
                    fileName = src.name,
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext),
                    sizeBytes = src.length(),
                    isVideo = isVideo,
                    encryptedFile = enc.path,
                    encryptedThumb = thumb.takeIf { it.exists() }?.path,
                    sha256 = sha,
                    lockedAt = System.currentTimeMillis(),
                    originalModified = src.lastModified(),
                ),
            )
            if (!src.delete()) throw IllegalStateException("Could not remove the original")
            true
        } catch (e: Exception) {
            // The original is still in place: drop the half-made vault entry.
            if (rowId != 0L) dao.delete(rowId)
            enc.delete()
            thumb.delete()
            false
        }
    }

    /** Decrypts back to the original location (or a free name next to it). */
    suspend fun restore(id: Long): File? = withContext(Dispatchers.IO) {
        val item = dao.get(id) ?: return@withContext null
        val original = File(item.originalPath)
        original.parentFile?.mkdirs()
        val target = freeName(original)
        try {
            File(item.encryptedFile).inputStream().buffered().use { input ->
                target.outputStream().buffered().use { out -> VaultCrypto.decrypt(input, out) }
            }
            target.setLastModified(item.originalModified)
            File(item.encryptedFile).delete()
            item.encryptedThumb?.let { File(it).delete() }
            dao.delete(id)
            scanAndWait(context, listOf(target.path))
            scanner.startScan(force = true)
            target
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    /** Decrypted thumbnail bytes for the vault grid (kept in memory only). */
    suspend fun thumbnail(item: VaultItemEntity): ByteArray? = withContext(Dispatchers.IO) {
        val path = item.encryptedThumb ?: return@withContext null
        runCatching {
            val out = ByteArrayOutputStream()
            File(path).inputStream().buffered().use { VaultCrypto.decrypt(it, out) }
            out.toByteArray()
        }.getOrNull()
    }

    private fun makeThumbnail(src: File, isVideo: Boolean): ByteArray? = runCatching {
        val bmp: Bitmap = if (isVideo) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(src.path)
                // getScaledFrameAtTime is API 27+; minSdk is 26.
                mmr.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { f ->
                    val scale = THUMB.toFloat() / maxOf(f.width, f.height)
                    if (scale >= 1f) f else Bitmap.createScaledBitmap(f, (f.width * scale).toInt().coerceAtLeast(1), (f.height * scale).toInt().coerceAtLeast(1), true)
                }
            } finally {
                mmr.release()
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(src.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= THUMB && bounds.outHeight / (sample * 2) >= THUMB) sample *= 2
            BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it); bmp.recycle() }.toByteArray()
    }.getOrNull()

    private fun freeName(original: File): File {
        if (!original.exists()) return original
        var n = 2
        while (true) {
            val f = File(original.parentFile, "${original.nameWithoutExtension} ($n).${original.extension}")
            if (!f.exists()) return f
            n++
        }
    }

    private class DigestSink(private val md: MessageDigest) : OutputStream() {
        override fun write(b: Int) = md.update(b.toByte())
        override fun write(b: ByteArray, off: Int, len: Int) = md.update(b, off, len)
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private companion object { const val THUMB = 320 }
}
