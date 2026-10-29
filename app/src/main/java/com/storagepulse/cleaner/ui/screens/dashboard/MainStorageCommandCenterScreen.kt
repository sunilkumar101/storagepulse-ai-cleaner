package com.storagepulse.cleaner.ui.screens.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.storage.model.BurstGroup
import com.storagepulse.cleaner.data.storage.model.CleanupBreakdown
import com.storagepulse.cleaner.data.storage.model.FileRef
import com.storagepulse.cleaner.data.storage.model.ScanMode
import com.storagepulse.cleaner.data.storage.model.ScanState
import com.storagepulse.cleaner.data.storage.model.StorageCategory
import com.storagepulse.cleaner.data.storage.model.StorageSnapshot
import com.storagepulse.cleaner.data.storage.model.VolumeStats
import com.storagepulse.cleaner.ui.components.AutoSizeText
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import com.storagepulse.cleaner.ui.components.obsidianCard
import com.storagepulse.cleaner.ui.theme.Amber
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.AppsCyan
import com.storagepulse.cleaner.ui.theme.DestructiveRed
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.Indigo
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.OnEmeraldLight
import com.storagepulse.cleaner.ui.theme.PhotoViolet
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextMuted
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.theme.VideoRose
import com.storagepulse.cleaner.ui.util.daysSince
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatCount
import java.io.File

/** Where each dashboard control leads. */
data class DashboardActions(
    val onReviewAndClean: () -> Unit,
    val onOpenTreemap: () -> Unit,
    val onOpenSwipeDeck: () -> Unit,
    val onOpenCompressor: () -> Unit,
    val onOpenChatVaults: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/**
 * Screen 2 — Main Storage Command Center.
 * Design: design_screenshots/stitch_storagepulse_ai_cleaner_app/main_storage_command_center/
 */
@Composable
fun MainStorageCommandCenterScreen(
    actions: DashboardActions,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = (state.scan as? ScanState.Done)?.snapshot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) {
        DashboardHeader(
            scanning = state.scan is ScanState.Scanning,
            onRescan = viewModel::rescan,
            onAvatar = actions.onOpenSettings,
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HeroTelemetryCard(volume = state.volume, snapshot = snapshot)
                SafeToCleanCard(scan = state.scan, onReview = actions.onReviewAndClean)
                HeavyweightsSection(
                    scan = state.scan,
                    onVideo = actions.onOpenSwipeDeck,
                    onBurst = actions.onReviewAndClean,
                )
                TacticalGrid(snapshot = snapshot, heavyVideos = state.heavyVideos, actions = actions)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ header

@Composable
private fun DashboardHeader(scanning: Boolean, onRescan: () -> Unit, onAvatar: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 16.dp, end = 8.dp),
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
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.dash_subtitle),
                    style = Telemetry.labelCaps,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onRescan, enabled = !scanning) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.dash_rescan),
                    tint = if (scanning) TextMuted else TextPrimary,
                )
            }
            IconButton(onClick = onAvatar) {
                Box(
                    Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.st_open),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (scanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Emerald,
                trackColor = SurfaceStroke,
            )
        } else {
            HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
        }
    }
}

// ------------------------------------------------------------------ hero card

