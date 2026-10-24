package com.mizani.money.domain.analytics

import com.mizani.money.core.model.Insight
import com.mizani.money.core.model.InsightKind
import com.mizani.money.core.model.InsightTone
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.data.repo.LedgerSnapshot
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Deterministic, explainable rules. No generative AI, no opaque scoring. Every insight
 * states the values it was derived from and can be expanded to show the calculation.
 */
class InsightEngine(
    private val data: LedgerSnapshot,
    private val engine: AnalyticsEngine,
    private val currency: Currency,
) {

    private fun money(minor: Long): String = MoneyFormat.format(minor, currency)

    fun generate(metrics: DashboardMetrics, today: LocalDate = LocalDate.now()): List<Insight> {
        val insights = mutableListOf<Insight>()
        val period = metrics.period

        if (metrics.transactionCount == 0 && data.transactions.isEmpty()) {
            insights += Insight(
                id = "start",
                kind = InsightKind.DATA_COMPLETENESS,
                tone = InsightTone.NEUTRAL,
                title = "Start with one real transaction",
                body = "Record money you actually received or spent. Insights appear once there is a little history to compare.",
                explanation = "Mizani never estimates your income or spending without records. Add a transaction to begin.",
                actionLabel = "Add transaction",
            )
            return insights
        }

        val budget = metrics.budget
        if (budget != null && budget.elapsedFraction >= 0.2) {
            val days = period.days
            val day = (ChronoUnit.DAYS.between(period.start, today) + 1).toInt().coerceIn(1, days)
            budget.lines
                .filter { it.line.plannedMinor > 0 }
                .sortedByDescending { it.paceVarianceMinor }
                .take(2)
                .forEach { line ->
                    val threshold = maxOf(1_000L, (line.expectedToDateMinor * 0.1).toLong())
                    if (line.paceVarianceMinor > threshold) {
                        val name = line.category?.name ?: "A category"
                        insights += Insight(
                            id = "pace-${line.line.categoryId}",
                            kind = InsightKind.BUDGET_PACE,
                            tone = InsightTone.ATTENTION,
                            title = "$name is ahead of your planned pace",
                            body = "Spent ${money(line.spentMinor)} against a ${money(line.availableMinor)} budget — ${money(line.paceVarianceMinor)} more than the ${money(line.expectedToDateMinor)} planned pace at day $day of $days.",
                            explanation = "Compares your spending so far with the amount your plan expected you to use by this point in the period.",
                            actionLabel = "Review budget",
                        )
                    }
                }
        }

        if (budget != null) {
            val unfunded = budget.lines
                .filter { it.line.essential && it.line.fundedMinor < it.line.plannedMinor }
                .maxByOrNull { it.line.plannedMinor - it.line.fundedMinor }
            if (unfunded != null) {
                val gap = unfunded.line.plannedMinor - unfunded.line.fundedMinor
                val name = unfunded.category?.name ?: "An essential category"
                insights += Insight(
                    id = "unfunded-${unfunded.line.categoryId}",
                    kind = InsightKind.UNFUNDED_PRIORITY,
                    tone = InsightTone.NEUTRAL,
                    title = "$name is not fully funded",
                    body = "$name still needs ${money(gap)} to be fully funded for this period.",
                    explanation = "Shows what remains to set aside for this essential category. Funding is separate from money already spent.",
                    actionLabel = "Update funding",
                )
            }
        }

        metrics.upcoming
            .filter { !it.isIncome }
            .filter { ChronoUnit.DAYS.between(today, it.dueDate) in 0..7 }
            .minByOrNull { it.dueDate }
            ?.let { upcoming ->
                val days = ChronoUnit.DAYS.between(today, upcoming.dueDate).toInt()
                val whenText = when (days) {
                    0 -> "today"
                    1 -> "tomorrow"
                    else -> "in $days days"
                }
                val shortfall = upcoming.amountMinor > metrics.walletLiquidMinor
                insights += Insight(
                    id = "upcoming-${upcoming.id}",
                    kind = InsightKind.UPCOMING_COMMITMENT,
                    tone = if (shortfall) InsightTone.ATTENTION else InsightTone.NEUTRAL,
                    title = "${upcoming.name} is due $whenText",
                    body = if (shortfall) {
                        "${money(upcoming.amountMinor)} is due ${DateText.short(upcoming.dueDate)}. Your tracked liquid balance is ${money(metrics.walletLiquidMinor)}."
                    } else {
                        "${money(upcoming.amountMinor)} is due ${DateText.short(upcoming.dueDate)}. You can mark it paid when it happens."
                    },
                    explanation = "Based on the due date and amount you entered. Your tracked liquid balance includes active non-debt wallets.",
                )
            }

        val currentMonth = Periods.monthOf(today)
        val previousMonth = Periods.monthOf(today.minusMonths(1))
        val feeNow = engine.feesMinor(currentMonth)
        val feeBefore = engine.feesMinor(previousMonth)
        if (feeNow > 0 && feeBefore > 0 && feeNow > feeBefore * 1.15) {
            val increase = feeNow - feeBefore
            insights += Insight(
                id = "fees",
                kind = InsightKind.FEE_TREND,
                tone = InsightTone.ATTENTION,
                title = "Fees are higher than last month",
                body = "You paid ${money(feeNow)} in transaction and withdrawal fees this month, ${money(increase)} more than last month.",
                explanation = "Compares the fees you recorded this month with last month. Fees are tracked separately from transaction amounts.",
                actionLabel = "See fee trend",
            )
        }

        val incomeBySource = data.transactions
            .filter {
                it.type == TransactionType.INCOME && it.deletedAt == null &&
                    it.localDateEpochDay in currentMonth.start.toEpochDay()..currentMonth.end.toEpochDay()
            }
            .groupBy { it.incomeSourceId ?: it.categoryId ?: AnalyticsEngine.UNCATEGORISED }
            .mapValues { entry -> entry.value.sumOf { it.amountMinor } }
        val incomeTotal = incomeBySource.values.sum()
        if (incomeBySource.size >= 2 && incomeTotal > 0) {
            val top = incomeBySource.maxByOrNull { it.value }!!
            val share = top.value.toDouble() / incomeTotal
            if (share >= 0.7) {
                val label = data.incomeSourceName(top.key.takeIf { id ->
                    data.incomeSources.any { it.id == id }
                }) .let {
                    if (it == "Income") data.categoryName(top.key) else it
                }
                insights += Insight(
                    id = "income-concentration",
                    kind = InsightKind.INCOME_CONCENTRATION,
                    tone = InsightTone.NEUTRAL,
                    title = "Most of this month's income came from one source",
                    body = "${"%.0f".format(share * 100)}% of your recorded income came from $label.",
                    explanation = "Compares this month's recorded income by source. This describes past activity and does not predict future income.",
                )
            }
        }

        val trend = spendingTrend(today)
        if (trend != null) {
            insights += trend
        }

        if (metrics.savingsContributionsMinor > 0) {
            insights += Insight(
                id = "savings",
                kind = InsightKind.SAVINGS_PROGRESS,
                tone = InsightTone.POSITIVE,
                title = "You set money aside this period",
                body = "You put ${money(metrics.savingsContributionsMinor)} toward your goals.",
                explanation = "Savings contributions are tracked separately from ordinary spending. " +
                    "Transfers between your own wallets are not counted as expenditure.",
            )
        } else if (metrics.savingsRatePercent != null && metrics.incomeMinor > 0) {
            insights += Insight(
                id = "savings-rate",
                kind = InsightKind.SAVINGS_PROGRESS,
                tone = InsightTone.NEUTRAL,
                title = "No goal contributions yet this period",
                body = "Your savings rate is ${metrics.savingsRatePercent}% of income received.",
                explanation = "Compares goal contributions with income received this period. Only amounts you recorded are included.",
            )
        }

        if (metrics.essentialCoveragePercent != null && metrics.essentialCoveragePercent < 100) {
            insights += Insight(
                id = "essential-coverage",
                kind = InsightKind.ESSENTIAL_COVERAGE,
                tone = InsightTone.NEUTRAL,
                title = "Income received so far does not cover all essentials",
                body = "Income received covers ${metrics.essentialCoveragePercent}% of the essential expenses you planned for this period.",
                explanation = "Compares income received with essential amounts in your current plan. Expected income is not included.",
            )
        }

        val lastTransaction = data.transactions
            .filter { it.deletedAt == null }
            .maxByOrNull { it.localDateEpochDay }
        if (lastTransaction != null && metrics.period.contains(today)) {
            val days = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(lastTransaction.localDateEpochDay), today).toInt()
            if (days >= 7) {
                insights += Insight(
                    id = "quiet-week",
                    kind = InsightKind.DATA_COMPLETENESS,
                    tone = InsightTone.NEUTRAL,
                    title = "No records for $days days",
                    body = "Your analytics only reflect what you record. Add recent cash or mobile-money activity to keep the picture current.",
                    explanation = "Based on the date of your most recent recorded transaction. Mizani does not estimate missing activity.",
                    actionLabel = "Add transaction",
                )
            }
        }

        return insights
            .distinctBy { it.kind to it.id }
            .sortedWith(compareBy({ if (it.tone == InsightTone.ATTENTION) 0 else 1 }, { it.kind.ordinal }))
            .take(6)
    }

    private fun spendingTrend(today: LocalDate): Insight? {
        val current = Periods.monthOf(today)
        if (engine.transactionsIn(current).count { it.type == TransactionType.EXPENSE } < 3) return null
        val history = (1L..3L).map { back ->
            val month = YearMonth.from(today).minusMonths(back)
            val range = DateRange(month.atDay(1), month.atEndOfMonth())
            engine.categorySpendTotals(range)
        }
        if (history.any { it.isEmpty() }) return null
        val categories = engine.categorySpendTotals(current)
            .filterKeys { it != AnalyticsEngine.UNCATEGORISED }
            .mapValues { it.value }
        var best: Triple<String, Long, Long>? = null
        for ((categoryId, amount) in categories) {
            if (amount <= 0) continue
            val prior = history.mapNotNull { it[categoryId] }.filter { it > 0 }.sorted()
            if (prior.size < 3) continue
            val median = prior[1]
            if (median > 0 && amount > median * 1.25) {
                val currentBest = best
                if (currentBest == null || amount - median > currentBest.second - currentBest.third) {
                    best = Triple(categoryId, amount, median)
                }
            }
        }
        val found = best ?: return null
        val name = data.category(found.first)?.name ?: "A category"
        val delta = found.second - found.third
        return Insight(
            id = "trend-${found.first}",
            kind = InsightKind.SPENDING_TREND,
            tone = InsightTone.NEUTRAL,
            title = "$name is above your usual monthly level",
            body = "You have spent ${money(found.second)} on $name this month, about ${money(delta)} more than your recent median of ${money(found.third)}.",
            explanation = "Compares this month's spending with recent months that had recorded spending in this category.",
        )
    }
}
