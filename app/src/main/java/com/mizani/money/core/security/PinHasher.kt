package com.mizani.money.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PBKDF2-HMAC-SHA256 used for the optional app-lock PIN and encrypted backups.
 * Implemented locally so behavior is identical on API 24..37 and in JVM tests.
 * Deliberately uses a per-secret random salt and a constant-time comparison.
 */
object PinHasher {

    const val DEFAULT_ITERATIONS = 120_000
    private const val KEY_LENGTH = 32

    fun newSalt(size: Int = 16): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }

    fun hash(
        secret: String,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS,
        keyLength: Int = KEY_LENGTH,
    ): ByteArray {
        require(secret.isNotEmpty()) { "Secret must not be empty" }
        require(salt.isNotEmpty()) { "Salt must not be empty" }
        require(iterations > 0) { "Iterations must be positive" }
        require(keyLength > 0) { "Key length must be positive" }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val output = ByteArray(keyLength)
        var generated = 0
        var blockIndex = 1
        val block = ByteArray(4)
        while (generated < keyLength) {
            block[0] = (blockIndex ushr 24).toByte()
            block[1] = (blockIndex ushr 16).toByte()
            block[2] = (blockIndex ushr 8).toByte()
            block[3] = blockIndex.toByte()
            mac.update(salt)
            mac.update(block)
            var u = mac.doFinal()
            val accumulator = u.copyOf()
            for (iteration in 1 until iterations) {
                u = mac.doFinal(u)
                for (index in accumulator.indices) {
                    accumulator[index] = (accumulator[index].toInt() xor u[index].toInt()).toByte()
                }
            }
            val toCopy = minOf(keyLength - generated, accumulator.size)
            System.arraycopy(accumulator, 0, output, generated, toCopy)
            generated += toCopy
            blockIndex++
        }
        return output
    }

    fun verify(
        secret: String,
        salt: ByteArray,
        expected: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS,
    ): Boolean {
        if (expected.isEmpty()) return false
        return try {
            MessageDigest.isEqual(hash(secret, salt, iterations, expected.size), expected)
        } catch (_: Exception) {
            false
        }
    }
}
