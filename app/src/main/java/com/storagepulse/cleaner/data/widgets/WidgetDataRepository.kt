package com.storagepulse.cleaner.data.widgets

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.storage.model.ScanState
import com.storagepulse.cleaner.data.storage.model.StorageCategory
import com.storagepulse.cleaner.data.trash.SafeTrashRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetStore by preferencesDataStore(name = "widget")

/** Everything the 3 home screen widgets and the Screen 10 previews show. */
data class WidgetData(
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val freeBytes: Long = 0,
    /** Category split from the last full scan (empty until the first scan). */
    val categoryBytes: Map<StorageCategory, Long> = emptyMap(),
    /** "Safe to clean" bytes from the last scan: duplicates, WA sent copies, APKs, thumbnails. */
    val recoverableBytes: Long = 0,
    val hasScan: Boolean = false,
    val screenshotCount: Int = 0,
    val screenshotBytes: Long = 0,
    val heavyVideoCount: Int = 0,
    val heavyVideoBytes: Long = 0,
    val trashBytes: Long = 0,
    val updatedAt: Long = 0,
) {
    val usedPercent: Int get() = if (totalBytes > 0) (usedBytes * 100 / totalBytes).toInt() else 0
    /** Daily clutter = screenshots + heavy (≥100 MB) videos waiting for review. */
    val clutterBytes: Long get() = screenshotBytes + heavyVideoBytes
}

/**
 * Builds [WidgetData] cheaply (volume stats + two MediaStore queries + the last scan) and caches
 * it in DataStore so widgets render instantly without touching the disk.
 */
@Singleton
class WidgetDataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: StorageScannerRepository,
    private val trash: SafeTrashRepository,
) {
    val data: Flow<WidgetData> = context.widgetStore.data.map { it.toWidgetData() }

    suspend fun current(): WidgetData = data.first()

    /**
     * Recomputes and stores the numbers. [scan] = run a full storage scan first (Rescan button,
     * weekly job); otherwise the last scan in memory is reused if there is one. [force] = false
     * reuses a finished scan and only scans when there is none yet.
     */
    suspend fun refresh(scan: Boolean, force: Boolean = true): WidgetData = withContext(Dispatchers.IO) {
        if (scan) {
            scanner.startScan(force = force)
            withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                scanner.state.filter { it is ScanState.Done || it is ScanState.Failed }.first()
            }
        }
        val previous = current()
        val snapshot = (scanner.state.value as? ScanState.Done)?.snapshot
        val volume = scanner.volumeStats()
        val shots = sumOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DATA} LIKE ?",
            arrayOf("%/Screenshots/%"),
        )
        val heavy = sumOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.SIZE} >= ?",
            arrayOf(HEAVY_VIDEO_BYTES.toString()),
        )
        val fresh = WidgetData(
            totalBytes = volume.totalBytes,
            usedBytes = volume.usedBytes,
            freeBytes = volume.freeBytes,
            categoryBytes = snapshot?.categoryBytes ?: previous.categoryBytes,
            recoverableBytes = snapshot?.cleanup?.totalBytes ?: previous.recoverableBytes,
            hasScan = snapshot != null || previous.hasScan,
            screenshotCount = shots.first,
            screenshotBytes = shots.second,
            heavyVideoCount = heavy.first,
            heavyVideoBytes = heavy.second,
            trashBytes = trash.observeTotals().first().bytes,
            updatedAt = System.currentTimeMillis(),
        )
        save(fresh)
        fresh
    }

    private fun sumOf(uri: Uri, selection: String, args: Array<String>): Pair<Int, Long> = try {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), selection, args, null)?.use { c ->
            var bytes = 0L
            while (c.moveToNext()) bytes += c.getLong(0)
            c.count to bytes
        } ?: (0 to 0L)
    } catch (e: SecurityException) {
        0 to 0L // no media permission yet
    }

    private suspend fun save(d: WidgetData) {
        context.widgetStore.edit { p ->
            p[K.total] = d.totalBytes
            p[K.used] = d.usedBytes
            p[K.free] = d.freeBytes
            StorageCategory.entries.forEach { c -> p[K.cat(c)] = d.categoryBytes[c] ?: 0L }
            p[K.recoverable] = d.recoverableBytes
            p[K.hasScan] = d.hasScan
            p[K.shots] = d.screenshotCount
            p[K.shotBytes] = d.screenshotBytes
            p[K.heavy] = d.heavyVideoCount
            p[K.heavyBytes] = d.heavyVideoBytes
            p[K.trash] = d.trashBytes
            p[K.updated] = d.updatedAt
        }
    }

    private fun Preferences.toWidgetData() = WidgetData(
        totalBytes = this[K.total] ?: 0,
        usedBytes = this[K.used] ?: 0,
        freeBytes = this[K.free] ?: 0,
        categoryBytes = if (this[K.hasScan] == true) StorageCategory.entries.associateWith { this[K.cat(it)] ?: 0L } else emptyMap(),
        recoverableBytes = this[K.recoverable] ?: 0,
        hasScan = this[K.hasScan] ?: false,
        screenshotCount = this[K.shots] ?: 0,
        screenshotBytes = this[K.shotBytes] ?: 0,
        heavyVideoCount = this[K.heavy] ?: 0,
        heavyVideoBytes = this[K.heavyBytes] ?: 0,
        trashBytes = this[K.trash] ?: 0,
        updatedAt = this[K.updated] ?: 0,
    )

    private object K {
        val total = longPreferencesKey("total")
        val used = longPreferencesKey("used")
        val free = longPreferencesKey("free")
        fun cat(c: StorageCategory) = longPreferencesKey("cat_${c.name}")
        val recoverable = longPreferencesKey("recoverable")
        val hasScan = booleanPreferencesKey("has_scan")
        val shots = intPreferencesKey("shots")
        val shotBytes = longPreferencesKey("shot_bytes")
        val heavy = intPreferencesKey("heavy")
        val heavyBytes = longPreferencesKey("heavy_bytes")
        val trash = longPreferencesKey("trash")
        val updated = longPreferencesKey("updated")
    }

    companion object {
        const val HEAVY_VIDEO_BYTES = 100_000_000L
        private const val SCAN_TIMEOUT_MS = 3 * 60_000L
    }
}
