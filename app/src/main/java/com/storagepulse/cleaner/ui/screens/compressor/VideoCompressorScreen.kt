package com.storagepulse.cleaner.ui.screens.compressor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoFile
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.ads.AdGateResult
import com.storagepulse.cleaner.data.compressor.CompressionTier
import com.storagepulse.cleaner.data.compressor.PreviewResult
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.CleanerSubNav
import com.storagepulse.cleaner.ui.components.CleanerTab
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.Indigo
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
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
import java.util.Locale
import kotlin.math.roundToInt

data class CompressorNav(
    val onBack: () -> Unit,
    val onTab: (CleanerTab) -> Unit,
)

/**
 * Screen 6 — Video Compressor.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/lossless_video_photo_size_compressor/
 */
@Composable
fun VideoCompressorScreen(nav: CompressorNav, viewModel: CompressorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf(false) }

    // Encoding can take minutes: keep the screen on so OEM battery managers don't kill it.
    val busy = state.batch is BatchState.Running || state.preview is PreviewState.Loading
    val view = LocalView.current
    DisposableEffect(busy) {
        view.keepScreenOn = busy
        onDispose { view.keepScreenOn = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Header(onBack = nav.onBack)
        CleanerSubNav(active = CleanerTab.COMPRESSOR, onSelect = nav.onTab)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    state.loading -> InfoCard(stringResource(R.string.cx_loading))
                    state.queue.isEmpty() -> InfoCard(stringResource(R.string.cx_empty))
                    else -> {
                        ComparisonCard(state)
                        SizeCard(state, viewModel)
                        EncoderCard(state)
                        TierCard(state, viewModel)
                        KeepToggle(state.keepInSafeTrash, viewModel::setKeepInSafeTrash)
                        QueueRow(state, onToggle = viewModel::toggleQueued, onEdit = { editing = true })
                        if (state.needsFullAccess) InfoCard(stringResource(R.string.cx_needs_full))
                        ActionArea(
                            state = state,
                            viewModel = viewModel,
                            onStart = { context.findActivity()?.let(viewModel::startBatch) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (editing) EditListSheet(state, onToggle = { v -> viewModel.addToQueue(v) }, onDismiss = { editing = false })

    (state.batch as? BatchState.Done)?.let { done ->
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            containerColor = SurfaceCard,
            title = { Text(stringResource(R.string.cx_done_title), color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cx_done_body, done.compressed, done.skipped, done.failed, formatBytes(done.savedBytes)), color = TextPrimary)
                    if (state.keepInSafeTrash && done.compressed > 0) Text(stringResource(R.string.cx_done_trash_note), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    if (done.adResult == AdGateResult.UNAVAILABLE) Text(stringResource(R.string.cx_done_ad_unavailable), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissResult) { Text(stringResource(R.string.cx_ok), color = EmeraldLight) } },
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.disclosure_back), tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cx_title), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1)
                Text(stringResource(R.string.ai_subtitle), style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
    }
}

// ------------------------------------------------------------------ comparison

@Composable
private fun ComparisonCard(state: CompressorUiState) {
    val focus = state.focus
    val preview = (state.preview as? PreviewState.Ready)?.result
    Column(
        Modifier.fillMaxWidth().obsidianCard(radius = 20).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(if (preview != null) Emerald else AmberLight, CircleShape))
            Spacer(Modifier.width(8.dp))
            AutoSizeText(
                if (preview != null) stringResource(R.string.cx_quality, String.format(Locale.US, "%.1f", preview.psnrDb)) else stringResource(R.string.cx_quality_measuring),
                Telemetry.labelCaps.copy(fontSize = 11.sp),
                if (preview != null) EmeraldLight else AmberLight,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TelemetryBadge(
                stringResource(R.string.cx_encoder_badge, if (state.hevc) "H.265" else "H.264", if (state.hardware) "HW" else "SW"),
                IndigoLight,
            )
        }
        when (val p = state.preview) {
            is PreviewState.Ready -> SplitCompare(p.result, state)
            is PreviewState.Loading, PreviewState.Idle -> PreviewPlaceholder(
                text = if (focus == null) stringResource(R.string.cx_preview_none) else stringResource(R.string.cx_preview_loading),
                posterUri = focus?.video?.uri?.toString(),
            )
            is PreviewState.Failed -> PreviewPlaceholder(stringResource(R.string.cx_preview_failed, p.message), focus?.video?.uri?.toString())
        }
        Text(stringResource(R.string.cx_slide_hint), style = Telemetry.sm, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** Drag the handle to wipe between the original frame (left) and the real encoded frame (right). */
@Composable
private fun SplitCompare(preview: PreviewResult, state: CompressorUiState) {
    var split by remember { mutableFloatStateOf(0.5f) }
    val aspect = preview.original.width.toFloat() / preview.original.height
    val focus = state.focus
    val plan = focus?.let { CompressionTierLabels.resolution(it, state) }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
    ) {
        val boxWidth = maxWidth
        val boxHeight = (boxWidth / aspect).coerceAtMost(380.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(boxHeight)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, drag ->
                        change.consume()
                        split = (split + drag / size.width).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) { detectTapGestures { at -> split = (at.x / size.width).coerceIn(0f, 1f) } },
        ) {
            FrameImage(preview.original)
            FrameImage(
                preview.compressed,
                Modifier.drawWithContent { clipRect(left = size.width * split) { this@drawWithContent.drawContent() } },
            )
            // Divider + handle
            Box(Modifier.fillMaxHeight().width(2.dp).offset { IntOffset(((boxWidth * split - 1.dp).roundToPx()), 0) }.background(EmeraldLight))
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((boxWidth * split - 18.dp).roundToPx(), 0) }
                    .size(36.dp)
                    .background(ObsidianCanvas, CircleShape)
                    .border(2.dp, EmeraldLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.DragIndicator, null, tint = EmeraldLight, modifier = Modifier.size(20.dp)) }
            // Top badges: one per half, each confined to its half so they never overlap.
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    TelemetryBadge(
                        stringResource(R.string.cx_original_badge, plan?.first ?: ""),
                        TextPrimary,
                        modifier = Modifier.background(ObsidianCanvas.copy(alpha = 0.7f), RoundedCornerShape(50)),
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
                    TelemetryBadge(
                        stringResource(R.string.cx_encoded_badge, if (state.hevc) "H.265" else "H.264", plan?.second ?: ""),
                        EmeraldLight,
                        modifier = Modifier.background(ObsidianCanvas.copy(alpha = 0.7f), RoundedCornerShape(50)),
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameImage(bitmap: Bitmap, modifier: Modifier = Modifier) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier.fillMaxSize())
}

@Composable
private fun PreviewPlaceholder(text: String, posterUri: String?) {
    Box(
        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (posterUri != null) {
            AsyncImage(model = posterUri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(ObsidianCanvas.copy(alpha = 0.55f)))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, textAlign = TextAlign.Center)
            LinearProgressIndicator(Modifier.width(160.dp), color = Emerald, trackColor = SurfaceStroke)
        }
    }
}

// ------------------------------------------------------------------ sizes & encoder

@Composable
private fun SizeCard(state: CompressorUiState, vm: CompressorViewModel) {
    val selected = state.selected
    val original = selected.sumOf { it.video.sizeBytes }
    val compressed = estimateTotal(state, vm, state.tier)
    val delta = if (original > 0) ((1.0 - compressed.toDouble() / original) * 100).roundToInt() else 0
    val longest = selected.mapNotNull { it.details }.maxByOrNull { minOf(it.width, it.height) }
    Row(
        Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.cx_original_size), style = Telemetry.labelCaps, color = TextSecondary)
            AutoSizeText(formatBytes(original), Telemetry.xl.copy(fontSize = 26.sp), VideoRose, minSize = 14.sp)
            AutoSizeText(
                androidx.compose.ui.res.pluralStringResource(R.plurals.cx_videos_meta, selected.size, selected.size, longest?.let { CompressionTierLabels.res(minOf(it.width, it.height)) } ?: "—"),
                MaterialTheme.typography.bodySmall,
                TextSecondary,
                minSize = 9.sp,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
            Row(
                Modifier.background(Indigo, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ArrowDownward, null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                Text("-$delta%", style = Telemetry.sm, color = TextPrimary)
            }
            Text(stringResource(R.string.cx_delta), style = Telemetry.labelCaps, color = TextSecondary)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.cx_compressed_size), style = Telemetry.labelCaps, color = TextSecondary)
            AutoSizeText(formatBytes(compressed), Telemetry.xl.copy(fontSize = 26.sp), EmeraldLight, minSize = 14.sp)
            TelemetryBadge(stringResource(R.string.cx_save, formatBytes((original - compressed).coerceAtLeast(0))), EmeraldLight)
        }
    }
}

