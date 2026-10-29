package com.storagepulse.cleaner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.storagepulse.cleaner.data.dedup.MediaFingerprintDao
import com.storagepulse.cleaner.data.dedup.MediaFingerprintEntity
import com.storagepulse.cleaner.data.trash.TrashDao
import com.storagepulse.cleaner.data.trash.TrashItemEntity
import com.storagepulse.cleaner.data.vault.VaultDao
import com.storagepulse.cleaner.data.vault.VaultItemEntity

@Database(
    entities = [TrashItemEntity::class, MediaFingerprintEntity::class, VaultItemEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trashDao(): TrashDao
    abstract fun fingerprintDao(): MediaFingerprintDao
    abstract fun vaultDao(): VaultDao

    companion object {
        const val NAME = "storagepulse.db"

        /** v2 adds the photo fingerprint cache. Safe Trash rows are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `media_fingerprints` (
                        `path` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `width` INTEGER NOT NULL,
                        `height` INTEGER NOT NULL,
                        `dHash` INTEGER NOT NULL,
                        `laplacianVariance` REAL NOT NULL,
                        `complexity` REAL NOT NULL,
                        `sha256` TEXT,
                        PRIMARY KEY(`path`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_fingerprints_sizeBytes` ON `media_fingerprints` (`sizeBytes`)")
            }
        }

        /** v3 adds the private vault index. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `vault_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`originalPath` TEXT NOT NULL, `fileName` TEXT NOT NULL, `mimeType` TEXT, `sizeBytes` INTEGER NOT NULL, " +
                        "`isVideo` INTEGER NOT NULL, `encryptedFile` TEXT NOT NULL, `encryptedThumb` TEXT, `sha256` TEXT NOT NULL, " +
                        "`lockedAt` INTEGER NOT NULL, `originalModified` INTEGER NOT NULL)",
                )
            }
        }
    }
}
