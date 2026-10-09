package com.eltavine.sereingram.settings.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ImageLoader
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * The pictures the user chose for tiles of the SereinGram page, a small square file each in the
 * app's own storage, named after the key of the tile's row. They stay on this device, out of backups.
 */
internal object TilePictures {
    // Reading a picked image may wait on its provider, such as a cloud drive, which would hold up Telegram's own queues.
    val io: Executor = Executors.newSingleThreadExecutor { Thread(it, "serein-tiles") }

    private val shown = ConcurrentHashMap<String, Pair<Long, Bitmap>>()

    /** The picture of [key]'s tile, if the user chose one; small, and kept once read, as rows ask for it while they bind. */
    fun of(key: String): Bitmap? {
        val file = fileOf(key)
        val changed = file.lastModified()
        if (changed == 0L) {
            shown.remove(key)
            return null
        }
        shown[key]?.takeIf { it.first == changed }?.let { return it.second }
        return BitmapFactory.decodeFile(file.path)?.also { shown[key] = changed to it }
    }

    fun has(key: String): Boolean = fileOf(key).isFile

    /** Makes the image at [uri] the picture of [key]'s tile, the square in its middle, made small; off the UI thread. */
    fun save(key: String, uri: Uri) {
        val image = ImageLoader.loadBitmap(null, uri, SIZE.toFloat(), SIZE.toFloat(), false) ?: throw IOException("$uri is no image")
        val side = min(image.width, image.height)
        val square = Bitmap.createBitmap(image, (image.width - side) / 2, (image.height - side) / 2, side, side)
        val picture = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
        val file = fileOf(key)
        file.parentFile?.mkdirs()
        val written = File(file.parentFile, "${file.name}.part")
        written.outputStream().use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!written.renameTo(file)) {
            written.delete()
            throw IOException("could not keep the picture of $key")
        }
        shown.remove(key)
    }

    fun remove(key: String) {
        fileOf(key).delete()
        shown.remove(key)
    }

    private fun fileOf(key: String): File = File(File(ApplicationLoader.applicationContext.filesDir, FOLDER), "$key.png")

    private const val FOLDER = "serein_tiles"

    // A 28 dp tile on the densest screens, with some room to spare.
    private const val SIZE = 144
}
