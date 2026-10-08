package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.hooks.MessageMenuHooks
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.R

/** When a kept message was deleted, shown in its menu, after NagramXF's request. */
internal class DeletedAtEntry(private val kept: MessageMap<Long>) : MessageMenuHooks.Entry {
    override val option: Int = MessageMenuHooks.FIRST_OPTION + 2

    override val icon: Int = R.drawable.msg_delete

    override fun isShown(account: Int, message: Any): Boolean = deletedAt(account, message) != null

    override fun title(account: Int, message: Any): CharSequence {
        val seconds = (deletedAt(account, message) ?: 0L) / 1000
        return LocaleController.formatString(R.string.serein_history_deleted_at, LocaleController.formatDateTime(seconds, true))
    }

    override fun onSelected(account: Int, message: Any, host: Any) = Unit

    private fun deletedAt(account: Int, message: Any): Long? {
        val shown = message as MessageObject
        return if (isInHistory(shown)) kept[account, shown.dialogId, shown.id] else null
    }
}

/** Scheduled messages and quick replies number their ids apart from a chat's history. */
internal fun isInHistory(message: MessageObject): Boolean = !message.scheduled && !message.isQuickReply
