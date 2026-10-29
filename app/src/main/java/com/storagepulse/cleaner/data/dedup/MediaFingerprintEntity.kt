package com.storagepulse.cleaner.data.dedup

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Cached image fingerprint. Valid while [sizeBytes] and [lastModified] still match the file,
 * so a second analysis only decodes new or changed photos.
 */
@Entity(tableName = "media_fingerprints", indices = [Index("sizeBytes")])
data class MediaFingerprintEntity(
    @PrimaryKey val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val width: Int,
    val height: Int,
    /** 64-bit difference hash of a 9×8 grayscale thumbnail. */
    val dHash: Long,
    /** Variance of the Laplacian on a ~256 px thumbnail (higher = sharper). */
    val laplacianVariance: Double,
    /** Pixel variance of the 9×8 thumbnail; near-flat images are excluded from similarity. */
    val complexity: Double,
    /** Full-file SHA-256, only computed for files that share their size with another file. */
    val sha256: String? = null,
)

@Dao
interface MediaFingerprintDao {
    @Query("SELECT * FROM media_fingerprints")
    suspend fun all(): List<MediaFingerprintEntity>

    @Upsert
    suspend fun upsert(items: List<MediaFingerprintEntity>)

    @Query("DELETE FROM media_fingerprints WHERE path IN (:paths)")
    suspend fun delete(paths: List<String>)
}
