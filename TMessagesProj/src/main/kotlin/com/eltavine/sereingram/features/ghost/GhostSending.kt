package com.eltavine.sereingram.features.ghost

import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.SendHooks
import org.telegram.messenger.DialogObject
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager

/**
 * Sends messages a moment later as scheduled ones while ghost mode hides the
 * online status; [now] is Telegram's server time in seconds.
 */
internal class GhostSending(
    private val options: Options,
    private val now: (account: Int) -> Int = { ConnectionsManager.getInstance(it).currentTime },
) : SendHooks.Rewriter {
    override fun beforeSend(account: Int, message: Any) {
        if (!options.get(GhostOptions.sendScheduled)) {
            return
        }
        val params = message as SendMessagesHelper.SendMessageParams
        params.scheduleDate = ghostScheduleDate(
            params.scheduleDate,
            now = now(account),
            onlineHidden = NagramGhost.onlineHidden,
            enabled = true,
            schedulable = schedulable(account, params),
        )
    }

    // Quick replies, retries, suggested posts and the like have their own timing.
    private fun schedulable(account: Int, params: SendMessagesHelper.SendMessageParams): Boolean =
        params.retryMessageObject == null &&
            params.quick_reply_shortcut == null &&
            params.quick_reply_shortcut_id == 0 &&
            params.sendMessageChatArguments == null &&
            params.suggestionParams == null &&
            params.ephemeralReceiverBotId == 0L &&
            !DialogObject.isEncryptedDialog(params.peer) &&
            params.peer != UserConfig.getInstance(account).clientUserId
}
