package com.eltavine.sereingram.features.downloads

import java.io.InputStream

/** Compares every byte through EOF, with bounded memory and independently short provider reads. */
public fun sameContents(first: InputStream, second: InputStream): Boolean {
    val left = ByteArray(64 * 1024)
    val right = ByteArray(left.size)
    while (true) {
        val count = first.readChunk(left)
        if (count != second.readChunk(right)) return false
        if (count == 0) return true
        for (index in 0 until count) {
            if (left[index] != right[index]) return false
        }
    }
}

private fun InputStream.readChunk(buffer: ByteArray): Int {
    var count = 0
    while (count < buffer.size) {
        val read = read(buffer, count, buffer.size - count)
        if (read < 0) break
        if (read == 0) {
            val byte = read()
            if (byte < 0) break
            buffer[count++] = byte.toByte()
        } else {
            count += read
        }
    }
    return count
}
