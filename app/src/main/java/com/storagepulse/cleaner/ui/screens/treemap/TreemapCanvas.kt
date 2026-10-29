package com.storagepulse.cleaner.ui.screens.treemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagepulse.cleaner.data.storage.model.StorageNode
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.JetBrainsMono
import com.storagepulse.cleaner.ui.theme.PlusJakartaSans
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatBytesCompact

/** [parent] = the level-1 folder a nested block sits in (null for level-1 blocks). */
private data class PlacedBlock(val block: Block, val rect: Rect, val level: Int, val parent: StorageNode? = null)

/**
 * Squarified treemap of [node]'s children, with a second nested level inside every block
 * that is large enough. Single tap = [onSelect], double tap = [onDrill].
 */
@Composable
fun TreemapCanvas(
    node: StorageNode,
    selectedPath: String?,
    otherLabel: String,
    onSelect: (StorageNode) -> Unit,
    onDrill: (StorageNode) -> Unit,
    /** Tap on a "Smaller items" block: path of the folder it belongs to. */
    onOther: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val other by rememberUpdatedState(onOther)
    val density = LocalDensity.current
    val select by rememberUpdatedState(onSelect)
    val drill by rememberUpdatedState(onDrill)

    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val placed = remember(node, widthPx, heightPx) {
            with(density) { layoutTwoLevels(node, Size(widthPx, heightPx), otherLabel, 3.dp.toPx(), 36.dp.toPx(), 96.dp.toPx(), 84.dp.toPx()) }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(placed) {
                    fun hit(at: Offset): PlacedBlock? =
                        placed.filter { it.rect.contains(at) }.maxByOrNull { it.level }
                    detectTapGestures(
                        // "Smaller items" has no node: list the files of the folder it belongs to.
                        onTap = { at ->
                            val p = hit(at) ?: return@detectTapGestures
                            p.block.node?.let(select) ?: other(p.parent?.path ?: node.path)
                        },
                        onDoubleTap = { at ->
                            val p = hit(at) ?: return@detectTapGestures
                            val n = p.block.node
                            when {
                                n == null -> p.parent?.let(drill) // nested "Smaller items": open its folder
                                p.block.canDrill -> drill(n)
                                else -> select(n)
                            }
                        },
                    )
                },
        ) {
            placed.forEach { p -> drawBlock(p, p.block.path != null && p.block.path == selectedPath, textMeasurer) }
        }
    }
}

private fun layoutTwoLevels(
    node: StorageNode,
    size: Size,
    otherLabel: String,
    gap: Float,
    headerHeight: Float,
    minNestedWidth: Float,
    minNestedHeight: Float,
): List<PlacedBlock> {
    val out = ArrayList<PlacedBlock>()
    val top = blocksFor(node, maxItems = 12, minFraction = 0.012, otherLabel = otherLabel)
    val topRects = squarify(top.map { it to it.sizeBytes.toDouble() }, Rect(0f, 0f, size.width, size.height))
    for ((block, r) in topRects) {
        val rect = r.deflate(gap / 2)
        out += PlacedBlock(block, rect, 1)
        val child = block.node ?: continue
        if (!child.isDirectory || child.children.isEmpty()) continue
        if (rect.width < minNestedWidth || rect.height < minNestedHeight) continue
        val inner = Rect(rect.left + gap * 2, rect.top + headerHeight, rect.right - gap * 2, rect.bottom - gap * 2)
        if (inner.width <= 0f || inner.height <= 0f) continue
        val nested = blocksFor(child, maxItems = 6, minFraction = 0.04, otherLabel = otherLabel)
        squarify(nested.map { it to it.sizeBytes.toDouble() }, inner).forEach { (b, nr) ->
            out += PlacedBlock(b, nr.deflate(gap / 2), 2, parent = child)
        }
    }
    return out
}

private val nameStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
private val sizeStyle = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp)

private fun DrawScope.drawBlock(p: PlacedBlock, selected: Boolean, measurer: TextMeasurer) {
    val r = p.rect
    if (r.width < 2f || r.height < 2f) return
    val color = p.block.color
    val radius = CornerRadius(if (p.level == 1) 10.dp.toPx() else 7.dp.toPx())
    drawRoundRect(
        color = color.copy(alpha = if (p.level == 1) 0.14f else 0.20f),
        topLeft = r.topLeft, size = r.size, cornerRadius = radius,
    )
    drawRoundRect(
        color = if (selected) EmeraldLight else color.copy(alpha = if (p.level == 1) 0.55f else 0.35f),
        topLeft = r.topLeft, size = r.size, cornerRadius = radius,
        style = Stroke(width = if (selected) 2.dp.toPx() else 1.dp.toPx()),
    )
    drawLabels(p, measurer)
}

/**
 * DiskUsage-style label rules, so labels never overlap or show half a number:
 * - the size is drawn only if it fits completely (full "1.4 GB", else compact "1.4G", else not at all);
 * - the name is drawn only when at least ~4 characters fit (ellipsized after that);
 * - nothing is drawn in blocks too small to read.
 */
private fun DrawScope.drawLabels(p: PlacedBlock, measurer: TextMeasurer) {
    val pad = 6.dp.toPx()
    val r = p.rect
    val maxW = (r.width - pad * 2).toInt()
    if (maxW <= 14.dp.toPx() || r.height < 18.dp.toPx()) return
    val nameColor = if (p.level == 1) p.block.color.copy(alpha = 1f).compositeOverTextTint() else TextPrimary

    fun measure(text: String, style: TextStyle, ellipsize: Boolean) = measurer.measure(
        text = text, style = style,
        overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip,
        maxLines = 1, softWrap = false,
        constraints = if (ellipsize) Constraints(maxWidth = maxW) else Constraints(),
    )

    // Size: full, then compact, else skip.
    val sizeLayout = listOf(formatBytes(p.block.sizeBytes), formatBytesCompact(p.block.sizeBytes))
        .map { measure(it, sizeStyle.copy(color = TextSecondary), ellipsize = false) }
        .firstOrNull { it.size.width <= maxW }

    val showName = r.height >= 34.dp.toPx() && maxW >= 40.dp.toPx()
    if (showName) {
        val name = measure(p.block.label, nameStyle.copy(color = nameColor), ellipsize = true)
        drawText(name, topLeft = Offset(r.left + pad, r.top + pad - 1.dp.toPx()))
        if (sizeLayout != null) {
            val y = if (p.level == 1) r.top + pad + name.size.height else r.bottom - pad - sizeLayout.size.height
            // Nested blocks pin the size bottom-left like the design, unless that would hit the name.
            val safeY = y.coerceAtLeast(r.top + pad + name.size.height)
            if (safeY + sizeLayout.size.height <= r.bottom - 2.dp.toPx()) {
                drawText(sizeLayout, topLeft = Offset(r.left + pad, safeY))
            }
        }
    } else if (sizeLayout != null && sizeLayout.size.height <= r.height - 4.dp.toPx()) {
        drawText(sizeLayout, topLeft = Offset(r.left + pad, r.top + (r.height - sizeLayout.size.height) / 2))
    }
}

/** Category colors are dark-ish; lighten them for readable header text. */
private fun androidx.compose.ui.graphics.Color.compositeOverTextTint() =
    androidx.compose.ui.graphics.lerp(this, TextPrimary, 0.25f)