/** Sum of per-video estimates; the focused video uses the real preview projection when ready. */
private fun estimateTotal(state: CompressorUiState, vm: CompressorViewModel, tier: CompressionTier): Long {
    val preview = (state.preview as? PreviewState.Ready)?.result
    return state.selected.sumOf { item ->
        val plan = vm.plan(item, tier) ?: return@sumOf item.video.sizeBytes
        when {
            !plan.worthIt -> item.video.sizeBytes
            preview != null && tier == state.tier && item == state.focus -> minOf(preview.projectedBytes, item.video.sizeBytes)
            else -> plan.estimatedBytes
        }
    }
}

@Composable
private fun EncoderCard(state: CompressorUiState) {
    val speed = (state.preview as? PreviewState.Ready)?.result?.speedX
    Row(
        Modifier.fillMaxWidth().obsidianCard(radius = 14).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(Emerald.copy(alpha = 0.14f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Memory, null, tint = EmeraldLight)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AutoSizeText(
                stringResource(
                    R.string.cx_hw_title,
                    stringResource(if (state.hardware) R.string.cx_hw_hardware else R.string.cx_hw_software),
                    state.encoderName,
                ),
                MaterialTheme.typography.bodyMedium,
                TextPrimary,
                minSize = 10.sp,
            )
            AutoSizeText(
                if (speed != null) stringResource(R.string.cx_hw_speed, String.format(Locale.US, "%.1f", speed)) else stringResource(R.string.cx_hw_speed_unknown),
                Telemetry.sm,
                TextSecondary,
                minSize = 8.sp,
            )
        }
    }
}

