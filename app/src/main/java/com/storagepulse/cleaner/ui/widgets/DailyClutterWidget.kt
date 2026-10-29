package com.storagepulse.cleaner.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.ui.navigation.Routes
import com.storagepulse.cleaner.ui.util.formatBytes

/** 2×2 "Daily Clutter": screenshots + heavy videos, and Swipe to Purge (opens the swipe deck). Pro. */
class DailyClutterWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent { Content(state) }
    }

    @Composable
    private fun Content(s: WidgetState) {
        val context = LocalContext.current
        if (!s.isPro) {
            LockedWidget(context.getString(R.string.wg_clutter_title))
            return
        }
        val d = s.data
        val swipe = actionStartActivity(openRouteIntent(context, Routes.SWIPE_DECK))
        Column(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).padding(12.dp).clickable(swipe)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Mono(context.getString(R.string.wg_clutter_caps), WColors.Muted, 9.sp)
                    Mono(formatBytes(d.clutterBytes), WColors.Text, 20.sp, bold = true)
                }
                Box(GlanceModifier.rounded(WColors.Raised).padding(6.dp)) { Icon(R.drawable.ic_widget_clutter, WColors.Amber, 16.dp) }
            }
            Spacer(GlanceModifier.height(8.dp))
            Line(context.resources.getQuantityString(R.plurals.wg_screenshots, d.screenshotCount, d.screenshotCount))
            Spacer(GlanceModifier.height(4.dp))
            Line(context.resources.getQuantityString(R.plurals.wg_heavy_videos, d.heavyVideoCount, d.heavyVideoCount))
            Spacer(GlanceModifier.defaultWeight())
            Row(
                GlanceModifier.fillMaxWidth().rounded(WColors.Indigo).padding(horizontal = 10.dp, vertical = 8.dp).clickable(swipe),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Label(context.getString(R.string.wg_swipe), WColors.Text, 12.sp, bold = true, modifier = GlanceModifier.defaultWeight())
                Icon(R.drawable.ic_widget_arrow, WColors.Text, 14.dp)
            }
        }
    }

    @Composable
    private fun Line(text: String) {
        Row(GlanceModifier.fillMaxWidth().rounded(WColors.Raised).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Dot(WColors.Amber, 5.dp)
            Spacer(GlanceModifier.width(6.dp))
            Mono(text, WColors.TextDim, 10.sp)
        }
    }
}

class DailyClutterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyClutterWidget()
}
