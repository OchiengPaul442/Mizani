package com.mizani.money.core.time

import com.mizani.money.core.model.Frequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodsTest {

    @Test
    fun `month end is clamped for short months`() {
        assertEquals(
            LocalDate.of(2024, 2, 29),
            Periods.nextOccurrence(LocalDate.of(2024, 1, 31), Frequency.MONTHLY),
        )
        assertEquals(
            LocalDate.of(2023, 2, 28),
            Periods.nextOccurrence(LocalDate.of(2023, 1, 31), Frequency.MONTHLY),
        )
    }

    @Test
    fun `leap day yearly recurrence clamps`() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            Periods.nextOccurrence(LocalDate.of(2024, 2, 29), Frequency.YEARLY),
        )
    }

    @Test
    fun `weekly and biweekly recurrences are exact`() {
        assertEquals(
            LocalDate.of(2026, 10, 29),
            Periods.nextOccurrence(LocalDate.of(2026, 10, 22), Frequency.WEEKLY),
        )
        assertEquals(
            LocalDate.of(2026, 11, 5),
            Periods.nextOccurrence(LocalDate.of(2026, 10, 22), Frequency.BIWEEKLY),
        )
    }

    @Test
    fun `week starts on Monday`() {
        val sunday = LocalDate.of(2026, 10, 25)
        assertEquals(DateRange(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)), Periods.weekOf(sunday))
    }

    @Test
    fun `date range day count is inclusive`() {
        assertEquals(31, DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).days)
        assertEquals(1, DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)).days)
    }

    @Test
    fun `elapsed fraction is bounded`() {
        val range = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertEquals(0.0, range.elapsedFraction(LocalDate.of(2026, 9, 30)), 0.0001)
        assertEquals(1.0, range.elapsedFraction(LocalDate.of(2026, 11, 2)), 0.0001)
        assertEquals(15.0 / 31.0, range.elapsedFraction(LocalDate.of(2026, 10, 15)), 0.0001)
    }

    @Test
    fun `days remaining is inclusive`() {
        val range = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertEquals(17, range.daysRemaining(LocalDate.of(2026, 10, 15)))
        assertEquals(0, range.daysRemaining(LocalDate.of(2026, 11, 1)))
    }
}
