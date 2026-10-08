package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.ports.KeptText
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class TranscriptTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 10, 7, hour, minute).atZone(zone).toInstant()

    private fun deleted(id: Int, text: String, sentAt: Int, deletedAt: Long, from: Long) =
        KeptText(messageId = id, date = sentAt, recordedAt = deletedAt, fromId = from, text = text)

    @Test
    fun messagesAreWrittenOldestFirstWithWhoSentThemAndWhenTheyWereDeleted() {
        val messages = sequenceOf(
            deleted(4, "hello", sentAt = at(9, 0).epochSecond.toInt(), deletedAt = at(9, 1).toEpochMilli(), from = 7),
            deleted(9, "", sentAt = at(12, 5).epochSecond.toInt(), deletedAt = at(12, 30).toEpochMilli(), from = 8),
        )
        val words = TranscriptWords(title = "Book club: deleted messages", deleted = "deleted", noText = "no text")
        val text = buildString { writeTranscript(messages, words, sender = { if (it == 7L) "Alice" else "Bob" }, zone = zone, out = this) }
        assertEquals(
            """
            Book club: deleted messages

            2026-10-07 09:00 Alice
            hello
            (deleted 2026-10-07 09:01)

            2026-10-07 12:05 Bob
            no text
            (deleted 2026-10-07 12:30)

            """.trimIndent(),
            text,
        )
    }

    @Test
    fun pagesAreReadOneAfterAnotherUntilOneComesShort() {
        val ids = (1..7).toList()
        val asked = mutableListOf<Int>()
        val messages = inPages(pageSize = 3) { after, limit ->
            asked += after
            ids.filter { it > after }.take(limit).map { deleted(it, "", sentAt = 0, deletedAt = 0, from = 1) }
        }
        assertEquals(ids, messages.map { it.messageId }.toList())
        assertEquals(listOf(0, 3, 6), asked)
    }
}
