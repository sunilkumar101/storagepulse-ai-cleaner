package com.storagepulse.cleaner.data.vault

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One file locked in the private vault. Content + thumbnail are encrypted files in filesDir. */
@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val encryptedFile: String,
    val encryptedThumb: String?,
    val sha256: String,
    val lockedAt: Long,
    val originalModified: Long,
)

@Dao
interface VaultDao {
    @Insert
    suspend fun insert(item: VaultItemEntity): Long

    @Query("SELECT * FROM vault_items ORDER BY lockedAt DESC")
    fun observeAll(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun get(id: Long): VaultItemEntity?

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM vault_items WHERE isVideo = 0")
    fun photoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_items WHERE isVideo = 1")
    fun videoCount(): Flow<Int>
}
