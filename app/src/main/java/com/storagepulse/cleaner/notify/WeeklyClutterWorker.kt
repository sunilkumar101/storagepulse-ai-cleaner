package com.storagepulse.cleaner.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.ui.navigation.Routes
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.widgets.WidgetUpdater
import com.storagepulse.cleaner.ui.widgets.openRouteIntent
import com.storagepulse.cleaner.ui.widgets.widgetDeps
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Every Sunday at 8 PM: scans (read-only), then posts one notification with the clutter that is
 * ready to review. Never moves or deletes anything by itself.
 */
class WeeklyClutterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val data = try {
            widgetDeps(applicationContext).widgetData().refresh(scan = true)
        } catch (e: Exception) {
            return Result.retry()
        }
        WidgetUpdater.updateAll(applicationContext)
        val total = data.recoverableBytes + data.clutterBytes
        if (total > 0 && canNotify(applicationContext)) {
            ensureChannel(applicationContext)
            val ctx = applicationContext
            val tap = PendingIntent.getActivity(
                ctx, 0, openRouteIntent(ctx, Routes.AI_CLEAN),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_widget_pulse)
                .setContentTitle(ctx.getString(R.string.notify_weekly_title, formatBytes(total)))
                .setContentText(
                    ctx.getString(R.string.notify_weekly_body, formatBytes(data.recoverableBytes), data.screenshotCount, data.heavyVideoCount),
                )
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        ctx.getString(R.string.notify_weekly_body, formatBytes(data.recoverableBytes), data.screenshotCount, data.heavyVideoCount),
                    ),
                )
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            try {
                NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // Permission revoked between the check and the post: skip this week.
            }
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "weekly_clutter_notification"
        private const val CHANNEL = "weekly_clutter"
        private const val NOTIFICATION_ID = 1001

        fun canNotify(context: Context): Boolean =
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()

        private fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) != null) return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notify_channel_weekly), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        /** Next Sunday 20:00, then every 7 days. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.of(20, 0))
            if (!next.isAfter(now)) next = next.plusWeeks(1)
            val delay = Duration.between(now, next).toMillis()
            val request = PeriodicWorkRequestBuilder<WeeklyClutterWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
