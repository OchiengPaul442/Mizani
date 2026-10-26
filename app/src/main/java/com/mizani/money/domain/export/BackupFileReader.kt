package com.mizani.money.domain.export

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Reads user-selected backup files without allowing an unbounded allocation. */
internal object BackupFileReader {
    const val MAX_BYTES = 20 * 1024 * 1024

    fun read(input: InputStream, maxBytes: Int = MAX_BYTES): ByteArray {
        require(maxBytes in 0 until Int.MAX_VALUE)
        val output = ByteArrayOutputStream(minOf(maxBytes, BUFFER_BYTES))
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0

        while (true) {
            val bytesUntilLimit = maxBytes - total
            val count = input.read(buffer, 0, minOf(buffer.size, bytesUntilLimit + 1))
            if (count < 0) break
            if (count == 0) {
                val nextByte = input.read()
                if (nextByte < 0) break
                if (total == maxBytes) throw BackupException("Backup is too large")
                output.write(nextByte)
                total++
                continue
            }
            if (count > bytesUntilLimit) throw BackupException("Backup is too large")
            output.write(buffer, 0, count)
            total += count
        }

        return output.toByteArray()
    }

    private const val BUFFER_BYTES = 8 * 1024
}
