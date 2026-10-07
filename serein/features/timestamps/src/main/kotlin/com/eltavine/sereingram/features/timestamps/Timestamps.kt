package com.eltavine.sereingram.features.timestamps

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption
import java.time.Instant
import java.time.ZoneId

/** How message times are written; off by default, as in Telegram. */
public object TimestampOptions {
    public val showDate: Option<Boolean> = booleanOption("timestamps_show_date")

    public val all: List<Option<*>> = listOf(showDate)
}

/** How much of the date a message's time needs so that it can be read without tapping. */
public enum class DateShown { NONE, DAY_AND_MONTH, FULL }

/** After NagramX's request: no date today, day and month this year, the full date before that. */
public fun dateShown(sentAt: Instant, now: Instant, zone: ZoneId): DateShown {
    val sent = sentAt.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when {
        !sent.isBefore(today) -> DateShown.NONE
        sent.year == today.year -> DateShown.DAY_AND_MONTH
        else -> DateShown.FULL
    }
}

/** [time] with [date] put right before its clock, so marks in front of it stay in front. */
public fun withDate(time: String, clock: String, date: String): String {
    val at = time.lastIndexOf(clock)
    return if (at < 0 || clock.isEmpty()) time else time.substring(0, at) + date + " " + time.substring(at)
}
