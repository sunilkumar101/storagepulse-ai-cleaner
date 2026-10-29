package com.storagepulse.cleaner.ui.screens.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.outlined.CheckCircle
import com.storagepulse.cleaner.ui.components.NativeAdCard
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.apps.AppSpace
import com.storagepulse.cleaner.data.trash.TrashItemEntity
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.screens.treemap.ageText
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.AppsCyan
import com.storagepulse.cleaner.ui.theme.DestructiveRed
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.Indigo
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.OnDestructive
import com.storagepulse.cleaner.ui.theme.OnEmeraldLight
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.SurfaceSunken
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextMuted
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.theme.VideoRose
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatCount
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Screen 9 — App Space & Safe 30-Day Recycle Bin.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/safe_recycle_bin_unused_apps_analyzer/
 */
@Composable
fun SafeTrashAndAppsScreen(viewModel: SafeTrashAndAppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    val totals by viewModel.trashTotals.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }
    var showAllTrash by rememberSaveable { mutableStateOf(false) }
    var celebrate by rememberSaveable { mutableStateOf<Long?>(null) }
    val adsEnabled by viewModel.adsEnabled.collectAsStateWithLifecycle()
    val activity = androidx.activity.compose.LocalActivity.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val text = when (val m = message) {
        is ToolsMessage.Restored -> androidx.compose.ui.res.pluralStringResource(R.plurals.tl_msg_restored, m.n, m.n)
        is ToolsMessage.Emptied -> androidx.compose.ui.res.pluralStringResource(R.plurals.tl_msg_emptied, m.n, m.n)
        is ToolsMessage.Installers -> androidx.compose.ui.res.pluralStringResource(R.plurals.tl_msg_installers, m.n, m.n)
        null -> null
    }
    LaunchedEffect(message) {
        val m = message
        if (m is ToolsMessage.Emptied && m.bytes > SafeTrashAndAppsViewModel.BIG_CLEANUP_BYTES) {
            // Big clean-up: a celebration first; the (capped) interstitial only after the user closes it.
            celebrate = m.bytes
            viewModel.consumeMessage()
        } else if (text != null) {
            snackbar.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Box(
        Modifier.fillMaxSize().background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(onRescan = viewModel::refresh)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { Tabs(state.tab, totals.bytes, viewModel::setTab) }
                    item { TotalCard(state.unusedBytes + state.installerBytes, totals.bytes) }
                    if (state.tab == ToolsTab.APPS) {
                        if (!state.usageAccess) {
                            item { UsageAccessCard { context.startActivity(viewModel.usageAccessIntent()) } }
                        } else {
                            item { AppFilters(state, viewModel::setFilter) }
                            item { AppsHeader(state) }
                            if (state.appsLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Emerald, trackColor = SurfaceStroke) }
                            if (!state.appsLoading && state.shownApps.isEmpty()) item { Text(stringResource(R.string.tl_no_unused), color = TextSecondary) }
                            state.shownApps.forEachIndexed { i, app ->
                                item(key = app.packageName) {
                                    AppCard(
                                        app,
                                        onClearCache = { context.startActivity(viewModel.appInfoIntent(app.packageName)) },
                                        onUninstall = { context.startActivity(viewModel.uninstallIntent(app.packageName)) },
                                    )
                                }
                                // One native ad card after the 3rd app (free users only).
                                if (i == 2 && adsEnabled) item(key = "native_ad") { NativeAdCard(viewModel.ads, adsEnabled) }
                            }
                        }
                        item { InstallersCard(state, viewModel::trashInstallers) }
                    } else {
                        item {
                            TrashSection(
                                items = if (showAllTrash) trashItems else trashItems.take(8),
                                total = trashItems.size,
                                bytes = totals.bytes,
                                busy = state.busy,
                                onRestore = viewModel::restore,
                                onRestoreAll = viewModel::restoreAll,
                                onEmpty = { confirmEmpty = true },
                                onShowAll = { showAllTrash = true },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    celebrate?.let { bytes ->
        AlertDialog(
            onDismissRequest = { celebrate = null; activity?.let(viewModel::afterBigCleanup) },
            containerColor = SurfaceCard,
            icon = { Icon(Icons.Outlined.CheckCircle, null, tint = Emerald) },
            title = { Text(stringResource(R.string.tl_celebrate_title, formatBytes(bytes)), color = TextPrimary) },
            text = { Text(stringResource(R.string.tl_celebrate_body), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { celebrate = null; activity?.let(viewModel::afterBigCleanup) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = OnEmeraldLight),
                ) { Text(stringResource(R.string.tl_celebrate_ok)) }
            },
        )
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            containerColor = SurfaceCard,
            icon = { Icon(Icons.Outlined.Warning, null, tint = DestructiveRed) },
            title = { Text(stringResource(R.string.tl_empty_title, formatBytes(totals.bytes)), color = TextPrimary) },
            text = { Text(androidx.compose.ui.res.pluralStringResource(R.plurals.tl_empty_body, totals.count, formatCount(totals.count)), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { confirmEmpty = false; viewModel.emptyTrash() },
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive),
                ) { Text(stringResource(R.string.tl_empty_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text(stringResource(R.string.purge_cancel), color = TextPrimary) } },
        )
    }
}

@Composable
private fun Header(onRescan: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Emerald, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tl_title), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.tl_subtitle), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onRescan) { Icon(Icons.Outlined.Refresh, stringResource(R.string.tl_rescan), tint = TextPrimary) }
        }
        HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
    }
}

