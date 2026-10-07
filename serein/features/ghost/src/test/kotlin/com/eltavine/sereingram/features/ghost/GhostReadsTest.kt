package com.eltavine.sereingram.features.ghost

import kotlin.test.Test
import kotlin.test.assertEquals

class GhostReadsTest {
    @Test
    fun readsGoOutAsUsualWhileReceiptsAreSent() {
        UnheldRead.entries.forEach { assertEquals(Hold.SEND, hold(it, readsHidden = false)) }
    }

    @Test
    fun hiddenReadsAreDroppedButViewsStillLoadUncounted() {
        assertEquals(Hold.DROP, hold(UnheldRead.DISCUSSION, readsHidden = true))
        assertEquals(Hold.DROP, hold(UnheldRead.SECRET_CHAT, readsHidden = true))
        assertEquals(Hold.SEND_UNCOUNTED, hold(UnheldRead.VIEW_COUNT, readsHidden = true))
    }
}
