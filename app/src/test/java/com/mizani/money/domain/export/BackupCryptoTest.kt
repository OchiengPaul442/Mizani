package com.mizani.money.domain.export

import com.mizani.money.core.security.PinHasher
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun `new backups use the v2 envelope and round trip`() {
        val passphrase = "river market budget phrase"
        val plaintext = "Mizani backup payload".toByteArray(StandardCharsets.UTF_8)
        val encrypted = BackupCrypto.encrypt(plaintext, passphrase)

        assertEquals("MZNB2", String(encrypted.copyOfRange(0, 5), StandardCharsets.US_ASCII))
        assertArrayEquals(plaintext, BackupCrypto.decrypt(encrypted, passphrase))
    }

    @Test
    fun `legacy v1 backups remain restorable`() {
        val passphrase = "legacy backup phrase"
        val plaintext = "old backup data".toByteArray(StandardCharsets.UTF_8)
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val key = PinHasher.hash(passphrase, salt, iterations = 100_000)
        val encrypted = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            "MZNB1".toByteArray(StandardCharsets.US_ASCII) + salt + cipher.iv + cipher.doFinal(plaintext)
        } finally {
            key.fill(0)
        }

        assertTrue(encrypted.size > plaintext.size)
        assertArrayEquals(plaintext, BackupCrypto.decrypt(encrypted, passphrase))
    }

    @Test
    fun `wrong passphrase is rejected with a generic backup error`() {
        val encrypted = BackupCrypto.encrypt("private ledger".toByteArray(), "correct backup phrase")

        try {
            BackupCrypto.decrypt(encrypted, "incorrect backup phrase")
            fail("Expected the wrong passphrase to be rejected")
        } catch (error: BackupException) {
            assertEquals("Incorrect passphrase or damaged backup", error.message)
        }
    }

    @Test
    fun `modified ciphertext is rejected`() {
        val encrypted = BackupCrypto.encrypt("private ledger".toByteArray(), "correct backup phrase")
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        try {
            BackupCrypto.decrypt(encrypted, "correct backup phrase")
            fail("Expected tampered ciphertext to be rejected")
        } catch (error: BackupException) {
            assertEquals("Incorrect passphrase or damaged backup", error.message)
        }
    }

    @Test
    fun `refuses to create a backup larger than the restore limit`() {
        try {
            BackupCrypto.encrypt(ByteArray(BackupFileReader.MAX_BYTES), "long enough backup phrase")
            throw AssertionError("Expected an oversized backup to be rejected")
        } catch (error: BackupException) {
            assertEquals("Backup is too large to create", error.message)
        }
    }
}
