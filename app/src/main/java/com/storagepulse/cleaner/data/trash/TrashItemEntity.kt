package com.storagepulse.cleaner.data.trash

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One quarantined file. Restorable until [expiresAt]; purged by [TrashPurgeWorker] after that. */
@Entity(
    tableName = "trash_items",
    indices = [Index("expiresAt"), Index("deletedAt")],
)
data class TrashItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val quarantinePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val deletedAt: Long,
    val expiresAt: Long,
)

data class TrashTotals(
    val count: Int,
    val bytes: Long,
)
