package com.storagepulse.cleaner.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColor
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.storagepulse.cleaner.MainActivity
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.billing.ProRepository
import com.storagepulse.cleaner.data.settings.AccentColor
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.data.widgets.WidgetData
import com.storagepulse.cleaner.data.widgets.WidgetDataRepository
import com.storagepulse.cleaner.ui.theme.accentOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDeps {
    fun widgetData(): WidgetDataRepository
    fun pro(): ProRepository
    fun settings(): SettingsRepository
}

fun widgetDeps(context: Context): WidgetDeps =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetDeps::class.java)

/** What every widget needs to draw, read once per update. */
data class WidgetState(val data: WidgetData, val isPro: Boolean, val accent: AccentColor)

suspend fun loadWidgetState(context: Context): WidgetState {
    val deps = widgetDeps(context)
    return WidgetState(deps.widgetData().current(), deps.pro().isPro.first(), deps.settings().accent.first())
}

// ---------------------------------------------------------------- colors (Obsidian Telemetry)

object WColors {
    val Card = Color(0xFF111827)
    val Raised = Color(0xFF1C1F29)
    val Stroke = Color(0xFF1F2937)
    val Text = Color(0xFFDFE2EF)
    val TextDim = Color(0xFFBBCABF)
    val Muted = Color(0xFF86948A)
    val Amber = Color(0xFFFFB95F)
    val Indigo = Color(0xFF6366F1)
}

/** Same color in light and dark: the widgets are always Obsidian. */
fun fixed(c: Color): GlanceColor = ColorProvider(day = c, night = c)

fun GlanceModifier.rounded(c: Color): GlanceModifier =
    background(ImageProvider(R.drawable.widget_round), colorFilter = ColorFilter.tint(fixed(c)))

@Composable
fun Dot(color: Color, size: Dp = 6.dp) {
    Box(GlanceModifier.size(size).background(ImageProvider(R.drawable.widget_dot), colorFilter = ColorFilter.tint(fixed(color)))) {}
}

@Composable
fun Icon(res: Int, color: Color, size: Dp = 16.dp) {
    Image(ImageProvider(res), contentDescription = null, modifier = GlanceModifier.size(size), colorFilter = ColorFilter.tint(fixed(color)))
}

@Composable
fun Mono(text: String, color: Color, size: TextUnit = 11.sp, bold: Boolean = false, modifier: GlanceModifier = GlanceModifier) {
    Text(text, modifier, style = TextStyle(color = fixed(color), fontSize = size, fontFamily = FontFamily.Monospace, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium), maxLines = 1)
}

@Composable
fun Label(text: String, color: Color, size: TextUnit = 13.sp, bold: Boolean = false, modifier: GlanceModifier = GlanceModifier) {
    Text(text, modifier, style = TextStyle(color = fixed(color), fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium), maxLines = 1)
}

/** Accent pair (bright fill, dark text on it) for the chosen telemetry accent. */
fun accentColors(accent: AccentColor): Pair<Color, Color> = accentOf(accent).let { it.light to it.onLight }

// ---------------------------------------------------------------- deep links

/** Opens the app on [route]. A unique data Uri keeps each widget button's PendingIntent apart. */
fun openRouteIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("storagepulse://open/$route".toUri())
        .putExtra(MainActivity.EXTRA_ROUTE, route)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

// ---------------------------------------------------------------- updates

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        StorageBoostWidget().updateAll(context)
        DailyClutterWidget().updateAll(context)
        MinimalStripWidget().updateAll(context)
    }
}

/** "Rescan" on the 4×1 strip: full scan in the background, then every widget redraws. */
class RescanAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetRefreshWorker.enqueue(context, scan = true)
    }
}

/**
 * Refreshes widget numbers. Periodic (every 6 h, battery not low, no scan) + one-off after the app
 * goes to the background or the user taps Rescan. Never deletes or moves anything.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        widgetDeps(applicationContext).widgetData().refresh(scan = inputData.getBoolean(KEY_SCAN, false))
        WidgetUpdater.updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val KEY_SCAN = "scan"
        private const val PERIODIC = "widget_refresh_periodic"
        private const val ONE_OFF = "widget_refresh_now"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun enqueue(context: Context, scan: Boolean) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInputData(workDataOf(KEY_SCAN to scan))
                .build()
            // A Rescan replaces a pending quick refresh; a quick refresh never cancels a Rescan.
            val policy = if (scan) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            WorkManager.getInstance(context).enqueueUniqueWork(ONE_OFF, policy, request)
        }
    }
}
