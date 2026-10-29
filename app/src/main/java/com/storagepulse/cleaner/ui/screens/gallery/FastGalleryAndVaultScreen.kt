package com.storagepulse.cleaner.ui.screens.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.gallery.MediaEntry
import com.storagepulse.cleaner.data.gallery.MediaFolder
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.screens.treemap.ageText
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
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextMuted
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.util.BiometricAuth
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatCount
import kotlinx.coroutines.launch

/**
 * Screen 8 — Fast Offline Gallery & Biometric Private Vault.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/fast_offline_gallery_biometric_vault/
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FastGalleryAndVaultScreen(
    onOpenVault: () -> Unit,
    onCleanScreenshots: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val vault by viewModel.vaultSummary.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val searchFocus = remember { FocusRequester() }
    var confirmGps by rememberSaveable { mutableStateOf(false) }
    var confirmVault by rememberSaveable { mutableStateOf(false) }
    var cropTarget by remember { mutableStateOf<MediaEntry?>(null) }

    BackHandler(enabled = state.openFolder != null || state.selected.isNotEmpty()) {
        if (state.selected.isNotEmpty()) viewModel.clearSelection() else viewModel.closeFolder()
    }

    val msgText = when (val m = message) {
        is GalleryMessage.GpsStripped -> androidx.compose.ui.res.pluralStringResource(R.plurals.ga_gps_done, m.stripped, m.stripped, m.skipped)
        is GalleryMessage.Locked -> androidx.compose.ui.res.pluralStringResource(R.plurals.vault_lock_done, m.locked, m.locked, m.failed)
        GalleryMessage.CoverSet -> stringResource(R.string.ga_cover_done)
        is GalleryMessage.Cropped -> stringResource(R.string.ga_crop_done, m.name)
        GalleryMessage.CropFailed -> stringResource(R.string.ga_crop_failed)
        null -> null
    }
    LaunchedEffect(message) {
        if (msgText != null) {
            snackbar.showSnackbar(msgText)
            viewModel.consumeMessage()
        }
    }

    fun unlock() {
        val reason = BiometricAuth.unavailableReason(context)
        if (reason != null) {
            scope.launch { snackbar.showSnackbar(resources.getString(reason)) }
            return
        }
        BiometricAuth.authenticate(
            context,
            resources.getString(R.string.vault_prompt_title),
            resources.getString(R.string.vault_prompt_sub),
            onSuccess = { viewModel.unlockVault(); onOpenVault() },
            onError = { err -> scope.launch { snackbar.showSnackbar(resources.getString(R.string.vault_auth_failed, err)) } },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.fillMaxSize()) {
            GalleryHeader()
            val columns = if (state.showsMedia) state.columns else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(if (state.showsMedia) 4.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (state.showsMedia) 4.dp else 12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        TitleRow(state.mode, viewModel::setMode)
                        SearchRow(state, searchFocus, viewModel::setQuery, viewModel::cycleSort, viewModel::setColumns)
                        state.index?.let { StatsRow(it.entries.size, it.totalBytes, it.indexMs) }
                        VaultHero(vault, onUnlock = ::unlock)
                        SectionHeader(state, viewModel::closeFolder, viewModel::clearSelection)
                        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Emerald, trackColor = SurfaceStroke)
                    }
                }
                if (state.showsMedia) {
                    items(state.media, key = { it.path }) { e ->
                        MediaTile(e, e.path in state.selected, onToggle = { viewModel.toggle(e.path) })
                    }
                    if (!state.loading && state.media.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.ga_empty), color = TextSecondary) }
                    }
                } else {
                    items(state.folders, key = { it.path }) { f ->
                        FolderCard(f, onOpen = { viewModel.openFolder(f.path) }, onCleanOld = onCleanScreenshots)
                    }
                }
            }
        }

        QuickActionPill(
            state = state,
            onSearch = {
                scope.launch { gridState.scrollToItem(0) }
                runCatching { searchFocus.requestFocus() }
            },
            onCover = viewModel::setCover,
            onGps = { confirmGps = true },
            onVault = { confirmVault = true },
            onCrop = { state.selectedEntries.singleOrNull()?.let { cropTarget = it } },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 12.dp, end = 12.dp))
    }

    if (confirmGps) {
        val n = state.selected.size
        AlertDialog(
            onDismissRequest = { confirmGps = false },
            containerColor = SurfaceCard,
            title = { Text(androidx.compose.ui.res.pluralStringResource(R.plurals.ga_gps_title, n, n), color = TextPrimary) },
            text = { Text(stringResource(R.string.ga_gps_body), color = TextSecondary) },
            confirmButton = {
                Button(onClick = { confirmGps = false; viewModel.stripGps() }, colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive)) {
                    Text(stringResource(R.string.ga_gps_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { confirmGps = false }) { Text(stringResource(R.string.ga_cancel), color = TextPrimary) } },
        )
    }
    if (confirmVault) {
        AlertDialog(
            onDismissRequest = { confirmVault = false },
            containerColor = SurfaceCard,
            title = { Text(androidx.compose.ui.res.pluralStringResource(R.plurals.vault_lock_title, state.selected.size, state.selected.size), color = TextPrimary) },
            text = { Text(stringResource(R.string.vault_lock_body), color = TextSecondary) },
            confirmButton = {
                Button(onClick = { confirmVault = false; viewModel.lockSelected() }, colors = ButtonDefaults.buttonColors(containerColor = Indigo, contentColor = TextPrimary)) {
                    Text(stringResource(R.string.vault_lock_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { confirmVault = false }) { Text(stringResource(R.string.ga_cancel), color = TextPrimary) } },
        )
    }
    cropTarget?.let { e ->
        CropEditorDialog(
            entry = e,
            onSave = { l, t, r, b -> cropTarget = null; viewModel.saveCrop(e.path, l, t, r, b) },
            onDismiss = { cropTarget = null },
        )
    }
}

// ------------------------------------------------------------------ header & controls

@Composable
private fun GalleryHeader() {
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Emerald, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.dash_brand), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1)
                Text(stringResource(R.string.ga_subtitle), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
    }
}

@Composable
private fun TitleRow(mode: GalleryMode, onMode: (GalleryMode) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ga_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Emerald, CircleShape))
                Spacer(Modifier.width(6.dp))
                AutoSizeText(stringResource(R.string.ga_offline), Telemetry.labelCaps, EmeraldLight)
            }
        }
        Row(Modifier.background(SurfaceCard, RoundedCornerShape(50)).border(1.dp, SurfaceStroke, RoundedCornerShape(50)).padding(4.dp)) {
            listOf(GalleryMode.DCIM to R.string.ga_mode_dcim, GalleryMode.ALBUMS to R.string.ga_mode_albums).forEach { (m, label) ->
                val on = m == mode
                Text(
                    stringResource(label),
                    style = Telemetry.sm,
                    color = if (on) OnEmeraldLight else TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) EmeraldLight else Color.Transparent)
                        .clickable { onMode(m) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchRow(
    state: GalleryUiState,
    focus: FocusRequester,
    onQuery: (String) -> Unit,
    onSort: () -> Unit,
    onColumns: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.weight(1f).heightIn(min = 48.dp).background(SurfaceCard, RoundedCornerShape(14.dp)).border(1.dp, SurfaceStroke, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (state.query.isEmpty()) Text(stringResource(R.string.ga_search_hint), style = MaterialTheme.typography.bodyMedium, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(EmeraldLight),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            if (state.query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Close, null, tint = TextSecondary) }
            }
        }
        val sortLabel = stringResource(
            when (state.sort) {
                GallerySort.SIZE -> R.string.ga_sort_size
                GallerySort.DATE -> R.string.ga_sort_date
                GallerySort.NAME -> R.string.ga_sort_name
            },
        )
        IconButton(onClick = onSort, modifier = Modifier.size(48.dp).background(SurfaceCard, RoundedCornerShape(12.dp))) {
            Icon(Icons.Outlined.SwapVert, stringResource(R.string.ga_sort, sortLabel), tint = TextPrimary)
        }
        if (state.showsMedia) {
            Row(Modifier.background(SurfaceCard, RoundedCornerShape(12.dp)).padding(3.dp)) {
                listOf(2, 3, 4).forEach { n ->
                    val on = n == state.columns
                    Text(
                        stringResource(R.string.ga_columns, n),
                        style = Telemetry.sm,
                        color = if (on) EmeraldLight else TextSecondary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) SurfaceRaised else Color.Transparent).clickable { onColumns(n) }.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(count: Int, bytes: Long, indexMs: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AutoSizeText(stringResource(R.string.ga_stats, formatCount(count), formatBytes(bytes)), Telemetry.sm, TextPrimary, Modifier.weight(1f))
        TelemetryBadge(stringResource(R.string.ga_indexed, indexMs.toInt()), EmeraldLight)
    }
}

@Composable
private fun VaultHero(vault: VaultSummary, onUnlock: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Indigo.copy(alpha = 0.25f), SurfaceCard)))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Indigo.copy(alpha = 0.35f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Lock, null, tint = TextPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.vault_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                TelemetryBadge(stringResource(R.string.vault_badge), IndigoLight)
                AutoSizeText(stringResource(R.string.vault_counts, androidx.compose.ui.res.pluralStringResource(R.plurals.vault_photos, vault.photos, vault.photos), androidx.compose.ui.res.pluralStringResource(R.plurals.vault_videos, vault.videos, vault.videos)), MaterialTheme.typography.bodySmall, TextSecondary, minSize = 9.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            AutoSizeText(
                stringResource(if (vault.hardwareBacked) R.string.vault_keystore_hw else R.string.vault_keystore_sw),
                Telemetry.sm,
                TextSecondary,
                Modifier.weight(1f),
                minSize = 8.sp,
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onUnlock, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Indigo, contentColor = TextPrimary)) {
                Icon(Icons.Outlined.Fingerprint, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.vault_unlock))
            }
        }
    }
}

@Composable
private fun SectionHeader(state: GalleryUiState, onBack: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.openFolder != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.ga_back_to_albums), tint = TextPrimary) }
        }
        val title = when {
            state.openFolder != null -> state.folders.firstOrNull { it.path == state.openFolder }?.name ?: state.openFolder.substringAfterLast('/')
            state.showsMedia -> stringResource(R.string.ga_mode_dcim)
            else -> stringResource(R.string.ga_folders_title)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (state.selected.isNotEmpty()) {
            TextButton(onClick = onClear) { Text(stringResource(R.string.ga_selected, formatCount(state.selected.size)) + " ✕", color = EmeraldLight) }
        } else if (state.showsMedia) {
            Text(stringResource(R.string.ga_select_hint), style = Telemetry.sm, color = TextMuted, maxLines = 1)
        } else {
            val sortLabel = stringResource(
                when (state.sort) {
                    GallerySort.SIZE -> R.string.ga_sort_size
                    GallerySort.DATE -> R.string.ga_sort_date
                    GallerySort.NAME -> R.string.ga_sort_name
                },
            )
            Text(stringResource(R.string.ga_sorted_by, sortLabel), style = Telemetry.sm, color = TextSecondary)
        }
    }
}

// ------------------------------------------------------------------ grid items

@Composable
private fun FolderCard(f: MediaFolder, onOpen: () -> Unit, onCleanOld: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).obsidianCard(radius = 16).clickable(onClick = onOpen),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.1f).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(SurfaceRaised)) {
            f.cover?.let { AsyncImage(model = it.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, ObsidianCanvas.copy(alpha = 0.9f)))))
            if (f.pinnedCover) {
                Icon(
                    Icons.Outlined.PushPin,
                    stringResource(R.string.ga_pinned),
                    tint = TextPrimary,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(ObsidianCanvas.copy(alpha = 0.7f), CircleShape).padding(4.dp).size(16.dp),
                )
            }
            TelemetryBadge(
                formatBytes(f.bytes),
                EmeraldLight,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(ObsidianCanvas.copy(alpha = 0.75f), RoundedCornerShape(50)),
            )
            AutoSizeText(
                f.name,
                MaterialTheme.typography.titleMedium,
                TextPrimary,
                Modifier.align(Alignment.BottomStart).padding(10.dp),
                minSize = 11.sp,
            )
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(androidx.compose.ui.res.pluralStringResource(R.plurals.ga_items, f.items, formatCount(f.items)), style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                Text(ageText(f.latest), style = Telemetry.sm, color = TextSecondary, maxLines = 1)
            }
            if (f.oldItems > 0) {
                Text(
                    stringResource(R.string.ga_old_items, formatCount(f.oldItems)),
                    style = Telemetry.sm,
                    color = AmberLight,
                    maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AmberLight.copy(alpha = 0.14f)).clickable(onClick = onCleanOld).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTile(e: MediaEntry, selected: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceRaised)
            .combinedClickable(onClick = onToggle, onLongClick = onToggle),
    ) {
        AsyncImage(model = e.uri, contentDescription = e.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (e.isVideo) Icon(Icons.Outlined.PlayCircle, null, tint = TextPrimary, modifier = Modifier.align(Alignment.Center).size(28.dp))
        if (selected) {
            Box(Modifier.fillMaxSize().background(Emerald.copy(alpha = 0.25f)).border(2.dp, EmeraldLight, RoundedCornerShape(6.dp)))
            Icon(Icons.Outlined.CheckCircle, null, tint = EmeraldLight, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(ObsidianCanvas, CircleShape))
        }
    }
}

// ------------------------------------------------------------------ quick-action pill

@Composable
private fun QuickActionPill(
    state: GalleryUiState,
    onSearch: () -> Unit,
    onCover: () -> Unit,
    onGps: () -> Unit,
    onVault: () -> Unit,
    onCrop: () -> Unit,
    modifier: Modifier,
) {
    val sel = state.selectedEntries
    val single = sel.singleOrNull()
    val anyPhoto = sel.any { !it.isVideo }
    Row(
        modifier
            .background(SurfaceCard.copy(alpha = 0.97f), RoundedCornerShape(50))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillIcon(Icons.Outlined.Search, stringResource(R.string.ga_action_search), true, onSearch)
        PillIcon(Icons.Outlined.Image, stringResource(R.string.ga_action_cover), single != null && !state.busy, onCover)
        PillIcon(Icons.Outlined.Crop, stringResource(R.string.ga_action_crop), single != null && !single.isVideo && !state.busy, onCrop)
        PillText(Icons.Outlined.LocationOff, stringResource(R.string.ga_action_gps), anyPhoto && !state.busy, EmeraldLight, onGps)
        PillText(Icons.Outlined.Shield, stringResource(R.string.ga_action_vault), sel.isNotEmpty() && !state.busy, IndigoLight, onVault)
    }
}

@Composable
private fun PillIcon(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(icon, desc, tint = if (enabled) TextPrimary else TextMuted)
    }
}

@Composable
private fun PillText(icon: ImageVector, label: String, enabled: Boolean, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) color.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (enabled) color else TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (enabled) color else TextMuted, maxLines = 1)
    }
}

