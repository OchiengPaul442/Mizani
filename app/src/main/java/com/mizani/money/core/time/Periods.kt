package com.mizani.money.core.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Inclusive date range used for reporting. Local dates are always used for reporting so
 * that changing timezone never shifts a historical transaction into another day.
 */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start)) { "End date must not be before start date" }
    }

    val days: Int
        get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun overlaps(other: DateRange): Boolean = !end.isBefore(other.start) && !start.isAfter(other.end)

    fun elapsedFraction(today: LocalDate): Double {
        if (today.isBefore(start)) return 0.0
        if (today.isAfter(end)) return 1.0
        val elapsed = (ChronoUnit.DAYS.between(start, today) + 1).toDouble()
        return (elapsed / days.toDouble()).coerceIn(0.0, 1.0)
    }

    fun daysRemaining(today: LocalDate): Int {
        if (today.isAfter(end)) return 0
        if (today.isBefore(start)) return days
        return (ChronoUnit.DAYS.between(today, end) + 1).toInt()
    }

    fun shiftDays(days: Long): DateRange = DateRange(start.plusDays(days), end.plusDays(days))
}

enum class PeriodPreset(val label: String) {
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    LAST_MONTH("Previous month"),
    CUSTOM("Custom period"),
}

object Periods {

    fun today(): LocalDate = LocalDate.now()

    fun weekOf(anchor: LocalDate): DateRange {
        val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return DateRange(monday, monday.plusDays(6))
    }

    fun monthOf(anchor: LocalDate): DateRange {
        val month = YearMonth.from(anchor)
        return DateRange(month.atDay(1), month.atEndOfMonth())
    }

    fun fourWeekFrom(anchor: LocalDate): DateRange =
        DateRange(anchor, anchor.plusDays(27))

    fun lastMonth(anchor: LocalDate): DateRange =
        monthOf(anchor.minusMonths(1))

    fun previous(range: DateRange): DateRange {
        val length = range.days.toLong()
        return DateRange(range.start.minusDays(length), range.start.minusDays(1))
    }

    fun preset(preset: PeriodPreset, anchor: LocalDate = today()): DateRange = when (preset) {
        PeriodPreset.THIS_WEEK -> weekOf(anchor)
        PeriodPreset.THIS_MONTH -> monthOf(anchor)
        PeriodPreset.LAST_MONTH -> lastMonth(anchor)
        PeriodPreset.CUSTOM -> monthOf(anchor)
    }

    fun budgetRange(type: com.mizani.money.core.model.BudgetPeriodType, anchor: LocalDate): DateRange =
        when (type) {
            com.mizani.money.core.model.BudgetPeriodType.WEEKLY -> weekOf(anchor)
            com.mizani.money.core.model.BudgetPeriodType.MONTHLY -> monthOf(anchor)
            com.mizani.money.core.model.BudgetPeriodType.FOUR_WEEK -> fourWeekFrom(
                anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            )
            com.mizani.money.core.model.BudgetPeriodType.CUSTOM -> monthOf(anchor)
        }

    fun nextOccurrence(
        current: LocalDate,
        frequency: com.mizani.money.core.model.Frequency,
    ): LocalDate = when (frequency) {
        com.mizani.money.core.model.Frequency.WEEKLY -> current.plusWeeks(1)
        com.mizani.money.core.model.Frequency.BIWEEKLY -> current.plusWeeks(2)
        com.mizani.money.core.model.Frequency.MONTHLY -> addMonthsClamped(current, 1)
        com.mizani.money.core.model.Frequency.QUARTERLY -> addMonthsClamped(current, 3)
        com.mizani.money.core.model.Frequency.YEARLY -> addMonthsClamped(current, 12)
    }

    /** Adds months while clamping the day to the target month's length (31 Jan -> 28/29 Feb). */
    fun addMonthsClamped(date: LocalDate, months: Long): LocalDate {
        val target = YearMonth.from(date).plusMonths(months)
        val day = minOf(date.dayOfMonth, target.lengthOfMonth())
        return target.atDay(day)
    }
}

object DateText {
    private val shortFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val mediumFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK)
    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)

    fun short(date: LocalDate): String = shortFormatter.format(date)
    fun medium(date: LocalDate): String = mediumFormatter.format(date)
    fun month(date: LocalDate): String = monthFormatter.format(date)
    fun weekday(date: LocalDate): String = weekdayFormatter.format(date)

    fun relative(date: LocalDate, today: LocalDate = Periods.today()): String {
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        return when {
            days == 0 -> "Today"
            days == 1 -> "Tomorrow"
            days == -1 -> "Yesterday"
            days in 2..6 -> "In $days days"
            days in -6..-2 -> "${-days} days ago"
            date.year == today.year -> short(date)
            else -> medium(date)
        }
    }

    fun rangeLabel(range: DateRange): String {
        val sameMonth = range.start.year == range.end.year &&
            range.start.month == range.end.month
        return if (sameMonth) {
            "${range.start.dayOfMonth}–${range.end.dayOfMonth} ${month(range.start)}"
        } else {
            "${short(range.start)} – ${short(range.end)}"
        }
    }

    fun greeting(hour: Int): String = when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
