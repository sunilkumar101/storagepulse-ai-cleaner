package com.storagepulse.cleaner.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.storage.model.StorageCategory
import com.storagepulse.cleaner.data.widgets.WidgetData
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.Indigo
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextMuted
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.widgets.formatGb

private val legend = listOf(R.string.wg_cat_videos, R.string.wg_cat_photos, R.string.wg_cat_wa, R.string.wg_cat_apps, R.string.wg_cat_sys)

/** Compose twin of the 4×2 Glance widget, fed by the same [WidgetData]. */
@Composable
fun BoostWidgetPreview(d: WidgetData) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().obsidianCard(20).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MonitorHeart, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            AutoSizeText(stringResource(R.string.app_short_name), MaterialTheme.typography.titleSmall, TextPrimary, Modifier.weight(1f), minSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            Row(Modifier.background(SurfaceRaised, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(AmberLight, CircleShape))
                Spacer(Modifier.width(4.dp))
                AutoSizeText(stringResource(R.string.wg_used_of, formatGb(d.usedBytes), formatGb(d.totalBytes), d.usedPercent), Telemetry.labelCaps, AmberLight, minSize = 7.sp)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceStroke)) {
            val barWidth = maxWidth
            Row {
                if (d.hasScan && d.totalBytes > 0) {
                    StorageCategory.entries.forEach { c ->
                        val w = barWidth * ((d.categoryBytes[c] ?: 0L).toFloat() / d.totalBytes)
                        Box(Modifier.width(w).height(8.dp).background(Color(c.colorArgb)))
                    }
                } else if (d.totalBytes > 0) {
                    Box(Modifier.width(barWidth * (d.usedBytes.toFloat() / d.totalBytes)).height(8.dp).background(accent))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            StorageCategory.entries.forEachIndexed { i, c ->
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(Color(c.colorArgb), CircleShape))
                    Spacer(Modifier.width(3.dp))
                    AutoSizeText(stringResource(legend[i]), Telemetry.sm.copy(fontSize = 10.sp), TextSecondary, minSize = 7.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wg_recoverable), style = Telemetry.labelCaps, color = TextMuted, maxLines = 1)
                AutoSizeText(
                    if (d.hasScan) stringResource(R.string.wg_ready, formatBytes(d.recoverableBytes)) else stringResource(R.string.wg_scan_needed),
                    Telemetry.md.copy(fontSize = 15.sp),
                    accent,
                    minSize = 9.sp,
                )
            }
            Row(Modifier.background(accent, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (d.hasScan && d.recoverableBytes > 0) stringResource(R.string.wg_boost, formatBytes(d.recoverableBytes)) else stringResource(R.string.wg_clean),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Compose twin of the 2×2 Glance widget. */
@Composable
fun ClutterWidgetPreview(d: WidgetData) {
    Column(Modifier.fillMaxWidth().obsidianCard(20).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wg_clutter_caps), style = Telemetry.labelCaps, color = TextMuted)
                AutoSizeText(formatBytes(d.clutterBytes), Telemetry.md.copy(fontSize = 24.sp), TextPrimary, minSize = 12.sp)
            }
            Box(Modifier.background(SurfaceRaised, RoundedCornerShape(10.dp)).padding(6.dp)) {
                Icon(Icons.Outlined.DeleteSweep, null, tint = AmberLight, modifier = Modifier.size(18.dp))
            }
        }
        PreviewLine(pluralStringResource(R.plurals.wg_screenshots, d.screenshotCount, d.screenshotCount))
        PreviewLine(pluralStringResource(R.plurals.wg_heavy_videos, d.heavyVideoCount, d.heavyVideoCount))
        Row(Modifier.fillMaxWidth().background(Indigo, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.wg_swipe), style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PreviewLine(text: String) {
    Row(Modifier.fillMaxWidth().background(SurfaceRaised, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).background(AmberLight, CircleShape))
        Spacer(Modifier.width(8.dp))
        AutoSizeText(text, Telemetry.sm, TextSecondary, minSize = 8.sp)
    }
}

/** Compose twin of the 4×1 Glance strip. */
@Composable
fun StripWidgetPreview(d: WidgetData) {
    val accent = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().obsidianCard(20).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        StripStat(stringResource(R.string.wg_free_caps), formatBytes(d.freeBytes), accent, Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(28.dp).background(SurfaceStroke))
        Spacer(Modifier.width(10.dp))
        StripStat(stringResource(R.string.wg_trash_caps), formatBytes(d.trashBytes), AmberLight, Modifier.weight(1f))
        Row(Modifier.background(SurfaceRaised, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Refresh, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.wg_rescan), style = MaterialTheme.typography.labelLarge, color = accent, maxLines = 1)
        }
    }
}

@Composable
private fun StripStat(label: String, value: String, dot: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(dot, CircleShape))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = Telemetry.labelCaps, color = TextMuted, maxLines = 1)
            AutoSizeText(value, Telemetry.md, TextPrimary, minSize = 9.sp)
        }
    }
}
