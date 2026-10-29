package com.storagepulse.cleaner.data.trash

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Insert
    suspend fun insert(item: TrashItemEntity): Long

    @Query("SELECT * FROM trash_items WHERE id = :id")
    suspend fun get(id: Long): TrashItemEntity?

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<TrashItemEntity>>

    @Query("SELECT COUNT(*) AS count, COALESCE(SUM(sizeBytes), 0) AS bytes FROM trash_items")
    fun observeTotals(): Flow<TrashTotals>

    @Query("SELECT * FROM trash_items")
    suspend fun all(): List<TrashItemEntity>

    @Query("SELECT * FROM trash_items WHERE expiresAt <= :now")
    suspend fun expired(now: Long): List<TrashItemEntity>

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun delete(id: Long)
}
