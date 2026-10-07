package com.eltavine.sereingram.features.messagemenu

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageMenuOptionsTest {
    private fun sender(userId: Long = 7, isSelf: Boolean = false, isBlocked: Boolean = false, inGroup: Boolean = true) =
        Sender(userId, isSelf, isBlocked, inGroup)

    @Test
    fun blockingIsOfferedForOtherPeopleInGroups() {
        assertTrue(offersBlock(enabled = true, sender()))
    }

    @Test
    fun blockingIsNotOfferedWhereItMakesNoSense() {
        assertFalse(offersBlock(enabled = false, sender()))
        assertFalse(offersBlock(enabled = true, sender(inGroup = false)))
        assertFalse(offersBlock(enabled = true, sender(userId = -100)))
        assertFalse(offersBlock(enabled = true, sender(isSelf = true)))
        assertFalse(offersBlock(enabled = true, sender(isBlocked = true)))
    }
}
