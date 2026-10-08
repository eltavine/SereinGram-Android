package com.eltavine.sereingram.ports

/** Names one account gave people and chats on this device only, by Telegram's id for them. */
public interface LocalNameStore {
    public fun all(): Map<Long, String>

    /** A null or blank [name] removes the local name. */
    public fun set(peerId: Long, name: String?)

    /** Removes every local name of the account. */
    public fun clearAll()
}
