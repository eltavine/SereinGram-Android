package com.eltavine.sereingram.features.messagemenu

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** Items SereinGram can add to the menu of a message; each can be turned off. */
public object MessageMenuOptions {
    public val blockSender: Option<Boolean> = booleanOption("message_menu_block_sender", default = true)

    public val all: List<Option<*>> = listOf(blockSender)
}

/** Who sent a message, as far as blocking them from its menu goes. */
public class Sender(
    public val userId: Long,
    public val isSelf: Boolean,
    public val isBlocked: Boolean,
    /** The message is in a group or a channel's comments, where the chat itself offers no way to block. */
    public val inGroup: Boolean,
)

/** NagramX's most wanted missing item: blocking someone from a message they sent in a group. */
public fun offersBlock(enabled: Boolean, sender: Sender): Boolean =
    enabled && sender.inGroup && sender.userId > 0 && !sender.isSelf && !sender.isBlocked
