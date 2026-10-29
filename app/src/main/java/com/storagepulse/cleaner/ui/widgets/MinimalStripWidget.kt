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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.ui.navigation.Routes
import com.storagepulse.cleaner.ui.util.formatBytes

/** 4×1 "Minimal Dock Strip": free space | Safe Trash and Rescan. Free for everyone. */
class MinimalStripWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent { Content(state) }
    }

    @Composable
    private fun Content(s: WidgetState) {
        val context = LocalContext.current
        val (accent, _) = accentColors(s.accent)
        val d = s.data
        Row(
            GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(actionStartActivity(openRouteIntent(context, Routes.DASHBOARD))),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Stat(context.getString(R.string.wg_free_caps), formatBytes(d.freeBytes), accent, GlanceModifier.defaultWeight())
            Box(GlanceModifier.width(1.dp).fillMaxHeight().background(fixed(WColors.Stroke))) {}
            Spacer(GlanceModifier.width(10.dp))
            Stat(context.getString(R.string.wg_trash_caps), formatBytes(d.trashBytes), WColors.Amber, GlanceModifier.defaultWeight())
            Row(
                GlanceModifier.rounded(WColors.Raised).padding(horizontal = 10.dp, vertical = 7.dp).clickable(actionRunCallback<RescanAction>()),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Icon(R.drawable.ic_widget_refresh, accent, 14.dp)
                Spacer(GlanceModifier.width(4.dp))
                Label(context.getString(R.string.wg_rescan), accent, 12.sp, bold = true)
            }
        }
    }

    @Composable
    private fun Stat(label: String, value: String, dot: androidx.compose.ui.graphics.Color, modifier: GlanceModifier) {
        Row(modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
            Dot(dot)
            Spacer(GlanceModifier.width(6.dp))
            Column {
                Mono(label, WColors.Muted, 9.sp)
                Mono(value, WColors.Text, 13.sp, bold = true)
            }
        }
    }
}

class MinimalStripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MinimalStripWidget()
}