@Composable
private fun Tabs(tab: ToolsTab, trashBytes: Long, onTab: (ToolsTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(16.dp)).border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(ToolsTab.APPS to R.string.tl_tab_apps, ToolsTab.TRASH to R.string.tl_tab_trash).forEach { (t, label) ->
            val on = t == tab
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) SurfaceRaised else Color.Transparent).clickable { onTab(t) }.heightIn(min = 48.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(if (t == ToolsTab.APPS) Icons.Outlined.Apps else Icons.Outlined.DeleteSweep, null, tint = if (on) EmeraldLight else TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                AutoSizeText(stringResource(label), MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), if (on) TextPrimary else TextSecondary, Modifier.weight(1f, fill = false), minSize = 10.sp)
                if (t == ToolsTab.TRASH && trashBytes > 0) {
                    Spacer(Modifier.width(6.dp))
                    TelemetryBadge(formatBytes(trashBytes), AmberLight)
                }
            }
        }
    }
}

@Composable
private fun TotalCard(appsAndInstallers: Long, trash: Long) {
    val total = appsAndInstallers + trash
    Column(Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tl_total_title), style = Telemetry.labelCaps, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AutoSizeText(formatBytes(total), Telemetry.xl.copy(fontSize = 30.sp), EmeraldLight, minSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    TelemetryBadge(stringResource(R.string.tl_ready), EmeraldLight)
                }
            }
            Box(Modifier.size(44.dp).background(Emerald.copy(alpha = 0.14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Speed, null, tint = EmeraldLight)
            }
        }
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(SurfaceStroke), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val t = total.coerceAtLeast(1).toFloat()
            if (appsAndInstallers > 0) Box(Modifier.weight(appsAndInstallers / t).fillMaxHeight().background(Brush.horizontalGradient(listOf(Indigo, AmberLight))))
            if (trash > 0) Box(Modifier.weight(trash / t).fillMaxHeight().background(VideoRose))
        }
        Row {
            LegendDot(AmberLight, stringResource(R.string.tl_legend_apps, formatBytes(appsAndInstallers)), Modifier.weight(1f))
            LegendDot(VideoRose, stringResource(R.string.tl_legend_trash, formatBytes(trash)), Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            AutoSizeText(stringResource(R.string.tl_api_note), Telemetry.sm, TextSecondary, minSize = 8.sp)
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

// ------------------------------------------------------------------ apps

@Composable
private fun UsageAccessCard(onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().obsidianCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.tl_usage_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(stringResource(R.string.tl_usage_body), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = OnEmeraldLight)) {
            Text(stringResource(R.string.tl_usage_cta))
        }
    }
}

