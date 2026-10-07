package com.eltavine.sereingram.features.ghost

/** Ways of telling a chat that its messages were seen which Nagram's ghost mode lets through. */
public enum class UnheldRead {
    /** Reading the comments of a channel post. */
    DISCUSSION,

    /** Reading a secret chat. */
    SECRET_CHAT,

    /** Counting a view of a channel post. */
    VIEW_COUNT,
}

public enum class Hold {
    SEND,
    DROP,

    /** Send the request, but ask Telegram not to count the view. */
    SEND_UNCOUNTED,
}

/** How [read] goes out; [readsHidden] when ghost mode keeps read receipts back. */
public fun hold(read: UnheldRead, readsHidden: Boolean): Hold = when {
    !readsHidden -> Hold.SEND
    read == UnheldRead.VIEW_COUNT -> Hold.SEND_UNCOUNTED
    else -> Hold.DROP
}
