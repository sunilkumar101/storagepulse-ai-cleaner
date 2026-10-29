package com.storagepulse.cleaner.data.gallery

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.storagepulse.cleaner.data.storage.scanAndWait
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class MediaEntry(
    val id: Long,
    val uri: Uri,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val modified: Long,
    val isVideo: Boolean,
) {
    val folder: String get() = path.substringBeforeLast('/')
}

data class MediaFolder(
    val path: String,
    val name: String,
    val items: Int,
    val bytes: Long,
    val latest: Long,
    val cover: MediaEntry?,
    val pinnedCover: Boolean,
    /** Screenshot folders: items older than 90 days (offered to AI Clean). */
    val oldItems: Int,
)

data class GalleryIndex(
    val entries: List<MediaEntry>,
    val folders: List<MediaFolder>,
    val indexMs: Long,
) {
    val totalBytes: Long get() = entries.sumOf { it.sizeBytes }
}

data class GpsStripResult(val stripped: Int, val skipped: Int)

/** EXIF orientation of a photo in clockwise degrees (0, 90, 180, 270). */
fun exifDegrees(path: String): Int = runCatching {
    when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}.getOrDefault(0)

private val Context.galleryStore by preferencesDataStore(name = "gallery")

/** Photos + videos from MediaStore in one query (Fossify-style), grouped into folders in memory. */
@Singleton
class GalleryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val coverKey = stringSetPreferencesKey("cover_pins") // "folder\u0000mediaPath"

    suspend fun load(): GalleryIndex = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        val entries = query()
        val pins = context.galleryStore.data.first()[coverKey].orEmpty()
            .mapNotNull { it.split('\u0000').takeIf { p -> p.size == 2 }?.let { p -> p[0] to p[1] } }
            .toMap()
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
        val folders = entries.groupBy { it.folder }.map { (path, items) ->
            val pinned = pins[path]?.let { p -> items.firstOrNull { it.path == p } }
            val isScreens = path.lowercase().contains("screenshot")
            MediaFolder(
                path = path,
                name = displayName(path),
                items = items.size,
                bytes = items.sumOf { it.sizeBytes },
                latest = items.maxOf { it.modified },
                cover = pinned ?: items.maxByOrNull { it.modified },
                pinnedCover = pinned != null,
                oldItems = if (isScreens) items.count { it.modified < cutoff } else 0,
            )
        }.sortedByDescending { it.bytes }
        GalleryIndex(entries, folders, (System.nanoTime() - started) / 1_000_000)
    }

    private fun query(): List<MediaEntry> {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val out = ArrayList<MediaEntry>()
        context.contentResolver.query(uri, projection, selection, args, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")?.use { c ->
            val idC = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dataC = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val nameC = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeC = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val modC = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val typeC = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            while (c.moveToNext()) {
                val path = c.getString(dataC) ?: continue
                if (path.contains("/.")) continue // hidden folders incl. Safe Trash
                val isVideo = c.getInt(typeC) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val id = c.getLong(idC)
                out += MediaEntry(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id),
                    path = path,
                    name = c.getString(nameC) ?: File(path).name,
                    sizeBytes = if (c.isNull(sizeC)) File(path).length() else c.getLong(sizeC),
                    modified = c.getLong(modC) * 1000,
                    isVideo = isVideo,
                )
            }
        }
        return out
    }

    private fun displayName(path: String): String {
        val lower = path.lowercase()
        val name = path.substringAfterLast('/')
        return when {
            lower.endsWith("/dcim/camera") -> "Camera (DCIM)"
            lower.contains("whatsapp") && lower.contains("images") -> "WhatsApp Images"
            lower.contains("whatsapp") && lower.contains("video") -> "WhatsApp Video"
            else -> name
        }
    }

    suspend fun setCover(folder: String, mediaPath: String) {
        context.galleryStore.edit { prefs ->
            val rest = prefs[coverKey].orEmpty().filterNot { it.startsWith(folder + '\u0000') }
            prefs[coverKey] = rest.toSet() + (folder + '\u0000' + mediaPath)
        }
    }

    /**
     * Removes GPS location tags from JPEG photos in place (other EXIF such as date and
     * orientation stays). Videos and non-JPEG files are skipped.
     */
    suspend fun stripGps(paths: List<String>): GpsStripResult = withContext(Dispatchers.IO) {
        var stripped = 0
        var skipped = 0
        for (p in paths) {
            val lower = p.lowercase()
            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
                skipped++
                continue
            }
            try {
                val exif = ExifInterface(p)
                GPS_TAGS.forEach { exif.setAttribute(it, null) }
                exif.saveAttributes()
                stripped++
            } catch (e: Exception) {
                skipped++
            }
        }
        if (stripped > 0) scanAndWait(context, paths)
        GpsStripResult(stripped, skipped)
    }

    /** True if the JPEG carries a GPS position. */
    fun hasGps(path: String): Boolean = runCatching {
        ExifInterface(path).getLatLong(FloatArray(2))
    }.getOrDefault(false)

    /**
     * Saves the visible region (normalised 0..1 rect) as a new JPEG next to the original
     * ("name_crop.jpg"). The original is never modified.
     */
    suspend fun saveCrop(path: String, left: Float, top: Float, right: Float, bottom: Float): File? = withContext(Dispatchers.IO) {
        runCatching {
            val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(path)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(path, false)
            } ?: return@runCatching null
            val w = decoder.width
            val h = decoder.height
            // The rect is in DISPLAYED (EXIF-rotated) coordinates; the decoder reads raw pixels.
            val degrees = exifDegrees(path)
            val (rl, rt, rr, rb) = toRaw(degrees, left, top, right, bottom)
            val rect = Rect(
                (rl * w).toInt().coerceIn(0, w - 1),
                (rt * h).toInt().coerceIn(0, h - 1),
                (rr * w).toInt().coerceIn(1, w),
                (rb * h).toInt().coerceIn(1, h),
            )
            val raw = decoder.decodeRegion(rect, null)
            decoder.recycle()
            val bmp = if (degrees == 0) raw else {
                val m = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also { if (it !== raw) raw.recycle() }
            }
            val src = File(path)
            var out = File(src.parentFile, "${src.nameWithoutExtension}_crop.jpg")
            var n = 2
            while (out.exists()) out = File(src.parentFile, "${src.nameWithoutExtension}_crop$n.jpg").also { n++ }
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            bmp.recycle()
            scanAndWait(context, listOf(out.path))
            out
        }.getOrNull()
    }

    /** Maps a normalised rect from the displayed (rotated) image back to raw pixel space. */
    private fun toRaw(degrees: Int, l: Float, t: Float, r: Float, b: Float): List<Float> {
        fun map(x: Float, y: Float): Pair<Float, Float> = when (degrees) {
            90 -> y to (1 - x)
            180 -> (1 - x) to (1 - y)
            270 -> (1 - y) to x
            else -> x to y
        }
        val a = map(l, t)
        val c = map(r, b)
        return listOf(minOf(a.first, c.first), minOf(a.second, c.second), maxOf(a.first, c.first), maxOf(a.second, c.second))
    }

    private companion object {
        val GPS_TAGS = listOf(
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD, ExifInterface.TAG_GPS_AREA_INFORMATION,
            ExifInterface.TAG_GPS_IMG_DIRECTION, ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
            ExifInterface.TAG_GPS_SPEED, ExifInterface.TAG_GPS_SPEED_REF,
            ExifInterface.TAG_GPS_TRACK, ExifInterface.TAG_GPS_TRACK_REF,
            ExifInterface.TAG_GPS_DEST_LATITUDE, ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
            ExifInterface.TAG_GPS_DEST_LONGITUDE, ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
            ExifInterface.TAG_GPS_DEST_BEARING, ExifInterface.TAG_GPS_DEST_BEARING_REF,
            ExifInterface.TAG_GPS_DEST_DISTANCE, ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
            ExifInterface.TAG_GPS_SATELLITES, ExifInterface.TAG_GPS_STATUS,
            ExifInterface.TAG_GPS_MEASURE_MODE, ExifInterface.TAG_GPS_DOP,
            ExifInterface.TAG_GPS_MAP_DATUM, ExifInterface.TAG_GPS_DIFFERENTIAL,
            ExifInterface.TAG_GPS_VERSION_ID,
        )
    }
}
