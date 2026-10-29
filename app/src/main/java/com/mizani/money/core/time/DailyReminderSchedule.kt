package com.mizani.money.core.time

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Wall-clock scheduling helpers for the optional daily transaction reminder. */
object DailyReminderSchedule {
    const val MINUTES_PER_DAY = 24 * 60
    const val DEFAULT_MINUTE_OF_DAY = 20 * 60

    fun nextOccurrence(now: ZonedDateTime, minuteOfDay: Int): ZonedDateTime {
        val today = occurrenceOn(now.toLocalDate().toEpochDay(), minuteOfDay, now.zone)
        return if (today.isAfter(now)) today else {
            occurrenceOn(now.toLocalDate().plusDays(1).toEpochDay(), minuteOfDay, now.zone)
        }
    }

    fun occurrenceOn(epochDay: Long, minuteOfDay: Int, zone: ZoneId): ZonedDateTime {
        require(minuteOfDay in 0 until MINUTES_PER_DAY)
        return LocalDate.ofEpochDay(epochDay)
            .atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
            .atZone(zone)
    }

    fun formatForDisplay(minuteOfDay: Int, locale: Locale): String {
        require(minuteOfDay in 0 until MINUTES_PER_DAY)
        return DateTimeFormatter.ofPattern("h:mm a", locale)
            .format(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
    }

    fun delayUntilNextOccurrenceMillis(now: ZonedDateTime, minuteOfDay: Int): Long =
        Duration.between(now, nextOccurrence(now, minuteOfDay)).toMillis().coerceAtLeast(0L)
}
