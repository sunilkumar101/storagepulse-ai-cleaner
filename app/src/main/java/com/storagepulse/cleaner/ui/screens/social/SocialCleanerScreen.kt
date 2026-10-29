package com.storagepulse.cleaner.ui.screens.social

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.social.SocialApp
import com.storagepulse.cleaner.data.social.SocialCategory
import com.storagepulse.cleaner.data.social.SocialFile
import com.storagepulse.cleaner.data.storage.FileClassifier
import com.storagepulse.cleaner.data.storage.StorageAccess
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.CleanerSubNav
import com.storagepulse.cleaner.ui.components.CleanerTab
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.screens.treemap.ageText
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
import com.storagepulse.cleaner.ui.theme.SurfaceSunken
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextMuted
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.theme.VideoRose
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatCount
import java.io.File

data class SocialNav(val onBack: () -> Unit, val onTab: (CleanerTab) -> Unit)

private val appLabels = mapOf(
    SocialApp.WHATSAPP to R.string.so_app_whatsapp,
    SocialApp.TELEGRAM to R.string.so_app_telegram,
    SocialApp.TIKTOK to R.string.so_app_tiktok,
    SocialApp.INSTAGRAM to R.string.so_app_instagram,
)
private val categoryColors = mapOf(
    SocialCategory.SENT to EmeraldLight,
    SocialCategory.RECEIVED_VIDEO to VideoRose,
    SocialCategory.VOICE to IndigoLight,
    SocialCategory.STATUS to AmberLight,
    SocialCategory.OTHER to TextMuted,
)

/**
 * Screen 7 — Social / Chat Storage Cleaner.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/social_media_chat_storage_cleaner/
 */
@Composable
fun SocialCleanerScreen(nav: SocialNav, viewModel: SocialCleanerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var review by remember { mutableStateOf<SocialCategory?>(null) }
    val audio = rememberAudioPreview()

    // Came back from granting All Files Access.
    LifecycleResumeEffect(state.needsFullAccess) {
        if (state.needsFullAccess && StorageAccess.hasAllFilesAccess(context)) viewModel.refresh()
        onPauseOrDispose { audio.pause() }
    }

    val undo = stringResource(R.string.ai_undo)
    val text = when (val m = message) {
        is SocialMessage.Cleaned -> pluralStringResource(R.plurals.so_msg_cleaned, m.files, formatCount(m.files), formatBytes(m.bytes))
        is SocialMessage.Restored -> stringResource(R.string.so_msg_restored, m.files)
        SocialMessage.NeedsFullAccess -> stringResource(R.string.so_msg_full_access)
        null -> null
    }
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        if (text == null) return@LaunchedEffect
        if (m is SocialMessage.Cleaned) {
            val r = snackbar.showSnackbar(text, actionLabel = undo, duration = SnackbarDuration.Long)
            viewModel.consumeMessage()
            if (r == SnackbarResult.ActionPerformed) viewModel.undo(m.trashIds)
        } else {
            snackbar.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(nav.onBack)
            CleanerSubNav(active = CleanerTab.SOCIAL, onSelect = nav.onTab)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 170.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when {
                        state.needsFullAccess -> item {
                            NeedsAccess(onAllow = {
                                StorageAccess.allFilesAccessIntents(context).any { runCatching { context.startActivity(it) }.isSuccess }
                            })
                        }
                        state.loading -> item { Info(stringResource(R.string.so_scanning), progress = true) }
                        else -> {
                            item { AppTabs(state, viewModel::setApp) }
                            item { Filters(state.filter, viewModel::setFilter) }
                            item { Footprint(state) }
                            if (state.visible.none { it.category != SocialCategory.OTHER }) {
                                item { Info(stringResource(R.string.so_empty)) }
                            }
                            SocialCategory.entries.filter { it != SocialCategory.OTHER }.forEach { c ->
                                val files = state.inCategory(c)
                                if (files.isNotEmpty()) {
                                    item(key = c.name) {
                                        CategoryCard(
                                            category = c,
                                            app = state.app,
                                            files = files,
                                            selected = state.selected,
                                            audio = audio,
                                            onToggleAll = { all -> viewModel.setCategory(c, all) },
                                            onToggle = viewModel::toggle,
                                            onClean = { viewModel.cleanCategory(c) },
                                            onReview = { review = c },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!state.needsFullAccess && !state.loading) {
            BottomBar(state, onClean = { confirm = true }, modifier = Modifier.align(Alignment.BottomCenter))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp, start = 12.dp, end = 12.dp))
    }

    if (confirm) {
        val files = state.selectedVisible
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = SurfaceCard,
            title = { Text(stringResource(R.string.so_confirm_title, formatBytes(files.sumOf { it.sizeBytes })), color = TextPrimary) },
            text = { Text(pluralStringResource(R.plurals.so_confirm_body, files.size, formatCount(files.size)), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { confirm = false; audio.stop(); viewModel.cleanSelected() },
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = OnDestructive),
                ) { Text(stringResource(R.string.so_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.purge_cancel), color = TextPrimary) } },
        )
    }

    review?.let { c ->
        ReviewSheet(
            title = categoryTitle(c, state.app),
            files = state.inCategory(c),
            selected = state.selected,
            audio = if (c == SocialCategory.VOICE) audio else null,
            onToggle = viewModel::toggle,
            onDismiss = { review = null },
        )
    }
}

// ------------------------------------------------------------------ audio

/** One shared player so only one voice note plays at a time. */
class AudioPreview(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    var playingPath by mutableStateOf<String?>(null)
        private set

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) playingPath = null
            }
        })
    }

    fun toggle(path: String) {
        if (playingPath == path) {
            stop()
            return
        }
        player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(File(path))))
        player.prepare()
        player.play()
        playingPath = path
    }

    fun pause() { player.pause() }
    fun stop() { player.stop(); playingPath = null }
}

