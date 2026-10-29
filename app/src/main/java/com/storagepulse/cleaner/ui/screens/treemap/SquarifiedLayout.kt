package com.storagepulse.cleaner.ui.screens.treemap

import androidx.compose.ui.geometry.Rect
import kotlin.math.max

/**
 * Squarified treemap (Bruls, Huizing & van Wijk): fills [bounds] with one rectangle per
 * item, area ∝ weight, keeping rectangles as close to square as possible.
 * [items] must be sorted by weight, largest first. Zero/negative weights are skipped.
 */
fun <T> squarify(items: List<Pair<T, Double>>, bounds: Rect): List<Pair<T, Rect>> {
    val positive = items.filter { it.second > 0.0 }
    val total = positive.sumOf { it.second }
    if (positive.isEmpty() || total <= 0.0 || bounds.width <= 0f || bounds.height <= 0f) return emptyList()

    val scale = (bounds.width.toDouble() * bounds.height) / total
    val areas = positive.map { it.first to it.second * scale }

    val out = ArrayList<Pair<T, Rect>>(areas.size)
    var free = bounds
    val row = ArrayList<Pair<T, Double>>()

    fun worst(r: List<Pair<T, Double>>, side: Double): Double {
        if (r.isEmpty()) return Double.MAX_VALUE
        val sum = r.sumOf { it.second }
        val maxA = r.maxOf { it.second }
        val minA = r.minOf { it.second }
        val s2 = sum * sum
        val w2 = side * side
        return max(w2 * maxA / s2, s2 / (w2 * minA))
    }

    fun layoutRow(r: List<Pair<T, Double>>) {
        val sum = r.sumOf { it.second }
        if (free.width >= free.height) {
            // Column on the left side.
            val colWidth = (sum / free.height).toFloat()
            var y = free.top
            r.forEach { (item, area) ->
                val h = (area / colWidth).toFloat()
                out += item to Rect(free.left, y, free.left + colWidth, y + h)
                y += h
            }
            free = Rect(free.left + colWidth, free.top, free.right, free.bottom)
        } else {
            // Row along the top.
            val rowHeight = (sum / free.width).toFloat()
            var x = free.left
            r.forEach { (item, area) ->
                val w = (area / rowHeight).toFloat()
                out += item to Rect(x, free.top, x + w, free.top + rowHeight)
                x += w
            }
            free = Rect(free.left, free.top + rowHeight, free.right, free.bottom)
        }
    }

    for (entry in areas) {
        val side = minOf(free.width, free.height).toDouble()
        if (row.isEmpty() || worst(row + entry, side) <= worst(row, side)) {
            row += entry
        } else {
            layoutRow(row)
            row.clear()
            row += entry
        }
    }
    if (row.isNotEmpty()) layoutRow(row)
    return out
}