@Composable
private fun AppFilters(state: ToolsUiState, onFilter: (AppFilter) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(stringResource(R.string.tl_chip_unused, formatBytes(state.unusedBytes)), state.filter == AppFilter.UNUSED_30D, Modifier.weight(1f)) { onFilter(AppFilter.UNUSED_30D) }
        Chip(stringResource(R.string.tl_chip_all, formatBytes(state.apps.sumOf { it.totalBytes })), state.filter == AppFilter.ALL, Modifier.weight(1f)) { onFilter(AppFilter.ALL) }
    }
}

@Composable
private fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(if (on) EmeraldLight else SurfaceCard).border(1.dp, if (on) EmeraldLight else SurfaceStroke, RoundedCornerShape(50)).clickable(onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        AutoSizeText(text, Telemetry.sm, if (on) OnEmeraldLight else TextPrimary, minSize = 8.sp)
    }
}

@Composable
private fun AppsHeader(state: ToolsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (state.filter == AppFilter.UNUSED_30D) R.string.tl_unused_title else R.string.tl_all_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            val list = state.shownApps
            TelemetryBadge(androidx.compose.ui.res.pluralStringResource(R.plurals.tl_unused_badge, list.size, list.size, formatBytes(list.sumOf { it.totalBytes })), AmberLight)
        }
        Text(stringResource(R.string.tl_unused_sub), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun AppCard(app: AppSpace, onClearCache: () -> Unit, onUninstall: () -> Unit) {
    val days = app.unusedDays()
    Column(Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceRaised), contentAlignment = Alignment.Center) {
                app.icon?.let { Image(it.asImageBitmap(), null, modifier = Modifier.size(40.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.tl_app_meta, app.versionName ?: "?", formatBytes(app.appBytes), formatBytes(app.dataBytes), formatBytes(app.cacheBytes)),
                    style = Telemetry.sm,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(formatBytes(app.totalBytes), style = Telemetry.lg, color = VideoRose, maxLines = 1)
        }
        val badge = when {
            app.lastUsed == 0L -> androidx.compose.ui.res.pluralStringResource(R.plurals.tl_never_opened, days.toInt(), days.toInt())
            days >= ToolsUiState.UNUSED_DAYS -> stringResource(R.string.tl_unused_for, days.toInt())
            else -> stringResource(R.string.tl_used_recently, ageText(app.lastUsed))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = AmberLight, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            TelemetryBadge(badge, if (days >= ToolsUiState.UNUSED_DAYS) AmberLight else EmeraldLight)
        }
        // App binaries vs data split
        Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(SurfaceStroke)) {
            val t = app.totalBytes.coerceAtLeast(1).toFloat()
            if (app.appBytes > 0) Box(Modifier.weight(app.appBytes / t).fillMaxHeight().background(IndigoLight))
            if (app.dataBytes > 0) Box(Modifier.weight(app.dataBytes / t).fillMaxHeight().background(AmberLight))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onClearCache,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = TextPrimary),
            ) {
                Icon(Icons.Outlined.CleaningServices, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                AutoSizeText(stringResource(R.string.tl_clear_cache, formatBytes(app.cacheBytes)), MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), TextPrimary, minSize = 9.sp)
            }
            Button(
                onClick = onUninstall,
                modifier = Modifier.weight(1.2f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                // Bright white on #EF4444, per the destructive-action rule.
                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive),
            ) {
                Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                AutoSizeText(stringResource(R.string.tl_uninstall, formatBytes(app.totalBytes)), MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), OnDestructive, minSize = 9.sp)
            }
        }
    }
}

