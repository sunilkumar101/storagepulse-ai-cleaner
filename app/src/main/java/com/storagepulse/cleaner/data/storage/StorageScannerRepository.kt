package com.storagepulse.cleaner.data.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.util.Log
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.storagepulse.cleaner.data.storage.model.BurstGroup
import com.storagepulse.cleaner.data.storage.model.CleanupBreakdown
import com.storagepulse.cleaner.data.storage.model.ClutterFile
import com.storagepulse.cleaner.data.storage.model.ClutterKind
import com.storagepulse.cleaner.data.storage.model.DuplicateGroup
import com.storagepulse.cleaner.data.storage.model.FileRef
import com.storagepulse.cleaner.data.storage.model.Heavyweights
import com.storagepulse.cleaner.data.storage.model.ScanMode
import com.storagepulse.cleaner.data.storage.model.ScanState
import com.storagepulse.cleaner.data.storage.model.StorageCategory
import com.storagepulse.cleaner.data.storage.model.StorageNode
import com.storagepulse.cleaner.data.storage.model.StorageSnapshot
import com.storagepulse.cleaner.data.storage.model.VolumeStats
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fast storage scanner.
 * - [volumeStats] is instant (StorageStatsManager, StatFs fallback).
 * - [startScan] walks the whole shared storage with up to 16 parallel IO workers when
 *   All Files Access is granted, or reads MediaStore in Scoped Mode.
 * Results are cached in [state] for the whole process.
 */
