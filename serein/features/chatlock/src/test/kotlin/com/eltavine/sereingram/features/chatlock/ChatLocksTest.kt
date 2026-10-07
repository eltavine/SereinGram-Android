package com.eltavine.sereingram.features.chatlock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatLocksTest {
    private val settings = LockSettings(lockedChats = setOf(-100L), lockArchive = true, lockSecretChats = false)

    @Test
    fun lockedChatsTheArchiveAndSecretChatsAskWhenTheyAreLocked() {
        assertTrue(isLocked(LockTarget.Chat(-100, secret = false), settings))
        assertFalse(isLocked(LockTarget.Chat(42, secret = false), settings))
        assertFalse(isLocked(LockTarget.Chat(7, secret = true), settings))
        assertTrue(isLocked(LockTarget.Chat(7, secret = true), LockSettings(emptySet(), lockArchive = false, lockSecretChats = true)))
        assertTrue(isLocked(LockTarget.Archive, settings))
        assertFalse(isLocked(LockTarget.Archive, LockSettings(emptySet(), lockArchive = false, lockSecretChats = false)))
    }

    @Test
    fun aLockedArchiveLocksTheChatsInItWhereverTheyOpen() {
        assertTrue(isLocked(LockTarget.Chat(42, secret = false, archived = true), settings))
        assertFalse(isLocked(LockTarget.Chat(42, secret = false, archived = true), LockSettings(emptySet(), lockArchive = false, lockSecretChats = false)))
    }

    @Test
    fun anUnlockLastsItsWindowOnly() {
        var now = 1_000L
        val window = UnlockWindow(millis = 100) { now }
        assertFalse(window.isOpen())
        window.unlock()
        assertTrue(window.isOpen())
        now += 99
        assertTrue(window.isOpen())
        now += 1
        assertFalse(window.isOpen())
    }

    @Test
    fun storageKeysNeverChange() {
        assertEquals(listOf("chat_lock_chats", "chat_lock_archive", "chat_lock_secret_chats"), ChatLockOptions.all.map { it.key })
        assertTrue(ChatLockOptions.all.all { it.secret }, "a backup must not be able to lift a lock")
    }
}
