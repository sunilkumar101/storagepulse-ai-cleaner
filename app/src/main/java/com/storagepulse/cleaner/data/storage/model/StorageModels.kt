package com.storagepulse.cleaner.data.storage.model

/** The 5 dashboard partitions. Colors are the DESIGN.md category tokens. */
enum class StorageCategory(val colorArgb: Long) {
    VIDEOS(0xFFF43F5E),
    PHOTOS(0xFF8B5CF6),
    SOCIAL(0xFF10B981),
    APPS(0xFF06B6D4),
    SYSTEM(0xFFF59E0B), // system + cache + everything not in another bucket (audio, documents…)
}

/** Whole-volume numbers straight from StorageStatsManager (instant, no scan needed). */
data class VolumeStats(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0)
    val usedFraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

/**
 * One folder (or large file) in the in-memory disk tree.
 * Folders are always kept; files only when >= [StorageNode.FILE_NODE_MIN_BYTES] to keep memory small.
 * [sizeBytes] and [fileCount] are always exact totals of everything below the folder.
 */
data class StorageNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val fileCount: Int,
    val category: StorageCategory?,
    val lastModified: Long,
    val children: List<StorageNode> = emptyList(),
) {
    companion object {
        const val FILE_NODE_MIN_BYTES = 1_000_000L
    }
}

data class FileRef(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
)

data class BurstGroup(
    val folder: String,
    val files: List<FileRef>,
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

enum class ClutterKind { DUPLICATE, WHATSAPP_SENT, APK_INSTALLER, THUMBNAIL }

/** A file that is safe to move to Safe Trash. Duplicates carry the copy that is kept. */
data class ClutterFile(
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val kind: ClutterKind,
    val keeperPath: String? = null,
)

/** Same bytes in several places. [paths] first = the copy we keep. */
data class DuplicateGroup(
    val sizeBytes: Long,
    val paths: List<String>,
) {
    val keeper: String get() = paths.first()
    val extraCopies: List<String> get() = paths.drop(1)
    val wastedBytes: Long get() = sizeBytes * (paths.size - 1)
}

/** What the "Safe to Clean" card adds up. Every item ends in Safe Trash, never hard-deleted. */
data class CleanupBreakdown(
    val duplicateBytes: Long = 0,
    val duplicateFiles: Int = 0,
    val whatsAppSentBytes: Long = 0,
    val whatsAppSentFiles: Int = 0,
    val apkInstallerBytes: Long = 0,
    val apkInstallerFiles: Int = 0,
    val thumbnailBytes: Long = 0,
    val thumbnailFiles: Int = 0,
) {
    val totalBytes: Long get() = duplicateBytes + whatsAppSentBytes + apkInstallerBytes + thumbnailBytes
    val totalFiles: Int get() = duplicateFiles + whatsAppSentFiles + apkInstallerFiles + thumbnailFiles

    companion object {
        fun from(clutter: List<ClutterFile>): CleanupBreakdown {
            fun of(kind: ClutterKind) = clutter.filter { it.kind == kind }
            val dup = of(ClutterKind.DUPLICATE)
            val sent = of(ClutterKind.WHATSAPP_SENT)
            val apk = of(ClutterKind.APK_INSTALLER)
            val thumb = of(ClutterKind.THUMBNAIL)
            return CleanupBreakdown(
                duplicateBytes = dup.sumOf { it.sizeBytes }, duplicateFiles = dup.size,
                whatsAppSentBytes = sent.sumOf { it.sizeBytes }, whatsAppSentFiles = sent.size,
                apkInstallerBytes = apk.sumOf { it.sizeBytes }, apkInstallerFiles = apk.size,
                thumbnailBytes = thumb.sumOf { it.sizeBytes }, thumbnailFiles = thumb.size,
            )
        }
    }
}

data class Heavyweights(
    val largestVideo: FileRef? = null,
    val largestBurst: BurstGroup? = null,
)

enum class ScanMode { FULL_DISK, MEDIA_ONLY }

data class StorageSnapshot(
    val volume: VolumeStats,
    val categoryBytes: Map<StorageCategory, Long>,
    val root: StorageNode?,
    val cleanup: CleanupBreakdown,
    /** Every clutter file found (feeds Safe to Clean, Treemap purge, AI Clean). */
    val clutter: List<ClutterFile>,
    /** Duplicate groups, most wasted bytes first. */
    val duplicateGroups: List<DuplicateGroup>,
    val heavyweights: Heavyweights,
    /** Videos over 100 MB (Swipe Deck candidates). */
    val largeVideoCount: Int,
    val videoCount: Int,
    val scannedFiles: Int,
    val mode: ScanMode,
    val durationMs: Long,
    val scannedAt: Long,
)

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val filesSeen: Int, val bytesSeen: Long) : ScanState
    data class Done(val snapshot: StorageSnapshot) : ScanState
    data class Failed(val message: String) : ScanState
}