// ------------------------------------------------------------------ tiers

@Composable
private fun TierCard(state: CompressorUiState, vm: CompressorViewModel) {
    Column(
        Modifier.fillMaxWidth().obsidianCard(radius = 20).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, tint = EmeraldLight)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.cx_tier_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.cx_tier_sub, if (state.hevc) "HEVC" else "AVC"), style = Telemetry.sm, color = TextSecondary)
        }
        TierRow(
            selected = state.tier == CompressionTier.HIGH_FIDELITY,
            title = stringResource(R.string.cx_tier_high),
            badge = null,
            sub = stringResource(R.string.cx_tier_high_sub),
            value = "~" + formatBytes(estimateTotal(state, vm, CompressionTier.HIGH_FIDELITY)),
            caption = stringResource(R.string.cx_est_out),
            onClick = { vm.setTier(CompressionTier.HIGH_FIDELITY) },
        )
        TierRow(
            selected = state.tier == CompressionTier.BALANCED_1080,
            title = stringResource(R.string.cx_tier_balanced),
            badge = stringResource(R.string.cx_tier_balanced_badge),
            sub = stringResource(R.string.cx_tier_balanced_sub),
            value = "~" + formatBytes(estimateTotal(state, vm, CompressionTier.BALANCED_1080)),
            caption = stringResource(R.string.cx_best),
            onClick = { vm.setTier(CompressionTier.BALANCED_1080) },
        )
        TierRow(
            selected = state.tier == CompressionTier.COMPACT_720,
            title = stringResource(R.string.cx_tier_compact),
            badge = null,
            sub = stringResource(R.string.cx_tier_compact_sub),
            value = "~" + formatBytes(estimateTotal(state, vm, CompressionTier.COMPACT_720)),
            caption = stringResource(R.string.cx_light),
            onClick = { vm.setTier(CompressionTier.COMPACT_720) },
        )
    }
}

@Composable
private fun TierRow(
    selected: Boolean,
    title: String,
    badge: String?,
    sub: String,
    value: String,
    caption: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Indigo.copy(alpha = 0.28f) else SurfaceSunken)
            .border(1.dp, if (selected) IndigoLight.copy(alpha = 0.6f) else SurfaceStroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            null,
            tint = if (selected) EmeraldLight else TextMuted,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (badge != null) TelemetryBadge("★ $badge", EmeraldLight)
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = Telemetry.md, color = if (selected) EmeraldLight else TextPrimary, maxLines = 1)
            Text(caption, style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1)
        }
    }
}

// ------------------------------------------------------------------ toggle, queue, CTA

@Composable
private fun KeepToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(Emerald.copy(alpha = 0.14f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Shield, null, tint = EmeraldLight)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.cx_keep_title), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(stringResource(if (on) R.string.cx_keep_on else R.string.cx_keep_off), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = on,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = EmeraldLight, checkedThumbColor = OnEmeraldLight),
        )
    }
}

