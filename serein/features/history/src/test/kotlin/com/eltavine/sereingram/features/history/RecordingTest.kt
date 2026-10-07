package com.eltavine.sereingram.features.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingTest {
    @Test
    fun deletionsAreKeptOnlyWhenSavingIsOn() {
        assertFalse(recordsDeletion(saveDeleted = false, saveInBotChats = true, Change(botChat = false)))
        assertTrue(recordsDeletion(saveDeleted = true, saveInBotChats = false, Change(botChat = false)))
    }

    @Test
    fun botChatsAreLeftOutUnlessAsked() {
        assertFalse(recordsDeletion(saveDeleted = true, saveInBotChats = false, Change(botChat = true)))
        assertTrue(recordsDeletion(saveDeleted = true, saveInBotChats = true, Change(botChat = true)))
        assertFalse(recordsEdit(saveEdits = true, saveInBotChats = false, Change(botChat = true, textChanged = true)))
    }

    @Test
    fun onlyTextOrMediaEditsAreKept() {
        assertFalse(recordsEdit(saveEdits = true, saveInBotChats = false, Change(botChat = false)))
        assertTrue(recordsEdit(saveEdits = true, saveInBotChats = false, Change(botChat = false, textChanged = true)))
        assertTrue(recordsEdit(saveEdits = true, saveInBotChats = false, Change(botChat = false, mediaChanged = true)))
        assertFalse(recordsEdit(saveEdits = false, saveInBotChats = true, Change(botChat = false, textChanged = true)))
    }

    @Test
    fun storageKeysNeverChange() {
        assertEquals(
            listOf(
                "history_save_deleted",
                "history_save_edits",
                "history_save_in_bot_chats",
                "history_deleted_mark",
                "history_backup_media",
                "history_backup_in_private_chats",
                "history_backup_in_groups",
                "history_backup_in_channels",
            ),
            HistoryOptions.all.map { it.key },
        )
    }
}
