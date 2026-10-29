package com.storagepulse.cleaner.data.trash

import android.content.Context
import android.os.Environment
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.storagepulse.cleaner.data.db.AppDatabase
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.data.storage.StorageAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Runs on a real device with All Files Access granted. Only touches its own test file
 * in Download/ and its own quarantine entries.
 */
@RunWith(AndroidJUnit4::class)
class SafeTrashRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: SafeTrashRepository
    private lateinit var testFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        assumeTrue("All Files Access not granted", StorageAccess.hasAllFilesAccess(context))
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = SafeTrashRepository(context, db.trashDao(), SettingsRepository(context))
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        testFile = File(dir, "storagepulse_trash_test_${System.nanoTime()}.txt").apply {
            writeText("StoragePulse Safe Trash test file")
        }
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::testFile.isInitialized) testFile.delete()
    }

    @Test
    fun moveRestorePurge_neverLosesTheFileByAccident() = runBlocking {
        val originalBytes = testFile.readBytes()

        // 1. Move to Safe Trash: original gone, quarantined copy exists, 30-day expiry recorded.
        val moved = repo.moveToSafeTrash(listOf(testFile)).single()
        assertTrue("move failed: $moved", moved is TrashMoveResult.Moved)
        val item = (moved as TrashMoveResult.Moved).item
        assertFalse(testFile.exists())
        assertTrue(File(item.quarantinePath).exists())
        assertEquals(TimeUnit.DAYS.toMillis(30), item.expiresAt - item.deletedAt)
        assertEquals(1, repo.observeTotals().first().count)

        // 2. Restore: file back at its original path with identical bytes.
        val restored = repo.restoreFromTrash(item.id)
        assertTrue("restore failed: $restored", restored is TrashRestoreResult.Restored)
        assertTrue(testFile.exists())
        assertTrue(originalBytes.contentEquals(testFile.readBytes()))
        assertFalse(File(item.quarantinePath).exists())
        assertEquals(0, repo.observeTotals().first().count)

        // 3. Expired purge does NOT touch items that are still within retention.
        val again = repo.moveToSafeTrash(listOf(testFile)).single() as TrashMoveResult.Moved
        assertEquals(0, repo.purgeExpired(now = System.currentTimeMillis()))
        assertTrue(File(again.item.quarantinePath).exists())

        // 4. After expiry, purge removes it for good.
        assertEquals(1, repo.purgeExpired(now = again.item.expiresAt + 1))
        assertFalse(File(again.item.quarantinePath).exists())
        assertEquals(0, repo.observeTotals().first().count)
    }

    @Test
    fun restore_whenOriginalNameIsTaken_keepsBothFiles() = runBlocking {
        val moved = repo.moveToSafeTrash(listOf(testFile)).single() as TrashMoveResult.Moved
        testFile.writeText("a new file with the same name")

        val restored = repo.restoreFromTrash(moved.item.id) as TrashRestoreResult.Restored
        val restoredFile = File(restored.path)
        try {
            assertTrue(testFile.exists())
            assertTrue(restoredFile.exists())
            assertTrue(restoredFile.name.contains("(restored)"))
        } finally {
            restoredFile.delete()
        }
    }
}
