package com.mizani.money.domain.notifications

import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.data.repo.LedgerSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The kinds of local, descriptive money check-ins Mizani can offer. */
enum class HabitNotificationKind {
    SPENDING_REVIEW,
    INCOME_REVIEW,
    INVESTMENT_REVIEW,
    DEBT_REVIEW,
    GOAL_REVIEW,
}

data class HabitNotificationPreferences(
    val enabled: Boolean,
    val spendingEnabled: Boolean,
    val incomeEnabled: Boolean,
    val investmentEnabled: Boolean,
    val debtEnabled: Boolean,
    val goalEnabled: Boolean,
)

/**
 * Chooses at most one useful check-in from the encrypted local snapshot.
 *
 * This deliberately produces a category, never a notification sentence or financial value.
 * The UI can show the underlying detail only after the user opens and unlocks the app. Rules are
 * descriptive rather than predictive: an expected income rule is not treated as received money,
 * a stale investment valuation is not an investment recommendation, and a debt date is not a
 * credit assessment.
 */
object HabitNotificationEngine {
    private const val SPENDING_WINDOW_DAYS = 7L
    private const val MIN_SPENDING_ENTRIES = 3
    private const val INCOME_LOOK_BEHIND_DAYS = 1L
    private const val INCOME_LOOK_AHEAD_DAYS = 2L
    private const val DEBT_LOOK_AHEAD_DAYS = 3L
    private const val INVESTMENT_REVIEW_AFTER_DAYS = 30L
    private const val GOAL_LOOK_AHEAD_DAYS = 14L

    /**
     * Returns null when no category is due or when the generic weekly cooldown would make the
     * alert repetitive. A successful worker run persists only the generic date, not the selected
     * financial category.
     */
    fun choose(
        snapshot: LedgerSnapshot,
        today: LocalDate,
        preferences: HabitNotificationPreferences,
        lastShownEpochDay: Long?,
    ): HabitNotificationKind? {
        if (!preferences.enabled || !cooldownElapsed(today, lastShownEpochDay)) return null

        val candidates = buildList {
            if (preferences.debtEnabled && hasDebtReviewDue(snapshot, today)) {
                add(Candidate(HabitNotificationKind.DEBT_REVIEW, priority = 100))
            }
            if (preferences.incomeEnabled && hasIncomeReviewDue(snapshot, today)) {
                add(Candidate(HabitNotificationKind.INCOME_REVIEW, priority = 80))
            }
            if (preferences.investmentEnabled && hasInvestmentReviewDue(snapshot, today)) {
                add(Candidate(HabitNotificationKind.INVESTMENT_REVIEW, priority = 70))
            }
            if (preferences.goalEnabled && hasGoalReviewDue(snapshot, today)) {
                add(Candidate(HabitNotificationKind.GOAL_REVIEW, priority = 60))
            }
            if (preferences.spendingEnabled && hasSpendingReviewDue(snapshot, today)) {
                add(Candidate(HabitNotificationKind.SPENDING_REVIEW, priority = 40))
            }
        }

        return candidates
            .asSequence()
            .sortedByDescending { it.priority }
            .map { it.kind }
            .firstOrNull()
    }

    private fun hasSpendingReviewDue(snapshot: LedgerSnapshot, today: LocalDate): Boolean {
        // A weekly review is anchored to Monday, but accepts the prior seven inclusive dates so
        // it can summarize the week that just ended without looking at any personal values here.
        if (today.dayOfWeek != DayOfWeek.MONDAY) return false
        val start = today.minusDays(SPENDING_WINDOW_DAYS - 1L).toEpochDay()
        val end = today.toEpochDay()
        return snapshot.transactions.count { transaction ->
            transaction.deletedAt == null &&
                transaction.type == TransactionType.EXPENSE &&
                transaction.amountMinor > 0L &&
                transaction.localDateEpochDay in start..end
        } >= MIN_SPENDING_ENTRIES
    }

    private fun hasIncomeReviewDue(snapshot: LedgerSnapshot, today: LocalDate): Boolean {
        val start = today.minusDays(INCOME_LOOK_BEHIND_DAYS).toEpochDay()
        val end = today.plusDays(INCOME_LOOK_AHEAD_DAYS).toEpochDay()
        return snapshot.recurring.any { rule ->
            rule.deletedAt == null && !rule.archived && rule.type == RecurringType.INCOME &&
                rule.amountMinor > 0L && rule.nextDueEpochDay in start..end
        }
    }

    private fun hasDebtReviewDue(snapshot: LedgerSnapshot, today: LocalDate): Boolean {
        val latestUsefulDate = today.plusDays(DEBT_LOOK_AHEAD_DAYS)
        return snapshot.debts.any { debt ->
            debt.deletedAt == null && !debt.archived && debt.currentBalanceMinor > 0L &&
                debt.periodicPaymentMinor > 0L && debt.nextDueEpochDay != null &&
                !LocalDate.ofEpochDay(debt.nextDueEpochDay).isAfter(latestUsefulDate)
        }
    }

    private fun hasInvestmentReviewDue(snapshot: LedgerSnapshot, today: LocalDate): Boolean {
        val oldestAcceptableDate = today.minusDays(INVESTMENT_REVIEW_AFTER_DAYS)
        return snapshot.activeInvestments.any { holding ->
            !LocalDate.ofEpochDay(holding.valuationDateEpochDay).isAfter(oldestAcceptableDate)
        }
    }

    private fun hasGoalReviewDue(snapshot: LedgerSnapshot, today: LocalDate): Boolean {
        val latestUsefulDate = today.plusDays(GOAL_LOOK_AHEAD_DAYS)
        return snapshot.goals.any { goal ->
            goal.deletedAt == null && !goal.archived && goal.targetMinor > 0L &&
                goal.allocatedMinor < goal.targetMinor && goal.targetEpochDay != null &&
                !LocalDate.ofEpochDay(goal.targetEpochDay).isAfter(latestUsefulDate)
        }
    }

    private fun cooldownElapsed(today: LocalDate, lastShownEpochDay: Long?): Boolean {
        if (lastShownEpochDay == null) return true
        val days = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(lastShownEpochDay), today)
        // A single generic date is intentionally retained instead of a financial category or
        // value. One local check-in per week is enough to be useful without creating a profile of
        // which financial areas are active in DataStore.
        return days >= 6L
    }

    private data class Candidate(
        val kind: HabitNotificationKind,
        val priority: Int,
    )
}
