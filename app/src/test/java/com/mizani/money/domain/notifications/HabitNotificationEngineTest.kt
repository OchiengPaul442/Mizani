package com.mizani.money.domain.notifications

import com.mizani.money.core.model.InvestmentAssetType
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.data.local.DebtEntity
import com.mizani.money.data.local.InvestmentHoldingEntity
import com.mizani.money.domain.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HabitNotificationEngineTest {

    private val enabled = HabitNotificationPreferences(
        enabled = true,
        spendingEnabled = true,
        incomeEnabled = true,
        investmentEnabled = true,
        debtEnabled = true,
        goalEnabled = true,
    )

    @Test
    fun `weekly spending review requires enough recent entries on monday`() {
        val monday = LocalDate.of(2026, 11, 2)
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            transactions = (0 until 6).map { index ->
                Fixtures.transaction(
                    id = "expense-$index",
                    type = TransactionType.EXPENSE,
                    amount = 10,
                    walletId = "cash",
                    date = if (index < 3) {
                        monday.minusDays(index.toLong())
                    } else {
                        monday.plusDays(7L - (index - 3).toLong())
                    },
                )
            },
        )

        assertEquals(
            HabitNotificationKind.SPENDING_REVIEW,
            HabitNotificationEngine.choose(snapshot, monday, enabled, null),
        )
        assertNull(HabitNotificationEngine.choose(snapshot, monday.plusDays(1), enabled, null))
    }

    @Test
    fun `debt and expected income take priority over a weekly spending review`() {
        val monday = LocalDate.of(2026, 11, 2)
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            transactions = (0 until 6).map { index ->
                Fixtures.transaction(
                    id = "expense-$index",
                    type = TransactionType.EXPENSE,
                    amount = 10,
                    walletId = "cash",
                    date = if (index < 3) {
                        monday.minusDays(index.toLong())
                    } else {
                        monday.plusDays(7L - (index - 3).toLong())
                    },
                )
            },
            recurring = listOf(
                Fixtures.recurring(
                    id = "salary",
                    type = RecurringType.INCOME,
                    amount = 100,
                    nextDue = monday.plusDays(1),
                ),
            ),
            debts = listOf(
                DebtEntity(
                    id = "loan",
                    name = "Loan",
                    lender = "Lender",
                    currentBalanceMinor = 1_000,
                    periodicPaymentMinor = 100,
                    nextDueEpochDay = monday.plusDays(2).toEpochDay(),
                    createdAt = Fixtures.NOW,
                    updatedAt = Fixtures.NOW,
                ),
            ),
        )

        assertEquals(
            HabitNotificationKind.DEBT_REVIEW,
            HabitNotificationEngine.choose(snapshot, monday, enabled, null),
        )
    }

    @Test
    fun `stale investments and unfinished goals produce descriptive reviews`() {
        val today = LocalDate.of(2026, 11, 5)
        val investment = InvestmentHoldingEntity(
            id = "holding",
            name = "Fund",
            provider = "Provider",
            assetType = InvestmentAssetType.FUNDS,
            costBasisMinor = 100,
            currentValueMinor = 110,
            currencyCode = "UGX",
            valuationDateEpochDay = today.minusDays(31).toEpochDay(),
            createdAt = Fixtures.NOW,
            updatedAt = Fixtures.NOW,
        )
        val goal = Fixtures.goal("school", target = 1_000, allocated = 100, targetDate = today.plusDays(7))

        assertEquals(
            HabitNotificationKind.INVESTMENT_REVIEW,
            HabitNotificationEngine.choose(
                Fixtures.snapshot().copy(investments = listOf(investment), goals = listOf(goal)),
                today,
                enabled,
                null,
            ),
        )
        assertEquals(
            HabitNotificationKind.GOAL_REVIEW,
            HabitNotificationEngine.choose(
                Fixtures.snapshot(goals = listOf(goal)),
                today,
                enabled.copy(goalEnabled = true, investmentEnabled = false),
                null,
            ),
        )
    }

    @Test
    fun `generic weekly cooldown prevents repetitive alerts`() {
        val monday = LocalDate.of(2026, 11, 2)
        val snapshot = Fixtures.snapshot(
            wallets = listOf(Fixtures.wallet("cash")),
            transactions = (0 until 6).map { index ->
                Fixtures.transaction(
                    id = "expense-$index",
                    type = TransactionType.EXPENSE,
                    amount = 10,
                    walletId = "cash",
                    date = if (index < 3) {
                        monday.minusDays(index.toLong())
                    } else {
                        monday.plusDays(7L - (index - 3).toLong())
                    },
                )
            },
        )

        assertNull(
            HabitNotificationEngine.choose(
                snapshot,
                monday,
                enabled,
                monday.toEpochDay(),
            ),
        )
        assertNull(
            HabitNotificationEngine.choose(
                snapshot,
                monday.plusDays(1),
                enabled,
                monday.toEpochDay(),
            ),
        )
        assertEquals(
            HabitNotificationKind.SPENDING_REVIEW,
            HabitNotificationEngine.choose(
                snapshot,
                monday.plusDays(7),
                enabled,
                monday.toEpochDay(),
            ),
        )
    }

    @Test
    fun `disabled or archived financial records never create a check-in`() {
        val monday = LocalDate.of(2026, 11, 2)
        val archivedDebt = DebtEntity(
            id = "loan",
            name = "Loan",
            lender = "Lender",
            currentBalanceMinor = 1_000,
            periodicPaymentMinor = 100,
            nextDueEpochDay = monday.toEpochDay(),
            archived = true,
            createdAt = Fixtures.NOW,
            updatedAt = Fixtures.NOW,
        )
        val noCategories = enabled.copy(
            enabled = false,
            debtEnabled = false,
            incomeEnabled = false,
            investmentEnabled = false,
            spendingEnabled = false,
            goalEnabled = false,
        )

        assertNull(
            HabitNotificationEngine.choose(
                Fixtures.snapshot(debts = listOf(archivedDebt)),
                monday,
                noCategories,
                null,
            ),
        )
    }
}