@Singleton
class StorageScannerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val io = Dispatchers.IO.limitedParallelism(16)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private var scanJob: Job? = null

    /** Total / free bytes for internal shared storage. No permission needed. */
    fun volumeStats(): VolumeStats {
        val uuid: UUID = StorageManager.UUID_DEFAULT
        return try {
            val stats = context.getSystemService(StorageStatsManager::class.java)
            VolumeStats(totalBytes = stats.getTotalBytes(uuid), freeBytes = stats.getFreeBytes(uuid))
        } catch (e: Exception) {
            val statFs = StatFs(Environment.getDataDirectory().path)
            VolumeStats(totalBytes = statFs.totalBytes, freeBytes = statFs.availableBytes)
        }
    }

    /** Starts a scan unless one is running (or already done, when [force] is false). */
    fun startScan(force: Boolean = false) {
        if (scanJob?.isActive == true) return
        if (!force && _state.value is ScanState.Done) return
        scanJob = scope.launch {
            _state.value = ScanState.Scanning(0, 0)
            _state.value = try {
                val snapshot = scan()
                Log.i(TAG, "Scan ${snapshot.mode}: ${snapshot.scannedFiles} files in ${snapshot.durationMs} ms")
                ScanState.Done(snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ScanState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /**
     * Updates the cached snapshot after files left their folders (e.g. moved to Safe Trash),
     * so every screen reflects it instantly without a full rescan.
     * [removed] = path → size of each file that is gone.
     */
    fun applyRemoval(removed: Map<String, Long>) {
        if (removed.isEmpty()) return
        val current = (_state.value as? ScanState.Done)?.snapshot ?: return

        val categoryBytes = current.categoryBytes.toMutableMap()
        for ((path, size) in removed) {
            val lower = path.lowercase()
            val category = FileClassifier.classify(lower, FileClassifier.extensionOf(path))
            if (category != StorageCategory.SYSTEM) {
                categoryBytes[category] = ((categoryBytes[category] ?: 0L) - size).coerceAtLeast(0)
            }
        }
        // Bytes now sit in Safe Trash (still "used" until purged): they fall into System & Other.
        val volume = volumeStats()
        val known = categoryBytes.filterKeys { it != StorageCategory.SYSTEM }.values.sum()
        categoryBytes[StorageCategory.SYSTEM] = (volume.usedBytes - known).coerceAtLeast(0)

        val clutter = current.clutter.filterNot { it.path in removed }
        val groups = current.duplicateGroups
            .map { g -> g.copy(paths = g.paths.filterNot { it in removed }) }
            .filter { it.paths.size >= 2 }
        val heavy = current.heavyweights.copy(
            largestVideo = current.heavyweights.largestVideo?.takeUnless { it.path in removed },
            largestBurst = current.heavyweights.largestBurst
                ?.let { b -> b.copy(files = b.files.filterNot { it.path in removed }) }
                ?.takeIf { it.files.size >= 3 },
        )

        _state.value = ScanState.Done(
            current.copy(
                volume = volume,
                categoryBytes = categoryBytes,
                root = current.root?.let { StorageTree.withoutFiles(it, removed) },
                clutter = clutter,
                cleanup = CleanupBreakdown.from(clutter),
                duplicateGroups = groups,
                heavyweights = heavy,
            ),
        )
    }

    /**
     * Drops files that looked like duplicates in the fast scan but failed the byte-by-byte
     * check, so they are never offered for cleaning again (until the next full scan).
     */
    fun forgetFalseDuplicates(paths: Set<String>) {
        if (paths.isEmpty()) return
        val current = (_state.value as? ScanState.Done)?.snapshot ?: return
        val clutter = current.clutter.filterNot { it.kind == ClutterKind.DUPLICATE && it.path in paths }
        val groups = current.duplicateGroups
            .map { g -> g.copy(paths = g.paths.filterNot { it in paths }) }
            .filter { it.paths.size >= 2 }
        _state.value = ScanState.Done(
            current.copy(clutter = clutter, cleanup = CleanupBreakdown.from(clutter), duplicateGroups = groups),
        )
    }

    private suspend fun scan(): StorageSnapshot = coroutineScope {
        val started = System.currentTimeMillis()
        val volume = volumeStats()
        val collector = Collector()

        val ticker = launch {
            while (isActive) {
                _state.value = ScanState.Scanning(collector.files.get(), collector.bytes.get())
                delay(PROGRESS_TICK_MS)
            }
        }

        val mode: ScanMode
        var root: StorageNode? = null
        when {
            StorageAccess.hasAllFilesAccess(context) -> {
                mode = ScanMode.FULL_DISK
                val top = Environment.getExternalStorageDirectory()
                root = withContext(io) { walk(top.toPath(), collector) }
            }
            StorageAccess.hasMediaAccess(context) -> {
                mode = ScanMode.MEDIA_ONLY
                withContext(Dispatchers.IO) { readMediaStore(collector) }
            }
            else -> throw IllegalStateException("No storage permission")
        }

        val duplicateGroups = withContext(io) { findDuplicates(collector) }
        val burst = findLargestBurst(collector)
        ticker.cancel()

        val clutter = buildList {
            collector.sentList.forEach { add(ClutterFile(it.path, it.sizeBytes, it.lastModified, ClutterKind.WHATSAPP_SENT)) }
            collector.apkList.forEach { add(ClutterFile(it.path, it.sizeBytes, it.lastModified, ClutterKind.APK_INSTALLER)) }
            collector.thumbList.forEach { add(ClutterFile(it.path, it.sizeBytes, it.lastModified, ClutterKind.THUMBNAIL)) }
            duplicateGroups.forEach { group ->
                group.extraCopies.forEach { copy ->
                    add(ClutterFile(copy, group.sizeBytes, 0L, ClutterKind.DUPLICATE, keeperPath = group.keeper))
                }
            }
        }

        val walked = StorageCategory.entries.associateWith { collector.categoryBytes[it.ordinal].get() }
        val apps = walked.getValue(StorageCategory.APPS) + installedAppCodeBytes()
        val known = walked.getValue(StorageCategory.VIDEOS) +
            walked.getValue(StorageCategory.PHOTOS) +
            walked.getValue(StorageCategory.SOCIAL) + apps
        // Whatever the scan cannot see (OS, app data, other files) is the remainder of "used".
        val system = (volume.usedBytes - known).coerceAtLeast(walked.getValue(StorageCategory.SYSTEM))

        StorageSnapshot(
            volume = volume,
            categoryBytes = mapOf(
                StorageCategory.VIDEOS to walked.getValue(StorageCategory.VIDEOS),
                StorageCategory.PHOTOS to walked.getValue(StorageCategory.PHOTOS),
                StorageCategory.SOCIAL to walked.getValue(StorageCategory.SOCIAL),
                StorageCategory.APPS to apps,
                StorageCategory.SYSTEM to system,
            ),
            root = root,
            cleanup = CleanupBreakdown.from(clutter),
            clutter = clutter,
            duplicateGroups = duplicateGroups,
            heavyweights = Heavyweights(
                largestVideo = collector.largestVideo.get(),
                largestBurst = burst,
            ),
            largeVideoCount = collector.largeVideos.get(),
            videoCount = collector.videos.get(),
            scannedFiles = collector.files.get(),
            mode = mode,
            durationMs = System.currentTimeMillis() - started,
            scannedAt = System.currentTimeMillis(),
        )
    }

    // ---------------------------------------------------------------- full-disk walk

    /** Walks one folder; sub-folders are walked in parallel on [io]. Never follows symlinks. */
    private suspend fun walk(dir: Path, collector: Collector): StorageNode = coroutineScope {
        val subDirs = ArrayList<Path>()
        val fileNodes = ArrayList<StorageNode>()
        var ownBytes = 0L
        var ownFiles = 0
        var newest = 0L

        try {
            Files.newDirectoryStream(dir).use { stream ->
                for (entry in stream) {
                    val attrs = runCatching {
                        Files.readAttributes(entry, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
                    }.getOrNull()
                    if (attrs == null || attrs.isSymbolicLink) {
                        // unreadable or symlink: skip
                    } else if (attrs.isDirectory) {
                        if (!isExcluded(entry)) subDirs.add(entry)
                    } else if (attrs.isRegularFile) {
                        val size = attrs.size()
                        val modified = attrs.lastModifiedTime().toMillis()
                        val path = entry.toString()
                        val name = entry.fileName.toString()
                        val category = collector.onFile(path, name, size, modified)
                        ownBytes += size
                        ownFiles++
                        if (modified > newest) newest = modified
                        if (size >= StorageNode.FILE_NODE_MIN_BYTES) {
                            fileNodes.add(StorageNode(name, path, false, size, 1, category, modified))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Access denied (e.g. Android/data on 11+) or folder vanished: count what we have.
        }

        val childDirs = subDirs.map { async(io) { walk(it, collector) } }.awaitAll()
        val children = (childDirs + fileNodes).sortedByDescending { it.sizeBytes }
        StorageNode(
            name = dir.fileName?.toString() ?: dir.toString(),
            path = dir.toString(),
            isDirectory = true,
            sizeBytes = ownBytes + childDirs.sumOf { it.sizeBytes },
            fileCount = ownFiles + childDirs.sumOf { it.fileCount },
            category = null,
            lastModified = maxOf(newest, childDirs.maxOfOrNull { it.lastModified } ?: 0L),
            children = children,
        )
    }

    private fun isExcluded(dir: Path): Boolean =
        dir.fileName?.toString() == SafeTrashRepository.TRASH_DIR_NAME

    // ---------------------------------------------------------------- scoped mode

    /** Scoped Mode: only the photos/videos (and audio) MediaStore lets us see. */
    private fun readMediaStore(collector: Collector) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val uri = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            val dataCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            while (c.moveToNext()) {
                val path = c.getString(dataCol) ?: continue
                val name = c.getString(nameCol) ?: File(path).name
                val size = c.getLong(sizeCol)
                if (size <= 0) continue
                collector.onFile(path, name, size, c.getLong(dateCol) * 1000)
            }
        }
    }

    // ---------------------------------------------------------------- apps

    /** Installed app code size. Needs Usage Access; returns 0 without it. */
    private fun installedAppCodeBytes(): Long = try {
        val stats = context.getSystemService(StorageStatsManager::class.java)
        stats.queryStatsForUser(StorageManager.UUID_DEFAULT, Process.myUserHandle()).appBytes
    } catch (e: Exception) {
        0L
    }

    // ---------------------------------------------------------------- duplicates

    /**
     * Same size + same head/tail fingerprint ⇒ duplicate. Fast estimate for the dashboard;
     * the duplicate cleaner screen verifies with a full hash before anything is moved.
     * Returns (reclaimable bytes, extra copies).
     */
    private suspend fun findDuplicates(collector: Collector): List<DuplicateGroup> = coroutineScope {
        val groups = collector.sizeGroups.filterValues { it.size >= 2 }
        groups.entries.map { (size, paths) ->
            async(io) {
                paths.toList()
                    .mapNotNull { p -> fingerprint(p, size)?.let { it to p } }
                    .groupBy({ it.first }, { it.second })
                    .values
                    .filter { it.size >= 2 }
                    .map { same -> DuplicateGroup(size, same.sortedWith(keeperOrder)) }
            }
        }.awaitAll().flatten().sortedByDescending { it.wastedBytes }
    }

    /**
     * Which copy to keep: camera/gallery folders first, chat/download copies last,
     * then the oldest file (most likely the original).
     */
    private val keeperOrder = compareBy<String>(
        { p ->
            val l = p.lowercase()
            when {
                l.contains("/dcim/") || l.contains("/pictures/") || l.contains("/movies/") -> 0
                l.contains("/download") || FileClassifier.isSocial(l) -> 2
                else -> 1
            }
        },
        { p -> File(p).lastModified().takeIf { it > 0 } ?: Long.MAX_VALUE },
        { p -> p.length },
    )

    private fun fingerprint(path: String, size: Long): String? = try {
        RandomAccessFile(path, "r").use { raf ->
            val md = MessageDigest.getInstance("MD5")
            val buf = ByteArray(FINGERPRINT_CHUNK)
            val head = raf.read(buf, 0, buf.size)
            if (head > 0) md.update(buf, 0, head)
            if (size > FINGERPRINT_CHUNK * 2) {
                raf.seek(size - FINGERPRINT_CHUNK)
                val tail = raf.read(buf, 0, buf.size)
                if (tail > 0) md.update(buf, 0, tail)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
    } catch (e: Exception) {
        null
    }

    // ---------------------------------------------------------------- burst photos

    /** Camera shots taken ≤ 1.5 s apart, 3+ in a row. Returns the group using the most bytes. */
    private fun findLargestBurst(collector: Collector): BurstGroup? {
        var best: BurstGroup? = null
        for ((folder, queue) in collector.cameraPhotos) {
            val sorted = queue.sortedBy { it.lastModified }
            var run = ArrayList<FileRef>()
            fun close() {
                if (run.size >= 3) {
                    val group = BurstGroup(folder, run)
                    if (best == null || group.totalBytes > best!!.totalBytes) best = group
                }
                run = ArrayList()
            }
            for (photo in sorted) {
                val last = run.lastOrNull()
                if (last != null && photo.lastModified - last.lastModified > BURST_GAP_MS) close()
                run += photo
            }
            close()
        }
        return best
    }

    // ---------------------------------------------------------------- collector

    /** Thread-safe accumulators shared by all walker coroutines. */
    private class Collector {
        val files = AtomicInteger()
        val bytes = AtomicLong()
        val categoryBytes = Array(StorageCategory.entries.size) { AtomicLong() }

        val videos = AtomicInteger()
        val largeVideos = AtomicInteger()
        val largestVideo = AtomicReference<FileRef?>(null)

        val sentList = ConcurrentLinkedQueue<FileRef>()
        val apkList = ConcurrentLinkedQueue<FileRef>()
        val thumbList = ConcurrentLinkedQueue<FileRef>()

        /** size → paths, for duplicate detection (files ≥ 100 KB only). */
        val sizeGroups = ConcurrentHashMap<Long, ConcurrentLinkedQueue<String>>()
        /** camera folder → photos, for burst detection. */
        val cameraPhotos = ConcurrentHashMap<String, ConcurrentLinkedQueue<FileRef>>()

        fun onFile(path: String, name: String, size: Long, modified: Long): StorageCategory {
            val lower = path.lowercase()
            val ext = FileClassifier.extensionOf(name)
            val category = FileClassifier.classify(lower, ext)
            categoryBytes[category.ordinal].addAndGet(size)
            files.incrementAndGet()
            bytes.addAndGet(size)

            if (FileClassifier.isVideo(ext)) {
                videos.incrementAndGet()
                if (size >= LARGE_VIDEO_BYTES) largeVideos.incrementAndGet()
                val ref = FileRef(path, name, size, modified)
                largestVideo.accumulateAndGet(ref) { cur, new ->
                    if (cur == null || new!!.sizeBytes > cur.sizeBytes) new else cur
                }
            }

            when {
                FileClassifier.isThumbnail(lower) -> {
                    thumbList.add(FileRef(path, name, size, modified))
                }
                FileClassifier.isWhatsAppSent(lower) -> {
                    sentList.add(FileRef(path, name, size, modified))
                }
                FileClassifier.isApk(ext) -> {
                    apkList.add(FileRef(path, name, size, modified))
                }
                size >= DUPLICATE_MIN_BYTES && !lower.contains("/android/") -> {
                    sizeGroups.getOrPut(size) { ConcurrentLinkedQueue() }.add(path)
                }
            }

            if (FileClassifier.isPhoto(ext) &&
                (lower.contains("/dcim/camera/") || lower.contains("burst"))
            ) {
                val folder = path.substringBeforeLast('/')
                cameraPhotos.getOrPut(folder) { ConcurrentLinkedQueue() }
                    .add(FileRef(path, name, size, modified))
            }
            return category
        }
    }

    private companion object {
        const val TAG = "StorageScanner"
        const val PROGRESS_TICK_MS = 250L
        const val LARGE_VIDEO_BYTES = 100_000_000L
        const val DUPLICATE_MIN_BYTES = 100_000L
        const val FINGERPRINT_CHUNK = 16 * 1024
        const val BURST_GAP_MS = 1_500L
    }
}
