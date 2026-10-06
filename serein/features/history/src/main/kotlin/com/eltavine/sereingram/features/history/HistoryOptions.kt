package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.booleanOption

/** Message history, after AyuGram's message saving; each account decides for itself. */
public object HistoryOptions {
    public val saveDeleted: Option<Boolean> = booleanOption("history_save_deleted", scope = OptionScope.ACCOUNT)
    public val saveEdits: Option<Boolean> = booleanOption("history_save_edits", scope = OptionScope.ACCOUNT)

    /** Bots edit and delete their own messages constantly, so their chats are left out by default. */
    public val saveInBotChats: Option<Boolean> = booleanOption("history_save_in_bot_chats", scope = OptionScope.ACCOUNT)

    public val all: List<Option<*>> = listOf(saveDeleted, saveEdits, saveInBotChats)
}
