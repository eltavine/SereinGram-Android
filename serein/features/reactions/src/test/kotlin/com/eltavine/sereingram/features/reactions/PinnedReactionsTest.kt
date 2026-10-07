package com.eltavine.sereingram.features.reactions

import kotlin.test.Test
import kotlin.test.assertEquals

class PinnedReactionsTest {
    @Test
    fun emojiAreReadWithOrWithoutSeparators() {
        assertEquals(listOf("👍", "❤️", "🔥"), pinnedEmoji("👍❤️🔥"))
        assertEquals(listOf("👍", "❤️", "🔥"), pinnedEmoji(" 👍, ❤️，🔥 👍 "))
        assertEquals(listOf("👨‍💻", "🤷‍♂️"), pinnedEmoji("👨‍💻 🤷‍♂️"))
        assertEquals(listOf("😂"), pinnedEmoji("ok 😂 lol!"))
        assertEquals(listOf("‼️", "🇯🇵"), pinnedEmoji("‼️、🇯🇵"))
        assertEquals(emptyList(), pinnedEmoji(""))
    }

    @Test
    fun pinnedReactionsComeFirstWhenTheChatOffersThem() {
        val offered = listOf("👍", "❤", null, "🔥", "😂")
        assertEquals(listOf(4, 1), pinnedFirst(offered, listOf("😂", "❤️", "🦄")))
        assertEquals(emptyList(), pinnedFirst(offered, emptyList()))
    }
}
