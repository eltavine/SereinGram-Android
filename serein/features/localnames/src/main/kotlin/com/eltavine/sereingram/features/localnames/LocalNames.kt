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

    /** Removes every local name of [account], from the store and from memory. */
    public fun forget(account: Int) {
        stores(account).clearAll()
        accounts.remove(account)
    }

    private fun names(account: Int): ConcurrentHashMap<Long, String> =
        accounts.getOrPut(account) { ConcurrentHashMap(stores(account).all()) }
}

/** A name as Telegram has it: a person's first and last name, or a chat's title as the first. */
public data class PeerName(val first: String, val last: String = "") {
    public val full: String get() = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
}

/**
 * The names Telegram gave people and chats that show a local name instead,
 * so that whatever leaves the device or goes into Telegram's database can
 * carry them rather than the local name.
 */
public class OriginalNames {
    private class Renamed(val original: PeerName, val shown: PeerName)

    private val names = ConcurrentHashMap<Pair<Int, Long>, Renamed>()

    /**
     * The name to show instead of [current], remembering [current] unless it
     * is the local name shown before; null when [current] already is [local].
     */
    public fun replace(account: Int, peerId: Long, current: PeerName, local: String): PeerName? {
        val shown = PeerName(local)
        if (current == shown) {
            return null
        }
        names.compute(account to peerId) { _, before ->
            Renamed(if (before != null && current == before.shown) before.original else current, shown)
        }
        return shown
    }

    /** Telegram's name for a peer that shows [current], when [current] is the local name it was given. */
    public fun behind(account: Int, peerId: Long, current: PeerName): PeerName? =
        names[account to peerId]?.takeIf { it.shown == current }?.original

    public fun of(account: Int, peerId: Long): PeerName? = names[account to peerId]?.original

    public fun forget(account: Int, peerId: Long) {
        names.remove(account to peerId)
    }

    public fun forgetAccount(account: Int) {
        names.keys.removeIf { it.first == account }
    }
}
