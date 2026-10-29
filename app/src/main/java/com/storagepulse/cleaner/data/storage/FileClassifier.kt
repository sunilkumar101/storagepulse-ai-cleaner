package com.storagepulse.cleaner.data.storage

import com.storagepulse.cleaner.data.storage.model.StorageCategory

/** Path/extension rules that put a file into one dashboard category. */
object FileClassifier {

    private val videoExt = setOf(
        "mp4", "mkv", "mov", "3gp", "3g2", "webm", "avi", "m4v", "ts", "mts", "m2ts", "flv", "wmv",
    )
    private val photoExt = setOf(
        "jpg", "jpeg", "png", "heic", "heif", "webp", "gif", "bmp", "avif",
        // RAW
        "dng", "raw", "cr2", "cr3", "nef", "arw", "orf", "rw2", "raf", "srw", "pef",
    )
    private val apkExt = setOf("apk", "apks", "xapk", "apkm", "obb")
    private val cacheExt = setOf("tmp", "temp", "log", "bak", "crdownload", "part")

    private val socialMarkers = listOf(
        "/whatsapp/", "/com.whatsapp/", "/com.whatsapp.w4b/", "/whatsapp business/",
        "/telegram/", "/org.telegram.messenger/",
        "/instagram/", "/com.instagram.android/",
        "/messenger/", "/snapchat/", "/viber/", "/signal/", "/imo/",
    )

    fun extensionOf(name: String): String =
        name.substringAfterLast('.', "").lowercase()

    fun isVideo(ext: String) = ext in videoExt
    fun isPhoto(ext: String) = ext in photoExt
    fun isApk(ext: String) = ext == "apk" || ext == "apks" || ext == "xapk" || ext == "apkm"

    /** [lowerPath] must be lower-cased and end with "/" for folders or be a full file path. */
    fun isSocial(lowerPath: String) = socialMarkers.any { lowerPath.contains(it) }

    fun isThumbnail(lowerPath: String) = lowerPath.contains("/.thumbnails/")

    /** WhatsApp keeps a second copy of every photo/video you send in ".../Sent/". */
    fun isWhatsAppSent(lowerPath: String) =
        lowerPath.contains("whatsapp") && lowerPath.contains("/sent/")

    fun classify(lowerPath: String, ext: String): StorageCategory = when {
        isSocial(lowerPath) -> StorageCategory.SOCIAL
        lowerPath.contains("/android/data/") || lowerPath.contains("/android/obb/") -> StorageCategory.APPS
        ext in apkExt -> StorageCategory.APPS
        ext in videoExt -> StorageCategory.VIDEOS
        ext in photoExt -> StorageCategory.PHOTOS
        else -> StorageCategory.SYSTEM
    }

    @Suppress("unused")
    fun isCacheLike(lowerPath: String, ext: String) =
        ext in cacheExt || lowerPath.contains("/cache/") || isThumbnail(lowerPath)
}
