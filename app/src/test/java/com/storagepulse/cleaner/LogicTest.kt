package com.storagepulse.cleaner

import androidx.compose.ui.geometry.Rect
import com.storagepulse.cleaner.data.dedup.ImageSignals
import com.storagepulse.cleaner.ui.navigation.DeepLinks
import com.storagepulse.cleaner.ui.navigation.Routes
import com.storagepulse.cleaner.ui.screens.treemap.squarify
import com.storagepulse.cleaner.ui.util.formatBytes
import com.storagepulse.cleaner.ui.util.formatBytesCompact
import com.storagepulse.cleaner.ui.util.formatCount
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Pure-JVM unit tests for logic that the UI and the cleaners depend on. */
class LogicTest {

    @After
    fun clearLinks() = DeepLinks.consume()

    // ---------------------------------------------------------------- formatting

    @Test
    fun formatBytes_usesDecimalUnitsLikeAndroidSettings() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("0 B", formatBytes(-5))
        assertEquals("999 B", formatBytes(999))
        assertEquals("1 KB", formatBytes(1_000))
        assertEquals("322 KB", formatBytes(322_051))
        assertEquals("24 MB", formatBytes(24_449_836))
        assertEquals("1.5 GB", formatBytes(1_500_000_000))
        assertEquals("128.0 GB", formatBytes(128_000_000_000))
    }

    @Test
    fun formatBytesCompact_andCount() {
        assertEquals("1.4G", formatBytesCompact(1_400_000_000))
        assertEquals("12G", formatBytesCompact(12_000_000_000))
        assertEquals("494M", formatBytesCompact(494_000_000))
        assertEquals("17,719", formatCount(17_719))
    }

    // ---------------------------------------------------------------- treemap layout

    @Test
    fun squarify_fillsBoundsWithAreasProportionalToWeight() {
        val bounds = Rect(0f, 0f, 400f, 300f)
        val weights = listOf("a" to 42.1, "b" to 24.6, "c" to 18.6, "d" to 11.4, "e" to 9.1, "f" to 6.2)
        val out = squarify(weights, bounds)
        assertEquals(weights.size, out.size)
        val total = weights.sumOf { it.second }
        val boundsArea = bounds.width * bounds.height
        var covered = 0.0
        for ((key, r) in out) {
            val weight = weights.first { it.first == key }.second
            val area = (r.width * r.height).toDouble()
            covered += area
            assertTrue("$key outside bounds: $r", r.left >= -0.01f && r.top >= -0.01f && r.right <= 400.01f && r.bottom <= 300.01f)
            assertTrue("$key area not proportional", abs(area / boundsArea - weight / total) < 0.001)
        }
        assertTrue("treemap must cover the bounds", abs(covered - boundsArea) < 1.0)
    }

    @Test
    fun squarify_skipsZeroAndNegativeWeights() {
        val out = squarify(listOf("x" to 5.0, "zero" to 0.0, "neg" to -1.0), Rect(0f, 0f, 10f, 10f))
        assertEquals(listOf("x"), out.map { it.first })
    }

    // ---------------------------------------------------------------- AI dedup signals

    @Test
    fun hamming_countsDifferingBits() {
        assertEquals(0, ImageSignals.hamming(0x0F0FL, 0x0F0FL))
        assertEquals(4, ImageSignals.hamming(0x0FL, 0x00L))
        assertEquals(64, ImageSignals.hamming(0L, -1L))
    }

    @Test
    fun sharpnessScore_isMonotonicAndClamped() {
        assertEquals(0, ImageSignals.sharpnessScore(0.0))
        assertTrue(ImageSignals.sharpnessScore(500.0) < ImageSignals.sharpnessScore(5_000.0))
        assertTrue(ImageSignals.sharpnessScore(1e12) <= 100)
    }

    // ---------------------------------------------------------------- widget deep links

    @Test
    fun deepLinks_acceptOnlyWhitelistedRoutes() {
        DeepLinks.offer("vault") // never reachable from a widget/notification intent
        assertNull(DeepLinks.pending.value)
        DeepLinks.offer(null)
        assertNull(DeepLinks.pending.value)
        DeepLinks.offer(Routes.SWIPE_DECK)
        assertEquals(Routes.SWIPE_DECK, DeepLinks.pending.value)
        DeepLinks.consume()
        assertNull(DeepLinks.pending.value)
    }
}
