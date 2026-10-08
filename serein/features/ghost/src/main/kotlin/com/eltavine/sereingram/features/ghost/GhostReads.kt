package com.eltavine.sereingram.features.ghost

/** Ways of telling a chat that its messages were seen which Nagram's ghost mode lets through. */
public enum class UnheldRead {
    /** Reading the comments of a channel post. */
    DISCUSSION,

    /** Reading a secret chat. */
    SECRET_CHAT,

    /** Reading a channel's direct messages, as one of its admins. */
    DIRECT_MESSAGES,

    /** Counting a view of a channel post. */
    VIEW_COUNT,
}

public enum class Hold {
    SEND,
    DROP,

    /** Send the request, but ask Telegram not to count the view. */
    SEND_UNCOUNTED,
}

/**
 * When a message goes out while ghost mode hides the online status, after
 * AyuGram's scheduled messages: a few seconds later, as a scheduled message,
 * since sending one right away would show the user online. Messages already
 * scheduled, and those that cannot be scheduled, keep their time.
 */
public fun ghostScheduleDate(scheduleDate: Int, now: Int, onlineHidden: Boolean, enabled: Boolean, schedulable: Boolean): Int =
    if (scheduleDate == 0 && onlineHidden && enabled && schedulable) now + SEND_DELAY_SECONDS else scheduleDate

/** Telegram does not take scheduled messages that are due only a moment later. */
public const val SEND_DELAY_SECONDS: Int = 12

/** How [read] goes out; [readsHidden] when ghost mode keeps read receipts back. */
public fun hold(read: UnheldRead, readsHidden: Boolean): Hold = when {
    !readsHidden -> Hold.SEND
    read == UnheldRead.VIEW_COUNT -> Hold.SEND_UNCOUNTED
    else -> Hold.DROP
}
