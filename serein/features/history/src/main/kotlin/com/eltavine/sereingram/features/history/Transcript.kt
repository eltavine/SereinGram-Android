package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.ports.KeptText
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
 * Writes a plain text transcript of kept deleted [messages], which come
 * oldest first, to [out], for keeping outside SereinGram; times are written
 * the same way in every language. [messages] may be read as they are written.
 */
public fun writeTranscript(messages: Sequence<KeptText>, words: TranscriptWords, sender: (Long) -> String, zone: ZoneId, out: Appendable) {
    val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone)
    out.appendLine(words.title)
    messages.forEach { message ->
        out.appendLine()
        out.appendLine("${time.format(Instant.ofEpochSecond(message.date.toLong()))} ${sender(message.fromId)}")
        out.appendLine(message.text.ifEmpty { words.noText })
        out.appendLine("(${words.deleted} ${time.format(Instant.ofEpochMilli(message.recordedAt))})")
    }
}

/** Every kept deleted message of a chat, oldest first, read [pageSize] at a time through [page]. */
public fun inPages(pageSize: Int, page: (afterMessageId: Int, limit: Int) -> List<KeptText>): Sequence<KeptText> = sequence {
    var after = 0
    while (true) {
        val messages = page(after, pageSize)
        yieldAll(messages)
        if (messages.size < pageSize) {
            break
        }
        after = messages.last().messageId
    }
}
