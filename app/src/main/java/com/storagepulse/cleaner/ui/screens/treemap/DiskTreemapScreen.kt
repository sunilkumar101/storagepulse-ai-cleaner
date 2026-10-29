package com.storagepulse.cleaner.ui.screens.treemap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.storage.StorageAccess
import com.storagepulse.cleaner.data.storage.model.ClutterKind
import com.storagepulse.cleaner.data.storage.model.CleanupBreakdown
import com.storagepulse.cleaner.data.storage.model.DuplicateGroup
import com.storagepulse.cleaner.data.storage.model.ScanState
import com.storagepulse.cleaner.data.storage.model.StorageNode
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
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
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatCount
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Screen 3 — Interactive DiskUsage Treemap / Sunburst Explorer.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/interactive_diskusage_treemap_sunburst_explorer/
 */
@Composable
fun DiskTreemapScreen(
    onCompressFolder: (String) -> Unit,
    viewModel: TreemapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var previewPath by rememberSaveable { mutableStateOf<String?>(null) }

    // Back gesture zooms out until the storage root.
    BackHandler(enabled = state.canZoomOut) { viewModel.zoomOut() }

    // Came back from granting All Files Access: map the whole disk.
    LifecycleResumeEffect(state.scan) {
        val done = state.scan as? ScanState.Done
        if (done != null && done.snapshot.root == null && StorageAccess.hasAllFilesAccess(context)) {
            viewModel.rescan()
        }
        onPauseOrDispose { }
    }

    val purge = state.purge
    if (purge is PurgeState.Finished) {
        val r = purge.result
        val moved = pluralStringResource(R.plurals.purge_done, r.movedFiles, formatCount(r.movedFiles), formatBytes(r.movedBytes))
        val msg = if (r.skippedFiles > 0) {
            moved + " " + pluralStringResource(R.plurals.purge_skipped, r.skippedFiles, r.skippedFiles)
        } else {
            moved
        }
        LaunchedEffect(purge) {
            // Show first: resetting the state first would cancel this effect.
            snackbar.showSnackbar(msg)
            viewModel.dismissPurge()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.fillMaxSize()) {
            TreemapHeader(scanning = state.scan is ScanState.Scanning, onRescan = viewModel::rescan)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val root = state.root
                    val current = state.current
                    when {
                        root != null && current != null -> {
                            BreadcrumbBar(
                                root = root,
                                current = current,
                                onGoTo = viewModel::goTo,
                                onUp = { viewModel.zoomOut() },
                            )
                            ModeToggle(mode = state.mode, onMode = viewModel::setMode)
                            MapCard(
                                state = state,
                                onSelect = viewModel::select,
                                onDrill = viewModel::drillInto,
                                onZoomOut = { viewModel.zoomOut() },
                                onReset = { viewModel.goTo(root.path) },
                                onOther = { folder -> previewPath = folder },
                            )
                            state.selected?.let { sel ->
                                InspectorCard(
                                    node = sel,
                                    state = state,
                                    onView = { previewPath = sel.path },
                                    onCompress = {
                                        onCompressFolder(if (sel.isDirectory) sel.path else sel.path.substringBeforeLast('/'))
                                    },
                                    onPurge = viewModel::requestPurge,
                                )
                            }
                            if (state.selectedDuplicates.isNotEmpty()) {
                                DuplicateVictims(
                                    groups = state.selectedDuplicates,
                                    onOpen = { path -> previewPath = path.substringBeforeLast('/') },
                                )
                            }
                        }
                        state.scan is ScanState.Done -> NeedsFullAccessCard(
                            onAllow = {
                                StorageAccess.allFilesAccessIntents(context).any { intent ->
                                    runCatching { context.startActivity(intent) }.isSuccess
                                }
                            },
                        )
                        state.scan is ScanState.Failed -> StatusCard(stringResource(R.string.tm_failed))
                        else -> StatusCard(
                            stringResource(
                                R.string.tm_scanning,
                                formatCount((state.scan as? ScanState.Scanning)?.filesSeen ?: 0),
                            ),
                            progress = true,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }

    when (purge) {
        is PurgeState.Confirm -> PurgeConfirmDialog(
            breakdown = purge.breakdown,
            onConfirm = viewModel::confirmPurge,
            onDismiss = viewModel::dismissPurge,
        )
        is PurgeState.Working -> PurgeProgressDialog(purge.done, purge.total)
        else -> Unit
    }

    previewPath?.let { path ->
        FolderPreviewSheet(
            path = path,
            title = if (path == state.root?.path) stringResource(R.string.tm_root_name) else File(path).name,
            sizeOf = { p -> state.root?.let { com.storagepulse.cleaner.data.storage.StorageTree.find(it, p)?.sizeBytes } },
            onOpenFolder = { folder ->
                previewPath = null
                state.root?.let { com.storagepulse.cleaner.data.storage.StorageTree.find(it, folder) }
                    ?.let(viewModel::drillInto)
            },
            onDismiss = { previewPath = null },
        )
    }
}

// ------------------------------------------------------------------ header & navigation

@Composable
private fun TreemapHeader(scanning: Boolean, onRescan: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(Emerald, CircleShape))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.dash_brand),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    maxLines = 1,
                )
                Text(stringResource(R.string.tm_subtitle), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onRescan, enabled = !scanning) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.dash_rescan),
                    tint = if (scanning) TextMuted else TextPrimary,
                )
            }
        }
        if (scanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Emerald, trackColor = SurfaceStroke)
        } else {
            HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
        }
    }
}

