package com.mizani.money.domain.export

import com.mizani.money.core.security.PinHasher
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Versioned AES-GCM backup envelope. New files use the stronger V2 KDF; V1 remains readable. */
internal object BackupCrypto {
    private val legacyMagic = "MZNB1".toByteArray(StandardCharsets.US_ASCII)
    private val currentMagic = "MZNB2".toByteArray(StandardCharsets.US_ASCII)
    private const val LEGACY_ITERATIONS = 100_000
    private const val CURRENT_ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
    private const val ENVELOPE_OVERHEAD_BYTES = 5 + SALT_BYTES + IV_BYTES + GCM_TAG_BYTES

    fun encrypt(plain: ByteArray, passphrase: String): ByteArray {
        if (plain.size > BackupFileReader.MAX_BYTES - ENVELOPE_OVERHEAD_BYTES) {
            throw BackupException("Backup is too large to create")
        }
        val salt = PinHasher.newSalt(SALT_BYTES)
        val key = PinHasher.hash(passphrase, salt, iterations = CURRENT_ITERATIONS)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            val iv = cipher.iv
            require(iv.size == IV_BYTES) { "Unsupported AES-GCM IV size" }
            val encrypted = cipher.doFinal(plain)
            try {
                ByteArray(currentMagic.size + salt.size + iv.size + encrypted.size).also { envelope ->
                    var offset = 0
                    arrayOf(currentMagic, salt, iv, encrypted).forEach { part ->
                        part.copyInto(envelope, destinationOffset = offset)
                        offset += part.size
                    }
                }
            } finally {
                iv.fill(0)
                encrypted.fill(0)
            }
        } finally {
            key.fill(0)
            salt.fill(0)
        }
    }

    fun decrypt(bytes: ByteArray, passphrase: String): ByteArray {
        if (bytes.size > BackupFileReader.MAX_BYTES) throw BackupException("Backup is too large")
        val version = when {
            bytes.startsWith(currentMagic) -> currentMagic to CURRENT_ITERATIONS
            bytes.startsWith(legacyMagic) -> legacyMagic to LEGACY_ITERATIONS
            else -> throw BackupException("Unsupported backup format")
        }
        val (magic, iterations) = version
        val minimumBytes = magic.size + SALT_BYTES + IV_BYTES + GCM_TAG_BYTES
        if (bytes.size < minimumBytes) throw BackupException("This is not a valid Mizani backup")

        val saltStart = magic.size
        val ivStart = saltStart + SALT_BYTES
        val encryptedStart = ivStart + IV_BYTES
        val salt = bytes.copyOfRange(saltStart, ivStart)
        val iv = bytes.copyOfRange(ivStart, encryptedStart)
        val key = PinHasher.hash(passphrase, salt, iterations = iterations)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(bytes, encryptedStart, bytes.size - encryptedStart)
        } catch (_: Exception) {
            throw BackupException("Incorrect passphrase or damaged backup")
        } finally {
            key.fill(0)
            salt.fill(0)
            iv.fill(0)
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
