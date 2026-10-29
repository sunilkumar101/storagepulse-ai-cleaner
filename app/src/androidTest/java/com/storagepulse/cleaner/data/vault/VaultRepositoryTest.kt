package com.storagepulse.cleaner.data.vault

import android.content.Context
import android.os.Environment
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.storagepulse.cleaner.data.db.AppDatabase
import com.storagepulse.cleaner.data.storage.StorageAccess
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.storage.scanAndWait
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import kotlin.random.Random

/** Real device, real Keystore. Only touches its own test file and its own vault entries. */
@RunWith(AndroidJUnit4::class)
class VaultRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    private lateinit var file: File
    private lateinit var original: ByteArray

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        assumeTrue("All Files Access not granted", StorageAccess.hasAllFilesAccess(context))
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = VaultRepository(context, db.vaultDao(), StorageScannerRepository(context))
        original = Random(42).nextBytes(2_500_000) // > 2 chunks of 1 MiB
        file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sptest_vault_${System.nanoTime()}.bin")
        file.writeBytes(original)
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::file.isInitialized) {
            file.delete()
            // Drop the MediaStore row the restore created, so no ghost entry stays behind.
            runBlocking { scanAndWait(context, listOf(file.path)) }
        }
    }

    @Test
    fun lockThenRestore_roundTripsExactBytes() = runBlocking<Unit> {
        val result = repo.lock(listOf(file.path))
        assertEquals(1, result.locked)
        assertFalse("original must be removed from public storage", file.exists())

        val item = repo.observe().first().single()
        val enc = File(item.encryptedFile)
        assertTrue(enc.exists())
        // Ciphertext must not contain the plaintext's first 64 bytes.
        val encBytes = enc.readBytes()
        val haystack = String(encBytes, Charsets.ISO_8859_1)
        val needle = String(original, 0, 64, Charsets.ISO_8859_1)
        assertFalse(haystack.contains(needle))

        val restored = repo.restore(item.id)
        assertNotNull(restored)
        assertArrayEquals(original, restored!!.readBytes())
        assertFalse(enc.exists())
        assertTrue(repo.observe().first().isEmpty())
        restored.delete()
    }

    @Test
    fun tamperedCiphertext_isRejected() {
        val out = ByteArrayOutputStream()
        VaultCrypto.encrypt(ByteArrayInputStream(original), out)
        val tmp = File(context.cacheDir, "tamper.spv").apply { writeBytes(out.toByteArray()) }
        RandomAccessFile(tmp, "rw").use { raf ->
            raf.seek(100)
            val b = raf.read()
            raf.seek(100)
            raf.write(b xor 0x01)
        }
        var failed = false
        try {
            tmp.inputStream().use { VaultCrypto.decrypt(it, ByteArrayOutputStream()) }
        } catch (e: Exception) {
            failed = true
        }
        tmp.delete()
        assertTrue("GCM must reject a modified byte", failed)
    }
}
