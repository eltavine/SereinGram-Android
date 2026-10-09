package com.eltavine.sereingram.settings.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.telegram.messenger.ApplicationLoader
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class TilePicturesTest {
    @Test
    fun aPictureIsTheSquareInTheMiddleOfTheImageMadeSmall() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        ApplicationLoader.applicationContext = context
        val wide = File(context.cacheDir, "wide.png")
        val image = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        Canvas(image).apply {
            drawColor(Color.RED)
            drawRect(100f, 0f, 200f, 100f, Paint().apply { color = Color.BLUE })
        }
        wide.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }

        TilePictures.save("ghost", Uri.fromFile(wide))
        val picture = TilePictures.of("ghost")
        assertTrue(TilePictures.has("ghost"))
        assertEquals(144, picture?.width)
        assertEquals(144, picture?.height)
        assertEquals(listOf(Color.BLUE, Color.BLUE, Color.BLUE), listOf(2, 72, 141).map { picture?.getPixel(it, 72) })

        TilePictures.remove("ghost")
        assertFalse(TilePictures.has("ghost"))
        assertNull(TilePictures.of("ghost"))
    }
}
