package com.storagepulse.cleaner.ui.screens.clean

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.dedup.MediaCluster
import com.storagepulse.cleaner.data.dedup.MediaItem
import com.storagepulse.cleaner.ui.components.TelemetryBadge
import com.storagepulse.cleaner.ui.theme.AmberLight
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.util.formatBytes
import java.util.Locale

/**
 * Side-by-side keeper vs candidate with one shared pinch-zoom / pan, so the same detail
 * (eyes, text, edges) is compared at the same spot in both photos.
 */
@Composable
fun CompareLoupeDialog(cluster: MediaCluster, onDismiss: () -> Unit) {
    val keeper = cluster.keeper ?: return
    val candidates = cluster.items.filter { it.path != keeper.path }
    if (candidates.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 8f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ObsidianCanvas)
                .safeDrawingPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.loupe_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                    Icon(Icons.Outlined.ZoomOutMap, stringResource(R.string.loupe_reset), tint = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.loupe_close), tint = TextPrimary)
                }
            }
            Text(stringResource(R.string.loupe_hint), style = Telemetry.sm, color = TextSecondary)

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .transformable(transform),
            ) {
                val sideBySide = maxWidth > maxHeight
                val panes: @Composable (Modifier) -> Unit = { paneModifier ->
                    LoupePane(keeper, stringResource(R.string.loupe_keeper), EmeraldLight, scale, offset, paneModifier)
                    LoupePane(
                        candidates[index],
                        stringResource(R.string.loupe_candidate, index + 1, candidates.size),
                        AmberLight,
                        scale,
                        offset,
                        paneModifier,
                    )
                }
                if (sideBySide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { panes(Modifier.weight(1f).fillMaxSize()) }
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) { panes(Modifier.weight(1f).fillMaxWidth()) }
                }
            }

            if (candidates.size > 1) {
                TextButton(onClick = { index = (index + 1) % candidates.size }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.loupe_next), color = EmeraldLight)
                }
            }
        }
    }
}

@Composable
private fun LoupePane(item: MediaItem, label: String, color: Color, scale: Float, offset: Offset, modifier: Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TelemetryBadge(label, color)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(
                    R.string.loupe_meta,
                    item.sharpness,
                    String.format(Locale.US, "%.1f MP", item.megapixels),
                    formatBytes(item.sizeBytes),
                ),
                style = Telemetry.sm,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceStroke, RoundedCornerShape(12.dp))
                .background(Color.Black),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.contentUri).size(2048).build(),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}
