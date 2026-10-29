package com.storagepulse.cleaner.data.dedup

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device photo deduplicator.
 * - dHash (64-bit) + sharpness from MediaStore thumbnails, cached in Room.
 * - SHA-256 only for files that share their exact size with another file.
 * - Clusters: exact duplicates, similar burst shots, blurry photos, old screenshots.
 * - Smart Keeper: the sharpest / highest-resolution photo of each cluster is protected.
 * Nothing leaves the device.
 */
@Singleton
class AiDeduplicatorEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: MediaFingerprintDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<DedupState>(DedupState.Idle)
    val state: StateFlow<DedupState> = _state.asStateFlow()
    private var job: Job? = null

    fun analyze(force: Boolean = false) {
        if (job?.isActive == true) return
        if (!force && _state.value is DedupState.Ready) return
        job = scope.launch {
            _state.value = DedupState.Analyzing(0, 0)
            _state.value = try {
                val result = run()
                Log.i(TAG, "Analyzed ${result.analyzedImages} images, ${result.clusters.size} clusters in ${result.durationMs} ms")
                DedupState.Ready(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Analysis failed", e)
                DedupState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /** Files left (moved to Safe Trash): drop them from every cluster without re-analysing. */
    fun remove(paths: Set<String>) {
        val ready = _state.value as? DedupState.Ready ?: return
        val clusters = ready.result.clusters.mapNotNull { c ->
            val items = c.items.filterNot { it.path in paths }
            val keeper = c.keeper?.takeUnless { it.path in paths }
            val needsPair = c.type == ClusterType.EXACT_DUPLICATES || c.type == ClusterType.SIMILAR_BURST
            when {
                items.isEmpty() -> null
                needsPair && items.size < 2 -> null
                else -> c.copy(items = items, keeper = keeper, suggested = c.suggested - paths)
            }
        }
        _state.value = DedupState.Ready(ready.result.copy(clusters = clusters))
        scope.launch { runCatching { dao.delete(paths.toList()) } }
    }

    // ------------------------------------------------------------------ pipeline

    private suspend fun run(): DedupResult = coroutineScope {
        val started = System.currentTimeMillis()
        val rows = withContext(Dispatchers.IO) { queryImages() }
        val cache = withContext(Dispatchers.IO) { dao.all().associateBy { it.path } }

        val fresh = ArrayList<MediaFingerprintEntity>()
        val prints = HashMap<String, MediaFingerprintEntity>(rows.size)
        val todo = rows.filter { row ->
            val cached = cache[row.path]
            if (cached != null && cached.sizeBytes == row.size && cached.lastModified == row.modified) {
                prints[row.path] = cached
                false
            } else {
                true
            }
        }

        val done = AtomicInteger(prints.size)
        val total = rows.size
        _state.value = DedupState.Analyzing(done.get(), total)
        val gate = Semaphore(Runtime.getRuntime().availableProcessors().coerceIn(2, DECODE_CONCURRENCY))
        val computed = todo.map { row ->
            async(Dispatchers.IO) {
                val fp = gate.withPermit { fingerprint(row) }
                val n = done.incrementAndGet()
                if (n % 25 == 0 || n == total) _state.value = DedupState.Analyzing(n, total)
                fp
            }
        }.awaitAll().filterNotNull()
        computed.forEach { prints[it.path] = it }
        fresh += computed

        // Exact duplicates need a full-file hash; only for files whose size collides.
        val sizeCollisions = prints.values.groupBy { it.sizeBytes }.filterValues { it.size >= 2 }.values.flatten()
        val hashed = sizeCollisions.map { fp ->
            async(Dispatchers.IO) {
                if (fp.sha256 != null) fp else gate.withPermit { sha256(fp.path)?.let { fp.copy(sha256 = it) } ?: fp }
            }
        }.awaitAll()
        hashed.forEach { fp ->
            if (fp.sha256 != null && prints[fp.path]?.sha256 == null) {
                prints[fp.path] = fp
                fresh.removeAll { it.path == fp.path }
                fresh += fp
            }
        }

        withContext(Dispatchers.IO) {
            fresh.chunked(500).forEach { dao.upsert(it) }
            val gone = cache.keys - prints.keys
            if (gone.isNotEmpty()) gone.chunked(500).forEach { dao.delete(it) }
        }

        val items = rows.mapNotNull { row ->
            val fp = prints[row.path] ?: return@mapNotNull null
            MediaItem(
                path = row.path,
                contentUri = row.uri.toString(),
                name = row.name,
                sizeBytes = row.size,
                lastModified = row.modified,
                dateTaken = row.taken,
                width = fp.width,
                height = fp.height,
                sharpness = ImageSignals.sharpnessScore(fp.laplacianVariance),
                dHash = fp.dHash,
            )
        }
        val clusters = buildClusters(items, prints)
        writeDebugStats(items, prints, clusters, rows.filter { it.path !in prints }.map { it.path })
        DedupResult(clusters, items.size, System.currentTimeMillis() - started)
    }

    private fun buildClusters(items: List<MediaItem>, prints: Map<String, MediaFingerprintEntity>): List<MediaCluster> {
        val out = ArrayList<MediaCluster>()
        val used = HashSet<String>()

        // 1) Exact duplicates: same size + same SHA-256.
        items.filter { prints[it.path]?.sha256 != null }
            .groupBy { it.sizeBytes to prints.getValue(it.path).sha256 }
            .values.filter { it.size >= 2 }
            .forEach { group ->
                val keeper = group.sortedWith(exactKeeperOrder).first()
                out += MediaCluster(
                    id = "exact:${keeper.path}",
                    type = ClusterType.EXACT_DUPLICATES,
                    items = listOf(keeper) + group.filter { it !== keeper },
                    keeper = keeper,
                    suggested = group.filter { it !== keeper }.map { it.path }.toSet(),
                    matchPercent = 100.0,
                )
                group.filter { it !== keeper }.forEach { used += it.path }
            }

        // 2) Similar burst shots: same folder, taken within the window, dHash distance ≤ limit.
        val candidates = items
            .filter { it.path !in used && (prints[it.path]?.complexity ?: 0.0) >= MIN_COMPLEXITY }
            .sortedBy { it.dateTaken }
        val parent = IntArray(candidates.size) { it }
        fun find(i: Int): Int { var x = i; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
        for (i in candidates.indices) {
            val a = candidates[i]
            val folderA = a.path.substringBeforeLast('/')
            var j = i + 1
            while (j < candidates.size && candidates[j].dateTaken - a.dateTaken <= BURST_WINDOW_MS) {
                val b = candidates[j]
                if (b.path.substringBeforeLast('/') == folderA && ImageSignals.hamming(a.dHash, b.dHash) <= MAX_HAMMING) {
                    parent[find(j)] = find(i)
                }
                j++
            }
        }
        candidates.indices.groupBy { find(it) }.values.filter { it.size >= 2 }.forEach { idx ->
            val group = idx.map { candidates[it] }
            val maxMp = group.maxOf { it.megapixels }.coerceAtLeast(0.01)
            val keeper = group.maxBy { it.sharpness * 0.7 + 30.0 * (it.megapixels / maxMp) }
            val others = group.filter { it !== keeper }
            out += MediaCluster(
                id = "similar:${keeper.path}",
                type = ClusterType.SIMILAR_BURST,
                items = listOf(keeper) + others.sortedBy { it.dateTaken },
                keeper = keeper,
                suggested = others.map { it.path }.toSet(),
                matchPercent = others.map { 100.0 * (64 - ImageSignals.hamming(keeper.dHash, it.dHash)) / 64 }.average(),
            )
            group.forEach { used += it.path }
        }

        // 3) Blurry camera photos (not already in a group).
        val blurry = items.filter {
            it.path !in used && it.path.lowercase().contains("/dcim/") &&
                !it.path.lowercase().contains("screenshot") &&
                it.sharpness < BLUR_SCORE_THRESHOLD && (it.megapixels == 0.0 || it.megapixels >= 1.0)
        }.sortedBy { it.sharpness }
        if (blurry.isNotEmpty()) {
            out += MediaCluster("blurry", ClusterType.BLURRY, blurry, null, blurry.map { it.path }.toSet(), 0.0)
            blurry.forEach { used += it.path }
        }

        // 4) Old screenshots — offered, never pre-selected (they may matter).
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(OLD_SCREENSHOT_DAYS)
        val shots = items.filter {
            it.path !in used && it.path.lowercase().contains("screenshot") && it.lastModified in 1 until cutoff
        }.sortedBy { it.lastModified }
        if (shots.isNotEmpty()) out += MediaCluster("screenshots", ClusterType.OLD_SCREENSHOTS, shots, null, emptySet(), 0.0)

        return out.sortedWith(compareBy<MediaCluster> { it.type.ordinal }.thenByDescending { it.totalBytes })
    }

    /** Debug builds only: sharpness distribution for tuning (logcat gets rate-limited on some OEMs). */
    private fun writeDebugStats(
        items: List<MediaItem>,
        prints: Map<String, MediaFingerprintEntity>,
        clusters: List<MediaCluster>,
        unprinted: List<String>,
    ) {
        val debuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return
        val camera = items.filter { it.path.lowercase().contains("/dcim/camera/") }
        val lv = camera.mapNotNull { prints[it.path]?.laplacianVariance }.sorted()
        if (lv.isEmpty()) return
        fun p(q: Int) = lv[(lv.size * q / 100).coerceAtMost(lv.size - 1)].toInt()
        runCatching {
            File(context.filesDir, "dedup_stats.txt").writeText(
                "camera=${lv.size} p1=${p(1)} p5=${p(5)} p10=${p(10)} p25=${p(25)} p50=${p(50)} p75=${p(75)} p90=${p(90)} p99=${p(99)}\n" +
                    clusters.groupBy { it.type }.map { (t, c) -> "$t clusters=${c.size} items=${c.sumOf { it.items.size }}" }.joinToString("\n") +
                    "\nunprinted=${unprinted.size} " + unprinted.filter { it.contains("SPTEST") }.take(10),
            )
        }
    }

    /** For bit-identical copies keep the camera/gallery copy, then the oldest. */
    private val exactKeeperOrder = compareBy<MediaItem>(
        { val l = it.path.lowercase(); if (l.contains("/dcim/") || l.contains("/pictures/")) 0 else if (l.contains("whatsapp") || l.contains("/download")) 2 else 1 },
        { it.lastModified },
    )

    // ------------------------------------------------------------------ IO helpers

    private data class Row(
        val uri: Uri, val path: String, val name: String, val size: Long,
        val modified: Long, val taken: Long, val width: Int, val height: Int,
    )

    private fun queryImages(): List<Row> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )
        val out = ArrayList<Row>()
        val base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // No SQL size filter: some rows (fresh or OEM-quirky) have NULL size/date; read those from the file.
        context.contentResolver.query(base, projection, null, null, null)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val data = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val name = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mod = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val taken = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val w = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val h = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            while (c.moveToNext()) {
                val path = c.getString(data) ?: continue
                if (path.contains("/.")) continue // hidden folders, incl. our Safe Trash
                val file = File(path)
                val size = if (c.isNull(sizeCol)) file.length() else c.getLong(sizeCol)
                if (size < MIN_BYTES) continue
                val modified = if (c.isNull(mod)) file.lastModified() else c.getLong(mod) * 1000
                out += Row(
                    uri = ContentUris.withAppendedId(base, c.getLong(id)),
                    path = path,
                    name = c.getString(name) ?: File(path).name,
                    size = size,
                    modified = modified,
                    taken = (if (c.isNull(taken)) 0L else c.getLong(taken)).takeIf { it > 0 } ?: modified,
                    width = c.getInt(w),
                    height = c.getInt(h),
                )
            }
        }
        return out
    }

    private fun fingerprint(row: Row): MediaFingerprintEntity? {
        val bmp = loadThumbnail(row) ?: return null
        return try {
            val (hash, complexity) = ImageSignals.dHash(bmp)
            MediaFingerprintEntity(
                path = row.path,
                sizeBytes = row.size,
                lastModified = row.modified,
                width = row.width,
                height = row.height,
                dHash = hash,
                laplacianVariance = ImageSignals.laplacianVariance(bmp),
                complexity = complexity,
            )
        } catch (e: Exception) {
            null
        } finally {
            bmp.recycle()
        }
    }

    /**
     * Small bitmap for hashing. With All Files Access the JPEG is decoded directly with
     * inSampleSize (DCT-scaled, no IPC) — several times faster than asking MediaProvider to
     * build thumbnails for thousands of photos. Falls back to MediaStore thumbnails.
     */
    private fun loadThumbnail(row: Row): Bitmap? = try {
        (exifThumbnail(row.path) ?: decodeSampled(row.path))?.let(::fitToBox) ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(row.uri, Size(THUMB_PX, THUMB_PX), null).let(::fitToBox)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }

    /**
     * Camera JPEGs carry a ~160 px preview inside the EXIF header. Reading it touches only the
     * first few KB of the file instead of decoding megabytes — the main speed-up on big galleries.
     */
    private fun exifThumbnail(path: String): Bitmap? {
        if (!directAccess) return null
        val lower = path.lowercase()
        if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) return null
        return try {
            val exif = android.media.ExifInterface(path)
            if (!exif.hasThumbnail()) return null
            exif.thumbnailBitmap?.takeIf { minOf(it.width, it.height) >= MIN_EXIF_THUMB_PX }
        } catch (e: Exception) {
            null
        }
    }

    /** Every source is measured in the same box so sharpness scores are comparable. */
    private fun fitToBox(b: Bitmap): Bitmap {
        val scale = THUMB_PX.toFloat() / maxOf(b.width, b.height)
        if (scale >= 1f) return b
        val fitted = Bitmap.createScaledBitmap(b, (b.width * scale).toInt().coerceAtLeast(1), (b.height * scale).toInt().coerceAtLeast(1), true)
        if (fitted !== b) b.recycle()
        return fitted
    }

    /** Full JPEG/PNG decodes are memory-heavy (progressive JPEGs especially): only a few at once. */
    private val heavyDecodeGate = java.util.concurrent.Semaphore(HEAVY_DECODE_CONCURRENCY)

    private fun decodeSampled(path: String): Bitmap? {
        if (!directAccess) return null
        heavyDecodeGate.acquire()
        try {
            return decodeSampledLocked(path)
        } finally {
            heavyDecodeGate.release()
        }
    }

    private fun decodeSampledLocked(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= THUMB_PX && bounds.outHeight / (sample * 2) >= THUMB_PX) sample *= 2
        val decoded: Bitmap? = BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            },
        )
        return decoded
    }

    private val directAccess: Boolean
        get() = com.storagepulse.cleaner.data.storage.StorageAccess.hasAllFilesAccess(context)

    private fun sha256(path: String): String? = try {
        val md = MessageDigest.getInstance("SHA-256")
        File(path).inputStream().buffered(256 * 1024).use { input ->
            val buf = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val TAG = "AiDeduplicator"
        const val DECODE_CONCURRENCY = 8
        const val THUMB_PX = 160
        const val MIN_EXIF_THUMB_PX = 96
        const val HEAVY_DECODE_CONCURRENCY = 3
        const val MIN_BYTES = 10_000L
        const val BURST_WINDOW_MS = 120_000L
        const val MAX_HAMMING = 10
        const val MIN_COMPLEXITY = 25.0
        const val OLD_SCREENSHOT_DAYS = 30L
    }
}
