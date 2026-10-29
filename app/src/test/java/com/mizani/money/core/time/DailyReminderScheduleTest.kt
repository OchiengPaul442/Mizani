package com.mizani.money.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DailyReminderScheduleTest {

    @Test
    fun nextOccurrenceUsesTheChosenLocalWallClockTime() {
        val zone = ZoneId.of("Africa/Kampala")
        val now = ZonedDateTime.of(2026, 10, 26, 19, 59, 30, 0, zone)

        val next = DailyReminderSchedule.nextOccurrence(now, 20 * 60)

        assertEquals(now.toLocalDate(), next.toLocalDate())
        assertEquals(LocalTime.of(20, 0), next.toLocalTime())
        assertEquals(30_000L, DailyReminderSchedule.delayUntilNextOccurrenceMillis(now, 20 * 60))
    }

    @Test
    fun ifTheReminderTimeHasPassedItSchedulesTomorrow() {
        val zone = ZoneId.of("Africa/Kampala")
        val now = ZonedDateTime.of(2026, 10, 26, 20, 1, 0, 0, zone)

        val next = DailyReminderSchedule.nextOccurrence(now, 20 * 60)

        assertEquals(now.toLocalDate().plusDays(1), next.toLocalDate())
        assertEquals(LocalTime.of(20, 0), next.toLocalTime())
    }

    @Test
    fun daylightSavingGapMovesToTheNextValidLocalTime() {
        val zone = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, zone)

        val next = DailyReminderSchedule.nextOccurrence(now, 2 * 60 + 30)

        assertEquals(LocalTime.of(3, 30), next.toLocalTime())
        assertEquals(zone, next.zone)
    }

    @Test
    fun reminderDisplayMakesAmAndPmExplicit() {
        assertEquals("12:00 AM", DailyReminderSchedule.formatForDisplay(0, Locale.US))
        assertEquals("12:00 PM", DailyReminderSchedule.formatForDisplay(12 * 60, Locale.US))
        assertEquals("8:00 PM", DailyReminderSchedule.formatForDisplay(20 * 60, Locale.US))
    }

    @Test
    fun occurrenceOnUsesTheRequestedLocalZoneAndMinute() {
        val zone = ZoneId.of("Africa/Kampala")

        val occurrence = DailyReminderSchedule.occurrenceOn(
            ZonedDateTime.of(2026, 10, 26, 0, 0, 0, 0, zone).toLocalDate().toEpochDay(),
            20 * 60,
            zone,
        )

        assertEquals(zone, occurrence.zone)
        assertEquals(LocalTime.of(20, 0), occurrence.toLocalTime())
    }
}
