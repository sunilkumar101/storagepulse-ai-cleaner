package com.storagepulse.cleaner.data.social

import android.os.Environment
import com.storagepulse.cleaner.data.storage.FileClassifier
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import com.storagepulse.cleaner.data.trash.TrashMoveResult
import com.storagepulse.cleaner.data.trash.TrashRestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import javax.inject.Singleton

enum class SocialApp(val roots: List<String>) {
    WHATSAPP(
        listOf(
            "Android/media/com.whatsapp/WhatsApp/Media",
            "WhatsApp/Media",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media",
            "WhatsApp Business/Media",
        ),
    ),
    TELEGRAM(
        listOf(
            "Android/media/org.telegram.messenger/Telegram",
            "Android/media/org.telegram.messenger.web/Telegram",
            "Telegram",
        ),
    ),
    TIKTOK(
        listOf(
            "Android/media/com.zhiliaoapp.musically",
            "Android/media/com.ss.android.ugc.trill",
            "DCIM/TikTok", "Movies/TikTok", "Pictures/TikTok",
            "Android/media/com.lemon.lvoverseas",
            "Movies/CapCut", "DCIM/CapCut",
        ),
    ),
    INSTAGRAM(
        listOf(
            "Android/media/com.instagram.android",
            "Pictures/Instagram", "Movies/Instagram", "DCIM/Instagram",
        ),
    ),
}

enum class SocialCategory {
    /** Copies of media you sent (WhatsApp keeps a duplicate in .../Sent/). */
    SENT,
    /** Received / saved videos — personal, reviewed by hand. */
    RECEIVED_VIDEO,
    /** Voice notes and audio. */
    VOICE,
    /** Status previews, stickers, GIFs — disposable cache. */
    STATUS,
    /** Everything else (received photos, documents) — counted, not offered here. */
    OTHER,
}

data class SocialFile(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: SocialCategory,
)

data class SocialScan(val app: SocialApp, val files: List<SocialFile>) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

data class SocialCleanResult(
    val movedPaths: Set<String>,
    val movedBytes: Long,
    val trashIds: List<Long>,
    val failed: Int,
) {
    val movedFiles: Int get() = movedPaths.size
}

/** Finds chat-app media on shared storage and moves chosen files to the 30-Day Safe Trash. */
@Singleton
class SocialChatCleanerRepository @Inject constructor(
    private val safeTrash: SafeTrashRepository,
    private val scanner: StorageScannerRepository,
) {
    suspend fun scanAll(): Map<SocialApp, SocialScan> = coroutineScope {
        SocialApp.entries.map { app -> async(Dispatchers.IO) { app to scan(app) } }.awaitAll().toMap()
    }

    /**
     * One stat per file (java.nio readAttributes, no symlink following) and roots walked in
     * parallel; roots that resolve to the same folder are walked once.
     */
    private suspend fun scan(app: SocialApp): SocialScan = coroutineScope {
        val base = Environment.getExternalStorageDirectory()
        val roots = app.roots.map { File(base, it) }.filter { it.isDirectory }
            .distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.path) }
        val files = roots.map { root -> async(Dispatchers.IO) { walk(app, root.toPath()) } }.awaitAll().flatten()
        SocialScan(app, files.sortedByDescending { it.sizeBytes })
    }

    private fun walk(app: SocialApp, root: Path): List<SocialFile> {
        val out = ArrayList<SocialFile>()
        val stack = ArrayDeque<Path>().apply { add(root) }
        while (stack.isNotEmpty()) {
            val dir = stack.removeLast()
            try {
                Files.newDirectoryStream(dir).use { stream ->
                    for (entry in stream) {
                        val attrs = runCatching {
                            Files.readAttributes(entry, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
                        }.getOrNull() ?: continue
                        val name = entry.fileName.toString()
                        if (attrs.isDirectory) {
                            if (name != ".trash" && name != SafeTrashRepository.TRASH_DIR_NAME) stack.add(entry)
                        } else if (attrs.isRegularFile && name != ".nomedia" && attrs.size() > 0) {
                            val path = entry.toString()
                            out += SocialFile(path, name, attrs.size(), attrs.lastModifiedTime().toMillis(), classify(app, path, name))
                        }
                    }
                }
            } catch (e: Exception) {
                // unreadable folder: skip
            }
        }
        return out
    }

    private fun classify(app: SocialApp, path: String, name: String): SocialCategory {
        val lower = path.lowercase()
        val ext = FileClassifier.extensionOf(name)
        return when (app) {
            SocialApp.WHATSAPP -> when {
                lower.contains("/sent/") -> SocialCategory.SENT
                lower.contains("/.statuses/") || lower.contains("stickers") || lower.contains("animated gifs") -> SocialCategory.STATUS
                lower.contains("voice notes") || lower.contains("whatsapp audio") || lower.contains("business audio") -> SocialCategory.VOICE
                lower.contains("whatsapp video") || lower.contains("business video") -> SocialCategory.RECEIVED_VIDEO
                else -> SocialCategory.OTHER
            }
            SocialApp.TELEGRAM -> when {
                lower.contains("telegram video") || FileClassifier.isVideo(ext) -> SocialCategory.RECEIVED_VIDEO
                lower.contains("telegram audio") || ext in AUDIO_EXT -> SocialCategory.VOICE
                ext == "webp" || ext == "tgs" -> SocialCategory.STATUS
                else -> SocialCategory.OTHER
            }
            SocialApp.TIKTOK, SocialApp.INSTAGRAM -> when {
                FileClassifier.isVideo(ext) -> SocialCategory.RECEIVED_VIDEO
                ext in AUDIO_EXT -> SocialCategory.VOICE
                lower.contains("/cache") || lower.contains("/temp") -> SocialCategory.STATUS
                else -> SocialCategory.OTHER
            }
        }
    }

    suspend fun moveToSafeTrash(files: List<SocialFile>): SocialCleanResult = withContext(Dispatchers.IO) {
        val results = safeTrash.moveToSafeTrash(files.map { File(it.path) })
        val moved = results.filterIsInstance<TrashMoveResult.Moved>()
        scanner.applyRemoval(moved.associate { it.item.originalPath to it.item.sizeBytes })
        SocialCleanResult(moved.map { it.item.originalPath }.toSet(), moved.sumOf { it.item.sizeBytes }, moved.map { it.item.id }, results.size - moved.size)
    }

    suspend fun restore(trashIds: List<Long>): Int {
        var n = 0
        trashIds.forEach { if (safeTrash.restoreFromTrash(it) is TrashRestoreResult.Restored) n++ }
        if (n > 0) scanner.startScan(force = true)
        return n
    }

    private companion object {
        val AUDIO_EXT = setOf("opus", "ogg", "m4a", "mp3", "aac", "amr", "wav", "oga")
    }
}
