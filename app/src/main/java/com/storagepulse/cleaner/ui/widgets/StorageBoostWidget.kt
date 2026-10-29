package com.storagepulse.cleaner.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.storage.model.StorageCategory
import com.storagepulse.cleaner.ui.navigation.Routes
import com.storagepulse.cleaner.ui.util.formatBytes
import androidx.compose.ui.graphics.Color

/** 4×2 "StoragePulse": used/total, category bar, recoverable cache and Boost (opens AI Clean). Pro. */
class StorageBoostWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(250.dp, 110.dp), DpSize(250.dp, 160.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent { Content(state) }
    }

    @Composable
    private fun Content(s: WidgetState) {
        val context = LocalContext.current
        if (!s.isPro) {
            LockedWidget(context.getString(R.string.wg_boost_title))
            return
        }
        val (accent, onAccent) = accentColors(s.accent)
        val d = s.data
        val tall = LocalSize.current.height >= 150.dp
        val open = actionStartActivity(openRouteIntent(context, Routes.DASHBOARD))
        Column(
            GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).padding(14.dp).clickable(open),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Icon(R.drawable.ic_widget_pulse, accent, 16.dp)
                Spacer(GlanceModifier.width(6.dp))
                Label(context.getString(R.string.app_short_name), WColors.Text, 13.sp, bold = true, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(6.dp))
                Row(GlanceModifier.rounded(WColors.Raised).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Dot(WColors.Amber)
                    Spacer(GlanceModifier.width(4.dp))
                    Mono(
                        context.getString(R.string.wg_used_of, formatGb(d.usedBytes), formatGb(d.totalBytes), d.usedPercent),
                        WColors.Amber,
                        10.sp,
                        bold = true,
                    )
                }
            }
            Spacer(GlanceModifier.height(10.dp))
            CategoryBar(s, accent)
            if (tall) {
                Spacer(GlanceModifier.height(6.dp))
                Legend()
            }
            Spacer(GlanceModifier.height(10.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Mono(context.getString(R.string.wg_recoverable), WColors.Muted, 9.sp)
                    Mono(
                        if (d.hasScan) context.getString(R.string.wg_ready, formatBytes(d.recoverableBytes)) else context.getString(R.string.wg_scan_needed),
                        accent,
                        13.sp,
                        bold = true,
                    )
                }
                Row(
                    GlanceModifier.wrapContentWidth().rounded(accent).padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(actionStartActivity(openRouteIntent(context, Routes.AI_CLEAN))),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Icon(R.drawable.ic_widget_bolt, onAccent, 14.dp)
                    Spacer(GlanceModifier.width(4.dp))
                    Label(
                        if (d.hasScan && d.recoverableBytes > 0) context.getString(R.string.wg_boost, formatBytes(d.recoverableBytes)) else context.getString(R.string.wg_clean),
                        onAccent,
                        12.sp,
                        bold = true,
                    )
                }
            }
        }
    }

    /** Videos | Photos | WA | Apps | Sys, proportional; a plain used bar before the first scan. */
    @Composable
    private fun CategoryBar(s: WidgetState, accent: Color) {
        val width = LocalSize.current.width - 28.dp
        val d = s.data
        Row(GlanceModifier.fillMaxWidth().height(8.dp).rounded(WColors.Stroke)) {
            if (d.hasScan && d.totalBytes > 0) {
                StorageCategory.entries.forEach { c ->
                    val w = width * ((d.categoryBytes[c] ?: 0L).toFloat() / d.totalBytes)
                    if (w > 1.dp) Box(GlanceModifier.width(w).height(8.dp).rounded(Color(c.colorArgb))) {}
                }
            } else if (d.totalBytes > 0) {
                Box(GlanceModifier.width(width * (d.usedBytes.toFloat() / d.totalBytes)).height(8.dp).rounded(accent)) {}
            }
        }
    }

    @Composable
    private fun Legend() {
        val context = LocalContext.current
        val names = listOf(R.string.wg_cat_videos, R.string.wg_cat_photos, R.string.wg_cat_wa, R.string.wg_cat_apps, R.string.wg_cat_sys)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            StorageCategory.entries.forEachIndexed { i, c ->
                Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Dot(Color(c.colorArgb), 5.dp)
                    Spacer(GlanceModifier.width(3.dp))
                    Mono(context.getString(names[i]), WColors.TextDim, 9.sp)
                }
            }
        }
    }
}

class StorageBoostWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StorageBoostWidget()
}

/** "114.2" style number for the used/total badge (GB, one decimal). */
fun formatGb(bytes: Long): String = String.format(java.util.Locale.US, "%.1f", bytes / 1e9)

/** Free users: the widget points to Pro instead of showing data. */
@Composable
fun LockedWidget(title: String) {
    val context = LocalContext.current
    Column(
        GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).padding(14.dp)
            .clickable(actionStartActivity(openRouteIntent(context, Routes.SETTINGS))),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Icon(R.drawable.ic_widget_lock, WColors.Indigo, 22.dp)
        Spacer(GlanceModifier.height(6.dp))
        Label(title, WColors.Text, 13.sp, bold = true)
        Spacer(GlanceModifier.height(2.dp))
        Mono(context.getString(R.string.wg_pro_locked), WColors.Muted, 10.sp)
    }
}
