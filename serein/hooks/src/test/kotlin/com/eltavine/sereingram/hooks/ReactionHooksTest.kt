package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals

class ReactionHooksTest {
    @Test
    fun arrangersMoveReactionsToTheFrontAndKeepTheRest() {
        val offered = listOf("👍", "❤️", "🔥", "😂")
        assertEquals(offered, ReactionHooks.arrange(0, offered))
        val installs = listOf(
            ReactionHooks.arrangers.install { _, _ -> throw IllegalStateException() },
            ReactionHooks.arrangers.install { _, reactions -> listOf(reactions.indexOf("🔥"), 9, reactions.indexOf("🔥"), -1, reactions.indexOf("😂")) },
        )
        try {
            assertEquals(listOf("🔥", "😂", "👍", "❤️"), ReactionHooks.arrange(0, offered))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
