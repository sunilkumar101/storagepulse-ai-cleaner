package com.storagepulse.cleaner.data.dedup

import android.graphics.Bitmap
import kotlin.math.exp

/** Pure image measurements used by the deduplicator. All work on small thumbnails. */
object ImageSignals {

    /**
     * Difference hash: shrink to 9×8 grayscale, then bit = "pixel brighter than its right
     * neighbour". 64 bits, robust to scaling, re-encoding and small exposure changes.
     */
    fun dHash(source: Bitmap): Pair<Long, Double> {
        val small = Bitmap.createScaledBitmap(source, 9, 8, true)
        val gray = IntArray(72)
        for (y in 0 until 8) for (x in 0 until 9) gray[y * 9 + x] = luma(small.getPixel(x, y))
        if (small !== source) small.recycle()

        var hash = 0L
        var bit = 0
        for (y in 0 until 8) for (x in 0 until 8) {
            if (gray[y * 9 + x] > gray[y * 9 + x + 1]) hash = hash or (1L shl bit)
            bit++
        }
        val mean = gray.average()
        val complexity = gray.sumOf { (it - mean) * (it - mean) } / gray.size
        return hash to complexity
    }

    /**
     * Variance of the 4-neighbour Laplacian over the grayscale image. Sharp edges give large
     * second derivatives; motion blur and defocus flatten them.
     */
    fun laplacianVariance(source: Bitmap): Double {
        val w = source.width
        val h = source.height
        if (w < 3 || h < 3) return 0.0
        val px = IntArray(w * h)
        source.getPixels(px, 0, w, 0, 0, w, h)
        val g = IntArray(px.size) { luma(px[it]) }
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val i = row + x
                val lap = (g[i - 1] + g[i + 1] + g[i - w] + g[i + w] - 4 * g[i]).toDouble()
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        if (n == 0) return 0.0
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    /** 0–100 sharpness score from Laplacian variance (saturating curve). */
    fun sharpnessScore(laplacianVariance: Double): Int =
        (100 * (1 - exp(-laplacianVariance / SHARPNESS_SCALE))).toInt().coerceIn(0, 100)

    fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun luma(c: Int): Int {
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    // Tuned on 2,343 real camera photos at 160 px: median variance ≈ 1,560 → score ≈ 65.
    private const val SHARPNESS_SCALE = 1500.0
}
