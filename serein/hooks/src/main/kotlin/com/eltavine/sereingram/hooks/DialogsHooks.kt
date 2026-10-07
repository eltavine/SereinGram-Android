package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram draws the chat list. */
public object DialogsHooks {
    public fun interface TitleStatus {
        /**
         * May draw into [slot], Telegram's status drawable beside the chat list
         * title where the emoji status goes; true when it did and keeps the slot.
         */
        public fun draw(account: Int, slot: Any, animated: Boolean): Boolean
    }

    public fun interface PreviewReplacer {
        /** Text to show instead of the last message of [dialogId] in the chat list, or null to show it. */
        public fun replace(account: Int, dialogId: Long): CharSequence?
    }

    public val titleStatuses: Handlers<TitleStatus> = Handlers()
    public val previewReplacers: Handlers<PreviewReplacer> = Handlers()

    /** What replaces a chat's last message in the chat list, if anything does. */
    @JvmStatic
    public fun replacePreview(account: Int, dialogId: Long): CharSequence? =
        previewReplacers.all.firstNotNullOfOrNull { replacer ->
            Faults.guard("preview replacer", fallback = null) { replacer.replace(account, dialogId) }
        }

    /** True when a module drew the status beside the title instead of Telegram. */
    @JvmStatic
    public fun drawTitleStatus(account: Int, slot: Any, animated: Boolean): Boolean =
        titleStatuses.all.any { status ->
            Faults.guard("title status", fallback = false) { status.draw(account, slot, animated) }
        }
}
