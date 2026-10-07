package com.eltavine.sereingram.features.history

/** The kinds of chat that the media backup of deleted messages tells apart, after AyuGram's. */
public enum class ChatKind { PRIVATE, GROUP, CHANNEL }

/** What a deleted message's media needs for SereinGram to keep a copy of it. */
public class MediaBackupPolicy(
    private val enabled: Boolean,
    private val kinds: Set<ChatKind>,
    private val maxBytes: Long = MAX_BYTES,
) {
    public fun backsUp(kind: ChatKind?, size: Long): Boolean =
        enabled && kind != null && kind in kinds && size in 1..maxBytes

    public companion object {
        /** Larger files are left to Telegram's cache: the backup must not fill the phone. */
        public const val MAX_BYTES: Long = 64L * 1024 * 1024
    }
}

/** The file a message's media is backed up under, unique per chat and message. */
public fun backupName(dialogId: Long, messageId: Int): String = "${dialogId}_$messageId"

/** Whether [name], a backup's file name, belongs to [dialogId]. */
public fun isBackupOf(name: String, dialogId: Long): Boolean = name.startsWith("${dialogId}_")
