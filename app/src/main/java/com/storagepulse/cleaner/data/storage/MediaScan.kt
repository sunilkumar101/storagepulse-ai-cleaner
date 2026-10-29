package com.storagepulse.cleaner.data.storage

import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Tells MediaStore about added or removed files and waits until it has processed them, so a
 * gallery reload right after sees the change. Gives up after [timeoutMs] instead of hanging.
 */
suspend fun scanAndWait(context: Context, paths: Collection<String>, timeoutMs: Long = 5_000) {
    if (paths.isEmpty()) return
    withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            val left = AtomicInteger(paths.size)
            MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
                if (left.decrementAndGet() == 0 && cont.isActive) cont.resume(Unit)
            }
        }
    }
}
