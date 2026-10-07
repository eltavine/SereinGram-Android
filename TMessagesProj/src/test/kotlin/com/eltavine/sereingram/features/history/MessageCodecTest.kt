package com.eltavine.sereingram.features.history

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader
import org.telegram.tgnet.TLRPC

// Telegram's serializer calls android.text.TextUtils and asks ApplicationLoader about the build.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MessageCodecTest {
    @Before
    fun setUp() {
        ApplicationLoader.applicationContext = ApplicationProvider.getApplicationContext()
        ApplicationLoader.applicationLoaderInstance = ApplicationLoader()
    }

    private fun message() = TLRPC.TL_message().apply {
        id = 4242
        date = 1_790_000_000
        message = "a message that was deleted"
        peer_id = TLRPC.TL_peerUser().apply { user_id = 777 }
        from_id = TLRPC.TL_peerUser().apply { user_id = 888 }
        flags = flags or TLRPC.MESSAGE_FLAG_HAS_FROM_ID or TLRPC.MESSAGE_FLAG_HAS_ENTITIES
        entities = arrayListOf(TLRPC.TL_messageEntityBold().apply { offset = 2; length = 7 })
    }

    @Test
    fun aMessageSurvivesTheRoundTrip() {
        val decoded = MessageCodec.decode(MessageCodec.encode(message()), selfId = 888)!!
        assertEquals(4242, decoded.id)
        assertEquals(1_790_000_000, decoded.date)
        assertEquals("a message that was deleted", decoded.message)
        assertEquals(777L, decoded.peer_id.user_id)
        assertEquals(888L, decoded.from_id.user_id)
        assertEquals(1, decoded.entities.size)
        assertEquals(7, decoded.entities[0].length)
    }

    @Test
    fun anOutgoingPhotoKeepsItsLocalFile() {
        val sent = message().apply {
            out = true
            flags = flags or TLRPC.MESSAGE_FLAG_HAS_MEDIA
            media = TLRPC.TL_messageMediaPhoto().apply {
                flags = 1
                photo = TLRPC.TL_photo().apply {
                    id = 99
                    file_reference = ByteArray(0)
                }
            }
            attachPath = "/storage/emulated/0/Download/photo.jpg"
        }
        val decoded = MessageCodec.decode(MessageCodec.encode(sent), selfId = 888)!!
        assertEquals(99L, decoded.media.photo.id)
        assertEquals("/storage/emulated/0/Download/photo.jpg", decoded.attachPath)
    }

    @Test
    fun bytesOfAnUnknownConstructorDecodeToNothing() {
        assertNull(MessageCodec.decode(byteArrayOf(0x11, 0x22, 0x33, 0x44), selfId = 1))
    }
}
