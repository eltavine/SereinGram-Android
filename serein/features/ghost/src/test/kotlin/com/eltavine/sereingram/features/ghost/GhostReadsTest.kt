package com.eltavine.sereingram.features.ghost

import kotlin.test.Test
import kotlin.test.assertEquals

class GhostReadsTest {
    @Test
    fun readsGoOutAsUsualWhileReceiptsAreSent() {
        UnheldRead.entries.forEach { assertEquals(Hold.SEND, hold(it, readsHidden = false)) }
    }

    @Test
    fun messagesWaitAMomentAsScheduledOnlyWhileTheOnlineStatusIsHidden() {
        assertEquals(1_012, ghostScheduleDate(0, now = 1_000, onlineHidden = true, enabled = true, schedulable = true))
        assertEquals(0, ghostScheduleDate(0, now = 1_000, onlineHidden = false, enabled = true, schedulable = true))
        assertEquals(0, ghostScheduleDate(0, now = 1_000, onlineHidden = true, enabled = false, schedulable = true))
        assertEquals(0, ghostScheduleDate(0, now = 1_000, onlineHidden = true, enabled = true, schedulable = false))
        assertEquals(5_000, ghostScheduleDate(5_000, now = 1_000, onlineHidden = true, enabled = true, schedulable = true))
    }

    @Test
    fun hiddenReadsAreDroppedButViewsStillLoadUncounted() {
        assertEquals(Hold.DROP, hold(UnheldRead.DISCUSSION, readsHidden = true))
        assertEquals(Hold.DROP, hold(UnheldRead.SECRET_CHAT, readsHidden = true))
        assertEquals(Hold.SEND_UNCOUNTED, hold(UnheldRead.VIEW_COUNT, readsHidden = true))
    }
}
