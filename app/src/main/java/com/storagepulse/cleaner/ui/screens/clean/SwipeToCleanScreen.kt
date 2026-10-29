package com.storagepulse.cleaner.ui.screens.clean

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.media.LargeVideoRepository
import com.storagepulse.cleaner.data.media.VideoDetails
import com.storagepulse.cleaner.data.media.VideoItem
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.CleanerSubNav
import com.storagepulse.cleaner.ui.components.CleanerTab
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.DestructiveRed
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.Indigo
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class SwipeNav(
    val onBack: () -> Unit,
    val onCompressor: (path: String?) -> Unit,
    val onSocial: () -> Unit,
    val onSafeTrash: () -> Unit,
)

private enum class Fling { UP, DOWN }

/**
 * Screen 5 — Swipe-to-Clean Large Media Reviewer.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/swipe_to_clean_large_media_reviewer/
 */
@Composable
fun SwipeToCleanScreen(nav: SwipeNav, viewModel: SwipeDeckViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val deck = rememberDeckPlayer()
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 120.dp.toPx() }
    val flyOut = with(density) { 900.dp.toPx() }

    val activity = androidx.activity.compose.LocalActivity.current
    // Session end = deck finished or the user leaves; the VM decides if an interstitial fits.
    LaunchedEffect(state.finished) { if (state.finished) activity?.let(viewModel::endSession) }
    val leave: () -> Unit = {
        activity?.let(viewModel::endSession)
        nav.onBack()
    }
    androidx.activity.compose.BackHandler(onBack = leave)

    val current = state.current
    LaunchedEffect(current?.uri) {
        offsetY.snapTo(0f)
        if (current != null) deck.load(current.uri.toString()) else deck.unload()
    }

    val msgText = when (val m = message) {
        is SwipeMessage.Trashed -> stringResource(R.string.sw_msg_trashed, m.name)
        is SwipeMessage.Failed -> stringResource(R.string.sw_msg_failed, m.reason)
        is SwipeMessage.Undone -> stringResource(R.string.sw_msg_undone, m.name)
        null -> null
    }
    LaunchedEffect(message) {
        if (msgText != null) {
            snackbar.showSnackbar(msgText)
            viewModel.consumeMessage() // after showing, or the effect cancels itself
        }
    }

    /** Animate the card off-screen, then commit the decision. */
    fun decide(direction: Fling) {
        if (state.busy || current == null) return
        scope.launch {
            val target = if (direction == Fling.UP) -flyOut else flyOut
            offsetY.animateTo(target, tween(220))
            if (direction == Fling.UP) {
                deck.unload() // release the file before it is moved
                viewModel.trashCurrent()
            } else {
                viewModel.keepCurrent()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            SwipeHeader(onBack = leave)
            CleanerSubNav(
                active = CleanerTab.SWIPE_DECK,
                onSelect = { tab ->
                    when (tab) {
                        CleanerTab.COMPRESSOR -> nav.onCompressor(null)
                        CleanerTab.SOCIAL -> nav.onSocial()
                        CleanerTab.SAFE_TRASH -> nav.onSafeTrash()
                        CleanerTab.SWIPE_DECK -> Unit
                    }
                },
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        state.loading -> CenterNote(stringResource(R.string.sw_loading), progress = true)
                        state.items.isEmpty() -> CenterNote(stringResource(R.string.sw_empty_title) + "\n" + stringResource(R.string.sw_empty_body))
                        state.finished -> FinishedCard(state, onRestart = viewModel::restart, onUndo = viewModel::undo)
                        current != null -> {
                            ProgressRow(state, onUndo = viewModel::undo)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationY = offsetY.value
                                        rotationZ = offsetY.value / 60f
                                        alpha = 1f - (kotlin.math.abs(offsetY.value) / flyOut).coerceIn(0f, 0.6f)
                                    },
                            ) {
                                SwipeCard(
                                    item = current,
                                    rank = state.index + 1,
                                    details = state.details,
                                    deck = deck,
                                    dragHint = offsetY.value / threshold,
                                    onDrag = { dy -> scope.launch { offsetY.snapTo(offsetY.value + dy) } },
                                    onDragEnd = {
                                        when {
                                            offsetY.value < -threshold -> decide(Fling.UP)
                                            offsetY.value > threshold -> decide(Fling.DOWN)
                                            else -> scope.launch { offsetY.animateTo(0f, spring()) }
                                        }
                                    },
                                )
                            }
                            ActionRow(
                                item = current,
                                details = state.details,
                                busy = state.busy,
                                onTrash = { decide(Fling.UP) },
                                onCompress = { deck.player.pause(); nav.onCompressor(current.path) },
                                onKeep = { decide(Fling.DOWN) },
                            )
                            Tip()
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }
}

// ------------------------------------------------------------------ header & sub-nav

@Composable
private fun SwipeHeader(onBack: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.disclosure_back), tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dash_brand), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1)
                Text(stringResource(R.string.ai_subtitle), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
    }
}

