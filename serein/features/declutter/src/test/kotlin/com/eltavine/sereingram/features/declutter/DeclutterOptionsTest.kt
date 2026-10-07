package com.eltavine.sereingram.features.declutter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeclutterOptionsTest {
    @Test
    fun storageKeysNeverChange() {
        assertEquals(
            listOf(
                "hide_premium_section",
                "hide_help_section",
                "hide_share_button",
                "hide_channel_gift_button",
                "hide_channel_message_button",
                "hide_reactions",
            ),
            DeclutterOptions.all.map { it.key },
        )
        assertTrue(DeclutterOptions.all.none { it.default == true })
    }

    @Test
    fun savedMessagesKeepTheirShareButton() {
        assertTrue(allowShareButton(hide = false, saved = false))
        assertFalse(allowShareButton(hide = true, saved = false))
        assertTrue(allowShareButton(hide = true, saved = true))
    }

    @Test
    fun tagsOnSavedMessagesStayWhenReactionsAreHidden() {
        assertTrue(hidesReactions(hide = true, tags = false))
        assertFalse(hidesReactions(hide = true, tags = true))
        assertFalse(hidesReactions(hide = false, tags = false))
    }
}
