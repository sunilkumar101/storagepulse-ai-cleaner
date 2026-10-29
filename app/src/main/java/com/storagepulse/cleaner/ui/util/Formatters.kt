package com.storagepulse.cleaner.ui.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/** Decimal units like Android Settings (a "128 GB" phone shows 128.0 GB). */
fun formatBytes(bytes: Long): String {
    val b = bytes.coerceAtLeast(0).toDouble()
    return when {
        b >= 1e9 -> String.format(Locale.US, "%.1f GB", b / 1e9)
        b >= 1e6 -> String.format(Locale.US, "%.0f MB", b / 1e6)
        b >= 1e3 -> String.format(Locale.US, "%.0f KB", b / 1e3)
        else -> String.format(Locale.US, "%.0f B", b)
    }
}

/** Tight form for small chart labels: "1.4G", "494M", "12K". */
fun formatBytesCompact(bytes: Long): String {
    val b = bytes.coerceAtLeast(0).toDouble()
    return when {
        b >= 1e9 -> String.format(Locale.US, if (b >= 1e10) "%.0fG" else "%.1fG", b / 1e9)
        b >= 1e6 -> String.format(Locale.US, "%.0fM", b / 1e6)
        b >= 1e3 -> String.format(Locale.US, "%.0fK", b / 1e3)
        else -> String.format(Locale.US, "%.0fB", b)
    }
}

fun formatCount(n: Int): String = String.format(Locale.US, "%,d", n)

fun daysSince(millis: Long, now: Long = System.currentTimeMillis()): Long =
    TimeUnit.MILLISECONDS.toDays((now - millis).coerceAtLeast(0))