@Composable
private fun rememberAudioPreview(): AudioPreview {
    val context = LocalContext.current
    val audio = remember { AudioPreview(context) }
    DisposableEffect(Unit) { onDispose { audio.player.release() } }
    return audio
}

// ------------------------------------------------------------------ header, tabs, filters

@Composable
private fun Header(onBack: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.disclosure_back), tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.so_title), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1)
                Text(stringResource(R.string.ai_subtitle), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
    }
}

@Composable
private fun AppTabs(state: SocialUiState, onApp: (SocialApp) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SocialApp.entries) { app ->
            val active = app == state.app
            val icon = when (app) {
                SocialApp.WHATSAPP -> Icons.AutoMirrored.Outlined.Chat
                SocialApp.TELEGRAM -> Icons.AutoMirrored.Outlined.Send
                SocialApp.TIKTOK -> Icons.Outlined.Movie
                SocialApp.INSTAGRAM -> Icons.Outlined.PhotoCamera
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) Emerald.copy(alpha = 0.16f) else SurfaceCard)
                    .border(1.dp, if (active) EmeraldLight else SurfaceStroke, RoundedCornerShape(16.dp))
                    .clickable { onApp(app) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp).background(SurfaceRaised, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = if (active) EmeraldLight else TextSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(appLabels.getValue(app)), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                    Text(formatBytes(state.scans[app]?.totalBytes ?: 0), style = Telemetry.sm, color = if (active) EmeraldLight else TextSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Filters(active: AgeFilter, onFilter: (AgeFilter) -> Unit) {
    val labels = mapOf(
        AgeFilter.ALL to R.string.so_filter_all,
        AgeFilter.OLDER_30D to R.string.so_filter_30d,
        AgeFilter.OLDER_6M to R.string.so_filter_6m,
        AgeFilter.OVER_50MB to R.string.so_filter_50mb,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AgeFilter.entries) { f ->
            val on = f == active
            Text(
                stringResource(labels.getValue(f)),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = if (on) OnEmeraldLight else TextPrimary,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) EmeraldLight else SurfaceCard)
                    .border(1.dp, if (on) EmeraldLight else SurfaceStroke, RoundedCornerShape(50))
                    .clickable { onFilter(f) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ footprint

@Composable
private fun Footprint(state: SocialUiState) {
    val visible = state.visible
    val total = visible.sumOf { it.sizeBytes }
    val selected = state.selectedVisible
    val appName = stringResource(appLabels.getValue(state.app)).uppercase()
    Column(
        Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                AutoSizeText(stringResource(R.string.so_footprint, appName), Telemetry.labelCaps, TextSecondary)
                Row(verticalAlignment = Alignment.Bottom) {
                    AutoSizeText(formatBytes(total), Telemetry.xl.copy(fontSize = 28.sp), TextPrimary, minSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.so_total), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.so_auto_selected), style = Telemetry.labelCaps, color = EmeraldLight)
                AutoSizeText(formatBytes(selected.sumOf { it.sizeBytes }), Telemetry.lg, EmeraldLight, minSize = 12.sp)
                AutoSizeText(stringResource(R.string.so_zero_loss), MaterialTheme.typography.bodySmall, TextSecondary, minSize = 9.sp)
            }
        }
        // Category spectrum
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(SurfaceStroke), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val byCat = visible.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.sizeBytes } }
            SocialCategory.entries.forEach { c ->
                val w = (byCat[c] ?: 0L).toFloat() / total.coerceAtLeast(1)
                if (w > 0.005f) Box(Modifier.weight(w).fillMaxHeight().background(categoryColors.getValue(c)))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            AutoSizeText(stringResource(R.string.so_local_only), Telemetry.sm, TextSecondary, Modifier.weight(1f), minSize = 8.sp)
            Text(stringResource(R.string.so_marked, formatCount(selected.size)), style = Telemetry.sm, color = TextPrimary)
        }
    }
}

