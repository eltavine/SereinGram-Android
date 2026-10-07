package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.ports.HistoryRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The words of a transcript, in the reader's language. */
public class TranscriptWords(
    public val title: String,
    public val deleted: String,
    public val noText: String,
)

/**
 * A plain text transcript of kept deleted messages, oldest first, for keeping
 * outside SereinGram; times are written the same way in every language.
 */
public fun transcript(records: List<HistoryRecord>, words: TranscriptWords, sender: (Long) -> String, zone: ZoneId): String {
    val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone)
    return buildString {
        appendLine(words.title)
        records.sortedBy { it.messageId }.forEach { record ->
            appendLine()
            appendLine("${time.format(Instant.ofEpochSecond(record.date.toLong()))} ${sender(record.fromId)}")
            appendLine(record.text.ifEmpty { words.noText })
            appendLine("(${words.deleted} ${time.format(Instant.ofEpochMilli(record.recordedAt))})")
        }
    }
}
