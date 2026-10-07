package com.eltavine.sereingram.features.timestamps

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampsTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = LocalDateTime.of(2026, 10, 7, 0, 30).atZone(zone).toInstant()

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12) = LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant()

    @Test
    fun todayNeedsNoDateAndEarlierDaysDo() {
        assertEquals(DateShown.NONE, dateShown(at(2026, 10, 7, hour = 0), now, zone))
        assertEquals(DateShown.DAY_AND_MONTH, dateShown(at(2026, 10, 6, hour = 23), now, zone))
        assertEquals(DateShown.DAY_AND_MONTH, dateShown(at(2026, 1, 1), now, zone))
        assertEquals(DateShown.FULL, dateShown(at(2025, 12, 31), now, zone))
    }

    @Test
    fun theDayDependsOnTheTimeZone() {
        val beforeMidnightInShanghai = LocalDateTime.of(2026, 10, 6, 15, 50).atZone(ZoneId.of("UTC")).toInstant()
        assertEquals(DateShown.DAY_AND_MONTH, dateShown(beforeMidnightInShanghai, now, zone))
        assertEquals(DateShown.NONE, dateShown(beforeMidnightInShanghai, now, ZoneId.of("UTC")))
    }

    @Test
    fun theDateGoesRightBeforeTheClock() {
        assertEquals("Oct 6 14:05", withDate("14:05", "14:05", "Oct 6"))
        assertEquals("deleted edited Oct 6 14:05 | 42", withDate("deleted edited 14:05 | 42", "14:05", "Oct 6"))
        assertEquals("Oct 6, 14:05", withDate("Oct 6, 14:05", "09:00", "Oct 6"))
        assertEquals("", withDate("", "14:05", "Oct 6"))
    }
}
