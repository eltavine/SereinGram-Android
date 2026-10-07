package com.eltavine.sereingram.features.qrcode

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.net.URI

/** Reading QR codes in the photos of a chat, after NagramX's item in the media viewer. */
public object QrCodeOptions {
    public val scanInMenu: Option<Boolean> = booleanOption("qr_scan_in_menu", default = true)

    public val all: List<Option<*>> = listOf(scanInMenu)
}

private val hints = mapOf(
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    DecodeHintType.TRY_HARDER to true,
)

/** The text of the QR code in an image of [width] × [height] ARGB [pixels], or null when there is none. */
public fun readQrCode(pixels: IntArray, width: Int, height: Int): String? {
    require(pixels.size >= width * height) { "${pixels.size} pixels for $width × $height" }
    val source = RGBLuminanceSource(width, height, pixels)
    val reader = MultiFormatReader().apply { setHints(hints) }
    // Dark codes on light backgrounds are the common case; screenshots of dark themes are not.
    return listOf(source, source.invert()).firstNotNullOfOrNull { image ->
        try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(image))).text
        } catch (_: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }
}

/** Whether [text] is a link Telegram can open: a web address or a tg: link. */
public fun isOpenable(text: String): Boolean {
    val scheme = runCatching { URI(text.trim()).scheme }.getOrNull()?.lowercase() ?: return false
    return scheme == "http" || scheme == "https" || scheme == "tg"
}
