package com.eltavine.sereingram.features.qrcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrCodesTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    private fun image(text: String, size: Int = 240, ink: Int = black, paper: Int = white): IntArray {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
        return IntArray(size * size) { i -> if (matrix[i % size, i / size]) ink else paper }
    }

    @Test
    fun readsTheTextOfACode() {
        assertEquals("https://t.me/sereingram", readQrCode(image("https://t.me/sereingram"), 240, 240))
        assertEquals("扫码登录", readQrCode(image("扫码登录"), 240, 240))
    }

    @Test
    fun readsLightCodesOnDarkBackgrounds() {
        assertEquals("tg://login?token=abc", readQrCode(image("tg://login?token=abc", ink = white, paper = black), 240, 240))
    }

    @Test
    fun findsNothingInAPlainImage() {
        assertNull(readQrCode(IntArray(100 * 100) { white }, 100, 100))
    }

    @Test
    fun onlyWebAndTelegramLinksAreOpenable() {
        assertTrue(isOpenable("https://example.com/a?b=c"))
        assertTrue(isOpenable("tg://resolve?domain=durov"))
        assertTrue(isOpenable(" HTTP://example.com "))
        assertFalse(isOpenable("WIFI:S:home;T:WPA;P:secret;;"))
        assertFalse(isOpenable("javascript:alert(1)"))
        assertFalse(isOpenable("plain words"))
    }
}
