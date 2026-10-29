package com.storagepulse.cleaner.ui.screens.treemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagepulse.cleaner.data.storage.model.StorageNode
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.JetBrainsMono
import com.storagepulse.cleaner.ui.theme.PlusJakartaSans
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.util.formatBytes
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val RINGS = 3
private const val MIN_SWEEP_DEG = 0.8f

/** One arc: [depth] 1 = innermost ring. Angles in degrees, 0 = 12 o'clock, clockwise. */
private data class Arc(val block: Block, val depth: Int, val start: Float, val sweep: Float)

/**
 * Concentric rings: ring 1 = children of [node], rings 2–3 = grandchildren and below.
 * Tap an arc = [onSelect], double tap = [onDrill], tap the centre = [onZoomOut].
 */
@Composable
fun SunburstCanvas(
    node: StorageNode,
    selectedPath: String?,
    otherLabel: String,
    onSelect: (StorageNode) -> Unit,
    onDrill: (StorageNode) -> Unit,
    onZoomOut: () -> Unit,
    onOther: (String) -> Unit,
    centerLabel: String,
    modifier: Modifier = Modifier,
) {
    val other by rememberUpdatedState(onOther)
    val measurer = rememberTextMeasurer()
    val select by rememberUpdatedState(onSelect)
    val drill by rememberUpdatedState(onDrill)
    val zoomOut by rememberUpdatedState(onZoomOut)
    val arcs = remember(node) { buildArcs(node, otherLabel) }

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(arcs) {
                    fun geometry(): Triple<Offset, Float, Float> {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val outer = min(size.width, size.height) / 2f - 8.dp.toPx()
                        val inner = outer * 0.30f
                        return Triple(c, inner, (outer - inner) / RINGS)
                    }
                    fun hit(at: Offset): Arc? {
                        val (c, inner, ring) = geometry()
                        val dx = at.x - c.x
                        val dy = at.y - c.y
                        val r = sqrt(dx * dx + dy * dy)
                        if (r < inner) return null
                        val depth = ((r - inner) / ring).toInt() + 1
                        if (depth > RINGS) return null
                        var angle = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        return arcs.firstOrNull { it.depth == depth && angle >= it.start && angle < it.start + it.sweep }
                    }
                    fun inCenter(at: Offset): Boolean {
                        val (c, inner, _) = geometry()
                        val dx = at.x - c.x
                        val dy = at.y - c.y
                        return sqrt(dx * dx + dy * dy) < inner
                    }
                    detectTapGestures(
                        onTap = { at ->
                            if (inCenter(at)) {
                                zoomOut()
                            } else {
                                hit(at)?.block?.let { b -> b.node?.let(select) ?: other(node.path) }
                            }
                        },
                        onDoubleTap = { at ->
                            val a = hit(at) ?: return@detectTapGestures
                            val n = a.block.node ?: return@detectTapGestures
                            if (a.block.canDrill) drill(n) else select(n)
                        },
                    )
                },
        ) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val outer = min(size.width, size.height) / 2f - 8.dp.toPx()
            val inner = outer * 0.30f
            val ring = (outer - inner) / RINGS
            val gap = 2.dp.toPx()

            arcs.forEach { a ->
                val mid = inner + ring * (a.depth - 0.5f)
                val selected = a.block.path != null && a.block.path == selectedPath
                val alpha = when (a.depth) { 1 -> 0.85f; 2 -> 0.6f; else -> 0.4f }
                drawArc(
                    color = if (selected) EmeraldLight else a.block.color.copy(alpha = alpha),
                    startAngle = a.start - 90f + 0.4f,
                    sweepAngle = (a.sweep - 0.8f).coerceAtLeast(0.2f),
                    useCenter = false,
                    topLeft = Offset(c.x - mid, c.y - mid),
                    size = Size(mid * 2, mid * 2),
                    style = Stroke(width = ring - gap),
                )
            }

            // Labels on inner-ring arcs that are wide enough.
            arcs.filter { it.depth == 1 && it.sweep >= 26f }.forEach { a ->
                val mid = inner + ring * 0.5f
                val theta = Math.toRadians((a.start + a.sweep / 2f - 90f).toDouble())
                val p = Offset(c.x + (mid * cos(theta)).toFloat(), c.y + (mid * sin(theta)).toFloat())
                val maxW = (ring * 1.5f).toInt().coerceAtLeast(1)
                val label = measurer.measure(
                    text = a.block.label,
                    style = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center),
                    overflow = TextOverflow.Ellipsis, maxLines = 1, softWrap = false,
                    constraints = Constraints(maxWidth = maxW),
                )
                drawText(label, topLeft = Offset(p.x - label.size.width / 2f, p.y - label.size.height / 2f))
            }

            // Centre: current folder name + size (tap to zoom out).
            drawCircle(SurfaceCard, radius = inner - gap, center = c)
            drawCircle(SurfaceStroke, radius = inner - gap, center = c, style = Stroke(1.dp.toPx()))
            val maxW = ((inner - gap) * 1.6f).toInt().coerceAtLeast(1)
            val name = measurer.measure(
                text = centerLabel,
                style = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary, textAlign = TextAlign.Center),
                overflow = TextOverflow.Ellipsis, maxLines = 2, softWrap = true,
                constraints = Constraints(maxWidth = maxW),
            )
            val sz = measurer.measure(
                text = formatBytes(node.sizeBytes),
                style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EmeraldLight, textAlign = TextAlign.Center),
                maxLines = 1, softWrap = false,
                constraints = Constraints(maxWidth = maxW),
            )
            val total = name.size.height + sz.size.height
            drawText(name, topLeft = Offset(c.x - name.size.width / 2f, c.y - total / 2f))
            drawText(sz, topLeft = Offset(c.x - sz.size.width / 2f, c.y - total / 2f + name.size.height))
            val hint = measurer.measure(
                text = "▲",
                style = TextStyle(fontSize = 8.sp, color = TextSecondary),
            )
            drawText(hint, topLeft = Offset(c.x - hint.size.width / 2f, c.y + total / 2f + 2.dp.toPx()))
        }
    }
}

private fun buildArcs(root: StorageNode, otherLabel: String): List<Arc> {
    val out = ArrayList<Arc>()
    fun add(parent: StorageNode, depth: Int, start: Float, sweep: Float) {
        if (depth > RINGS || sweep < MIN_SWEEP_DEG) return
        val total = parent.sizeBytes.coerceAtLeast(1L).toDouble()
        var angle = start
        blocksFor(parent, maxItems = 16, minFraction = 0.01, otherLabel = otherLabel).forEach { b ->
            val s = (b.sizeBytes / total * sweep).toFloat()
            if (s >= MIN_SWEEP_DEG) {
                out += Arc(b, depth, angle, s)
                val n = b.node
                if (n != null && n.isDirectory) add(n, depth + 1, angle, s)
            }
            angle += s
        }
    }
    add(root, 1, 0f, 360f)
    return out
}

