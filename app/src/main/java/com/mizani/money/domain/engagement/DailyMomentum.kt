package com.mizani.money.domain.engagement

import com.mizani.money.core.model.TransactionType
import com.mizani.money.data.local.TransactionEntity
import java.time.LocalDate

/**
 * Recognition-only progress derived from the local ledger.
 *
 * Momentum deliberately counts tracked calendar days rather than transaction count or money
 * value. That makes it useful for a person who records one small entry, avoids rewarding extra
 * spending, and means there is no engagement state to migrate or lose outside the ledger.
 */
data class DailyMomentum(
    val points: Int,
    val trackedDays: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val todayTracked: Boolean,
    val tier: MomentumTier,
) {
    val nextTier: MomentumTier?
        get() = MomentumTier.entries.firstOrNull { it.minimumPoints > points }

    val pointsToNextTier: Int?
        get() = nextTier?.let { (it.minimumPoints - points).coerceAtLeast(0) }

    /** The user-facing progress measure: each milestone is reached by recording whole days. */
    val trackedDaysToNextTier: Int?
        get() = nextTier?.let { (it.minimumTrackedDays - trackedDays).coerceAtLeast(0) }
}

enum class MomentumTier(
    val minimumTrackedDays: Int,
    val title: String,
) {
    STARTING_OUT(0, "Starting out"),
    FIRST_STEPS(3, "First steps"),
    STEADY(7, "Steady"),
    IN_RHYTHM(30, "In rhythm"),
    MOMENTUM(100, "Momentum"),

    ;

    val minimumPoints: Int
        get() = minimumTrackedDays * DailyMomentumCalculator.POINTS_PER_TRACKED_DAY
}

object DailyMomentumCalculator {
    const val POINTS_PER_TRACKED_DAY = 10

    fun calculate(
        transactions: List<TransactionEntity>,
        today: LocalDate,
    ): DailyMomentum {
        val trackedDates = transactions.asSequence()
            .filter { it.deletedAt == null && it.type != TransactionType.ADJUSTMENT }
            .map { LocalDate.ofEpochDay(it.localDateEpochDay) }
            .filter { !it.isAfter(today) }
            .toSet()
            .sorted()

        val currentStreak = currentStreak(trackedDates, today)
        val points = trackedDates.size * POINTS_PER_TRACKED_DAY
        val tier = MomentumTier.entries.last { it.minimumPoints <= points }

        return DailyMomentum(
            points = points,
            trackedDays = trackedDates.size,
            currentStreak = currentStreak,
            bestStreak = bestStreak(trackedDates),
            todayTracked = today in trackedDates,
            tier = tier,
        )
    }

    private fun currentStreak(dates: List<LocalDate>, today: LocalDate): Int {
        if (today !in dates) return 0
        var streak = 0
        var date = today
        while (date in dates) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    private fun bestStreak(dates: List<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in dates) {
            run = if (previous?.plusDays(1) == date) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }
        return best
    }
}
