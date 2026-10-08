package com.eltavine.sereingram.features.history

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.tgnet.TLRPC

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MediaChangeTest {
    private fun photo(id: Long) = TLRPC.TL_messageMediaPhoto().apply { photo = TLRPC.TL_photo().also { it.id = id } }

    private fun document(id: Long): TLRPC.MessageMedia =
        TLRPC.TL_messageMediaDocument_layer74().apply { document = TLRPC.TL_document().also { it.id = id } }

    @Test
    fun replacingAPhotoOrFileIsAnEdit() {
        assertFalse(mediaChanged(photo(1), photo(1)))
        assertTrue(mediaChanged(photo(1), photo(2)))
        assertTrue(mediaChanged(photo(1), document(1)))
        assertFalse(mediaChanged(document(5), TLRPC.TL_messageMediaDocument().apply { document = TLRPC.TL_document().also { it.id = 5 } }))
        assertTrue(mediaChanged(null, photo(1)))
        assertTrue(mediaChanged(photo(1), TLRPC.TL_messageMediaEmpty()))
    }

    @Test
    fun previewsAndWhatChangesByItselfAreNotEdits() {
        assertFalse(mediaChanged(null, TLRPC.TL_messageMediaWebPage()))
        assertFalse(mediaChanged(TLRPC.TL_messageMediaEmpty(), null))
        val moved = TLRPC.TL_messageMediaGeoLive().apply { geo = TLRPC.TL_geoPoint().also { it.lat = 1.0 } }
        assertFalse(mediaChanged(TLRPC.TL_messageMediaGeoLive(), moved))
    }
}
