package com.storagepulse.cleaner.ui.screens.treemap

import androidx.compose.ui.graphics.Color
import com.storagepulse.cleaner.data.storage.FileClassifier
import com.storagepulse.cleaner.data.storage.model.StorageCategory
import com.storagepulse.cleaner.data.storage.model.StorageNode
import com.storagepulse.cleaner.ui.theme.Indigo

/** One drawable block: a real node, or the merged "smaller items" remainder. */
data class Block(
    val node: StorageNode?,
    val label: String,
    val sizeBytes: Long,
    val color: Color,
) {
    val path: String? get() = node?.path
    val canDrill: Boolean get() = node?.isDirectory == true && node.children.isNotEmpty()
}

/**
 * Children of [parent] as blocks, largest first. Like DiskUsage's "remainder" rendering, tiny
 * items (below [minFraction] of the parent, or beyond [maxItems]) merge into one block so
 * every visible block is big enough to tap and label.
 */
fun blocksFor(
    parent: StorageNode,
    maxItems: Int,
    minFraction: Double,
    otherLabel: String,
): List<Block> {
    val total = parent.sizeBytes.coerceAtLeast(1L)
    val sorted = parent.children.filter { it.sizeBytes > 0 }.sortedByDescending { it.sizeBytes }
    val shown = ArrayList<Block>()
    var rest = 0L
    for (child in sorted) {
        if (shown.size < maxItems && child.sizeBytes.toDouble() / total >= minFraction) {
            shown += Block(child, child.name, child.sizeBytes, colorFor(child))
        } else {
            rest += child.sizeBytes
        }
    }
    // Bytes directly in this folder that are not drawn as nodes (small files) also go to "other".
    rest += (parent.sizeBytes - sorted.sumOf { it.sizeBytes }).coerceAtLeast(0)
    if (rest > 0) shown += Block(null, otherLabel, rest, OtherColor)
    return shown
}

val OtherColor = Color(0xFF64748B)

/** Category color for a folder/file, from its path (folders) or extension (files). */
fun colorFor(node: StorageNode): Color {
    val lower = node.path.lowercase() + if (node.isDirectory) "/" else ""
    val category = when {
        FileClassifier.isSocial(lower) -> StorageCategory.SOCIAL
        !node.isDirectory -> FileClassifier.classify(lower, FileClassifier.extensionOf(node.name))
        lower.contains("/dcim/") || lower.contains("/pictures/") || lower.contains("screenshot") ->
            StorageCategory.PHOTOS
        lower.contains("/movies/") || lower.contains("video") || lower.contains("vidmate") ||
            lower.contains("capcut") -> StorageCategory.VIDEOS
        lower.contains("/download") -> StorageCategory.APPS
        lower.contains("/android/") -> StorageCategory.SYSTEM
        else -> null
    }
    return category?.let { Color(it.colorArgb) } ?: Indigo
}
