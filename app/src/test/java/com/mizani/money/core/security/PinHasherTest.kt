package com.mizani.money.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `matches the published PBKDF2-HMAC-SHA256 vector`() {
        val output = PinHasher.hash("password", "salt".toByteArray(), iterations = 1, keyLength = 32)
        assertEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            output.toHex(),
        )
    }

    @Test
    fun `verifies the correct secret and rejects others`() {
        val salt = PinHasher.newSalt(16)
        val hash = PinHasher.hash("246810", salt, iterations = 2_000)
        assertTrue(PinHasher.verify("246810", salt, hash, iterations = 2_000))
        assertFalse(PinHasher.verify("246811", salt, hash, iterations = 2_000))
    }

    @Test
    fun `different salts produce different hashes`() {
        val a = PinHasher.hash("123456", ByteArray(16) { 1 }, iterations = 1_000)
        val b = PinHasher.hash("123456", ByteArray(16) { 2 }, iterations = 1_000)
        assertFalse(a.contentEquals(b))
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
