package com.eltavine.sereingram.features.localnames

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.PeerHooks
import com.eltavine.sereingram.ports.LocalNameStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader
import org.telegram.tgnet.TLRPC

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LocalNamesFeatureTest {
    @Before
    fun setUp() {
        ApplicationLoader.applicationContext = ApplicationProvider.getApplicationContext()
        if (!started) {
            feature.start(ModuleContext(Options { _, _ -> MemoryKeyValueStore() }) { _, _ -> })
            started = true
        }
    }

    @Test
    fun usersAreShownByTheirLocalNameAndTheirOwnIsRemembered() {
        val user = TLRPC.TL_user().apply {
            id = 7
            first_name = "Alice"
            last_name = "Smith"
        }
        PeerHooks.beforeUserPut(0, user)
        assertEquals("Mum", user.first_name)
        assertEquals("", user.last_name)
        assertEquals(PeerName("Alice", "Smith"), feature.originals.of(0, 7))
        PeerHooks.beforeUserPut(0, user)
        assertEquals(PeerName("Alice", "Smith"), feature.originals.of(0, 7))
    }

    @Test
    fun whatLeavesTheDeviceCarriesTelegramsNamesAndTheShownUserKeepsTheLocalOne() {
        val user = TLRPC.TL_user().apply {
            id = 7
            flags = 2 or 4 or 16
            first_name = "Alice"
            last_name = "Smith"
            phone = "15550100"
        }
        PeerHooks.beforeUserPut(0, user)
        val sent = PeerHooks.originalUser(0, user) as TLRPC.User
        assertNotSame(user, sent)
        assertEquals(listOf("Alice", "Smith", "15550100", 7L), listOf(sent.first_name, sent.last_name, sent.phone, sent.id))
        assertEquals("Mum", user.first_name)
        val fresh = TLRPC.TL_user().apply {
            id = 7
            first_name = "Alicia"
        }
        assertSame("a name fresh from Telegram is its own", fresh, PeerHooks.originalUser(0, fresh))
    }

    @Test
    fun storedChatsKeepTheirTitle() {
        val chat = TLRPC.TL_chat().apply {
            id = 100
            title = "Book club"
            photo = TLRPC.TL_chatPhotoEmpty()
        }
        PeerHooks.beforeChatPut(0, chat)
        assertEquals("Book club", (PeerHooks.originalChat(0, chat) as TLRPC.Chat).title)
        assertEquals("Readers", chat.title)
    }

    @Test
    fun chatsAndOtherAccountsKeepTheirNamesUnlessRenamed() {
        val chat = TLRPC.TL_chat().apply {
            id = 100
            title = "Book club"
        }
        PeerHooks.beforeChatPut(0, chat)
        assertEquals("Readers", chat.title)
        val elsewhere = TLRPC.TL_user().apply {
            id = 7
            first_name = "Alice"
        }
        PeerHooks.beforeUserPut(1, elsewhere)
        assertEquals("Alice", elsewhere.first_name)
    }

    private class MemoryStore(private val names: Map<Long, String>) : LocalNameStore {
        override fun all(): Map<Long, String> = names

        override fun set(peerId: Long, name: String?) = Unit

        override fun clearAll() = Unit
    }

    private companion object {
        val feature = LocalNamesFeature { account -> MemoryStore(if (account == 0) mapOf(7L to "Mum", -100L to "Readers") else emptyMap()) }
        var started = false
    }
}