// ------------------------------------------------------------------ category cards

@Composable
private fun categoryTitle(c: SocialCategory, app: SocialApp): String = stringResource(
    when (c) {
        SocialCategory.SENT -> R.string.so_sent_title
        SocialCategory.RECEIVED_VIDEO -> if (app == SocialApp.WHATSAPP || app == SocialApp.TELEGRAM) R.string.so_received_title else R.string.so_received_title_saved
        SocialCategory.VOICE -> R.string.so_voice_title
        SocialCategory.STATUS, SocialCategory.OTHER -> R.string.so_status_title
    },
)

@Composable
private fun CategoryCard(
    category: SocialCategory,
    app: SocialApp,
    files: List<SocialFile>,
    selected: Set<String>,
    audio: AudioPreview,
    onToggleAll: (Boolean) -> Unit,
    onToggle: (String) -> Unit,
    onClean: () -> Unit,
    onReview: () -> Unit,
) {
    val color = categoryColors.getValue(category)
    val chosen = files.filter { it.path in selected }
    val allChosen = chosen.size == files.size
    val (badge, tag, body) = when (category) {
        SocialCategory.SENT -> Triple(R.string.so_sent_badge, R.string.so_sent_tag, R.string.so_sent_body)
        SocialCategory.RECEIVED_VIDEO -> Triple(R.string.so_received_badge, R.string.so_received_tag, R.string.so_received_body)
        SocialCategory.VOICE -> Triple(R.string.so_voice_badge, R.string.so_voice_tag, R.string.so_voice_body)
        else -> Triple(R.string.so_status_badge, R.string.so_status_tag, R.string.so_status_body)
    }
    Column(
        Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TelemetryBadge(stringResource(badge), color)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(tag), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = { onToggleAll(!allChosen) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (allChosen) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    stringResource(if (allChosen) R.string.so_clear_selection else R.string.so_select_all),
                    tint = if (allChosen) EmeraldLight else TextMuted,
                )
            }
        }
        Text(categoryTitle(category, app), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            AutoSizeText(
                androidx.compose.ui.res.pluralStringResource(R.plurals.so_files_count, files.size, formatBytes(files.sumOf { it.sizeBytes }), formatCount(files.size)),
                Telemetry.lg,
                color,
                Modifier.weight(1f),
                minSize = 11.sp,
            )
            if (chosen.isNotEmpty()) {
                Text(stringResource(R.string.so_selected_count, formatBytes(chosen.sumOf { it.sizeBytes })), style = Telemetry.labelCaps, color = EmeraldLight)
            }
        }

        when (category) {
            SocialCategory.VOICE -> {
                files.take(3).forEach { f -> AudioRow(f, f.path in selected, audio.playingPath == f.path, onPlay = { audio.toggle(f.path) }, onToggle = { onToggle(f.path) }) }
                if (files.size > 3) {
                    TextButton(onClick = onReview) { Text(stringResource(R.string.so_voice_show_all, formatCount(files.size)), color = EmeraldLight) }
                }
            }
            else -> {
                ThumbStrip(files.filter { isVisual(it) }.take(10), selected, onToggle)
                if (category == SocialCategory.RECEIVED_VIDEO) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceSunken).clickable(onClick = onReview).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.so_received_cta), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = TextPrimary)
                    }
                }
            }
        }

        if (chosen.isNotEmpty() && category != SocialCategory.RECEIVED_VIDEO) {
            Button(
                onClick = onClean,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed.copy(alpha = 0.16f), contentColor = DestructiveRed),
            ) {
                Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                AutoSizeText(
                    stringResource(
                        if (category == SocialCategory.SENT) R.string.so_sent_cta else R.string.so_status_cta,
                        formatBytes(chosen.sumOf { it.sizeBytes }),
                    ),
                    MaterialTheme.typography.labelLarge,
                    DestructiveRed,
                    minSize = 10.sp,
                )
            }
        }
    }
}

private fun isVisual(f: SocialFile): Boolean {
    val ext = FileClassifier.extensionOf(f.name)
    return FileClassifier.isPhoto(ext) || FileClassifier.isVideo(ext)
}

