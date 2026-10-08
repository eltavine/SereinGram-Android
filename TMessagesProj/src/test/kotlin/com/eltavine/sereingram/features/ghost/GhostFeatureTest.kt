package com.eltavine.sereingram.features.ghost

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.RequestHooks
import com.eltavine.sereingram.hooks.SecretChatHooks
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.DialogObject
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
        if (!started) {
            GhostFeature.start(ModuleContext(options) { _, _ -> })
            started = true
        }
    }

    @After
    fun tearDown() {
        NekoConfig.sendReadMessagePackets = sendReads
        GhostOptions.all.forEach { options.reset(it, ACCOUNT) }
    }

    private fun user(id: Long) = TLRPC.TL_inputPeerUser().apply { user_id = id }

    private fun views(id: Long) = TLRPC.TL_messages_getMessagesViews().apply {
        peer = user(id)
        increment = true
    }

    private fun discussion(id: Long) = TLRPC.TL_messages_readDiscussion().apply { peer = user(id) }

    @Test
    fun readsGoOutUnchangedWhileReceiptsAreSent() {
        NekoConfig.sendReadMessagePackets = true
        val views = views(5)
        assertTrue(RequestHooks.intercept(ACCOUNT, views))
        assertTrue(views.increment)
        assertTrue(RequestHooks.intercept(ACCOUNT, discussion(5)))
    }

    @Test
    fun readsNagramLetsThroughAreHeldBackInGhostMode() {
        NekoConfig.sendReadMessagePackets = false
        val views = views(5)
        assertTrue(RequestHooks.intercept(ACCOUNT, views))
        assertFalse(views.increment)
        assertFalse(RequestHooks.intercept(ACCOUNT, discussion(5)))
        assertFalse(RequestHooks.intercept(ACCOUNT, TLRPC.TL_messages_readEncryptedHistory().apply { peer = TLRPC.TL_inputEncryptedChat() }))
        val directMessages = TLRPC.TL_messages_readSavedHistory().apply {
            parent_peer = TLRPC.TL_inputPeerChannel().apply { channel_id = 77 }
            peer = user(5)
        }
        assertFalse(RequestHooks.intercept(ACCOUNT, directMessages))
        assertTrue(RequestHooks.intercept(ACCOUNT, TLRPC.TL_messages_sendMessage()))
    }

    @Test
    fun secretChatsHearOfReadsOnlyWhenReceiptsAreSentOrTheyAreExcepted() {
        val secret = DialogObject.makeEncryptedDialogId(9)
        NekoConfig.sendReadMessagePackets = true
        assertTrue(SecretChatHooks.sendsReadReceipt(ACCOUNT, secret))
        NekoConfig.sendReadMessagePackets = false
        assertFalse(SecretChatHooks.sendsReadReceipt(ACCOUNT, secret))
        options.set(GhostOptions.readExceptions, secret.toString(), ACCOUNT)
        assertTrue(SecretChatHooks.sendsReadReceipt(ACCOUNT, secret))
    }

    @Test
    fun chatsWithExceptionsAreLetInOnReadsAndTyping() {
        NekoConfig.sendReadMessagePackets = false
        options.set(GhostOptions.readExceptions, "5", ACCOUNT)
        options.set(GhostOptions.typingExceptions, "6", ACCOUNT)
        val readHistory = TLRPC.TL_messages_readHistory().apply { peer = user(5) }
        assertTrue(RequestHooks.exemptsFromGhostMode(ACCOUNT, readHistory))
        assertFalse(RequestHooks.exemptsFromGhostMode(ACCOUNT, TLRPC.TL_messages_readHistory().apply { peer = user(6) }))
        assertTrue(RequestHooks.exemptsFromGhostMode(ACCOUNT, TLRPC.TL_messages_setTyping().apply { peer = user(6) }))
        assertFalse(RequestHooks.exemptsFromGhostMode(ACCOUNT + 1, readHistory))
        val views = views(5)
        assertTrue(RequestHooks.intercept(ACCOUNT, views))
        assertTrue(views.increment)
        assertTrue(RequestHooks.intercept(ACCOUNT, discussion(5)))
        assertFalse(RequestHooks.exemptsFromGhostMode(ACCOUNT, discussion(5)))
    }

    // Modules install their handlers for good, so the feature starts once for all tests.
    private companion object {
        const val ACCOUNT = 0
        val stores = HashMap<Pair<OptionScope, Int>, MemoryKeyValueStore>()
        val options = Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }
        var started = false
    }
}
