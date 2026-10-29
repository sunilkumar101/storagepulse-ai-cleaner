package com.storagepulse.cleaner.data.clean

import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.storage.model.ClutterFile
import com.storagepulse.cleaner.data.storage.model.ClutterKind
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import com.storagepulse.cleaner.data.trash.TrashMoveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CleanResult(
    val movedFiles: Int,
    val movedBytes: Long,
    /** Duplicates whose kept copy was missing or no longer identical: left untouched. */
    val skippedFiles: Int,
    val failedFiles: Int,
)

/**
 * Moves clutter to the 30-Day Safe Trash. Duplicates are compared byte-by-byte with the copy
 * that is kept right before moving, so a scan estimate can never cost the user a unique file.
 */
@Singleton
class ClutterCleaner @Inject constructor(
    private val safeTrash: SafeTrashRepository,
    private val scanner: StorageScannerRepository,
) {
    suspend fun moveToSafeTrash(
        items: List<ClutterFile>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): CleanResult = withContext(Dispatchers.IO) {
        val safe = ArrayList<File>()
        val notIdentical = HashSet<String>()
        var skipped = 0
        items.forEachIndexed { index, item ->
            val file = File(item.path)
            val ok = when (item.kind) {
                ClutterKind.DUPLICATE -> item.keeperPath?.let { identical(file, File(it)) } == true
                else -> file.isFile
            }
            if (ok) {
                safe += file
            } else {
                skipped++
                if (item.kind == ClutterKind.DUPLICATE) notIdentical += item.path
            }
            onProgress(index + 1, items.size)
        }

        val results = safeTrash.moveToSafeTrash(safe)
        val moved = results.filterIsInstance<TrashMoveResult.Moved>()
        scanner.applyRemoval(moved.associate { it.item.originalPath to it.item.sizeBytes })
        scanner.forgetFalseDuplicates(notIdentical)

        CleanResult(
            movedFiles = moved.size,
            movedBytes = moved.sumOf { it.item.sizeBytes },
            skippedFiles = skipped,
            failedFiles = results.size - moved.size,
        )
    }

    /** True only if both exist, are different files, and every byte matches. */
    private fun identical(a: File, b: File): Boolean {
        if (!a.isFile || !b.isFile) return false
        if (a.canonicalPath == b.canonicalPath) return false
        if (a.length() != b.length()) return false
        return try {
            a.inputStream().buffered(BUF).use { ia ->
                b.inputStream().buffered(BUF).use { ib ->
                    val ba = ByteArray(BUF)
                    val bb = ByteArray(BUF)
                    while (true) {
                        val na = ia.readNBytesCompat(ba)
                        val nb = ib.readNBytesCompat(bb)
                        if (na != nb) return false
                        if (na <= 0) return true
                        for (i in 0 until na) if (ba[i] != bb[i]) return false
                    }
                    @Suppress("UNREACHABLE_CODE")
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Fills [buf] as far as the stream allows (read() may return short counts). */
    private fun java.io.InputStream.readNBytesCompat(buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val n = read(buf, total, buf.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    private companion object {
        const val BUF = 256 * 1024
    }
}