@Composable
private fun ProgressRow(state: SwipeUiState, onUndo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AutoSizeText(stringResource(R.string.sw_card_of, state.index + 1, state.items.size), Telemetry.md, TextPrimary, minSize = 10.sp)
                Text(stringResource(R.string.sw_largest_first), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
            TelemetryBadge(stringResource(R.string.sw_freed, formatBytes(state.freedBytes)), EmeraldLight)
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onUndo,
                enabled = state.canUndo && !state.busy,
                modifier = Modifier.size(40.dp).background(SurfaceCard, CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.sw_undo), tint = if (state.canUndo) TextPrimary else TextMuted)
            }
        }
        LinearProgressIndicator(
            progress = { if (state.items.isEmpty()) 0f else state.index.toFloat() / state.items.size },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
            color = Emerald,
            trackColor = SurfaceStroke,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
    }
}

// ------------------------------------------------------------------ card

@Composable
private fun SwipeCard(
    item: VideoItem,
    rank: Int,
    details: VideoDetails?,
    deck: DeckPlayer,
    dragHint: Float,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val trashTint = (-dragHint).coerceIn(0f, 1f)
    val keepTint = dragHint.coerceIn(0f, 1f)
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCard)
            .border(
                2.dp,
                when {
                    trashTint > 0.05f -> DestructiveRed.copy(alpha = trashTint)
                    keepTint > 0.05f -> EmeraldLight.copy(alpha = keepTint)
                    else -> SurfaceStroke
                },
                RoundedCornerShape(24.dp),
            ),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            DeckVideoSurface(deck, posterUri = item.uri.toString(), modifier = Modifier.fillMaxSize())
            // Gesture layer above the video: drag = decide, tap = play/pause.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(item.uri) {
                        detectDragGestures(
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                            onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                        )
                    }
                    .pointerInput(item.uri) { detectTapGestures(onTap = { deck.toggle() }) },
            )
            // FlowRow: the quality badge wraps under the rank badge instead of being clipped.
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TelemetryBadge(
                    stringResource(R.string.sw_rank, rank, formatBytes(item.sizeBytes)),
                    VideoRose,
                    showDot = true,
                    modifier = Modifier.background(ObsidianCanvas.copy(alpha = 0.75f), RoundedCornerShape(50)),
                )
                details?.let { d ->
                    Row(
                        Modifier.background(ObsidianCanvas.copy(alpha = 0.75f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Videocam, null, tint = AmberLight, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        AutoSizeText(qualityLabel(d), Telemetry.labelCaps, TextPrimary, minSize = 7.sp)
                    }
                }
            }
            if (!deck.isPlaying) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(ObsidianCanvas.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PlayArrow, stringResource(R.string.sw_play), tint = EmeraldLight, modifier = Modifier.size(36.dp))
                }
            }
            // Live swipe hint.
            if (trashTint > 0.15f || keepTint > 0.15f) {
                Box(
                    Modifier.fillMaxSize().background(
                        if (trashTint > keepTint) DestructiveRed.copy(alpha = 0.25f * trashTint) else Emerald.copy(alpha = 0.25f * keepTint),
                    ),
                )
            }
        }
        CardFooter(item, details, deck)
    }
}

