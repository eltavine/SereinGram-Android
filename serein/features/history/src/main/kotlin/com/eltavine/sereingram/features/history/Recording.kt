package com.eltavine.sereingram.features.history

/** What is about to be lost, as the recorder sees it before Telegram drops it. */
public class Change(
    public val botChat: Boolean,
    public val textChanged: Boolean = false,
    public val mediaChanged: Boolean = false,
    /** The server hides the edit, as it does for keyboards of bots and moving live locations. */
    public val hidden: Boolean = false,
    /** The message destroys itself by design: it has a timer or its media can be viewed once. */
    public val selfDestructing: Boolean = false,
)

/** Messages that destroy themselves are their sender's to lose, so they are not kept. */
public fun recordsDeletion(saveDeleted: Boolean, saveInBotChats: Boolean, change: Change): Boolean =
    saveDeleted && (saveInBotChats || !change.botChat) && !change.selfDestructing

/**
 * An edit is kept only when it replaced the text or the media and the server
 * shows it as an edit; reaction and markup updates are not edits.
 */
public fun recordsEdit(saveEdits: Boolean, saveInBotChats: Boolean, change: Change): Boolean =
    saveEdits && (saveInBotChats || !change.botChat) && !change.hidden && !change.selfDestructing &&
        (change.textChanged || change.mediaChanged)
