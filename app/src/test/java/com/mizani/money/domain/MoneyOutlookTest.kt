package com.mizani.money.domain

import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.data.local.CommitmentEntity
import com.mizani.money.data.local.CommitmentPaymentEntity
import com.mizani.money.data.local.WorkspaceEntity
import com.mizani.money.data.repo.LedgerSnapshot
import com.mizani.money.domain.analytics.AnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MoneyOutlookTest {
    private val today = LocalDate.of(2026, 10, 15)

    @Test
    fun `reserves obligations on exactly thirty calendar dates including today`() {
        val cash = Fixtures.wallet("cash", type = WalletType.CASH, opening = 1_000)
        val snapshot = LedgerSnapshot(
            workspace = workspace(created = today, target = null),
            wallets = listOf(cash),
            commitments = listOf(
                commitment("due-today", today, target = 50),
                commitment("last-in-window", today.plusDays(29), target = 100),
                commitment("outside-window", today.plusDays(30), target = 200),
            ),
        )

        val outlook = AnalyticsEngine(snapshot).moneyOutlook(today)

        assertEquals(150L, outlook.reservedCommitmentsMinor)
        assertEquals(850L, outlook.spendableBalanceMinor)
    }

    @Test
    fun `short history uses saved target and excludes expected income`() {
        val cash = Fixtures.wallet("cash", type = WalletType.CASH, opening = 1_000)
        val expense = Fixtures.transaction(
            id = "expense",
            type = TransactionType.EXPENSE,
            amount = 100,
            walletId = cash.id,
            categoryId = "food",
            date = today,
        )
        val snapshot = LedgerSnapshot(
            workspace = workspace(created = today, target = 100),
            wallets = listOf(cash),
            transactions = listOf(expense),
            recurring = listOf(
                Fixtures.recurring("salary", RecurringType.INCOME, 50_000, today.plusDays(2)),
            ),
            commitments = listOf(
                CommitmentEntity(
                    id = "school",
                    name = "School fees",
                    targetMinor = 400,
                    dueEpochDay = today.plusDays(3).toEpochDay(),
                    categoryId = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )

        val outlook = AnalyticsEngine(snapshot).moneyOutlook(today)

        assertEquals(1, outlook.observedDays)
        assertNull(outlook.averageDailySpendMinor)
        assertTrue(outlook.usesDailyTarget)
        assertEquals(500L, outlook.spendableBalanceMinor)
        assertEquals(5L, outlook.daysCovered)
    }

    @Test
    fun `history average wins and paid commitment amount is not reserved twice`() {
        val cash = Fixtures.wallet("cash", type = WalletType.CASH, opening = 10_000)
        val firstExpense = Fixtures.transaction(
            id = "old-expense",
            type = TransactionType.EXPENSE,
            amount = 700,
            walletId = cash.id,
            categoryId = "food",
            date = today.minusDays(6),
        )
        val payment = Fixtures.transaction(
            id = "school-payment",
            type = TransactionType.EXPENSE,
            amount = 200,
            walletId = cash.id,
            categoryId = "fees",
            date = today.minusDays(4),
        )
        val commitment = CommitmentEntity(
            id = "school",
            name = "School fees",
            targetMinor = 500,
            dueEpochDay = today.plusDays(5).toEpochDay(),
            categoryId = "fees",
            createdAt = 1L,
            updatedAt = 1L,
        )
        val snapshot = LedgerSnapshot(
            workspace = workspace(created = today.minusDays(30), target = 50),
            wallets = listOf(cash),
            transactions = listOf(firstExpense, payment),
            commitments = listOf(commitment),
            commitmentPayments = listOf(
                CommitmentPaymentEntity(
                    id = "link",
                    commitmentId = commitment.id,
                    transactionId = payment.id,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )

        val outlook = AnalyticsEngine(snapshot).moneyOutlook(today)

        assertEquals(7, outlook.observedDays)
        assertEquals(900L, outlook.recordedExpensesMinor)
        assertEquals(129L, outlook.averageDailySpendMinor)
        assertEquals(300L, outlook.reservedCommitmentsMinor)
        assertEquals(8_800L, outlook.spendableBalanceMinor)
        assertEquals(68L, outlook.daysCovered)
        assertTrue(!outlook.usesDailyTarget)
    }

    @Test
    fun `debt wallets are excluded and zero spendable funds produce zero coverage`() {
        val snapshot = LedgerSnapshot(
            workspace = workspace(created = today, target = 100),
            wallets = listOf(
                Fixtures.wallet("cash", type = WalletType.CASH, opening = 0),
                Fixtures.wallet("debt", type = WalletType.DEBT, opening = 50_000),
            ),
            commitments = listOf(
                CommitmentEntity(
                    id = "rent",
                    name = "Rent",
                    targetMinor = 500,
                    dueEpochDay = today.plusDays(1).toEpochDay(),
                    categoryId = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )

        val outlook = AnalyticsEngine(snapshot).moneyOutlook(today)

        assertEquals(0L, outlook.liquidBalanceMinor)
        assertEquals(500L, outlook.shortfallMinor)
        assertEquals(0L, outlook.daysCovered)
    }

    private fun workspace(created: LocalDate, target: Long?) = WorkspaceEntity(
        id = "workspace",
        name = "Test workspace",
        countryCode = "UG",
        currencyCode = "UGX",
        createdAt = created.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedAt = 1L,
        outlookDailyTargetMinor = target,
    )

    private fun commitment(id: String, dueDate: LocalDate, target: Long) = CommitmentEntity(
        id = id,
        name = id,
        targetMinor = target,
        dueEpochDay = dueDate.toEpochDay(),
        categoryId = null,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
