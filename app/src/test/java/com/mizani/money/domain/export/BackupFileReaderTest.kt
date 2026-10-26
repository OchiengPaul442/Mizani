package com.mizani.money.domain.export

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupFileReaderTest {

    @Test
    fun `reads files at the configured size limit`() {
        val expected = ByteArray(32) { it.toByte() }

        assertArrayEquals(expected, BackupFileReader.read(ByteArrayInputStream(expected), maxBytes = expected.size))
    }

    @Test
    fun `rejects the first byte beyond the configured size limit`() {
        try {
            BackupFileReader.read(ByteArrayInputStream(ByteArray(9)), maxBytes = 8)
            fail("Expected an oversized backup to be rejected")
        } catch (error: BackupException) {
            assertEquals("Backup is too large", error.message)
        }
    }

    @Test
    fun `makes progress when an input stream returns zero for a bulk read`() {
        val source = object : InputStream() {
            private var next = 0

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = 0

            override fun read(): Int = if (next == 3) -1 else next++
        }

        assertArrayEquals(byteArrayOf(0, 1, 2), BackupFileReader.read(source, maxBytes = 3))
    }
}