@Composable
private fun QueueRow(state: CompressorUiState, onToggle: (String) -> Unit, onEdit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cx_queue_title), style = Telemetry.labelCaps, color = TextSecondary, modifier = Modifier.weight(1f))
            TelemetryBadge(androidx.compose.ui.res.pluralStringResource(R.plurals.cx_items, state.selected.size, state.selected.size), TextSecondary)
            TextButton(onClick = onEdit) { Text(stringResource(R.string.cx_edit_list), color = EmeraldLight) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.queue, key = { it.video.path }) { q ->
                Row(
                    Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .obsidianCard(radius = 14)
                        .clickable { onToggle(q.video.path) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.VideoFile, null, tint = EmeraldLight)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.video.name, style = Telemetry.sm, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                        Text(
                            listOfNotNull(formatBytes(q.video.sizeBytes), q.details?.let { CompressionTierLabels.res(minOf(it.width, it.height)) }).joinToString(" · "),
                            style = Telemetry.sm,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
                    Icon(
                        if (q.selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        null,
                        tint = if (q.selected) EmeraldLight else TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionArea(state: CompressorUiState, viewModel: CompressorViewModel, onStart: () -> Unit) {
    // Pro users never see the rewarded ad, so the button must not promise one.
    val adsEnabled by viewModel.adsEnabled.collectAsStateWithLifecycle()
    val count = state.selected.count { it.details != null }
    val original = state.selected.sumOf { it.video.sizeBytes }
    val saving = (original - estimateTotal(state, viewModel, state.tier)).coerceAtLeast(0)
    val speed = (state.preview as? PreviewState.Ready)?.result?.speedX
    val totalSeconds = state.selected.sumOf { (it.details?.durationMs ?: 0L) } / 1000.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (val b = state.batch) {
            is BatchState.Running -> Column(
                Modifier.fillMaxWidth().obsidianCard(radius = 18).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AutoSizeText(stringResource(R.string.cx_running, b.index + 1, b.total, b.name), MaterialTheme.typography.bodyMedium, TextPrimary, Modifier.weight(1f), minSize = 10.sp)
                    Text("${(b.overall * 100).roundToInt()}%", style = Telemetry.lg, color = EmeraldLight)
                }
                LinearProgressIndicator(
                    progress = { b.overall },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = Emerald,
                    trackColor = SurfaceStroke,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
                TextButton(onClick = viewModel::cancelBatch, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.cx_cancel), color = TextSecondary)
                }
            }
            else -> Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (count > 0) Brush.horizontalGradient(listOf(Indigo, Emerald)) else Brush.horizontalGradient(listOf(SurfaceRaised, SurfaceRaised)),
                    )
                    .clickable(enabled = count > 0 && state.batch != BatchState.WaitingForAd, onClick = onStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (adsEnabled) Icons.Outlined.SmartDisplay else Icons.Outlined.Compress, null, tint = if (count > 0) TextPrimary else TextMuted)
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AutoSizeText(
                            when {
                                state.batch == BatchState.WaitingForAd -> stringResource(R.string.cx_waiting_ad)
                                count == 0 -> stringResource(R.string.cx_cta_none)
                                !adsEnabled -> androidx.compose.ui.res.pluralStringResource(R.plurals.cx_cta_pro, count, count)
                                count == 1 -> stringResource(R.string.cx_cta_ad_one)
                                else -> stringResource(R.string.cx_cta_ad, count)
                            },
                            MaterialTheme.typography.titleMedium,
                            if (count > 0) TextPrimary else TextMuted,
                            minSize = 12.sp,
                        )
                        if (count > 0) AutoSizeText(stringResource(R.string.cx_cta_sub, formatBytes(saving)), Telemetry.sm, TextPrimary.copy(alpha = 0.85f), minSize = 8.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Timer, null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.cx_est_time, if (speed != null && speed > 0) formatDuration(totalSeconds / speed) else "—"),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditListSheet(state: CompressorUiState, onToggle: (com.storagepulse.cleaner.data.media.VideoItem) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = SurfaceCard) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cx_edit_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cx_done_edit), color = EmeraldLight) }
            }
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.candidates, key = { it.path }) { v ->
                    val inQueue = state.queue.firstOrNull { it.video.path == v.path }?.selected == true
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onToggle(v) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = v.uri.toString(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceRaised),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                            Text(formatBytes(v.sizeBytes), style = Telemetry.sm, color = TextSecondary)
                        }
                        Icon(
                            if (inQueue) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            null,
                            tint = if (inQueue) EmeraldLight else TextMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Row(Modifier.fillMaxWidth().obsidianCard().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Bolt, null, tint = AmberLight)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

/** Small label helpers shared by the cards. */
internal object CompressionTierLabels {
    fun res(shortSide: Int): String = when {
        shortSide >= 2160 -> "4K"
        shortSide >= 1440 -> "1440p"
        shortSide >= 1080 -> "1080p"
        shortSide >= 720 -> "720p"
        shortSide > 0 -> "${shortSide}p"
        else -> "—"
    }

    /** (original resolution, output resolution) for the focused video at the current tier. */
    fun resolution(item: QueueItem, state: CompressorUiState): Pair<String, String> {
        val d = item.details ?: return "—" to "—"
        val src = minOf(d.width, d.height)
        val out = when (state.tier) {
            CompressionTier.HIGH_FIDELITY -> src
            CompressionTier.BALANCED_1080 -> minOf(src, 1080)
            CompressionTier.COMPACT_720 -> minOf(src, 720)
        }
        return res(src) to res(out)
    }
}

private fun formatDuration(seconds: Double): String {
    val s = seconds.roundToInt().coerceAtLeast(1)
    return if (s >= 60) "${s / 60}m ${s % 60}s" else "${s}s"
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
