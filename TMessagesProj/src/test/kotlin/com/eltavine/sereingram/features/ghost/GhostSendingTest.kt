package com.eltavine.sereingram.features.ghost

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.Options
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.DialogObject
import org.telegram.messenger.SendMessagesHelper
import tw.nekomimi.nekogram.NekoConfig

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class GhostSendingTest {
    private val store = MemoryKeyValueStore()
    private val options = Options { _, _ -> store }
    private val sending = GhostSending(options) { 1_000 }
    private var sendOnline = true

    @Before
    fun setUp() {
        ApplicationLoader.applicationContext = ApplicationProvider.getApplicationContext()
        sendOnline = NekoConfig.sendOnlinePackets
        NekoConfig.sendOnlinePackets = false
        options.set(GhostOptions.sendScheduled, true)
    }

    @After
    fun tearDown() {
        NekoConfig.sendOnlinePackets = sendOnline
    }

    private fun sent(peer: Long, change: SendMessagesHelper.SendMessageParams.() -> Unit = {}): Int {
        val params = SendMessagesHelper.SendMessageParams.of("hi", peer).apply(change)
        sending.beforeSend(0, params)
        return params.scheduleDate
    }

    @Test
    fun messagesWaitAMomentWhileTheOnlineStatusIsHidden() {
        assertEquals(1_012, sent(42))
        assertEquals(1_012, sent(-100))
    }

    @Test
    fun messagesGoOutAtOnceWhenTheyCannotOrNeedNotWait() {
        assertEquals(5_000, sent(42) { scheduleDate = 5_000 })
        assertEquals(0, sent(42) { quick_reply_shortcut = "hello" })
        assertEquals(0, sent(DialogObject.makeEncryptedDialogId(7)))
        NekoConfig.sendOnlinePackets = true
        assertEquals(0, sent(42))
        NekoConfig.sendOnlinePackets = false
        options.set(GhostOptions.sendScheduled, false)
        assertEquals(0, sent(42))
    }
}
