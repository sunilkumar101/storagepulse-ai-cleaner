package com.storagepulse.cleaner.data.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Storage permission checks for full-disk mode and scoped (media-only) mode. */
object StorageAccess {

    /** Full-disk access: All Files Access on Android 11+, legacy read/write on 8–10. */
    fun hasAllFilesAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            legacyPermissions.all { context.isGranted(it) }
        }

    /** Runtime permissions that give full-disk access below Android 11. */
    val legacyPermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    /** Runtime permissions for scoped (photos + videos) mode on this OS version. */
    val mediaPermissions: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** True if the user allowed all media or (Android 14+) a selected subset. */
    fun hasMediaAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            context.isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
                context.isGranted(Manifest.permission.READ_MEDIA_VIDEO) ||
                context.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            context.isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
                context.isGranted(Manifest.permission.READ_MEDIA_VIDEO)
        else -> context.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * Settings screens for All Files Access, most specific first:
     * this app's toggle, then the general list (some OEM ROMs lack the per-app screen).
     */
    fun allFilesAccessIntents(context: Context): List<Intent> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return listOf(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.fromParts("package", context.packageName, null),
            ),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
    }

    /** This app's system "App info" page (Permissions live there). */
    fun appDetailsSettingsIntent(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )

    private fun Context.isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