@Composable
private fun BreadcrumbBar(
    root: StorageNode,
    current: StorageNode,
    onGoTo: (String) -> Unit,
    onUp: () -> Unit,
) {
    // The root ("/storage/emulated/0") is one short crumb; deeper segments jump to themselves.
    val rootLabel = stringResource(R.string.tm_root_crumb)
    val crumbs: List<Pair<String, String>> = buildList {
        add(rootLabel to root.path)
        var acc = root.path
        current.path.removePrefix(root.path).split('/').filter { it.isNotEmpty() }.forEach { seg ->
            acc = "$acc/$seg"
            add(seg to acc)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(current.path) { listState.animateScrollToItem((crumbs.size - 1).coerceAtLeast(0)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Plain 48dp box: an IconButton with a smaller size() drew its background over the crumbs.
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Emerald.copy(alpha = 0.12f))
                .clickable(role = androidx.compose.ui.semantics.Role.Button) { onGoTo(root.path) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SdStorage, stringResource(R.string.tm_root_jump), tint = EmeraldLight)
        }
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(crumbs) { index, (label, path) ->
                val last = index == crumbs.lastIndex
                if (index > 0) Text("/", style = Telemetry.md, color = TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = label,
                    style = Telemetry.md,
                    color = if (last) EmeraldLight else TextSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .then(if (last) Modifier.background(SurfaceRaised) else Modifier)
                        .clickable { onGoTo(path) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        TelemetryBadge(text = formatBytes(current.sizeBytes), color = EmeraldLight)
        IconButton(onClick = onUp, enabled = current.path != root.path) {
            Icon(
                Icons.AutoMirrored.Outlined.DriveFileMove,
                stringResource(R.string.tm_up),
                tint = if (current.path != root.path) TextPrimary else TextMuted,
            )
        }
    }
}

@Composable
private fun ModeToggle(mode: VisualMode, onMode: (VisualMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(50))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        ModeButton(Icons.Outlined.GridView, stringResource(R.string.tm_mode_blocks), mode == VisualMode.TREEMAP, Modifier.weight(1f)) {
            onMode(VisualMode.TREEMAP)
        }
        ModeButton(Icons.Outlined.PieChart, stringResource(R.string.tm_mode_rings), mode == VisualMode.SUNBURST, Modifier.weight(1f)) {
            onMode(VisualMode.SUNBURST)
        }
    }
}

@Composable
private fun ModeButton(icon: ImageVector, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) EmeraldLight else Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (active) OnEmeraldLight else TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        AutoSizeText(
            label,
            MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            if (active) OnEmeraldLight else TextSecondary,
            minSize = 10.sp,
        )
    }
}

// ------------------------------------------------------------------ map

@Composable
private fun MapCard(
    state: TreemapUiState,
    onSelect: (StorageNode) -> Unit,
    onDrill: (StorageNode) -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onOther: (String) -> Unit,
) {
    val current = state.current ?: return
    val other = stringResource(R.string.tm_other)
    val centerLabel = if (current.path == state.root?.path) stringResource(R.string.tm_root_name) else current.name
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tm_map_title), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.tm_scale), style = Telemetry.sm, color = TextMuted, maxLines = 1)
            IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.CenterFocusStrong, stringResource(R.string.tm_root_jump), tint = TextSecondary)
            }
        }
        Crossfade(targetState = state.mode, label = "mode") { mode ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .obsidianCard(fill = SurfaceSunken)
                    .padding(6.dp),
            ) {
                when (mode) {
                    VisualMode.TREEMAP -> TreemapCanvas(
                        node = current,
                        selectedPath = state.selected?.path,
                        otherLabel = other,
                        onSelect = onSelect,
                        onDrill = onDrill,
                        onOther = onOther,
                        modifier = Modifier.fillMaxSize(),
                    )
                    VisualMode.SUNBURST -> SunburstCanvas(
                        node = current,
                        selectedPath = state.selected?.path,
                        otherLabel = other,
                        onSelect = onSelect,
                        onDrill = onDrill,
                        onZoomOut = onZoomOut,
                        onOther = onOther,
                        centerLabel = centerLabel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Text(
            stringResource(if (state.mode == VisualMode.TREEMAP) R.string.tm_hint else R.string.tm_hint_rings),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    }
}

// ------------------------------------------------------------------ inspector

@Composable
private fun InspectorCard(
    node: StorageNode,
    state: TreemapUiState,
    onView: () -> Unit,
    onCompress: () -> Unit,
    onPurge: () -> Unit,
) {
    val clutterBytes = state.clutterBytes
    val breakdown = CleanupBreakdown.from(state.selectedClutter)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(SurfaceCard, SurfaceSunken)))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.FolderOpen, null, tint = EmeraldLight, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = node.path,
                style = Telemetry.md,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TelemetryBadge(
                text = stringResource(if (clutterBytes > 0) R.string.tm_safe_to_purge else R.string.tm_no_clutter),
                color = if (clutterBytes > 0) EmeraldLight else TextMuted,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(SurfaceSunken, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Stat(stringResource(R.string.tm_wasted), formatBytes(clutterBytes), Telemetry.lg, EmeraldLight, Modifier.weight(1.1f))
            Stat(
                stringResource(R.string.tm_payload),
                if (node.fileCount == 1) stringResource(R.string.tm_one_file) else stringResource(R.string.tm_files, formatCount(node.fileCount)),
                Telemetry.md, TextPrimary, Modifier.weight(1f),
            )
            Stat(stringResource(R.string.tm_last_ingestion), ageText(node.lastModified), Telemetry.md, TextPrimary, Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, null, tint = AmberLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(explainClutter(breakdown), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                icon = Icons.Outlined.Visibility,
                label = stringResource(R.string.tm_view),
                container = SurfaceRaised,
                content = TextPrimary,
                onClick = onView,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                icon = Icons.Outlined.Compress,
                label = stringResource(R.string.tm_compress),
                container = Indigo.copy(alpha = 0.28f),
                content = IndigoLight,
                onClick = onCompress,
                modifier = Modifier.weight(1.1f),
            )
            ActionButton(
                icon = Icons.Outlined.Delete,
                label = if (clutterBytes > 0) stringResource(R.string.tm_purge, formatBytes(clutterBytes)) else stringResource(R.string.tm_purge_none),
                // High-contrast destructive style: bright white on #EF4444.
                container = DestructiveRed,
                content = OnDestructive,
                enabled = clutterBytes > 0,
                onClick = onPurge,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueStyle: androidx.compose.ui.text.TextStyle, color: Color, modifier: Modifier) {
    Column(modifier) {
        AutoSizeText(label, Telemetry.labelCaps, TextSecondary, minSize = 7.sp)
        Spacer(Modifier.height(4.dp))
        AutoSizeText(value, valueStyle, color, minSize = 10.sp)
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = SurfaceRaised,
            disabledContentColor = TextMuted,
        ),
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        AutoSizeText(
            label,
            MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            if (enabled) content else TextMuted,
            minSize = 9.sp,
        )
    }
}

@Composable
private fun explainClutter(b: CleanupBreakdown): String {
    if (b.totalFiles == 0) return stringResource(R.string.tm_explain_none)
    val parts = buildList {
        if (b.whatsAppSentFiles > 0) add(pluralStringResource(R.plurals.tm_explain_sent, b.whatsAppSentFiles, b.whatsAppSentFiles))
        if (b.duplicateFiles > 0) add(pluralStringResource(R.plurals.tm_explain_dup, b.duplicateFiles, b.duplicateFiles))
        if (b.apkInstallerFiles > 0) add(pluralStringResource(R.plurals.tm_explain_apk, b.apkInstallerFiles, b.apkInstallerFiles))
        if (b.thumbnailFiles > 0) add(pluralStringResource(R.plurals.tm_explain_thumb, b.thumbnailFiles, b.thumbnailFiles))
    }
    return parts.joinToString(" · ")
}

@Composable
fun ageText(millis: Long): String {
    if (millis <= 0) return "—"
    val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> stringResource(R.string.tm_age_now)
        hours < 1 -> stringResource(R.string.tm_age_minutes, minutes.toInt())
        days < 1 -> stringResource(R.string.tm_age_hours, hours.toInt())
        days < 31 -> stringResource(R.string.tm_age_days, days.toInt())
        days < 365 -> stringResource(R.string.tm_age_months, (days / 30).toInt())
        else -> stringResource(R.string.tm_age_years, (days / 365).toInt())
    }
}

// ------------------------------------------------------------------ duplicates strip

@Composable
private fun DuplicateVictims(groups: List<DuplicateGroup>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.History, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.tm_dups_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(pluralStringResource(R.plurals.tm_dups_count, groups.size, groups.size), style = Telemetry.labelCaps, color = EmeraldLight)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(groups, key = { it.keeper }) { g ->
                val copy = g.extraCopies.first()
                Column(
                    Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .obsidianCard()
                        .clickable { onOpen(copy) }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceRaised)) {
                        AsyncImage(
                            model = File(g.keeper),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        TelemetryBadge(
                            text = formatBytes(g.wastedBytes),
                            color = EmeraldLight,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(ObsidianCanvas.copy(alpha = 0.7f), RoundedCornerShape(50)),
                        )
                    }
                    Text(
                        File(copy).name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(stringResource(R.string.tm_dups_copies, g.paths.size), style = Telemetry.sm, color = TextSecondary, maxLines = 1)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ states & dialogs

@Composable
private fun StatusCard(text: String, progress: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().obsidianCard().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        if (progress) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Emerald, trackColor = SurfaceStroke)
    }
}

@Composable
private fun NeedsFullAccessCard(onAllow: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().obsidianCard().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tm_needs_full_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(stringResource(R.string.tm_needs_full_body), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Button(
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = OnEmeraldLight),
        ) { Text(stringResource(R.string.tm_needs_full_cta), style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun PurgeConfirmDialog(breakdown: CleanupBreakdown, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text(stringResource(R.string.purge_title, formatBytes(breakdown.totalBytes)), color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pluralStringResource(R.plurals.purge_body, breakdown.totalFiles, formatCount(breakdown.totalFiles)), color = TextSecondary)
                listOf(
                    ClutterKind.WHATSAPP_SENT to R.plurals.tm_explain_sent,
                    ClutterKind.DUPLICATE to R.plurals.tm_explain_dup,
                    ClutterKind.APK_INSTALLER to R.plurals.tm_explain_apk,
                    ClutterKind.THUMBNAIL to R.plurals.tm_explain_thumb,
                ).forEach { (kind, res) ->
                    val n = when (kind) {
                        ClutterKind.WHATSAPP_SENT -> breakdown.whatsAppSentFiles
                        ClutterKind.DUPLICATE -> breakdown.duplicateFiles
                        ClutterKind.APK_INSTALLER -> breakdown.apkInstallerFiles
                        ClutterKind.THUMBNAIL -> breakdown.thumbnailFiles
                    }
                    if (n > 0) Text("• " + pluralStringResource(res, n, n), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
                Text(stringResource(R.string.purge_note), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive),
            ) { Text(stringResource(R.string.purge_confirm), maxLines = 1) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.purge_cancel), color = TextPrimary) }
        },
    )
}

@Composable
private fun PurgeProgressDialog(done: Int, total: Int) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfaceCard,
        confirmButton = {},
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.purge_working, done, total), color = TextPrimary)
                LinearProgressIndicator(
                    progress = { if (total > 0) done.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Emerald,
                    trackColor = SurfaceStroke,
                )
            }
        },
    )
}
