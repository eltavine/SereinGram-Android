package com.eltavine.sereingram.features.history

/** What is about to be lost, as the recorder sees it before Telegram drops it. */
public class Change(
    public val botChat: Boolean,
    public val textChanged: Boolean = false,
    public val mediaChanged: Boolean = false,
    /** The server hides the edit, as it does for keyboards of bots and moving live locations. */
    public val hidden: Boolean = false,
)

public fun recordsDeletion(saveDeleted: Boolean, saveInBotChats: Boolean, change: Change): Boolean =
    saveDeleted && (saveInBotChats || !change.botChat)

/**
 * An edit is kept only when it replaced the text or the media and the server
 * shows it as an edit; reaction and markup updates are not edits.
 */
public fun recordsEdit(saveEdits: Boolean, saveInBotChats: Boolean, change: Change): Boolean =
    saveEdits && (saveInBotChats || !change.botChat) && !change.hidden && (change.textChanged || change.mediaChanged)
