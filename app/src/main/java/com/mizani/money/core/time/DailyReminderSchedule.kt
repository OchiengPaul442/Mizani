package com.mizani.money.core.time

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/** Wall-clock scheduling helpers for the optional daily transaction reminder. */
object DailyReminderSchedule {
    const val MINUTES_PER_DAY = 24 * 60
    const val DEFAULT_MINUTE_OF_DAY = 20 * 60

    fun nextOccurrence(now: ZonedDateTime, minuteOfDay: Int): ZonedDateTime {
        require(minuteOfDay in 0 until MINUTES_PER_DAY)
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }

    fun delayUntilNextOccurrenceMillis(now: ZonedDateTime, minuteOfDay: Int): Long =
        Duration.between(now, nextOccurrence(now, minuteOfDay)).toMillis().coerceAtLeast(0L)
}
