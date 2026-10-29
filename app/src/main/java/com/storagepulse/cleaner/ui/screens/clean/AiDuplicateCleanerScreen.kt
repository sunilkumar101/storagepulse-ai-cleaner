package com.storagepulse.cleaner.ui.screens.clean

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.dedup.ClusterType
import com.storagepulse.cleaner.data.dedup.DedupState
import com.storagepulse.cleaner.data.dedup.MediaCluster
import com.storagepulse.cleaner.data.dedup.MediaItem
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.DestructiveRed
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.OnDestructive
import com.storagepulse.cleaner.ui.theme.OnEmeraldLight
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextMuted
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.theme.VideoRose
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TILES_COLLAPSED = 6

/**
 * Screen 4 — AI Duplicate & Blurry Photo Cleaner.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/ai_duplicate_blurry_photo_cleaner/
 */
@Composable
fun AiDuplicateCleanerScreen(viewModel: AiCleanViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var loupeFor by remember { mutableStateOf<MediaCluster?>(null) }

    val activity = androidx.activity.compose.LocalActivity.current
    val adsEnabled by viewModel.adsEnabled.collectAsStateWithLifecycle()
    val bulkAd = adsEnabled && state.selected.size > AiCleanViewModel.BULK_AD_THRESHOLD
    MessageEffect(message, snackbar, onUndo = viewModel::undo, onConsumed = viewModel::consumeMessage)

    // Keep the screen on while analysing: if the phone sleeps, aggressive OEM battery
    // managers kill the (now background) CPU-heavy process mid-analysis.
    val view = androidx.compose.ui.platform.LocalView.current
    val analyzing = state.engine is DedupState.Analyzing
    androidx.compose.runtime.DisposableEffect(analyzing) {
        view.keepScreenOn = analyzing
        onDispose { view.keepScreenOn = false }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.fillMaxSize()) {
            CleanHeader(
                analyzing = state.engine is DedupState.Analyzing,
                onRefresh = viewModel::reanalyze,
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 168.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { TitleBlock() }
                    item { FilterChips(state, viewModel::setFilter) }
                    item { SmartKeeperCard(state, viewModel::setSmartKeeper) }
                    when (val st = state.engine) {
                        is DedupState.Analyzing -> item { AnalyzingCard(st) }
                        is DedupState.Failed -> item { InfoCard(stringResource(R.string.ai_failed)) }
                        is DedupState.Ready -> if (state.clusters.isEmpty()) {
                            item { InfoCard(stringResource(R.string.ai_empty)) }
                        }
                        DedupState.Idle -> item { AnalyzingCard(null) }
                    }
                    items(state.clusters, key = { it.id }) { cluster ->
                        ClusterCard(
                            cluster = cluster,
                            selected = state.selected,
                            smartKeeper = state.smartKeeper,
                            onToggle = viewModel::toggle,
                            onTrashOne = viewModel::trashOne,
                            onCompare = { loupeFor = cluster },
                            onDeselect = { viewModel.deselectCluster(cluster) },
                            onSelectSuggested = { viewModel.selectSuggested(cluster) },
                            onPurge = { viewModel.trashCluster(cluster) },
                        )
                    }
                }
            }
        }

        StickyTrashBar(
            count = state.selected.size,
            bytes = state.selectedBytes,
            busy = state.busy,
            onTrash = { confirm = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp, start = 12.dp, end = 12.dp))
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = SurfaceCard,
            title = { Text(stringResource(R.string.ai_confirm_title, formatBytes(state.selectedBytes)), color = TextPrimary) },
            text = { Text(androidx.compose.ui.res.pluralStringResource(R.plurals.ai_confirm_body, state.selected.size, formatCount(state.selected.size)), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { confirm = false; activity?.let(viewModel::trashSelectedGated) ?: viewModel.trashSelected() },
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive),
                ) { Text(stringResource(if (bulkAd) R.string.ai_confirm_ad else R.string.ai_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.purge_cancel), color = TextPrimary) }
            },
        )
    }

    loupeFor?.let { cluster -> CompareLoupeDialog(cluster = cluster, onDismiss = { loupeFor = null }) }
}

