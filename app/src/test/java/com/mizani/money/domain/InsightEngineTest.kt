package com.mizani.money.domain

import com.mizani.money.core.model.InsightKind
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.time.DateRange
import com.mizani.money.domain.analytics.AnalyticsEngine
import com.mizani.money.domain.analytics.InsightEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightEngineTest {

    private val october = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
    private val today = LocalDate.of(2026, 10, 15)

    private fun insights(snapshot: com.mizani.money.data.repo.LedgerSnapshot) =
        InsightEngine(snapshot, AnalyticsEngine(snapshot), Currencies.UGX)
            .generate(AnalyticsEngine(snapshot).dashboard(october, today), today)

    @Test
    fun `empty data produces a completeness prompt and no invented conclusions`() {
        val result = insights(Fixtures.snapshot())
        assertEquals(1, result.size)
        assertEquals(InsightKind.DATA_COMPLETENESS, result.single().kind)
        assertTrue(result.single().explanation.contains("never estimates"))
    }

    @Test
    fun `legacy planning records do not surface removed feature insights`() {
        val budget = Fixtures.budget("b1", october.start, october.end)
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 10_000)),
            categories = listOf(Fixtures.category("school", "School fees")),
            budgets = listOf(budget),
            lines = listOf(Fixtures.line("l1", "b1", "school", planned = 450_000, funded = 100_000, essential = true)),
            recurring = listOf(
                Fixtures.recurring(
                    id = "rent",
                    type = RecurringType.BILL,
                    amount = 450_000,
                    nextDue = LocalDate.of(2026, 10, 18),
                ),
            ),
            transactions = listOf(
                Fixtures.transaction(
                    id = "income",
                    type = TransactionType.INCOME,
                    amount = 10_000,
                    walletId = "cash",
                    date = today,
                ),
            ),
            goals = listOf(Fixtures.goal("goal", target = 200_000)),
        )

        assertTrue(insights(snapshot).isEmpty())
    }

    @Test
    fun `fee increase creates a fee trend insight`() {
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash", opening = 500_000)),
            categories = listOf(Fixtures.category("food")),
            transactions = listOf(
                Fixtures.transaction(
                    id = "current",
                    type = TransactionType.EXPENSE,
                    amount = 100_000,
                    fee = 10_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 10, 2),
                ),
                Fixtures.transaction(
                    id = "previous",
                    type = TransactionType.EXPENSE,
                    amount = 100_000,
                    fee = 5_000,
                    walletId = "cash",
                    categoryId = "food",
                    date = LocalDate.of(2026, 9, 2),
                ),
            ),
        )
        val result = insights(snapshot)
        val fee = result.firstOrNull { it.kind == InsightKind.FEE_TREND }
            ?: error("Expected a fee trend insight")
        assertTrue(fee.body.contains("10,000"))
    }

}
