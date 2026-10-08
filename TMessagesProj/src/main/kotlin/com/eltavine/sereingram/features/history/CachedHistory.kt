package com.eltavine.sereingram.features.history

import org.telegram.messenger.MessagesStorage

/**
 * What Telegram's database holds of a chat's history, by the ids of messages
 * the server knows. Reads on Telegram's storage queue, where history loads.
 */
internal class CachedHistory(private val storage: MessagesStorage) {
    fun hasOlder(dialogId: Long, than: Int): Boolean =
        first("SELECT 1 FROM messages_v2 WHERE uid = ? AND mid > 0 AND mid < ? LIMIT 1", dialogId, than) != null

    fun nextNewer(dialogId: Long, than: Int): Int? =
        first("SELECT min(mid) FROM messages_v2 WHERE uid = ? AND mid > ?", dialogId, maxOf(than, 0))

    private fun first(sql: String, vararg args: Any): Int? {
        val cursor = storage.database.queryFinalized(sql, *args)
        try {
            return if (cursor.next() && !cursor.isNull(0)) cursor.intValue(0) else null
        } finally {
            cursor.dispose()
        }
    }
}
