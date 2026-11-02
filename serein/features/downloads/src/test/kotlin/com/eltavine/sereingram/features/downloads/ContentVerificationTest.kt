package com.eltavine.sereingram.features.downloads

import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ContentVerificationTest {
    private val content = ByteArray(200_000) { (it % 251).toByte() }

    @Test
    fun comparesEveryByteIncludingTheLastAcrossMultipleBuffers() {
        assertTrue(sameContents(content.inputStream(), content.inputStream()))
        for (index in listOf(0, 65_536, content.lastIndex)) {
            val corrupt = content.copyOf().apply { this[index] = (this[index] + 1).toByte() }
            assertFalse(sameContents(content.inputStream(), corrupt.inputStream()))
        }
    }

    @Test
    fun checksBothEndsAndDoesNotTrustTheSharedPrefix() {
        assertFalse(sameContents(content.inputStream(), content.copyOf(content.size - 1).inputStream()))
        assertFalse(sameContents(content.inputStream(), (content + byteArrayOf(0)).inputStream()))
        assertTrue(sameContents(byteArrayOf().inputStream(), byteArrayOf().inputStream()))
    }

    @Test
    fun handlesProviderReadsOfDifferentLengths() {
        val short = object : ByteArrayInputStream(content) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int = super.read(bytes, offset, minOf(length, 7))
        }
        assertTrue(sameContents(content.inputStream(), short))
    }

    @Test
    fun aReadErrorCannotBeMistakenForEquality() {
        val broken = object : ByteArrayInputStream(content) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int = throw IOException("Read failed")
        }
        assertFailsWith<IOException> { sameContents(content.inputStream(), broken) }
    }
}