@Composable
private fun HeroTelemetryCard(volume: VolumeStats, snapshot: StorageSnapshot?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .obsidianCard(radius = 20)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StorageGauge(fraction = volume.usedFraction)
            Spacer(Modifier.size(16.dp))
            BoxWithConstraints(Modifier.weight(1f)) {
                // Narrow phones (360dp): label above value instead of clipping both.
                val stacked = maxWidth < 210.dp
                Column(verticalArrangement = Arrangement.spacedBy(if (stacked) 6.dp else 8.dp)) {
                    MetricRow(stringResource(R.string.dash_occupied), formatBytes(volume.usedBytes), stacked)
                    MetricRow(stringResource(R.string.dash_hardware_max), formatBytes(volume.totalBytes), stacked)
                    LinearProgressIndicator(
                        progress = { volume.usedFraction },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                        color = Emerald,
                        trackColor = SurfaceStroke,
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                    MetricRow(
                        label = stringResource(R.string.dash_available_free),
                        value = formatBytes(volume.freeBytes),
                        stacked = stacked,
                        labelStyle = Telemetry.labelCaps,
                        valueStyle = Telemetry.lg,
                        color = EmeraldLight,
                    )
                }
            }
        }

        val bytes = snapshot?.categoryBytes.orEmpty()
        CategorySpectrumBar(bytes = bytes)
        CategoryLegend(
            bytes = bytes,
            ready = snapshot != null,
            // Scoped Mode cannot see app sizes; show "—" instead of a misleading 0 B.
            unknown = if (snapshot?.mode == ScanMode.MEDIA_ONLY) setOf(StorageCategory.APPS) else emptySet(),
        )

        if (snapshot?.mode == ScanMode.MEDIA_ONLY) {
            TelemetryBadge(text = stringResource(R.string.dash_scoped_note), color = AmberLight)
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    stacked: Boolean,
    labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    valueStyle: TextStyle = Telemetry.md,
    color: Color? = null,
) {
    val labelColor = color ?: TextSecondary
    val valueColor = color ?: TextPrimary
    if (stacked) {
        Column {
            Text(label, style = labelStyle, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = valueStyle, color = valueColor, maxLines = 1)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = labelStyle,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            Text(value, style = valueStyle, color = valueColor, maxLines = 1)
        }
    }
}

private val legendLabels = mapOf(
    StorageCategory.VIDEOS to R.string.cat_videos,
    StorageCategory.PHOTOS to R.string.cat_photos,
    StorageCategory.SOCIAL to R.string.cat_social,
    StorageCategory.APPS to R.string.cat_apps,
    StorageCategory.SYSTEM to R.string.cat_system,
)

@Composable
private fun CategoryLegend(
    bytes: Map<StorageCategory, Long>,
    ready: Boolean,
    unknown: Set<StorageCategory>,
) {
    BoxWithConstraints {
        // 2 columns only when each column has room for "Photos & RAW  24.1 GB".
        val columns = if (maxWidth >= 380.dp) 2 else 1
        val rows = StorageCategory.entries.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    group.forEach { category ->
                        LegendItem(
                            label = legendLabels.getValue(category),
                            color = Color(category.colorArgb),
                            value = when {
                                !ready || category in unknown -> stringResource(R.string.dash_scanning_value)
                                else -> formatBytes(bytes[category] ?: 0L)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (group.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(@StringRes label: Int, color: Color, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(4.dp))
        Text(value, style = Telemetry.sm, color = TextPrimary, maxLines = 1)
    }
}

// ------------------------------------------------------------------ safe to clean

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SafeToCleanCard(scan: ScanState, onReview: () -> Unit) {
    val cleanup: CleanupBreakdown? = (scan as? ScanState.Done)?.snapshot?.cleanup
    val total = cleanup?.totalBytes ?: 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Emerald.copy(alpha = 0.10f), SurfaceCard, Indigo.copy(alpha = 0.16f)),
                ),
            )
            .border(1.dp, SurfaceStroke, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Emerald.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = EmeraldLight, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (cleanup != null) {
                        stringResource(R.string.clean_title, formatBytes(total))
                    } else {
                        stringResource(R.string.clean_title_scanning)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Text(
                    stringResource(R.string.clean_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.size(8.dp))
            TelemetryBadge(
                text = stringResource(if (cleanup != null) R.string.clean_ready else R.string.clean_scanning),
                color = if (cleanup != null) EmeraldLight else AmberLight,
            )
        }

        if (cleanup != null) {
            // FlowRow wraps pills to the next line instead of clipping them.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (cleanup.duplicateBytes > 0) {
                    CleanPill(stringResource(R.string.pill_duplicates, formatBytes(cleanup.duplicateBytes)), IndigoLight)
                }
                if (cleanup.whatsAppSentBytes > 0) {
                    CleanPill(stringResource(R.string.pill_wa_sent, formatBytes(cleanup.whatsAppSentBytes)), EmeraldLight)
                }
                if (cleanup.apkInstallerBytes > 0) {
                    CleanPill(stringResource(R.string.pill_apks, formatBytes(cleanup.apkInstallerBytes)), AppsCyan)
                }
                if (cleanup.thumbnailBytes > 0) {
                    CleanPill(stringResource(R.string.pill_thumbnails, formatBytes(cleanup.thumbnailBytes)), AmberLight)
                }
            }
        }

        val scanningFiles = (scan as? ScanState.Scanning)?.filesSeen
        Button(
            onClick = onReview,
            enabled = cleanup != null && total > 0,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldLight,
                contentColor = OnEmeraldLight,
                disabledContainerColor = SurfaceRaised,
                disabledContentColor = TextSecondary,
            ),
        ) {
            Text(
                text = when {
                    cleanup != null && total > 0 -> stringResource(R.string.clean_cta, formatBytes(total))
                    cleanup != null -> stringResource(R.string.clean_cta_nothing)
                    else -> stringResource(R.string.clean_cta_scanning, formatCount(scanningFiles ?: 0))
                },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (cleanup != null && total > 0) {
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
            }
        }
    }
}

@Composable
private fun CleanPill(text: String, color: Color) {
    Text(
        text = text,
        style = Telemetry.sm,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ------------------------------------------------------------------ heavyweights

@Composable
private fun HeavyweightsSection(scan: ScanState, onVideo: () -> Unit, onBurst: () -> Unit) {
    val snapshot = (scan as? ScanState.Done)?.snapshot
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(R.string.heavy_title),
            trailing = when (scan) {
                is ScanState.Done -> stringResource(R.string.heavy_complete)
                is ScanState.Scanning -> stringResource(R.string.heavy_scanning, formatCount(scan.filesSeen))
                is ScanState.Failed -> stringResource(R.string.heavy_failed)
                ScanState.Idle -> ""
            },
            trailingColor = if (scan is ScanState.Failed) DestructiveRed else EmeraldLight,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VideoHeavyweightCard(snapshot?.heavyweights?.largestVideo, snapshot != null, onVideo, Modifier.weight(1f))
            BurstHeavyweightCard(snapshot?.heavyweights?.largestBurst, snapshot != null, onBurst, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String, trailingColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Telemetry.labelCaps, color = TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
        Text(trailing, style = Telemetry.labelCaps, color = trailingColor, maxLines = 1)
    }
}

@Composable
private fun VideoHeavyweightCard(video: FileRef?, ready: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val days = video?.let { daysSince(it.lastModified) }
    HeavyweightCard(
        thumbnail = video?.path,
        label = stringResource(R.string.heavy_video_label),
        labelColor = VideoRose,
        title = video?.name ?: if (ready) stringResource(R.string.heavy_none_video) else "…",
        size = video?.let { formatBytes(it.sizeBytes) },
        badge = when {
            days == null -> null
            days >= 365 -> stringResource(R.string.heavy_video_age_years, (days / 365).toInt())
            days >= 30 -> stringResource(R.string.heavy_video_age, days.toInt())
            else -> stringResource(R.string.heavy_video_new)
        },
        badgeColor = EmeraldLight,
        enabled = video != null,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun BurstHeavyweightCard(burst: BurstGroup?, ready: Boolean, onClick: () -> Unit, modifier: Modifier) {
    HeavyweightCard(
        thumbnail = burst?.files?.firstOrNull()?.path,
        label = stringResource(R.string.heavy_burst_label),
        labelColor = IndigoLight,
        title = burst?.let { stringResource(R.string.heavy_burst_title, it.files.size) }
            ?: if (ready) stringResource(R.string.heavy_none_burst) else "…",
        size = burst?.let { formatBytes(it.totalBytes) },
        badge = burst?.let { stringResource(R.string.heavy_burst_keep, it.files.size) },
        badgeColor = IndigoLight,
        enabled = burst != null,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun HeavyweightCard(
    thumbnail: String?,
    label: String,
    labelColor: Color,
    title: String,
    size: String?,
    badge: String?,
    badgeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (thumbnail != null) {
            AsyncImage(
                model = File(thumbnail),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ObsidianCanvas.copy(alpha = 0.75f), ObsidianCanvas),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = Telemetry.labelCaps, color = labelColor, maxLines = 1)
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (size != null || badge != null) {
                // Size is measured first (never clipped); the badge shrinks into what is left.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (size != null) Text(size, style = Telemetry.sm, color = TextSecondary, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Spacer(Modifier.weight(1f))
                    if (badge != null) TelemetryBadge(text = badge, color = badgeColor)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ tactical grid

@Composable
private fun TacticalGrid(snapshot: StorageSnapshot?, heavyVideos: Int?, actions: DashboardActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(R.string.diag_title),
            trailing = stringResource(R.string.diag_modules),
            trailingColor = TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiagnosticTile(
                icon = Icons.Outlined.AccountTree,
                iconTint = EmeraldLight,
                title = stringResource(R.string.diag_treemap),
                subtitle = stringResource(R.string.diag_treemap_sub),
                badge = null,
                badgeColor = EmeraldLight,
                onClick = actions.onOpenTreemap,
                modifier = Modifier.weight(1f),
            )
            DiagnosticTile(
                icon = Icons.Outlined.ViewCarousel,
                iconTint = VideoRose,
                title = stringResource(R.string.diag_swipe),
                subtitle = stringResource(R.string.diag_swipe_sub),
                badge = heavyVideos?.let { androidx.compose.ui.res.pluralStringResource(R.plurals.diag_swipe_badge, it, it) },
                badgeColor = VideoRose,
                onClick = actions.onOpenSwipeDeck,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiagnosticTile(
                icon = Icons.Outlined.VideoFile,
                iconTint = Amber,
                title = stringResource(R.string.diag_compressor),
                subtitle = stringResource(R.string.diag_compressor_sub),
                badge = snapshot?.let { stringResource(R.string.diag_compressor_badge, it.videoCount) },
                badgeColor = AmberLight,
                onClick = actions.onOpenCompressor,
                modifier = Modifier.weight(1f),
            )
            DiagnosticTile(
                icon = Icons.AutoMirrored.Outlined.Chat,
                iconTint = PhotoViolet,
                title = stringResource(R.string.diag_chat),
                subtitle = snapshot?.let {
                    stringResource(R.string.diag_chat_sub, formatBytes(it.categoryBytes[StorageCategory.SOCIAL] ?: 0L))
                } ?: stringResource(R.string.diag_chat_sub_scanning),
                badge = null,
                badgeColor = EmeraldLight,
                onClick = actions.onOpenChatVaults,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DiagnosticTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String?,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(16.dp))
            .obsidianCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(SurfaceRaised, RoundedCornerShape(10.dp))
                    .border(1.dp, SurfaceStroke, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.weight(1f))
            if (badge != null) {
                TelemetryBadge(text = badge, color = badgeColor)
            } else {
                Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Column {
            AutoSizeText(title, MaterialTheme.typography.titleMedium, TextPrimary, minSize = 13.sp)
            AutoSizeText(subtitle, MaterialTheme.typography.bodyMedium, TextSecondary, minSize = 10.sp)
        }
    }
}
