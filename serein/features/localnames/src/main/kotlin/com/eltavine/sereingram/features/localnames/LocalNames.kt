package com.eltavine.sereingram.features.localnames

import com.eltavine.sereingram.ports.LocalNameStore
import java.util.concurrent.ConcurrentHashMap

/**
 * Local names per account, read from the store once and then served from
 * memory, since Telegram asks for them every time a user or chat is put.
 */
public class LocalNames(private val stores: (account: Int) -> LocalNameStore) {
    private val accounts = ConcurrentHashMap<Int, ConcurrentHashMap<Long, String>>()

    public fun of(account: Int, peerId: Long): String? = names(account)[peerId]

    public fun all(account: Int): Map<Long, String> = HashMap(names(account))

    /** Sets or, for a null or blank [name], removes the local name; returns what is kept. */
    public fun set(account: Int, peerId: Long, name: String?): String? {
        val kept = name?.trim()?.takeIf { it.isNotEmpty() }
        if (kept == null) names(account).remove(peerId) else names(account)[peerId] = kept
        stores(account).set(peerId, kept)
        return kept
    }

    private fun names(account: Int): ConcurrentHashMap<Long, String> =
        accounts.getOrPut(account) { ConcurrentHashMap(stores(account).all()) }
}

/** The names Telegram gave people and chats before a local name replaced them, while it is known. */
public class OriginalNames {
    private val names = ConcurrentHashMap<Pair<Int, Long>, String>()

    public fun remember(account: Int, peerId: Long, name: String) {
        names[account to peerId] = name
    }

    public fun of(account: Int, peerId: Long): String? = names[account to peerId]

    public fun forget(account: Int, peerId: Long) {
        names.remove(account to peerId)
    }
}
