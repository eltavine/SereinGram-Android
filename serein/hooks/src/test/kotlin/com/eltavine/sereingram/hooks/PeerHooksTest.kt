package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PeerHooksTest {
    private class Named(var name: String)

    /** Renames whatever it is put to [shown] and gives back a copy named [original]. */
    private class Renamer(private val original: String, private val shown: String) :
        PeerHooks.UserRewriter, PeerHooks.ChatRewriter {
        override fun beforePut(account: Int, user: Any) {
            (user as Named).name = shown
        }

        override fun original(account: Int, user: Any): Any =
            if ((user as Named).name == shown) Named(original) else user
    }

    private object Broken : PeerHooks.UserRewriter {
        override fun beforePut(account: Int, user: Any): Unit = throw IllegalStateException()

        override fun original(account: Int, user: Any): Any = throw IllegalStateException()
    }

    @Test
    fun rewritersChangeUsersAndChatsAndABrokenOneIsSkipped() {
        val user = Named("Alice")
        val chat = Named("Book club")
        val installs = listOf(
            PeerHooks.userRewriters.install(Broken),
            PeerHooks.userRewriters.install(Renamer(original = "Alice", shown = "Ali")),
            PeerHooks.chatRewriters.install(Renamer(original = "Book club", shown = "Club")),
        )
        try {
            PeerHooks.beforeUserPut(0, user)
            PeerHooks.beforeChatPut(1, chat)
            assertEquals("Ali", user.name)
            assertEquals("Club", chat.name)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun originalsUndoTheRewritersInReverseAndLeaveThePutObjectAlone() {
        val user = Named("Alice")
        val installs = listOf(
            PeerHooks.userRewriters.install(Renamer(original = "Alice", shown = "Ali")),
            PeerHooks.userRewriters.install(Broken),
            PeerHooks.userRewriters.install(Renamer(original = "Ali", shown = "Boss")),
        )
        try {
            PeerHooks.beforeUserPut(0, user)
            assertEquals("Boss", user.name)
            assertEquals("Alice", (PeerHooks.originalUser(0, user) as Named).name)
            assertEquals("Boss", user.name)
            val stranger = Named("Carol")
            assertSame(stranger, PeerHooks.originalUser(0, stranger))
            assertNull(PeerHooks.originalUser(0, null))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun chatsWithoutRewritersAreTheirOwnOriginals() {
        val chat = Named("Book club")
        assertSame(chat, PeerHooks.originalChat(0, chat))
        assertNull(PeerHooks.originalChat(0, null))
    }
}
