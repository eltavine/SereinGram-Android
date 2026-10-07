package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.textOption

/** Message history, after AyuGram's message saving; each account decides for itself. */
public object HistoryOptions {
    public val saveDeleted: Option<Boolean> = booleanOption("history_save_deleted", scope = OptionScope.ACCOUNT)
    public val saveEdits: Option<Boolean> = booleanOption("history_save_edits", scope = OptionScope.ACCOUNT)

    /** Bots edit and delete their own messages constantly, so their chats are left out by default. */
    public val saveInBotChats: Option<Boolean> = booleanOption("history_save_in_bot_chats", scope = OptionScope.ACCOUNT)

    /** What stands before the time of a kept deleted message, after AyuGram's custom mark; empty for the default. */
    public val deletedMark: Option<String> = textOption("history_deleted_mark")

    /** Copies of the media of deleted messages, after AyuGram's saved media; channels are left out by default. */
    public val backupMedia: Option<Boolean> = booleanOption("history_backup_media", default = true, scope = OptionScope.ACCOUNT)
    public val backupInPrivateChats: Option<Boolean> =
        booleanOption("history_backup_in_private_chats", default = true, scope = OptionScope.ACCOUNT)
    public val backupInGroups: Option<Boolean> = booleanOption("history_backup_in_groups", default = true, scope = OptionScope.ACCOUNT)
    public val backupInChannels: Option<Boolean> = booleanOption("history_backup_in_channels", scope = OptionScope.ACCOUNT)

    public val all: List<Option<*>> = listOf(
        saveDeleted,
        saveEdits,
        saveInBotChats,
        deletedMark,
        backupMedia,
        backupInPrivateChats,
        backupInGroups,
        backupInChannels,
    )
}