@Composable
private fun ThumbStrip(files: List<SocialFile>, selected: Set<String>, onToggle: (String) -> Unit) {
    if (files.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(files, key = { it.path }) { f -> Thumb(f, f.path in selected, onToggle, Modifier.size(width = 104.dp, height = 84.dp)) }
    }
}

@Composable
private fun Thumb(f: SocialFile, checked: Boolean, onToggle: (String) -> Unit, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceRaised)
            .border(1.dp, if (checked) EmeraldLight else SurfaceStroke, RoundedCornerShape(10.dp))
            .clickable { onToggle(f.path) },
    ) {
        AsyncImage(model = File(f.path), contentDescription = f.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Text(
            formatBytes(f.sizeBytes),
            style = Telemetry.sm,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).background(ObsidianCanvas.copy(alpha = 0.7f), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp),
        )
        Icon(
            if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            null,
            tint = if (checked) EmeraldLight else TextPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(18.dp),
        )
    }
}

@Composable
private fun AudioRow(f: SocialFile, checked: Boolean, playing: Boolean, onPlay: () -> Unit, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceSunken).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPlay, modifier = Modifier.size(40.dp).background(SurfaceRaised, CircleShape)) {
            Icon(
                if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                if (playing) stringResource(R.string.so_pause) else stringResource(R.string.so_play, f.name),
                tint = EmeraldLight,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, style = Telemetry.sm, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            Text(
                stringResource(R.string.so_recorded, ageText(f.lastModified)) + " · " + formatBytes(f.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
            )
        }
        if (playing) Icon(Icons.Outlined.GraphicEq, null, tint = IndigoLight, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                null,
                tint = if (checked) EmeraldLight else TextMuted,
            )
        }
    }
}

// ------------------------------------------------------------------ bottom & sheets

@Composable
private fun BottomBar(state: SocialUiState, onClean: () -> Unit, modifier: Modifier) {
    val files = state.selectedVisible
    val bytes = files.sumOf { it.sizeBytes }
    val app = stringResource(appLabels.getValue(state.app))
    Column(
        modifier
            .fillMaxWidth()
            .background(ObsidianCanvas)
            .drawBehind { drawLine(SurfaceStroke, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(if (files.isEmpty()) TextMuted else EmeraldLight, CircleShape))
            Spacer(Modifier.width(6.dp))
            AutoSizeText(stringResource(R.string.so_bottom_targeted, formatBytes(bytes)), Telemetry.labelCaps, EmeraldLight, Modifier.weight(1f))
            Text(pluralStringResource(R.plurals.so_items, files.size, formatCount(files.size)), style = Telemetry.sm, color = TextSecondary)
        }
        Button(
            onClick = onClean,
            enabled = files.isNotEmpty() && !state.busy,
            modifier = Modifier.widthIn(max = 688.dp).fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DestructiveRed,
                contentColor = OnDestructive,
                disabledContainerColor = SurfaceRaised,
                disabledContentColor = TextMuted,
            ),
        ) {
            AutoSizeText(
                if (files.isEmpty()) stringResource(R.string.so_bottom_none) else stringResource(R.string.so_bottom_cta, app, formatBytes(bytes)),
                MaterialTheme.typography.labelLarge,
                if (files.isEmpty() || state.busy) TextMuted else OnDestructive,
                minSize = 11.sp,
            )
        }
        Text(stringResource(R.string.so_protected), style = Telemetry.sm, color = TextSecondary, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSheet(
    title: String,
    files: List<SocialFile>,
    selected: Set<String>,
    audio: AudioPreview?,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { audio?.stop(); onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = SurfaceCard) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = { audio?.stop(); onDismiss() }) { Text(stringResource(R.string.so_done), color = EmeraldLight) }
            }
            LazyColumn(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(files, key = { it.path }) { f ->
                    if (audio != null) {
                        AudioRow(f, f.path in selected, audio.playingPath == f.path, onPlay = { audio.toggle(f.path) }, onToggle = { onToggle(f.path) })
                    } else {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onToggle(f.path) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Thumb(f, f.path in selected, onToggle, Modifier.size(width = 88.dp, height = 64.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                                Text(formatBytes(f.sizeBytes) + " · " + ageText(f.lastModified), style = Telemetry.sm, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Info(text: String, progress: Boolean = false) {
    Column(Modifier.fillMaxWidth().obsidianCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        if (progress) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Emerald, trackColor = SurfaceStroke)
    }
}

@Composable
private fun NeedsAccess(onAllow: () -> Unit) {
    Column(Modifier.fillMaxWidth().obsidianCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.so_needs_full), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Button(
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = OnEmeraldLight),
        ) { Text(stringResource(R.string.so_allow)) }
    }
}

