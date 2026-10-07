package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals

class PeerHooksTest {
    private class Named(var name: String)

    @Test
    fun rewritersChangeUsersAndChatsAndABrokenOneIsSkipped() {
        val user = Named("Alice")
        val chat = Named("Book club")
        val installs = listOf(
            PeerHooks.userRewriters.install { _, _ -> throw IllegalStateException() },
            PeerHooks.userRewriters.install { _, u -> (u as Named).name = "Ali" },
            PeerHooks.chatRewriters.install { account, c -> (c as Named).name = "Club $account" },
        )
        try {
            PeerHooks.beforeUserPut(0, user)
            PeerHooks.beforeChatPut(1, chat)
            assertEquals("Ali", user.name)
            assertEquals("Club 1", chat.name)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
