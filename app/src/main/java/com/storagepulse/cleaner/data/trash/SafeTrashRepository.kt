package com.storagepulse.cleaner.data.trash

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.webkit.MimeTypeMap
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.data.storage.FileClassifier
import com.storagepulse.cleaner.data.storage.StorageAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TrashMoveResult {
    data class Moved(val item: TrashItemEntity) : TrashMoveResult
    data class Failed(val path: String, val reason: String) : TrashMoveResult
}

sealed interface TrashRestoreResult {
    data class Restored(val path: String) : TrashRestoreResult
    data class Failed(val reason: String) : TrashRestoreResult
}

/**
 * 30-Day Safe Trash. Nothing the app "deletes" is deleted: files are moved into a hidden
 * quarantine folder on the same storage (instant rename, no copy) and stay restorable until
 * they expire. Only [purgeFromTrashPermanently] (explicit user confirmation) and
 * [purgeExpired] (after the retention period) remove bytes for good.
 *
 * The quarantine folder lives in shared storage, not in app-private storage, so uninstalling
 * the app never destroys files that are still in the trash.
 */
@Singleton
class SafeTrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TrashDao,
    private val settings: SettingsRepository,
) {
    fun observeTrash(): Flow<List<TrashItemEntity>> = dao.observeAll()

    fun observeTotals(): Flow<TrashTotals> = dao.observeTotals()

    /**
     * Quarantine folder on the SAME volume as [source] (internal storage or SD card), so moving
     * is an instant rename instead of copying gigabytes across volumes. Falls back to internal
     * storage if the volume root is not writable.
     */
    private fun trashDirFor(source: File): File {
        val onVolume = File(volumeRootOf(source), TRASH_DIR_NAME)
        val dir = if (onVolume.isDirectory || onVolume.mkdirs()) onVolume else File(Environment.getExternalStorageDirectory(), TRASH_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) runCatching { noMedia.createNewFile() }
        return dir
    }

    private fun volumeRootOf(file: File): File {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.getSystemService(android.os.storage.StorageManager::class.java)
                ?.getStorageVolume(file)?.directory?.let { return it }
        }
        val p = file.absolutePath
        return when {
            p.startsWith("/storage/emulated/") -> Environment.getExternalStorageDirectory()
            p.startsWith("/storage/") -> File(p.split('/').take(3).joinToString("/")) // /storage/XXXX-XXXX
            else -> Environment.getExternalStorageDirectory()
        }
    }

    /** Moves each file into quarantine. Needs All Files Access (Full mode). */
    suspend fun moveToSafeTrash(files: List<File>): List<TrashMoveResult> = withContext(Dispatchers.IO) {
        if (!StorageAccess.hasAllFilesAccess(context)) {
            return@withContext files.map { TrashMoveResult.Failed(it.path, "All Files Access required") }
        }
        val retentionDays = settings.trashRetentionDays.first()
        val now = System.currentTimeMillis()
        files.map { source -> moveOne(source, trashDirFor(source), now, retentionDays) }
            .also { results ->
                val touched = results.filterIsInstance<TrashMoveResult.Moved>()
                    .map { it.item.originalPath }
                rescan(touched)
            }
    }

    private suspend fun moveOne(source: File, dir: File, now: Long, retentionDays: Int): TrashMoveResult {
        if (!source.isFile) return TrashMoveResult.Failed(source.path, "Not a file")
        if (source.canonicalPath.startsWith(dir.canonicalPath)) {
            return TrashMoveResult.Failed(source.path, "Already in Safe Trash")
        }
        val size = source.length()
        val target = File(dir, "${UUID.randomUUID()}_${source.name}")
        if (!moveFile(source, target)) return TrashMoveResult.Failed(source.path, "Could not move file")

        val entity = TrashItemEntity(
            originalPath = source.path,
            quarantinePath = target.path,
            fileName = source.name,
            sizeBytes = size,
            mimeType = mimeOf(source.name),
            deletedAt = now,
            expiresAt = now + TimeUnit.DAYS.toMillis(retentionDays.toLong()),
        )
        return try {
            TrashMoveResult.Moved(entity.copy(id = dao.insert(entity)))
        } catch (e: Exception) {
            // Could not record it: put the file back so nothing is lost untracked.
            moveFile(target, source)
            TrashMoveResult.Failed(source.path, "Database error: ${e.message}")
        }
    }

    /** Puts a file back where it was. If that name is taken, restores next to it with a suffix. */
    suspend fun restoreFromTrash(id: Long): TrashRestoreResult = withContext(Dispatchers.IO) {
        val item = dao.get(id) ?: return@withContext TrashRestoreResult.Failed("Not in trash")
        val quarantined = File(item.quarantinePath)
        if (!quarantined.exists()) {
            dao.delete(id)
            return@withContext TrashRestoreResult.Failed("Quarantined file is missing")
        }
        val destination = freeDestination(File(item.originalPath))
        destination.parentFile?.mkdirs()
        if (!moveFile(quarantined, destination)) {
            return@withContext TrashRestoreResult.Failed("Could not move file back")
        }
        dao.delete(id)
        rescan(listOf(destination.path))
        TrashRestoreResult.Restored(destination.path)
    }

    /**
     * Deletes the quarantined copy for good. Call ONLY after the user confirmed in a
     * destructive (#EF4444) dialog that states count and size.
     */
    suspend fun purgeFromTrashPermanently(id: Long): Boolean = withContext(Dispatchers.IO) {
        val item = dao.get(id) ?: return@withContext false
        val file = File(item.quarantinePath)
        val gone = !file.exists() || file.delete()
        if (gone) {
            dao.delete(id)
            rescan(listOf(file.path)) // drop the stale MediaStore row too
        }
        gone
    }

    /** "Restore All": returns how many files went back. */
    suspend fun restoreAll(): Int {
        var n = 0
        for (item in dao.all()) if (restoreFromTrash(item.id) is TrashRestoreResult.Restored) n++
        return n
    }

    /**
     * "Empty Trash": permanently deletes everything in Safe Trash. Call ONLY after the user
     * confirmed a destructive (#EF4444) dialog that states count and size.
     */
    suspend fun emptyTrashPermanently(): Int {
        var n = 0
        for (item in dao.all()) if (purgeFromTrashPermanently(item.id)) n++
        return n
    }

    /** Called daily by [TrashPurgeWorker]. Returns how many items were purged. */
    suspend fun purgeExpired(now: Long = System.currentTimeMillis()): Int {
        var purged = 0
        for (item in dao.expired(now)) {
            if (purgeFromTrashPermanently(item.id)) purged++
        }
        return purged
    }

    // ------------------------------------------------------------------ helpers

    /** Rename (instant on the same volume); falls back to copy → verify size → delete. */
    private fun moveFile(from: File, to: File): Boolean {
        if (from.renameTo(to)) return true
        return try {
            from.inputStream().use { input -> to.outputStream().use { input.copyTo(it) } }
            if (to.length() != from.length()) {
                to.delete()
                false
            } else if (!from.delete()) {
                to.delete() // original still exists: undo the copy
                false
            } else {
                to.setLastModified(from.lastModified())
                true
            }
        } catch (e: Exception) {
            to.delete()
            false
        }
    }

    private fun freeDestination(original: File): File {
        if (!original.exists()) return original
        val base = original.nameWithoutExtension
        val ext = original.extension.let { if (it.isEmpty()) "" else ".$it" }
        var n = 1
        while (true) {
            val candidate = File(original.parentFile, "$base (restored${if (n > 1) " $n" else ""})$ext")
            if (!candidate.exists()) return candidate
            n++
        }
    }

    private fun mimeOf(name: String): String? =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileClassifier.extensionOf(name))

    /** Tell MediaStore so galleries stop/start showing the file. */
    private fun rescan(paths: List<String>) {
        if (paths.isNotEmpty()) MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
    }

    companion object {
        const val TRASH_DIR_NAME = ".StoragePulseTrash"
    }
}
