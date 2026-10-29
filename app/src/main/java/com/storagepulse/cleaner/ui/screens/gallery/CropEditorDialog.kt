package com.storagepulse.cleaner.ui.screens.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.gallery.MediaEntry
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.OnEmeraldLight
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QuickPic-style crop: pinch to zoom and drag to frame; whatever is visible in the frame is
 * saved as a NEW copy (the original is untouched). Rect is passed back normalised (0..1) in
 * the displayed (EXIF-rotated) orientation.
 */
@Composable
fun CropEditorDialog(
    entry: MediaEntry,
    onSave: (Float, Float, Float, Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Displayed aspect ratio, honouring EXIF rotation.
    val aspect by produceState<Float?>(null, entry.path) {
        value = withContext(Dispatchers.IO) {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(entry.path, o)
            val rot = com.storagepulse.cleaner.data.gallery.exifDegrees(entry.path)
            val (w, h) = if (rot == 90 || rot == 270) o.outHeight to o.outWidth else o.outWidth to o.outHeight
            if (w > 0 && h > 0) w.toFloat() / h else null
        }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(ObsidianCanvas).safeDrawingPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.ga_crop_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(stringResource(R.string.ga_crop_hint), style = Telemetry.sm, color = TextSecondary)
            val a = aspect
            if (a == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = EmeraldLight, trackColor = SurfaceStroke)
                return@Column
            }
            val density = LocalDensity.current
            var frame by remember { mutableStateOf(Size.Zero) }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Color.Black).border(2.dp, EmeraldLight).onSizeChanged { frame = Size(it.width.toFloat(), it.height.toFloat()) }) {
                val boxW = with(density) { maxWidth.toPx() }
                val boxH = with(density) { maxHeight.toPx() }
                // Image fitted inside the frame at scale 1.
                val fitW = if (boxW / boxH > a) boxH * a else boxW
                val fitH = fitW / a
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 8f)
                                // Keep the image covering as much of the frame as it can.
                                val maxX = ((fitW * scale - boxW) / 2).coerceAtLeast(0f)
                                val maxY = ((fitH * scale - boxH) / 2).coerceAtLeast(0f)
                                offset = Offset((offset.x + pan.x).coerceIn(-maxX, maxX), (offset.y + pan.y).coerceIn(-maxY, maxY))
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(entry.uri).size(2048).build(),
                        contentDescription = entry.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    )
                }
            }
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ga_cancel), color = TextPrimary) }
                Button(
                    onClick = {
                        val boxW = frame.width
                        val boxH = frame.height
                        if (boxW <= 0f || boxH <= 0f) return@Button
                        val fitW = if (boxW / boxH > a) boxH * a else boxW
                        val fitH = fitW / a
                        // Visible frame [0,box] mapped back into the fitted, scaled image.
                        val left0 = boxW / 2 - fitW * scale / 2 + offset.x
                        val top0 = boxH / 2 - fitH * scale / 2 + offset.y
                        fun nx(x: Float) = ((x - left0) / (fitW * scale)).coerceIn(0f, 1f)
                        fun ny(y: Float) = ((y - top0) / (fitH * scale)).coerceIn(0f, 1f)
                        onSave(nx(0f), ny(0f), nx(boxW), ny(boxH))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = OnEmeraldLight),
                ) { Text(stringResource(R.string.ga_crop_save)) }
            }
        }
    }
}