@Composable
private fun MessageEffect(
    message: CleanMessage?,
    snackbar: SnackbarHostState,
    onUndo: (List<Long>) -> Unit,
    onConsumed: () -> Unit,
) {
    val undoLabel = stringResource(R.string.ai_undo)
    val text = when (message) {
        is CleanMessage.Moved -> androidx.compose.ui.res.pluralStringResource(R.plurals.ai_msg_moved, message.files, formatCount(message.files), formatBytes(message.bytes))
        CleanMessage.KeepOne -> stringResource(R.string.ai_msg_keep_one)
        CleanMessage.NeedsFullAccess -> stringResource(R.string.ai_msg_full_access)
        is CleanMessage.Restored -> stringResource(R.string.ai_msg_restored, message.files)
        CleanMessage.Failed -> stringResource(R.string.ai_msg_failed)
        CleanMessage.AdNotFinished -> stringResource(R.string.ai_msg_ad_not_finished)
        null -> null
    }
    LaunchedEffect(message) {
        if (message == null || text == null) return@LaunchedEffect
        // Consume only after showing: clearing the message first would cancel this effect.
        if (message is CleanMessage.Moved) {
            val r = snackbar.showSnackbar(text, actionLabel = undoLabel, duration = SnackbarDuration.Long)
            onConsumed()
            if (r == SnackbarResult.ActionPerformed) onUndo(message.trashIds)
        } else {
            snackbar.showSnackbar(text)
            onConsumed()
        }
    }
}

// ------------------------------------------------------------------ header

@Composable
private fun CleanHeader(analyzing: Boolean, onRefresh: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(Emerald, CircleShape))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dash_brand), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1)
                Text(stringResource(R.string.ai_subtitle), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onRefresh, enabled = !analyzing) {
                Icon(Icons.Outlined.Refresh, stringResource(R.string.ai_reanalyze), tint = if (analyzing) TextMuted else TextPrimary)
            }
        }
        if (analyzing) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Emerald, trackColor = SurfaceStroke)
        } else {
            HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
        }
    }
}

@Composable
private fun TitleBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TelemetryBadge(stringResource(R.string.ai_badge_device), EmeraldLight, showDot = true)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.Shield, null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.ai_badge_cloud), style = Telemetry.sm, color = TextSecondary, maxLines = 1)
        }
        Text(stringResource(R.string.ai_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(stringResource(R.string.ai_engine), style = Telemetry.sm, color = EmeraldLight)
    }
}

@Composable
private fun FilterChips(state: AiCleanUiState, onFilter: (DedupFilter) -> Unit) {
    val labels = mapOf(
        DedupFilter.ALL to R.string.ai_filter_all,
        DedupFilter.EXACT to R.string.ai_filter_exact,
        DedupFilter.SIMILAR to R.string.ai_filter_similar,
        DedupFilter.BLURRY to R.string.ai_filter_blurry,
        DedupFilter.SCREENSHOTS to R.string.ai_filter_screens,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DedupFilter.entries) { f ->
            val active = f == state.filter
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) EmeraldLight else SurfaceCard)
                    .border(1.dp, if (active) EmeraldLight else SurfaceStroke, RoundedCornerShape(50))
                    .clickable { onFilter(f) }
                    .semantics { role = Role.Tab }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(labels.getValue(f)),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (active) OnEmeraldLight else TextPrimary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    formatCount(state.counts[f] ?: 0),
                    style = Telemetry.sm,
                    color = if (active) OnEmeraldLight else TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SmartKeeperCard(state: AiCleanUiState, onToggle: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Emerald.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Shield, null, tint = EmeraldLight) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (state.smartKeeper) R.string.ai_keeper_title else R.string.ai_keeper_off_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (state.smartKeeper) TelemetryBadge(stringResource(R.string.ai_keeper_badge), EmeraldLight)
            }
            Switch(
                checked = state.smartKeeper,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = EmeraldLight, checkedThumbColor = OnEmeraldLight),
            )
        }
        Text(
            stringResource(if (state.smartKeeper) R.string.ai_keeper_body else R.string.ai_keeper_off_body),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        val ready = state.engine as? DedupState.Ready
        if (ready != null) {
            Text(
                stringResource(
                    R.string.ai_analyzed_in,
                    formatCount(ready.result.analyzedImages),
                    durationLabel(ready.result.durationMs),
                ),
                style = Telemetry.sm,
                color = TextSecondary,
            )
        }
        if (state.engine is DedupState.Ready) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.ShowChart, null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    androidx.compose.ui.res.pluralStringResource(
                        R.plurals.ai_flagged, state.flaggedFiles, formatCount(state.flaggedFiles), state.flaggedClusters,
                    ),
                    style = Telemetry.sm,
                    color = EmeraldLight,
                )
            }
        }
    }
}