@Composable
private fun CardFooter(item: VideoItem, details: VideoDetails?, deck: DeckPlayer) {
    val duration = deck.durationMs.takeIf { it > 0 } ?: details?.durationMs ?: item.durationMs
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.name,
                style = Telemetry.md.copy(fontSize = 15.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f),
            )
            codecLabel(details?.codecMime)?.let {
                Spacer(Modifier.width(8.dp))
                TelemetryBadge(it, TextSecondary)
            }
        }
        Text(
            listOfNotNull(
                item.folder,
                SimpleDateFormat("MMM d, yyyy", locale).format(Date(item.dateTaken)),
                details?.bitrateBps?.let { "${(it / 1_000_000.0).roundToInt()} Mbps" },
            ).joinToString(" · "),
            style = Telemetry.sm,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row {
            Text(clock(deck.positionMs), style = Telemetry.sm, color = EmeraldLight)
            Spacer(Modifier.weight(1f))
            Text(clock(duration), style = Telemetry.sm, color = TextSecondary)
        }
        Slider(
            value = if (duration > 0) (deck.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { f -> deck.seekTo((f * duration).toLong()) },
            colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = EmeraldLight, inactiveTrackColor = SurfaceStroke),
            modifier = Modifier.height(24.dp),
        )
        Row {
            Text(stringResource(R.string.sw_up_trash), style = Telemetry.labelCaps, color = DestructiveRed)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.sw_down_keep), style = Telemetry.labelCaps, color = EmeraldLight)
        }
    }
}

@Composable
private fun ActionRow(
    item: VideoItem,
    details: VideoDetails?,
    busy: Boolean,
    onTrash: () -> Unit,
    onCompress: () -> Unit,
    onKeep: () -> Unit,
) {
    val estimate = details?.let { LargeVideoRepository.estimateCompressedBytes(it) }
    val saving = estimate?.let { (100 - it * 100 / item.sizeBytes.coerceAtLeast(1)).toInt() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DeckButton(
            icon = Icons.Outlined.Delete,
            title = stringResource(R.string.sw_trash, formatBytes(item.sizeBytes)),
            subtitle = null,
            container = SurfaceCard,
            content = DestructiveRed,
            enabled = !busy,
            onClick = onTrash,
            modifier = Modifier.weight(1f),
        )
        DeckButton(
            icon = Icons.Outlined.AutoFixHigh,
            title = stringResource(R.string.sw_compress),
            subtitle = when {
                estimate == null || saving == null -> null
                saving < 15 -> stringResource(R.string.sw_compress_efficient)
                else -> stringResource(R.string.sw_compress_to, formatBytes(estimate), saving)
            },
            container = Indigo,
            content = TextPrimary,
            enabled = !busy,
            onClick = onCompress,
            modifier = Modifier.weight(1.2f),
        )
        DeckButton(
            icon = Icons.Outlined.Check,
            title = stringResource(R.string.sw_keep),
            subtitle = null,
            container = SurfaceCard,
            content = EmeraldLight,
            enabled = !busy,
            onClick = onKeep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DeckButton(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = SurfaceRaised,
            disabledContentColor = TextMuted,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            AutoSizeText(title, MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), if (enabled) content else TextMuted, minSize = 10.sp)
            subtitle?.let { AutoSizeText(it, Telemetry.sm, IndigoLight.copy(alpha = 0.9f), minSize = 8.sp) }
        }
    }
}

@Composable
private fun Tip() {
    Row(
        Modifier.fillMaxWidth().obsidianCard(radius = 50).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Lightbulb, null, tint = AmberLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.sw_tip), style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CenterNote(text: String, progress: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().obsidianCard().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        if (progress) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Emerald, trackColor = SurfaceStroke)
    }
}

@Composable
private fun FinishedCard(state: SwipeUiState, onRestart: () -> Unit, onUndo: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().obsidianCard().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.sw_done_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            stringResource(R.string.sw_done_body, state.reviewed, formatBytes(state.freedBytes)),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onUndo, enabled = state.canUndo, colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = TextPrimary)) {
                Text(stringResource(R.string.ai_undo))
            }
            Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = OnEmeraldLight)) {
                Text(stringResource(R.string.sw_restart))
            }
        }
    }
}

private fun qualityLabel(d: VideoDetails): String {
    val short = minOf(d.width, d.height)
    val res = when {
        short >= 2160 -> "4K"
        short >= 1440 -> "1440p"
        short >= 1080 -> "1080p"
        short >= 720 -> "720p"
        short > 0 -> "${short}p"
        else -> ""
    }
    return listOfNotNull(
        listOf(res, d.fps?.let { "${it}fps" }.orEmpty()).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { null },
        clock(d.durationMs),
    ).joinToString(" · ")
}

private fun codecLabel(mime: String?): String? = when (mime) {
    "video/hevc" -> "HEVC H.265"
    "video/avc" -> "AVC H.264"
    "video/av01" -> "AV1"
    "video/x-vnd.on2.vp9" -> "VP9"
    null -> null
    else -> mime.removePrefix("video/").uppercase()
}

private fun clock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

