package com.eltavine.sereingram.features.messagemenu

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** Items SereinGram can add to the menu of a message; each can be turned off. */
public object MessageMenuOptions {
    public val blockSender: Option<Boolean> = booleanOption("message_menu_block_sender", default = true)
    public val replyPrivately: Option<Boolean> = booleanOption("message_menu_reply_privately", default = true)

    public val all: List<Option<*>> = listOf(blockSender, replyPrivately)
}

/** Who sent a message, as far as the items of its menu go. */
public class Sender(
    public val userId: Long,
    public val isSelf: Boolean,
    public val isBlocked: Boolean,
    /** The message is in a group or a channel's comments rather than in a private chat. */
    public val inGroup: Boolean,
)

/** NagramX's most wanted missing item: blocking someone from a message they sent in a group. */
public fun offersBlock(enabled: Boolean, sender: Sender): Boolean =
    enabled && sender.inGroup && sender.userId > 0 && !sender.isSelf && !sender.isBlocked

/**
 * NagramX's and OctoGram's "reply in private": answering someone in the private chat with them,
 * replying to what they said in a group. [replyable] tells whether Telegram lets the message be
 * replied to from another chat, which protected chats and view-once media rule out.
 */
public fun offersPrivateReply(enabled: Boolean, sender: Sender, replyable: Boolean): Boolean =
    enabled && replyable && sender.inGroup && sender.userId > 0 && !sender.isSelf && !sender.isBlocked
