package com.storagepulse.cleaner.data.dedup

/** Photos scoring below this (0–100 sharpness) are offered as blurry. */
const val BLUR_SCORE_THRESHOLD = 8

enum class ClusterType { EXACT_DUPLICATES, SIMILAR_BURST, BLURRY, OLD_SCREENSHOTS }

data class MediaItem(
    val path: String,
    val contentUri: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val dateTaken: Long,
    val width: Int,
    val height: Int,
    /** 0–100 sharpness (Laplacian variance). */
    val sharpness: Int,
    val dHash: Long,
) {
    val megapixels: Double get() = width.toLong() * height / 1_000_000.0
}

/**
 * A group the user reviews. [keeper] is protected (Smart Keeper); [suggested] are the copies
 * pre-selected for Safe Trash. Blurry / screenshot groups have no keeper.
 */
data class MediaCluster(
    val id: String,
    val type: ClusterType,
    val items: List<MediaItem>,
    val keeper: MediaItem?,
    val suggested: Set<String>,
    /** Average dHash similarity in % for similar groups; 100 for exact duplicates. */
    val matchPercent: Double,
) {
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }
    val startTime: Long get() = items.minOf { it.dateTaken }
}

data class DedupResult(
    val clusters: List<MediaCluster>,
    val analyzedImages: Int,
    val durationMs: Long,
)

sealed interface DedupState {
    data object Idle : DedupState
    data class Analyzing(val done: Int, val total: Int) : DedupState
    data class Ready(val result: DedupResult) : DedupState
    data class Failed(val message: String) : DedupState
}
