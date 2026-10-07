package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/**
 * The moments before Telegram drops a version of a message. Both run on
 * Telegram's storage queue while the old data is still in its database.
 */
public object HistoryHooks {
    public fun interface DeletionListener {
        /** [dialogId] is 0 for private chats and basic groups, whose message ids are unique per account. */
        public fun beforeDeleted(account: Int, dialogId: Long, messageIds: List<Int>)
    }

    public fun interface EditListener {
        /** [previous] and [next] are Telegram's message objects; [sameMedia] as Telegram decided it. */
        public fun beforeEdited(account: Int, dialogId: Long, previous: Any, next: Any, sameMedia: Boolean)
    }

    public fun interface LoadListener {
        /**
         * Sees a batch of history Telegram read from its database before the chat
         * gets it, and may add to it. The lists hold Telegram's message, user and
         * chat objects; [mode] and [threadMessageId] are 0 for a chat's own history.
         */
        public fun afterLoaded(
            account: Int,
            dialogId: Long,
            mode: Int,
            threadMessageId: Long,
            messages: MutableList<Any?>,
            users: MutableList<Any?>,
            chats: MutableList<Any?>,
        )
    }

    public val deletionListeners: Handlers<DeletionListener> = Handlers()
    public val editListeners: Handlers<EditListener> = Handlers()
    public val loadListeners: Handlers<LoadListener> = Handlers()

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    public fun afterHistoryLoaded(
        account: Int,
        dialogId: Long,
        mode: Int,
        threadMessageId: Long,
        messages: MutableList<*>,
        users: MutableList<*>,
        chats: MutableList<*>,
    ) {
        loadListeners.all.forEach { listener ->
            Faults.guard("history load listener", fallback = Unit) {
                listener.afterLoaded(
                    account,
                    dialogId,
                    mode,
                    threadMessageId,
                    messages as MutableList<Any?>,
                    users as MutableList<Any?>,
                    chats as MutableList<Any?>,
                )
            }
        }
    }

    @JvmStatic
    public fun beforeMessagesDeleted(account: Int, dialogId: Long, messageIds: List<Int>) {
        deletionListeners.all.forEach { listener ->
            Faults.guard("history deletion listener", fallback = Unit) {
                listener.beforeDeleted(account, dialogId, messageIds)
            }
        }
    }

    @JvmStatic
    public fun beforeMessageEdited(account: Int, dialogId: Long, previous: Any, next: Any, sameMedia: Boolean) {
        editListeners.all.forEach { listener ->
            Faults.guard("history edit listener", fallback = Unit) {
                listener.beforeEdited(account, dialogId, previous, next, sameMedia)
            }
        }
    }
}
