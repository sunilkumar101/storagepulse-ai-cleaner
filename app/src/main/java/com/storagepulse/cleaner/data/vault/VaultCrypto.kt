package com.storagepulse.cleaner.data.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM with a non-exportable Android Keystore key.
 *
 * Files are encrypted in independent 1 MiB chunks (fresh IV per chunk, 128-bit tag each) so a
 * multi-GB video never has to fit in memory: GCM only authenticates at the end of a chunk.
 *
 * File format: "SPV1" | [ivLen:u8][iv][cipherLen:i32][ciphertext+tag] ... repeated.
 */
object VaultCrypto {
    private const val ALIAS = "storagepulse_vault_aes256"
    private const val MAGIC = "SPV1"
    private const val CHUNK = 1 shl 20
    private const val TAG_BITS = 128

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    /** True when the key lives in a TEE / secure element (StrongBox or TrustZone). */
    fun isHardwareBacked(): Boolean = runCatching {
        val k = key()
        val info = SecretKeyFactory.getInstance(k.algorithm, "AndroidKeyStore").getKeySpec(k, KeyInfo::class.java) as KeyInfo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            info.securityLevel != KeyProperties.SECURITY_LEVEL_SOFTWARE && info.securityLevel != KeyProperties.SECURITY_LEVEL_UNKNOWN
        } else {
            @Suppress("DEPRECATION")
            info.isInsideSecureHardware
        }
    }.getOrDefault(false)

    fun encrypt(input: InputStream, output: OutputStream) {
        val k = key()
        val out = DataOutputStream(output)
        out.write(MAGIC.toByteArray())
        val buf = ByteArray(CHUNK)
        while (true) {
            val n = readFully(input, buf)
            if (n <= 0) break
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, k) }
            val sealed = cipher.doFinal(buf, 0, n)
            out.writeByte(cipher.iv.size)
            out.write(cipher.iv)
            out.writeInt(sealed.size)
            out.write(sealed)
            if (n < buf.size) break
        }
        out.flush()
    }

    /** Throws if any chunk was modified (GCM tag check) or the file is not a vault file. */
    fun decrypt(input: InputStream, output: OutputStream) {
        val k = key()
        val inp = DataInputStream(input)
        val magic = ByteArray(4)
        inp.readFully(magic)
        if (String(magic) != MAGIC) throw IOException("Not a vault file")
        while (true) {
            val ivLen = try { inp.readUnsignedByte() } catch (e: EOFException) { break }
            val iv = ByteArray(ivLen).also { inp.readFully(it) }
            val len = inp.readInt()
            if (len < 0 || len > CHUNK + 64) throw IOException("Corrupt vault chunk")
            val sealed = ByteArray(len).also { inp.readFully(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(TAG_BITS, iv))
            }
            output.write(cipher.doFinal(sealed))
        }
        output.flush()
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val n = input.read(buf, total, buf.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }
}
