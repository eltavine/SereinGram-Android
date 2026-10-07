package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.RecordKind
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class TranscriptTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 10, 7, hour, minute).atZone(zone).toInstant()

    private fun deleted(id: Int, text: String, sentAt: Int, deletedAt: Long, from: Long) = HistoryRecord(
        RecordKind.DELETED, dialogId = -100, messageId = id, revision = 0, topicId = 0, date = sentAt,
        recordedAt = deletedAt, fromId = from, text = text, tlMessage = byteArrayOf(), apiLayer = 214,
    )

    @Test
    fun messagesAreWrittenOldestFirstWithWhoSentThemAndWhenTheyWereDeleted() {
        val records = listOf(
            deleted(9, "", sentAt = at(12, 5).epochSecond.toInt(), deletedAt = at(12, 30).toEpochMilli(), from = 8),
            deleted(4, "hello", sentAt = at(9, 0).epochSecond.toInt(), deletedAt = at(9, 1).toEpochMilli(), from = 7),
        )
        val words = TranscriptWords(title = "Book club: deleted messages", deleted = "deleted", noText = "no text")
        val text = transcript(records, words, sender = { if (it == 7L) "Alice" else "Bob" }, zone = zone)
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
}
