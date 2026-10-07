package com.eltavine.sereingram.features.ghost

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.RequestHooks
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader
import org.telegram.tgnet.TLRPC
import tw.nekomimi.nekogram.NekoConfig

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class GhostFeatureTest {
    private var sendReads = true

    @Before
    fun setUp() {
        ApplicationLoader.applicationContext = ApplicationProvider.getApplicationContext()
        sendReads = NekoConfig.sendReadMessagePackets
        val store = MemoryKeyValueStore()
        GhostFeature.start(ModuleContext(Options { _, _ -> store }) { _, _ -> })
    }

    @After
    fun tearDown() {
        NekoConfig.sendReadMessagePackets = sendReads
    }

    private fun views() = TLRPC.TL_messages_getMessagesViews().apply { increment = true }

    @Test
    fun readsGoOutUnchangedWhileReceiptsAreSent() {
        NekoConfig.sendReadMessagePackets = true
        val views = views()
        assertTrue(RequestHooks.intercept(0, views))
        assertTrue(views.increment)
        assertTrue(RequestHooks.intercept(0, TLRPC.TL_messages_readDiscussion()))
    }

    @Test
    fun readsNagramLetsThroughAreHeldBackInGhostMode() {
        NekoConfig.sendReadMessagePackets = false
        val views = views()
        assertTrue(RequestHooks.intercept(0, views))
        assertFalse(views.increment)
        assertFalse(RequestHooks.intercept(0, TLRPC.TL_messages_readDiscussion()))
        assertFalse(RequestHooks.intercept(0, TLRPC.TL_messages_readEncryptedHistory()))
        assertTrue(RequestHooks.intercept(0, TLRPC.TL_messages_sendMessage()))
    }
}