@Composable
private fun InstallersCard(state: ToolsUiState, onTrash: () -> Unit) {
    val list = state.installers
    Column(Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(AppsCyan.copy(alpha = 0.16f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null, tint = AppsCyan)
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.tl_installers_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(formatBytes(state.installerBytes), style = Telemetry.lg, color = AppsCyan)
        }
        if (list.isEmpty()) {
            Text(stringResource(R.string.tl_installers_none), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            return@Column
        }
        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.tl_installers_body, list.size, list.size), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        val installed = list.count { it.alreadyInstalled }
        if (installed > 0) TelemetryBadge(stringResource(R.string.tl_installers_installed, installed), DestructiveRed)
        list.take(4).forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.name, style = Telemetry.sm, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(formatBytes(f.sizeBytes), style = Telemetry.sm, color = TextSecondary)
            }
        }
        Button(
            onClick = onTrash,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive),
        ) {
            Icon(Icons.Outlined.DeleteSweep, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            AutoSizeText(androidx.compose.ui.res.pluralStringResource(R.plurals.tl_installers_cta, list.size, list.size, formatBytes(state.installerBytes)), MaterialTheme.typography.labelLarge, OnDestructive, minSize = 10.sp)
        }
    }
}

// ------------------------------------------------------------------ trash

@Composable
private fun TrashSection(
    items: List<TrashItemEntity>,
    total: Int,
    bytes: Long,
    busy: Boolean,
    onRestore: (Long) -> Unit,
    onRestoreAll: () -> Unit,
    onEmpty: () -> Unit,
    onShowAll: () -> Unit,
) {
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tl_trash_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(androidx.compose.ui.res.pluralStringResource(R.plurals.tl_trash_meta, total, formatBytes(bytes), formatCount(total)), style = Telemetry.sm, color = VideoRose)
            }
            items.minOfOrNull { it.expiresAt }?.let { soonest ->
                Column(horizontalAlignment = Alignment.End) {
                    TelemetryBadge(daysLeft(soonest, now).let { d -> androidx.compose.ui.res.pluralStringResource(R.plurals.tl_trash_next, d, d) }, AmberLight)
                    Text(stringResource(R.string.tl_trash_next_sub), style = Telemetry.sm, color = TextSecondary)
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(SurfaceSunken, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tl_trash_note), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        if (total == 0) {
            Text(stringResource(R.string.tl_trash_empty), color = TextSecondary)
            return@Column
        }
        Text(stringResource(R.string.tl_trash_recent), style = Telemetry.labelCaps, color = TextSecondary)
        items.forEach { item ->
            val exists = File(item.quarantinePath).exists()
            val left = daysLeft(item.expiresAt, now)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null, tint = IndigoLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.fileName, style = Telemetry.sm, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                    Text(
                        when {
                            !exists -> stringResource(R.string.tl_trash_missing, formatBytes(item.sizeBytes))
                            left <= 0 -> stringResource(R.string.tl_trash_item_today, formatBytes(item.sizeBytes))
                            else -> androidx.compose.ui.res.pluralStringResource(R.plurals.tl_trash_item, left, formatBytes(item.sizeBytes), left)
                        },
                        style = Telemetry.sm,
                        color = TextSecondary,
                    )
                }
                TextButton(onClick = { onRestore(item.id) }, enabled = !busy && exists) {
                    Text(stringResource(R.string.tl_restore), color = if (exists) EmeraldLight else TextMuted)
                }
            }
        }
        if (items.size < total) {
            TextButton(onClick = onShowAll) { Text(stringResource(R.string.tl_show_more, total), color = EmeraldLight) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onRestoreAll,
                enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = EmeraldLight),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Undo, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                AutoSizeText(stringResource(R.string.tl_restore_all), MaterialTheme.typography.labelLarge, EmeraldLight, minSize = 10.sp)
            }
            Button(
                onClick = onEmpty,
                enabled = !busy,
                modifier = Modifier.weight(1.2f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive),
            ) {
                AutoSizeText(stringResource(R.string.tl_empty, formatBytes(bytes)), MaterialTheme.typography.labelLarge, OnDestructive, minSize = 10.sp)
            }
        }
    }
}

/** Rounded up: a file trashed a minute ago still has "30 days left", not 29. */
private fun daysLeft(expiresAt: Long, now: Long): Int {
    val ms = (expiresAt - now).coerceAtLeast(0)
    val day = TimeUnit.DAYS.toMillis(1)
    return ((ms + day - 1) / day).toInt()
}

