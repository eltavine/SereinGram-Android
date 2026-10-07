package com.eltavine.sereingram.features.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationOptionsTest {
    @Test
    fun storageKeysNeverChangeAndEverythingStartsOff() {
        assertEquals(listOf("notifications_content_when_locked", "notifications_plain_answers"), NotificationOptions.all.map { it.key })
        assertTrue(NotificationOptions.all.none { it.default == true })
    }
}
