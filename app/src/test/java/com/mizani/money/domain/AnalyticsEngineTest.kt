package com.mizani.money.domain

import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.time.DateRange
import com.mizani.money.domain.analytics.AnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AnalyticsEngineTest {

    private val october = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun `transfer is excluded from income and expenses but its fee counts`() {
        val wallets = listOf(
            Fixtures.wallet("momo", opening = 100_000),
            Fixtures.wallet("cash", type = com.mizani.money.core.model.WalletType.CASH),
        )
        val transactions = listOf(
            Fixtures.transaction(
                id = "t1",
                type = TransactionType.TRANSFER,
                amount = 30_000,
                walletId = "momo",
                toWalletId = "cash",
                fee = 1_500,
                date = LocalDate.of(2026, 10, 5),
            ),
        )
        val snapshot = Fixtures.snapshot(wallets = wallets, transactions = transactions)
        val engine = AnalyticsEngine(snapshot)

        assertEquals(0L, engine.incomeMinor(october))
        assertEquals(1_500L, engine.expenseMinor(october))
        assertEquals(-1_500L, engine.netMinor(october))

        val balances = snapshot.walletBalances()
        assertEquals(68_500L, balances["momo"])
        assertEquals(30_000L, balances["cash"])
        assertEquals(98_500L, balances.values.sum())
    }

    @Test
    fun `refund reduces net spending in the original category`() {
        val transactions = listOf(
            Fixtures.transaction(
                id = "expense",
                type = TransactionType.EXPENSE,
                amount = 100_000,
                walletId = "cash",
                categoryId = "food",
                date = LocalDate.of(2026, 10, 3),
            ),
            Fixtures.transaction(
                id = "refund",
                type = TransactionType.REFUND,
                amount = 30_000,
                walletId = "cash",
                categoryId = "food",
                linkedTransactionId = "expense",
                date = LocalDate.of(2026, 10, 4),
            ),
        )
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", type = com.mizani.money.core.model.WalletType.CASH, opening = 100_000)),
            categories = listOf(Fixtures.category("food")),
            transactions = transactions,
        )
        val engine = AnalyticsEngine(snapshot)

        assertEquals(70_000L, engine.expenseMinor(october))
        assertEquals(70_000L, engine.categorySpend("food", october))
        assertEquals(70_000L, engine.categorySlices(october).single().amountMinor)
        assertEquals(30_000L, snapshot.walletBalances()["cash"])
    }

    @Test
    fun `refund with a fee counts the fee as spending and reduces the wallet by amount minus fee`() {
        val fees = DefaultCategories.feesId()
        val transactions = listOf(
            Fixtures.transaction(
                id = "expense",
                type = TransactionType.EXPENSE,
                amount = 100_000,
                walletId = "cash",
                categoryId = "food",
                date = LocalDate.of(2026, 10, 3),
            ),
            Fixtures.transaction(
                id = "refund",
                type = TransactionType.REFUND,
                amount = 30_000,
                fee = 500,
                feeCategoryId = fees,
                walletId = "cash",
                categoryId = "food",
                linkedTransactionId = "expense",
                date = LocalDate.of(2026, 10, 4),
            ),
        )
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", type = com.mizani.money.core.model.WalletType.CASH, opening = 100_000)),
            categories = listOf(
                Fixtures.category("food", "Food", com.mizani.money.core.model.CategoryType.EXPENSE),
                Fixtures.category(fees, "Fees", com.mizani.money.core.model.CategoryType.EXPENSE),
            ),
            transactions = transactions,
        )
        val engine = AnalyticsEngine(snapshot)

        // Wallet: opening 100k - expense 100k + (refund 30k - fee 500) = 29,500.
        assertEquals(29_500L, snapshot.walletBalances()["cash"])
        // Eligible spending: expense 100k - refund 30k + refund fee 500 = 70,500.
        assertEquals(70_500L, engine.expenseMinor(october))
        assertEquals(70_000L, engine.categorySpend("food", october))
        assertEquals(500L, engine.categorySpend(fees, october))
    }

    @Test
    fun `adjustment changes the wallet but not income or spending`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 0)),
            transactions = listOf(
                Fixtures.transaction(
                    id = "adjust",
                    type = TransactionType.ADJUSTMENT,
                    amount = 50_000,
                    walletId = "cash",
                    date = LocalDate.of(2026, 10, 2),
                ),
                Fixtures.transaction(
                    id = "expense",
                    type = TransactionType.EXPENSE,
                    amount = 10_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 10, 2),
                ),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        assertEquals(0L, engine.incomeMinor(october))
        assertEquals(10_000L, engine.expenseMinor(october))
        assertEquals(40_000L, snapshot.walletBalances()["cash"])
    }

    @Test
    fun `deleted transactions are excluded from analytics`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            transactions = listOf(
                Fixtures.transaction(
                    id = "deleted",
                    type = TransactionType.EXPENSE,
                    amount = 99_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 10, 2),
                    deleted = true,
                ),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        assertEquals(0L, engine.expenseMinor(october))
        assertEquals(0L, snapshot.walletBalances()["cash"])
    }

    @Test
    fun `expected income never becomes received income`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            recurring = listOf(
                Fixtures.recurring(
                    id = "salary",
                    type = RecurringType.INCOME,
                    amount = 500_000,
                    nextDue = LocalDate.of(2026, 10, 10),
                ),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        assertEquals(500_000L, engine.expectedIncome(october))
        assertEquals(0L, engine.incomeMinor(october))
        val dashboard = engine.dashboard(october, today)
        assertEquals(500_000L, dashboard.expectedIncomeMinor)
        assertEquals(0L, dashboard.receivedIncomeMinor)
        assertEquals(0L, dashboard.incomeMinor)
    }

    @Test
    fun `budget pace and safe to spend use elapsed days and commitments`() {
        val budget = Fixtures.budget("b1", october.start, october.end)
        val lines = listOf(
            Fixtures.line("l1", "b1", "food", planned = 300_000, funded = 300_000, essential = false),
            Fixtures.line("l2", "b1", "rent", planned = 450_000, funded = 450_000, essential = true),
        )
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 1_000_000)),
            categories = listOf(Fixtures.category("food", "Food"), Fixtures.category("rent", "Rent")),
            transactions = listOf(
                Fixtures.transaction(
                    id = "food-spend",
                    type = TransactionType.EXPENSE,
                    amount = 150_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 10, 5),
                ),
            ),
            budgets = listOf(budget),
            lines = lines,
            recurring = listOf(
                Fixtures.recurring(
                    id = "water",
                    type = RecurringType.BILL,
                    amount = 50_000,
                    nextDue = LocalDate.of(2026, 10, 20),
                    essential = false,
                    categoryId = "food",
                ),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        val progress = requireNotNull(engine.budgetProgress(october, budget, today)) {
            "Expected budget progress for a budget with lines"
        }

        assertEquals(150_000L, progress.spentMinor)
        assertEquals(750_000L, progress.availableMinor)
        assertEquals(600_000L, progress.remainingMinor)
        assertEquals(150_000L, progress.flexibleRemainingMinor)
        assertEquals(50_000L, progress.reservedCommitmentsMinor)
        assertEquals(5_882L, progress.safeToSpendDailyMinor)
        assertEquals(20.0, progress.usedPercent, 0.001)

        val food = progress.lines.first { it.line.categoryId == "food" }
        assertEquals(145_161L, food.expectedToDateMinor)
        assertEquals(4_839L, food.paceVarianceMinor)
    }

    @Test
    fun `refunds reduce a budget line`() {
        val budget = Fixtures.budget("b1", october.start, october.end)
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            categories = listOf(Fixtures.category("food")),
            transactions = listOf(
                Fixtures.transaction(
                    id = "e",
                    type = TransactionType.EXPENSE,
                    amount = 100_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 10, 3),
                ),
                Fixtures.transaction(
                    id = "r",
                    type = TransactionType.REFUND,
                    amount = 25_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 10, 4),
                ),
            ),
            budgets = listOf(budget),
            lines = listOf(Fixtures.line("l1", "b1", "food", planned = 100_000)),
        )
        val engine = AnalyticsEngine(snapshot)
        val progress = engine.budgetProgress(october, budget, today)!!
        assertEquals(75_000L, progress.spentMinor)
        assertEquals(25_000L, progress.remainingMinor)
    }

    @Test
    fun `safe to spend uses funded money not planned money`() {
        val budget = Fixtures.budget("b1", october.start, october.end)
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 500_000)),
            categories = listOf(Fixtures.category("food")),
            transactions = listOf(
                Fixtures.transaction(
                    id = "food-spend",
                    type = TransactionType.EXPENSE,
                    amount = 50_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 10, 5),
                ),
            ),
            budgets = listOf(budget),
            lines = listOf(
                Fixtures.line("l1", "b1", "food", planned = 300_000, funded = 100_000, essential = false),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        val progress = requireNotNull(engine.budgetProgress(october, budget, today)) {
            "Expected budget progress for a budget with lines"
        }

        // Only the funded part is spendable: (100,000 funded − 50,000 spent) / 17 days left.
        assertEquals(50_000L, progress.flexibleRemainingMinor)
        assertEquals(2_941L, progress.safeToSpendDailyMinor)
    }

    @Test
    fun `safe to spend is null when no flexible line is funded`() {
        val budget = Fixtures.budget("b1", october.start, october.end)
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            categories = listOf(Fixtures.category("food")),
            budgets = listOf(budget),
            lines = listOf(
                Fixtures.line("l1", "b1", "food", planned = 300_000, funded = 0, essential = false),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        val progress = requireNotNull(engine.budgetProgress(october, budget, today)) {
            "Expected budget progress for a budget with lines"
        }

        assertEquals(300_000L, progress.plannedMinor)
        assertEquals(0L, progress.flexibleRemainingMinor)
        assertNull(progress.safeToSpendDailyMinor)
    }

    @Test
    fun `previous comparable month is used for full months`() {
        val engine = AnalyticsEngine(Fixtures.snapshot())
        assertEquals(
            DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
            engine.previousComparable(october, today = LocalDate.of(2026, 11, 2)),
        )
        assertEquals(
            DateRange(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 30)),
            engine.previousComparable(DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7))),
        )
    }

    @Test
    fun `month to date compares with the same elapsed days of the previous month`() {
        val engine = AnalyticsEngine(Fixtures.snapshot())
        assertEquals(
            DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 23)),
            engine.previousComparable(october, today = LocalDate.of(2026, 10, 23)),
        )
    }

    @Test
    fun `month to date comparison clamps to the previous month length`() {
        val engine = AnalyticsEngine(Fixtures.snapshot())
        val march = DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        assertEquals(
            DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
            engine.previousComparable(march, today = LocalDate.of(2026, 3, 30)),
        )
        val leapMarch = DateRange(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 3, 31))
        assertEquals(
            DateRange(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)),
            engine.previousComparable(leapMarch, today = LocalDate.of(2028, 3, 30)),
        )
    }

    @Test
    fun `dashboard previous values use the month to date comparison range`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            transactions = listOf(
                income("early-sep", LocalDate.of(2026, 9, 10), 100_000),
                income("late-sep", LocalDate.of(2026, 9, 25), 900_000),
                income("oct", LocalDate.of(2026, 10, 5), 250_000),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        val dashboard = engine.dashboard(october, LocalDate.of(2026, 10, 23))

        assertEquals(DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 23)), dashboard.previousPeriod)
        // Only the Sep 10 income falls inside Sep 1-23; Sep 25 is after day 23 and excluded.
        assertEquals(100_000L, dashboard.previousIncomeMinor)
    }

    @Test
    fun `income variability reports median and range without a score`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            transactions = listOf(
                income("jul", LocalDate.of(2026, 7, 5), 100_000),
                income("aug", LocalDate.of(2026, 8, 5), 200_000),
                income("sep", LocalDate.of(2026, 9, 5), 300_000),
                income("oct", LocalDate.of(2026, 10, 5), 400_000),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        val variability = engine.incomeVariability(LocalDate.of(2026, 10, 22))!!
        assertEquals(250_000L, variability.medianMinor)
        assertEquals(100_000L, variability.lowestMinor)
        assertEquals(400_000L, variability.highestMinor)
        assertEquals(4, variability.monthsWithIncome)
        assertEquals(17, variability.daysSinceLastIncome)
    }

    @Test
    fun `runway-like liquid balance excludes debt wallets`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(
                Fixtures.wallet("cash", opening = 100_000),
                Fixtures.wallet("loan", type = com.mizani.money.core.model.WalletType.DEBT, opening = -40_000),
            ),
        )
        assertEquals(100_000L, snapshot.liquidBalance())
        assertEquals(60_000L, snapshot.walletBalances().values.sum())
    }

    @Test
    fun `forecast needs recent spending and states a basis`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 1_000_000)),
            transactions = listOf(
                Fixtures.transaction(
                    id = "e",
                    type = TransactionType.EXPENSE,
                    amount = 14_000,
                    walletId = "cash",
                    categoryId = "food",
                    // today - 13: the activity spans the full 14-day window.
                    date = LocalDate.of(2026, 10, 2),
                ),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        val forecast = engine.forecast(october, today)
        assertNotNull(forecast)
        // 14,000 spent to date + (14,000 / 14 observed days) * 17 remaining days = 31,000
        assertEquals(31_000L, forecast)
        assertNull(engine.forecast(october, LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun `forecast is null when the spending history covers less than seven days`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 1_000_000)),
            transactions = listOf(
                Fixtures.transaction(
                    id = "recent",
                    type = TransactionType.EXPENSE,
                    amount = 14_000,
                    walletId = "cash",
                    categoryId = "food",
                    // today - 2: only three observed days, not enough history to project from.
                    date = LocalDate.of(2026, 10, 13),
                ),
            ),
        )
        val engine = AnalyticsEngine(snapshot)
        assertNull(engine.forecast(october, today))
    }

    @Test
    fun `refund with a fee increments the fee category transaction count in slices`() {
        val fees = DefaultCategories.feesId()
        val transactions = listOf(
            Fixtures.transaction(
                id = "expense",
                type = TransactionType.EXPENSE,
                amount = 100_000,
                walletId = "cash",
                categoryId = "food",
                date = LocalDate.of(2026, 10, 3),
            ),
            Fixtures.transaction(
                id = "refund",
                type = TransactionType.REFUND,
                amount = 30_000,
                fee = 500,
                feeCategoryId = fees,
                walletId = "cash",
                categoryId = "food",
                linkedTransactionId = "expense",
                date = LocalDate.of(2026, 10, 4),
            ),
        )
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 100_000)),
            categories = listOf(
                Fixtures.category("food", "Food", com.mizani.money.core.model.CategoryType.EXPENSE),
                Fixtures.category(fees, "Fees", com.mizani.money.core.model.CategoryType.EXPENSE),
            ),
            transactions = transactions,
        )
        val engine = AnalyticsEngine(snapshot)
        val slices = engine.categorySlices(october)

        val foodSlice = slices.first { it.categoryId == "food" }
        val feesSlice = slices.first { it.categoryId == fees }
        // The refund counts once against the original category.
        assertEquals(2, foodSlice.transactionCount)
        // The refund's fee is a separate spending event in the fees category.
        assertEquals(1, feesSlice.transactionCount)
    }

    private fun income(id: String, date: LocalDate, amount: Long) =
        Fixtures.transaction(
            id = id,
            type = TransactionType.INCOME,
            amount = amount,
            walletId = "cash",
            categoryId = "salary",
            date = date,
        )
}
