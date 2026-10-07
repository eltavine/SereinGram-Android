package com.eltavine.sereingram.features.history

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaBackupTest {
    private val policy = MediaBackupPolicy(enabled = true, kinds = setOf(ChatKind.PRIVATE, ChatKind.GROUP), maxBytes = 100)

    @Test
    fun mediaIsBackedUpFromTheChosenKindsOfChatWithinTheLimit() {
        assertTrue(policy.backsUp(ChatKind.PRIVATE, 100))
        assertTrue(policy.backsUp(ChatKind.GROUP, 1))
        assertFalse(policy.backsUp(ChatKind.CHANNEL, 10))
        assertFalse(policy.backsUp(ChatKind.PRIVATE, 101))
        assertFalse(policy.backsUp(ChatKind.PRIVATE, 0))
        assertFalse(policy.backsUp(null, 10))
    }

    @Test
    fun nothingIsBackedUpWhenTheBackupIsOff() {
        assertFalse(MediaBackupPolicy(enabled = false, kinds = ChatKind.entries.toSet()).backsUp(ChatKind.PRIVATE, 10))
    }

    @Test
    fun backupNamesTellTheirChatApart() {
        assertTrue(isBackupOf(backupName(-100, 5), -100))
        assertFalse(isBackupOf(backupName(-1000, 5), -100))
        assertFalse(isBackupOf(backupName(100, 5), -100))
    }
}