@Composable
private fun AnalyzingCard(st: DedupState.Analyzing?) {
    Column(
        Modifier.fillMaxWidth().obsidianCard().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (st == null || st.total == 0) {
                stringResource(R.string.ai_analyzing_start)
            } else {
                stringResource(R.string.ai_analyzing, formatCount(st.done), formatCount(st.total))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
        if (st != null && st.total > 0) {
            LinearProgressIndicator(
                progress = { st.done.toFloat() / st.total },
                modifier = Modifier.fillMaxWidth(),
                color = Emerald,
                trackColor = SurfaceStroke,
            )
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Emerald, trackColor = SurfaceStroke)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth().obsidianCard().padding(16.dp),
    )
}

// ------------------------------------------------------------------ clusters

@Composable
private fun ClusterCard(
    cluster: MediaCluster,
    selected: Set<String>,
    smartKeeper: Boolean,
    onToggle: (String) -> Unit,
    onTrashOne: (String) -> Unit,
    onCompare: () -> Unit,
    onDeselect: () -> Unit,
    onSelectSuggested: () -> Unit,
    onPurge: () -> Unit,
) {
    var expanded by rememberSaveable(cluster.id) { mutableStateOf(false) }
    val accent = when (cluster.type) {
        ClusterType.EXACT_DUPLICATES -> EmeraldLight
        ClusterType.SIMILAR_BURST -> AmberLight
        ClusterType.BLURRY -> VideoRose
        ClusterType.OLD_SCREENSHOTS -> IndigoLight
    }
    val title = stringResource(
        when (cluster.type) {
            ClusterType.EXACT_DUPLICATES -> R.string.ai_type_exact
            ClusterType.SIMILAR_BURST -> R.string.ai_type_similar
            ClusterType.BLURRY -> R.string.ai_type_blurry
            ClusterType.OLD_SCREENSHOTS -> R.string.ai_type_screens
        },
    )
    val badge = when (cluster.type) {
        ClusterType.EXACT_DUPLICATES -> stringResource(R.string.ai_badge_sha)
        ClusterType.SIMILAR_BURST -> stringResource(R.string.ai_badge_match, String.format(Locale.US, "%.1f%%", cluster.matchPercent))
        ClusterType.BLURRY -> stringResource(R.string.ai_badge_blur, com.storagepulse.cleaner.data.dedup.BLUR_SCORE_THRESHOLD)
        ClusterType.OLD_SCREENSHOTS -> stringResource(R.string.ai_badge_old)
    }
    val selectedHere = cluster.items.filter { it.path in selected }
    val shown = if (expanded) cluster.items else cluster.items.take(TILES_COLLAPSED)

    Column(
        Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        }
        Text(
            stringResource(
                R.string.ai_cluster_meta,
                dateLabel(cluster.startTime),
                androidx.compose.ui.res.pluralStringResource(
                    R.plurals.ai_photos, cluster.items.size, cluster.items.size, formatBytes(cluster.totalBytes),
                ),
            ),
            style = Telemetry.sm,
            color = TextSecondary,
        )
        TelemetryBadge(badge, accent)

        shown.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { item ->
                    MediaTile(
                        item = item,
                        cluster = cluster,
                        isKeeper = item.path == cluster.keeper?.path,
                        protected = smartKeeper && item.path == cluster.keeper?.path,
                        checked = item.path in selected,
                        onToggle = { onToggle(item.path) },
                        onTrash = { onTrashOne(item.path) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (cluster.items.size > TILES_COLLAPSED) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) stringResource(R.string.ai_show_less) else stringResource(R.string.ai_show_all, cluster.items.size),
                    color = EmeraldLight,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (cluster.keeper != null && cluster.items.size >= 2) {
                TextButton(onClick = onCompare, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.CompareArrows, null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.ai_compare), style = Telemetry.sm, color = EmeraldLight, maxLines = 1)
                }
            }
            TextButton(
                onClick = if (selectedHere.isEmpty()) onSelectSuggested else onDeselect,
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Text(
                    if (selectedHere.isEmpty()) stringResource(R.string.ai_select_suggested) else stringResource(R.string.ai_deselect, selectedHere.size),
                    style = Telemetry.sm,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }
        // Own row so the amount is never clipped on narrow phones.
        if (selectedHere.isNotEmpty()) {
            Button(
                onClick = onPurge,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed.copy(alpha = 0.16f), contentColor = DestructiveRed),
            ) {
                Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.ai_purge_cluster, selectedHere.size, formatBytes(selectedHere.sumOf { it.sizeBytes })),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MediaTile(
    item: MediaItem,
    cluster: MediaCluster,
    isKeeper: Boolean,
    protected: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier,
) {
    val tag = when {
        isKeeper -> stringResource(R.string.ai_tag_keep)
        cluster.type == ClusterType.EXACT_DUPLICATES -> stringResource(R.string.ai_tag_duplicate)
        cluster.type == ClusterType.SIMILAR_BURST -> stringResource(R.string.ai_tag_similar)
        cluster.type == ClusterType.BLURRY -> stringResource(R.string.ai_tag_blurry)
        else -> stringResource(R.string.ai_tag_screenshot)
    }
    val tagColor = if (isKeeper) EmeraldLight else if (cluster.type == ClusterType.BLURRY) VideoRose else AmberLight
    val selectDesc = stringResource(R.string.ai_select, item.name)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceRaised)
                .border(
                    width = if (isKeeper) 2.dp else 1.dp,
                    color = if (isKeeper) EmeraldLight else if (checked) DestructiveRed.copy(alpha = 0.7f) else SurfaceStroke,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(enabled = !protected, onClick = onToggle)
                .semantics { contentDescription = selectDesc },
        ) {
            AsyncImage(
                model = item.contentUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, ObsidianCanvas.copy(alpha = 0.85f))),
                ),
            )
            TelemetryBadge(
                tag,
                tagColor,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).background(ObsidianCanvas.copy(alpha = 0.75f), RoundedCornerShape(50)),
            )
            Icon(
                imageVector = when {
                    protected -> Icons.Outlined.Lock
                    checked -> Icons.Outlined.CheckBox
                    else -> Icons.Outlined.CheckBoxOutlineBlank
                },
                contentDescription = null,
                tint = when {
                    protected -> EmeraldLight
                    checked -> EmeraldLight
                    else -> TextPrimary
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(ObsidianCanvas.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(2.dp)
                    .size(20.dp),
            )
            AutoSizeText(
                text = if (isKeeper) stringResource(R.string.ai_keep_score, item.sharpness) else stringResource(R.string.ai_score, item.sharpness),
                style = Telemetry.sm,
                color = if (isKeeper) EmeraldLight else TextPrimary,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                minSize = 8.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                Text(
                    if (isKeeper) stringResource(R.string.ai_size_safe, formatBytes(item.sizeBytes)) else stringResource(R.string.ai_size_minus, formatBytes(item.sizeBytes)),
                    style = Telemetry.sm,
                    color = if (isKeeper) EmeraldLight else TextSecondary,
                    maxLines = 1,
                )
            }
            if (!protected) {
                IconButton(onClick = onTrash, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.ai_trash_one, item.name), tint = DestructiveRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ sticky bar

@Composable
private fun StickyTrashBar(count: Int, bytes: Long, busy: Boolean, onTrash: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(ObsidianCanvas)
            .drawBehind { drawLine(SurfaceStroke, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (count > 0) {
            Text(
                androidx.compose.ui.res.pluralStringResource(R.plurals.ai_selected_summary, count, formatCount(count), formatBytes(bytes)),
                style = Telemetry.sm,
                color = EmeraldLight,
                maxLines = 1,
            )
        }
        Button(
            onClick = onTrash,
            enabled = count > 0 && !busy,
            modifier = Modifier.widthIn(max = 688.dp).fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DestructiveRed,
                contentColor = OnDestructive,
                disabledContainerColor = SurfaceRaised,
                disabledContentColor = TextMuted,
            ),
        ) {
            Icon(Icons.Outlined.Delete, null)
            Spacer(Modifier.width(8.dp))
            AutoSizeText(
                if (count > 0) stringResource(R.string.ai_trash_selected, formatCount(count), formatBytes(bytes)) else stringResource(R.string.ai_trash_none),
                MaterialTheme.typography.labelLarge,
                if (count > 0 && !busy) OnDestructive else TextMuted,
                minSize = 11.sp,
            )
        }
        Text(stringResource(R.string.ai_protected), style = Telemetry.sm, color = TextSecondary, maxLines = 1)
    }
}

private fun durationLabel(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 60) "${s / 60}m ${s % 60}s" else "${s}s"
}

private fun dateLabel(millis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))

