package com.eltavine.sereingram.features.chatlock

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.textOption

/**
 * What asks to be unlocked before it opens, per account, after Cherrygram's and OctoGram's locks.
 * They stay out of backups so that no backup can lift a lock.
 */
public object ChatLockOptions {
    public val lockedChats: Option<String> = textOption("chat_lock_chats", scope = OptionScope.ACCOUNT, backedUp = false)
    public val lockArchive: Option<Boolean> = booleanOption("chat_lock_archive", scope = OptionScope.ACCOUNT, backedUp = false)
    public val lockSecretChats: Option<Boolean> = booleanOption("chat_lock_secret_chats", scope = OptionScope.ACCOUNT, backedUp = false)

    public val all: List<Option<*>> = listOf(lockedChats, lockArchive, lockSecretChats)
}

/** A screen that a lock can keep closed. */
public sealed interface LockTarget {
    /** A chat, its profile or its topics; [secret] for a secret chat, [archived] for one in the archive. */
    public class Chat(public val dialogId: Long, public val secret: Boolean, public val archived: Boolean = false) : LockTarget

    public data object Archive : LockTarget
}

public class LockSettings(
    public val lockedChats: Set<Long>,
    public val lockArchive: Boolean,
    public val lockSecretChats: Boolean,
)

/** A locked archive locks the chats in it too, which search and folders open without the archive. */
public fun isLocked(target: LockTarget, settings: LockSettings): Boolean = when (target) {
    is LockTarget.Chat ->
        target.dialogId in settings.lockedChats || target.secret && settings.lockSecretChats || target.archived && settings.lockArchive
    LockTarget.Archive -> settings.lockArchive
}

/** After an unlock, locked screens open without asking again for [millis]. */
public class UnlockWindow(private val millis: Long = 60_000, private val clock: () -> Long) {
    private var openUntil = Long.MIN_VALUE

    @Synchronized
    public fun unlock() {
        openUntil = clock() + millis
    }

    /** Ends the window early, as when the app goes to the background. */
    @Synchronized
    public fun close() {
        openUntil = Long.MIN_VALUE
    }

    @Synchronized
    public fun isOpen(): Boolean = clock() < openUntil
}
