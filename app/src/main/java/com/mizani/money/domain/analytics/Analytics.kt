package com.mizani.money.domain.analytics

import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.Periods
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.repo.LedgerSnapshot
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Pure, deterministic calculation layer. No Android or UI dependencies: the same engine
 * powers Home, Insights and Reports and is directly unit-testable.
 *
 * Rules encoded here:
 *  - transfers never count as income or expense; their fee does;
 *  - a refund reduces net spending in the original category;
 *  - adjustments never count as income or expense;
 *  - expected income is never treated as received income;
 *  - analytics only include posted (non-tombstoned) transactions in the selected period.
 */
class AnalyticsEngine(private val data: LedgerSnapshot) {

    fun transactionsIn(range: DateRange): List<TransactionEntity> =
        data.transactions.filter { transaction ->
            transaction.deletedAt == null &&
                transaction.localDateEpochDay >= range.start.toEpochDay() &&
                transaction.localDateEpochDay <= range.end.toEpochDay()
        }

    fun incomeMinor(range: DateRange): Long = transactionsIn(range)
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amountMinor }

    fun feesMinor(range: DateRange): Long = transactionsIn(range)
        .sumOf { it.feeMinor }

    fun refundsMinor(range: DateRange): Long = transactionsIn(range)
        .filter { it.type == TransactionType.REFUND }
        .sumOf { it.amountMinor }

    /**
     * Total eligible spending, including fees and net of refunds. Transfers are excluded
     * except for their fee. This equals the sum of category slices by construction.
     */
    fun expenseMinor(range: DateRange): Long = categorySpendTotals(range).values.sum()

    fun netMinor(range: DateRange): Long = incomeMinor(range) - expenseMinor(range)

    fun categorySpendTotals(range: DateRange): Map<String, Long> {
        val totals = mutableMapOf<String, Long>()
        for (transaction in transactionsIn(range)) {
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    add(totals, transaction.categoryId ?: UNCATEGORISED, transaction.amountMinor)
                    if (transaction.feeMinor > 0) {
                        add(totals, transaction.feeCategoryId ?: DefaultCategories.feesId(), transaction.feeMinor)
                    }
                }
                TransactionType.TRANSFER -> {
                    if (transaction.feeMinor > 0) {
                        add(totals, transaction.feeCategoryId ?: DefaultCategories.feesId(), transaction.feeMinor)
                    }
                }
                TransactionType.REFUND -> {
                    add(totals, transaction.categoryId ?: UNCATEGORISED, -transaction.amountMinor)
                    if (transaction.feeMinor > 0) {
                        add(
                            totals,
                            transaction.feeCategoryId ?: DefaultCategories.feesId(),
                            transaction.feeMinor,
                        )
                    }
                }
                TransactionType.INCOME, TransactionType.ADJUSTMENT -> Unit
            }
        }
        return totals
    }

    fun categorySlices(range: DateRange): List<CategorySlice> {
        val totals = categorySpendTotals(range)
        val transactionCounts = mutableMapOf<String, Int>()
        for (transaction in transactionsIn(range)) {
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    val key = transaction.categoryId ?: UNCATEGORISED
                    transactionCounts[key] = (transactionCounts[key] ?: 0) + 1
                    if (transaction.feeMinor > 0) {
                        val feeKey = transaction.feeCategoryId ?: DefaultCategories.feesId()
                        transactionCounts[feeKey] = (transactionCounts[feeKey] ?: 0) + 1
                    }
                }
                TransactionType.TRANSFER -> {
                    if (transaction.feeMinor > 0) {
                        val feeKey = transaction.feeCategoryId ?: DefaultCategories.feesId()
                        transactionCounts[feeKey] = (transactionCounts[feeKey] ?: 0) + 1
                    }
                }
                TransactionType.REFUND -> {
                    val key = transaction.categoryId ?: UNCATEGORISED
                    transactionCounts[key] = (transactionCounts[key] ?: 0) + 1
                    if (transaction.feeMinor > 0) {
                        val feeKey = transaction.feeCategoryId ?: DefaultCategories.feesId()
                        transactionCounts[feeKey] = (transactionCounts[feeKey] ?: 0) + 1
                    }
                }
                else -> Unit
            }
        }
        val positiveTotal = totals.values.filter { it > 0 }.sum()
        return totals
            .filter { it.value != 0L }
            .map { (categoryId, amount) ->
                val category = data.category(categoryId)
                CategorySlice(
                    categoryId = categoryId,
                    name = category?.name ?: if (categoryId == DefaultCategories.feesId()) "Fees" else "Uncategorised",
                    colorHex = category?.colorHex ?: "#6D6D6D",
                    amountMinor = amount,
                    share = if (positiveTotal <= 0) 0.0 else amount.toDouble() / positiveTotal,
                    transactionCount = transactionCounts[categoryId] ?: 0,
                )
            }
            .sortedByDescending { it.amountMinor }
    }

    fun categorySpend(categoryId: String, range: DateRange): Long =
        categorySpendTotals(range)[categoryId] ?: 0L

    fun budgetFor(range: DateRange): BudgetEntity? = data.budgets
        .filter { it.deletedAt == null && it.startEpochDay <= range.end.toEpochDay() && it.endEpochDay >= range.start.toEpochDay() }
        .maxByOrNull { budget ->
            val start = maxOf(budget.startEpochDay, range.start.toEpochDay())
            val end = minOf(budget.endEpochDay, range.end.toEpochDay())
            end - start
        }

    fun budgetProgress(range: DateRange, budget: BudgetEntity? = budgetFor(range), today: LocalDate = LocalDate.now()): BudgetProgress? {
        val selected = budget ?: return null
        val period = DateRange(LocalDate.ofEpochDay(selected.startEpochDay), LocalDate.ofEpochDay(selected.endEpochDay))
        val lines = data.budgetLinesFor(selected.id)
        if (lines.isEmpty()) {
            return BudgetProgress(
                budget = selected,
                lines = emptyList(),
                plannedMinor = 0,
                fundedMinor = 0,
                spentMinor = 0,
                availableMinor = 0,
                remainingMinor = 0,
                unbudgetedSpendMinor = expenseMinor(period),
                usedPercent = 0.0,
                elapsedFraction = period.elapsedFraction(today),
                reservedCommitmentsMinor = 0,
                safeToSpendDailyMinor = null,
                flexibleRemainingMinor = 0,
            )
        }

        val previous = previousBudget(selected)
        val elapsed = period.elapsedFraction(today)
        val progressLines = lines.map { line ->
            val spent = categorySpend(line.categoryId, period)
            val rollover = rolloverFor(line, previous)
            val available = line.plannedMinor + rollover
            val remaining = available - spent
            val expected = (available * elapsed).toLong()
            val used = if (available <= 0) {
                if (spent > 0) 100.0 else 0.0
            } else {
                (spent.toDouble() / available * 100.0).coerceAtLeast(0.0)
            }
            BudgetLineProgress(
                line = line,
                category = data.category(line.categoryId),
                spentMinor = spent,
                rolloverMinor = rollover,
                availableMinor = available,
                remainingMinor = remaining,
                usedPercent = used,
                expectedToDateMinor = expected,
                paceVarianceMinor = spent - expected,
            )
        }

        val planned = lines.sumOf { it.plannedMinor }
        val funded = lines.sumOf { it.fundedMinor }
        val spent = progressLines.sumOf { it.spentMinor }
        val available = progressLines.sumOf { it.availableMinor }
        val remaining = available - spent
        val unbudgeted = expenseMinor(period) - spent
        // Safe-to-spend only ever uses funded money: planned-but-unfunded lines are not spendable.
        val fundedFlexible = lines
            .filter { !it.essential }
            .sumOf { it.fundedMinor }
        val flexibleRemaining = progressLines
            .filter { !it.line.essential }
            .sumOf { (it.line.fundedMinor + it.rolloverMinor - it.spentMinor).coerceAtLeast(0) }

        val reserved = reservedCommitments(period, today)
        val daysLeft = period.daysRemaining(today)
        val safeDaily = if (today in period.start..period.end && fundedFlexible > 0) {
            val pool = (flexibleRemaining - reserved).coerceAtLeast(0)
            pool / daysLeft.coerceAtLeast(1)
        } else {
            null
        }

        return BudgetProgress(
            budget = selected,
            lines = progressLines,
            plannedMinor = planned,
            fundedMinor = funded,
            spentMinor = spent,
            availableMinor = available,
            remainingMinor = remaining,
            unbudgetedSpendMinor = unbudgeted,
            usedPercent = if (available <= 0) 0.0 else (spent.toDouble() / available * 100.0),
            elapsedFraction = elapsed,
            reservedCommitmentsMinor = reserved,
            safeToSpendDailyMinor = safeDaily,
            flexibleRemainingMinor = flexibleRemaining,
        )
    }

    private fun previousBudget(budget: BudgetEntity): BudgetEntity? = data.budgets
        .filter {
            it.deletedAt == null &&
                it.startEpochDay < budget.startEpochDay &&
                it.periodType == budget.periodType
        }
        .maxByOrNull { it.startEpochDay }

    private fun rolloverFor(line: BudgetLineEntity, previous: BudgetEntity?): Long {
        if (line.rolloverRule == com.mizani.money.core.model.RolloverRule.NONE || previous == null) return 0L
        val previousLine = data.budgetLinesFor(previous.id).firstOrNull { it.categoryId == line.categoryId } ?: return 0L
        val previousPeriod = DateRange(
            LocalDate.ofEpochDay(previous.startEpochDay),
            LocalDate.ofEpochDay(previous.endEpochDay),
        )
        val previousSpent = categorySpend(line.categoryId, previousPeriod)
        val variance = previousLine.plannedMinor - previousSpent
        return when (line.rolloverRule) {
            com.mizani.money.core.model.RolloverRule.NONE -> 0L
            com.mizani.money.core.model.RolloverRule.POSITIVE -> variance.coerceAtLeast(0)
            com.mizani.money.core.model.RolloverRule.BOTH -> variance
        }
    }

    fun reservedCommitments(range: DateRange, today: LocalDate = LocalDate.now()): Long {
        if (today.isAfter(range.end)) return 0L
        val bills = data.recurring
            .filter {
                it.deletedAt == null && !it.archived && it.type == RecurringType.BILL &&
                    LocalDate.ofEpochDay(it.nextDueEpochDay) <= range.end
            }
            .fold(0L) { total, rule ->
                Math.addExact(total, Math.multiplyExact(rule.amountMinor, occurrencesInRange(rule, range).toLong()))
            }
        val debtPayments = data.debts
            .filter { it.deletedAt == null && !it.archived && it.nextDueEpochDay != null }
            .filter {
                val due = it.nextDueEpochDay?.let(LocalDate::ofEpochDay)
                due != null && range.contains(due)
            }
            .fold(0L) { total, debt -> Math.addExact(total, debt.periodicPaymentMinor) }
        val finiteCommitments = data.commitmentProgress()
            .asSequence()
            .filter { progress ->
                !progress.commitment.archived && progress.remainingWorkspaceMinor > 0L &&
                    range.contains(LocalDate.ofEpochDay(progress.commitment.dueEpochDay))
            }
            .fold(0L) { total, progress -> Math.addExact(total, progress.remainingWorkspaceMinor) }
        return Math.addExact(Math.addExact(bills, debtPayments), finiteCommitments)
    }

    /**
     * Estimates coverage using current liquid wallet balances less outflows due in the next
     * 30 days. Future income is deliberately excluded. History needs seven observed days and
     * recorded spending; otherwise a saved user target is used.
     */
    fun moneyOutlook(today: LocalDate = LocalDate.now()): MoneyOutlook {
        val liquid = data.liquidBalance()
        // DateRange is inclusive, so today plus 29 covers 30 calendar dates including today.
        val horizon = DateRange(today, today.plusDays(29))
        val reserved = reservedCommitments(horizon, today)
        val spendable = (liquid - reserved).coerceAtLeast(0L)
        val windowStart = today.minusDays(HISTORY_WINDOW_DAYS - 1L)
        val window = DateRange(windowStart, today)
        val recordedTransactions = transactionsIn(window).filter { it.type != TransactionType.ADJUSTMENT }
        val workspaceStart = data.workspace?.createdAt?.let { timestamp ->
            java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }
        val firstObserved = listOfNotNull(
            windowStart,
            workspaceStart,
            recordedTransactions.minOfOrNull { LocalDate.ofEpochDay(it.localDateEpochDay) },
        ).maxOrNull() ?: windowStart
        val observedDays = if (recordedTransactions.isEmpty()) 0 else {
            (ChronoUnit.DAYS.between(firstObserved, today) + 1L).toInt().coerceIn(0, HISTORY_WINDOW_DAYS)
        }
        val expenses = expenseMinor(window).coerceAtLeast(0L)
        val average = if (observedDays >= MIN_HISTORY_DAYS && expenses > 0L) {
            // Round up so fractional minor units never overstate estimated coverage.
            (expenses + observedDays - 1L) / observedDays
        } else null
        val target = data.workspace?.outlookDailyTargetMinor?.takeIf { it > 0L }
        val dailyCost = average ?: target
        return MoneyOutlook(
            liquidBalanceMinor = liquid,
            reservedCommitmentsMinor = reserved,
            spendableBalanceMinor = spendable,
            shortfallMinor = (reserved - liquid).coerceAtLeast(0L),
            recordedExpensesMinor = expenses,
            observedDays = observedDays,
            averageDailySpendMinor = average,
            dailyTargetMinor = target,
            daysCovered = dailyCost?.let { spendable / it },
            usesDailyTarget = average == null && target != null,
        )
    }

    fun upcoming(range: DateRange, today: LocalDate = LocalDate.now()): List<UpcomingCommitment> {
        val end = minOf(range.end, today.plusDays(45))
        val recurring = data.recurring
            .filter { it.deletedAt == null && !it.archived && it.nextDueEpochDay <= end.toEpochDay() }
            .flatMap { rule ->
                val category = data.category(rule.categoryId)
                val dates = occurrenceDatesNearToday(rule, today, end)
                dates.map { dueDate ->
                    UpcomingCommitment(
                        id = "${rule.id}:${dueDate.toEpochDay()}",
                        name = rule.name,
                        amountMinor = rule.amountMinor,
                        dueDate = dueDate,
                        isIncome = rule.type == RecurringType.INCOME,
                        essential = rule.essential,
                        categoryName = category?.name ?: if (rule.type == RecurringType.INCOME) "Income" else "Commitment",
                        colorHex = category?.colorHex ?: "#8A6D3B",
                        walletName = data.walletName(rule.walletId),
                    )
                }
            }
        val debts = data.debts
            .filter { it.deletedAt == null && !it.archived && it.nextDueEpochDay != null }
            .filter {
                it.nextDueEpochDay?.let { day -> day in today.toEpochDay()..end.toEpochDay() } == true
            }
            .map { debt ->
                UpcomingCommitment(
                    id = debt.id,
                    name = debt.name,
                    amountMinor = debt.periodicPaymentMinor,
                    dueDate = LocalDate.ofEpochDay(debt.nextDueEpochDay ?: today.toEpochDay()),
                    isIncome = false,
                    essential = true,
                    categoryName = "Debt payment",
                    colorHex = "#8C4A3C",
                    walletName = data.walletName(null),
                )
            }
        return (recurring + debts)
            .filter { it.amountMinor > 0 }
            .sortedBy { it.dueDate }
    }

    fun walletBalances(today: LocalDate = LocalDate.now()): List<WalletBalance> {
        val balances = data.walletBalances()
        return data.wallets
            .filter { it.deletedAt == null && !it.archived }
            .sortedBy { it.sortOrder }
            .map { WalletBalance(it, balances[it.id] ?: 0L) }
    }

    fun expectedIncome(range: DateRange): Long = data.recurring
        .filter { it.deletedAt == null && !it.archived && it.type == RecurringType.INCOME }
        .sumOf { rule -> occurrencesInRange(rule, range) * rule.amountMinor }

    fun occurrencesInRange(rule: RecurringRuleEntity, range: DateRange): Int {
        var due = firstOccurrenceOnOrAfter(rule, range.start)
        var count = 0
        while (!due.isAfter(range.end)) {
            count++
            due = Periods.nextOccurrence(due, rule.frequency)
        }
        return count
    }

    private fun firstOccurrenceOnOrAfter(rule: RecurringRuleEntity, start: LocalDate): LocalDate {
        var due = LocalDate.ofEpochDay(rule.nextDueEpochDay)
        while (due.isBefore(start)) due = Periods.nextOccurrence(due, rule.frequency)
        return due
    }

    private fun occurrenceDatesNearToday(
        rule: RecurringRuleEntity,
        today: LocalDate,
        end: LocalDate,
    ): List<LocalDate> {
        val nextDue = LocalDate.ofEpochDay(rule.nextDueEpochDay)
        val dates = mutableListOf<LocalDate>()
        // Keep one overdue occurrence visible so it can be recorded. Later missed cycles are
        // not posted automatically; the rule remains a forecast until the user records it.
        if (nextDue.isBefore(today)) dates += nextDue
        var due = firstOccurrenceOnOrAfter(rule, today)
        while (!due.isAfter(end)) {
            dates += due
            due = Periods.nextOccurrence(due, rule.frequency)
        }
        return dates
    }

    fun incomeVariability(today: LocalDate = LocalDate.now()): IncomeVariability? {
        val months = (0L..5L).map { back ->
            val month = YearMonth.from(today).minusMonths(back)
            val range = DateRange(month.atDay(1), month.atEndOfMonth())
            incomeMinor(range)
        }
        val nonZero = months.filter { it > 0 }.sorted()
        if (nonZero.isEmpty()) return null
        val lastIncome = data.transactions
            .filter { it.type == TransactionType.INCOME && it.deletedAt == null }
            .maxByOrNull { it.localDateEpochDay }
        val median = if (nonZero.size % 2 == 1) {
            nonZero[nonZero.size / 2]
        } else {
            (nonZero[nonZero.size / 2 - 1] + nonZero[nonZero.size / 2]) / 2
        }
        return IncomeVariability(
            medianMinor = median,
            lowestMinor = nonZero.first(),
            highestMinor = nonZero.last(),
            monthsWithIncome = nonZero.size,
            daysSinceLastIncome = lastIncome?.let {
                ChronoUnit.DAYS.between(LocalDate.ofEpochDay(it.localDateEpochDay), today).toInt()
            },
        )
    }

    /**
     * The comparison period used for the dashboard's prior income/expense values.
     * When the selected range is a full current calendar month and today falls inside it,
     * the comparison is month-to-date like-for-like: the previous month's start through
     * the same elapsed day count, clamped to that month's length.
     */
    fun previousComparable(range: DateRange, today: LocalDate = LocalDate.now()): DateRange {
        val month = YearMonth.from(range.start)
        val isFullMonth = range.start.dayOfMonth == 1 &&
            range.end.dayOfMonth == month.lengthOfMonth() &&
            range.end.month == month.month &&
            range.start.month == range.end.month
        return if (isFullMonth && range.contains(today)) {
            val elapsedDays = (ChronoUnit.DAYS.between(range.start, today) + 1).toInt()
            val previous = month.minusMonths(1)
            DateRange(previous.atDay(1), previous.atDay(minOf(elapsedDays, previous.lengthOfMonth())))
        } else if (isFullMonth) {
            val previous = month.minusMonths(1)
            DateRange(previous.atDay(1), previous.atEndOfMonth())
        } else {
            Periods.previous(range)
        }
    }

    /**
     * Projected period spend = spending to date + recent daily pace * days remaining.
     *
     * The pace only uses non-deleted expense, transfer-fee and refund transactions inside the
     * trailing 14-day window and divides by the days that window actually covers: the first
     * activity date through today, inclusive (at most 14, at least 7 to be meaningful). A
     * window with no such activity, or with less than seven observed days, forecasts nothing.
     */
    fun forecast(range: DateRange, today: LocalDate = LocalDate.now()): Long? {
        if (today.isBefore(range.start) || today.isAfter(range.end)) return null
        val daysRemaining = range.daysRemaining(today)
        if (daysRemaining <= 0) return null
        val todayDay = today.toEpochDay()
        val windowStartDay = today.minusDays(13).toEpochDay()
        val eligible = data.transactions.filter { transaction ->
            transaction.deletedAt == null &&
                transaction.localDateEpochDay in windowStartDay..todayDay &&
                when (transaction.type) {
                    TransactionType.EXPENSE -> transaction.amountMinor > 0 || transaction.feeMinor > 0
                    TransactionType.TRANSFER -> transaction.feeMinor > 0
                    TransactionType.REFUND -> transaction.amountMinor > 0
                    TransactionType.INCOME, TransactionType.ADJUSTMENT -> false
                }
        }
        if (eligible.isEmpty()) return null
        val observedDays = (todayDay - eligible.minOf { it.localDateEpochDay } + 1)
            .coerceIn(1L, 14L)
            .toInt()
        if (observedDays < MIN_FORECAST_HISTORY_DAYS) return null
        val netSpend = eligible.sumOf { transaction ->
            when (transaction.type) {
                TransactionType.EXPENSE -> transaction.amountMinor + transaction.feeMinor
                TransactionType.TRANSFER -> transaction.feeMinor
                TransactionType.REFUND -> -transaction.amountMinor + transaction.feeMinor
                TransactionType.INCOME, TransactionType.ADJUSTMENT -> 0L
            }
        }
        // A window that nets to zero or negative (refunds) never lowers the projection below
        // what has already been spent in the period.
        val averageDaily = (netSpend / observedDays).coerceAtLeast(0L)
        val spendToDate = expenseMinor(DateRange(range.start, today)).coerceAtLeast(0)
        return spendToDate + averageDaily * daysRemaining
    }

    fun dashboard(range: DateRange, today: LocalDate = LocalDate.now()): DashboardMetrics {
        val budget = budgetFor(range)
        val budgetProgress = budgetProgress(range, budget, today)
        val slices = categorySlices(range)
        val balances = walletBalances(today)
        val walletTotal = balances.sumOf { it.balanceMinor }
        val walletLiquid = balances
            .filter { it.wallet.type != WalletType.DEBT }
            .sumOf { it.balanceMinor }
        val byType = balances.groupBy { it.wallet.type }
            .map { (type, list) -> WalletTypeTotal(type, list.sumOf { it.balanceMinor }) }
            .sortedByDescending { it.totalMinor }
        val previous = previousComparable(range, today)
        val received = incomeMinor(range)
        val essentialPlanned = budgetProgress?.lines
            ?.filter { it.line.essential }
            ?.sumOf { it.line.plannedMinor } ?: 0L
        val essentialCoverage = if (essentialPlanned > 0) {
            ((received.toDouble() / essentialPlanned) * 100.0).toInt().coerceIn(0, 999)
        } else {
            null
        }
        val earliest = data.transactions
            .filter { it.deletedAt == null }
            .minOfOrNull { it.localDateEpochDay }
        val daysTracked = earliest?.let {
            (ChronoUnit.DAYS.between(LocalDate.ofEpochDay(it), today) + 1).toInt().coerceAtLeast(1)
        } ?: 0
        val monthRange = Periods.monthOf(today)
        val spendToDate = expenseMinor(DateRange(monthRange.start, minOf(today, monthRange.end)))
        return DashboardMetrics(
            period = range,
            incomeMinor = received,
            expenseMinor = expenseMinor(range),
            feesMinor = feesMinor(range),
            refundsMinor = refundsMinor(range),
            netMinor = received - expenseMinor(range),
            categorySlices = slices,
            budget = budgetProgress,
            upcoming = upcoming(range, today),
            walletBalances = balances,
            walletTotalMinor = walletTotal,
            walletLiquidMinor = walletLiquid,
            walletByType = byType,
            recentTransactions = data.transactions
                .filter { it.deletedAt == null }
                .sortedWith(compareByDescending<TransactionEntity> { it.localDateEpochDay }.thenByDescending { it.occurredAt })
                .take(5),
            safeToSpendDailyMinor = budgetProgress?.safeToSpendDailyMinor,
            budgetRemainingMinor = budgetProgress?.remainingMinor,
            budgetUsedPercent = budgetProgress?.usedPercent?.toInt(),
            forecastMinor = forecast(range, today),
            expectedIncomeMinor = expectedIncome(range),
            receivedIncomeMinor = received,
            previousIncomeMinor = incomeMinor(previous),
            previousExpenseMinor = expenseMinor(previous),
            previousPeriod = previous,
            essentialCoveragePercent = essentialCoverage,
            incomeVariability = incomeVariability(today),
            daysTracked = daysTracked,
            transactionCount = transactionsIn(range).size,
            unbudgetedSpendMinor = budgetProgress?.unbudgetedSpendMinor ?: expenseMinor(range),
            monthlySpendToDateMinor = spendToDate,
        )
    }

    private fun add(map: MutableMap<String, Long>, key: String, delta: Long) {
        map[key] = (map[key] ?: 0L) + delta
    }

    companion object {
        const val UNCATEGORISED = "uncategorised"

        private const val HISTORY_WINDOW_DAYS = 30
        private const val MIN_HISTORY_DAYS = 7

        /** Minimum number of days covered by recorded spending activity before a forecast is shown. */
        const val MIN_FORECAST_HISTORY_DAYS = 7
    }
}
