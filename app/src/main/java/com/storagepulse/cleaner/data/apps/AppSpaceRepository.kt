package com.storagepulse.cleaner.data.apps

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import com.storagepulse.cleaner.data.storage.FileClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class AppSpace(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val icon: Bitmap?,
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
    /** Last time the user had it in the foreground (0 = not in the usage history). */
    val lastUsed: Long,
    val installed: Long,
) {
    val totalBytes: Long get() = appBytes + dataBytes
    /** Days since last use; when there is no usage record, days since install. */
    fun unusedDays(now: Long = System.currentTimeMillis()): Long =
        TimeUnit.MILLISECONDS.toDays(now - (if (lastUsed > 0) lastUsed else installed)).coerceAtLeast(0)
}

data class StaleInstaller(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val packageName: String?,
    /** True when the same or a newer version of this app is already installed. */
    val alreadyInstalled: Boolean,
)

/** Installed apps by footprint + last use (UsageStats / StorageStats), and leftover APK files. */
@Singleton
class AppSpaceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val pm: PackageManager get() = context.packageManager

    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        // Android 10+ can jump straight to this app's toggle.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) data = Uri.fromParts("package", context.packageName, null)
    }

    fun appInfoIntent(pkg: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))

    fun uninstallIntent(pkg: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null))

    /** User-installed launcher apps (visible via the manifest <queries>), largest first. */
    suspend fun apps(): List<AppSpace> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = pm.queryIntentActivities(launcher, 0).map { it.activityInfo.packageName }.toSet() - context.packageName
        val now = System.currentTimeMillis()
        val usage = context.getSystemService(UsageStatsManager::class.java)
            .queryAndAggregateUsageStats(now - TimeUnit.DAYS.toMillis(730), now)
        val stats = context.getSystemService(StorageStatsManager::class.java)
        val user = Process.myUserHandle()
        packages.mapNotNull { pkg ->
            runCatching {
                val info = pm.getApplicationInfo(pkg, 0)
                // Pre-installed apps (even updated ones) only lose their updates on uninstall, so the
                // "free X" promise would be false: list user-installed apps only.
                if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) return@runCatching null
                val pkgInfo = pm.getPackageInfo(pkg, 0)
                val s = stats.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg, user)
                AppSpace(
                    packageName = pkg,
                    label = pm.getApplicationLabel(info).toString(),
                    versionName = pkgInfo.versionName,
                    icon = iconOf(pm.getApplicationIcon(info)),
                    appBytes = s.appBytes,
                    dataBytes = s.dataBytes,
                    cacheBytes = s.cacheBytes,
                    lastUsed = usage[pkg]?.lastTimeUsed ?: 0L,
                    installed = pkgInfo.firstInstallTime,
                )
            }.getOrNull()
        }.sortedByDescending { it.totalBytes }
    }

    private fun iconOf(d: Drawable): Bitmap? = runCatching { d.toBitmap(96, 96) }.getOrNull()

    /** Installer packages left in Download/ and Telegram folders. */
    suspend fun staleInstallers(): List<StaleInstaller> = withContext(Dispatchers.IO) {
        val base = Environment.getExternalStorageDirectory()
        val roots = listOf(
            "Download", "Downloads", "Telegram", "Android/media/org.telegram.messenger/Telegram",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents",
        ).map { File(base, it) }.filter { it.isDirectory }
        roots.flatMap { root ->
            root.walkTopDown().maxDepth(4).filter { f ->
                f.isFile && FileClassifier.isApk(FileClassifier.extensionOf(f.name))
            }.toList()
        }.distinctBy { it.canonicalPath }.map { f ->
            val archive = if (f.name.endsWith(".apk", true)) pm.getPackageArchiveInfo(f.path, 0) else null
            val pkg = archive?.packageName
            val installed = pkg?.let { runCatching { pm.getPackageInfo(it, 0) }.getOrNull() }
            val newerOrSame = installed != null && archive != null &&
                versionCode(installed) >= versionCode(archive)
            StaleInstaller(f.path, f.name, f.length(), f.lastModified(), pkg, newerOrSame)
        }.sortedByDescending { it.sizeBytes }
    }

    @Suppress("DEPRECATION")
    private fun versionCode(p: android.content.pm.PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) p.longVersionCode else p.versionCode.toLong()
}
