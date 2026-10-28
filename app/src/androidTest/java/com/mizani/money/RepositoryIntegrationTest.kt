package com.mizani.money

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.Periods
import com.mizani.money.data.repo.BudgetLineInput
import com.mizani.money.data.repo.CommitmentDraft
import com.mizani.money.data.repo.RecurringDraft
import com.mizani.money.data.repo.TransactionDraft
import com.mizani.money.domain.analytics.AnalyticsEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Exercises the real encrypted Room/SQLCipher write boundary and DAO mappings end to end. */
@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    @Before
    fun reset() {
        TestState.reset()
    }

    @Test
    fun encryptedRepositoryCrudAndAnalyticsInvariants() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val repo = app.container.financeRepository
        val today = LocalDate.now()
        val month = Periods.monthOf(today)

        repo.createWorkspace("UG", "UGX", "Cash", WalletType.CASH, 0)
        val workspace = repo.currentWorkspace()
        assertNotNull(workspace)
        val cash = repo.snapshot.first().wallets.first()
        val momo = repo.addWallet("MTN MoMo", WalletType.MOBILE_MONEY, 0, "#245B8A")
        val food = DefaultCategories.defaultEntityId("category", "food")
        val housing = DefaultCategories.defaultEntityId("category", "housing")
        val fees = DefaultCategories.defaultEntityId("category", DefaultCategories.FEES_KEY)

        val incomeId = repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.INCOME,
                amountMinor = 100_000,
                walletId = cash.id,
                categoryId = repo.snapshot.first().categories.first { it.type == com.mizani.money.core.model.CategoryType.INCOME }.id,
                date = month.start,
            ),
        )
        val expenseId = repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 25_000,
                walletId = cash.id,
                categoryId = food,
                date = minOf(month.start.plusDays(1), today),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.TRANSFER,
                amountMinor = 30_000,
                feeMinor = 1_500,
                walletId = cash.id,
                toWalletId = momo,
                date = today,
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.REFUND,
                amountMinor = 5_000,
                walletId = cash.id,
                categoryId = food,
                linkedTransactionId = expenseId,
                date = today,
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.ADJUSTMENT,
                amountMinor = 2_000,
                walletId = cash.id,
                date = today,
            ),
        )

        val snapshot = repo.snapshot.first()
        assertEquals(5, snapshot.transactions.size)
        val balances = snapshot.walletBalances()
        assertEquals(100_000L - 25_000L - 30_000L - 1_500L + 5_000L + 2_000L, balances[cash.id])
        assertEquals(30_000L, balances[momo])
        assertEquals(80_500L, snapshot.liquidBalance())

        val engine = AnalyticsEngine(snapshot)
        val period = DateRange(month.start, month.end)
        assertEquals(100_000L, engine.incomeMinor(period))
        assertEquals(21_500L, engine.expenseMinor(period)) // 25,000 - 5,000 refund + 1,500 fee
        assertEquals(1_500L, engine.feesMinor(period))
        assertEquals(fees, engine.categorySlices(period).first { it.amountMinor == 1_500L }.categoryId)

        val budgetId = repo.saveBudget(
            budgetId = null,
            name = "Test plan",
            periodType = BudgetPeriodType.MONTHLY,
            start = month.start,
            end = month.end,
            lines = listOf(
                BudgetLineInput(food, 100_000, 80_000, true, RolloverRule.POSITIVE, 1),
                BudgetLineInput(housing, 200_000, 150_000, true, RolloverRule.NONE, 1),
            ),
        )
        val withBudget = repo.snapshot.first()
        assertTrue(withBudget.budgets.any { it.id == budgetId })
        val progress = AnalyticsEngine(withBudget).budgetProgress(period, withBudget.budgets.first { it.id == budgetId }, today)
        assertNotNull(progress)
        assertEquals(20_000L, progress!!.spentMinor) // food 25,000 - 5,000 refund

        val goalId = repo.saveGoal(null, "School fees", 500_000, today.plusMonths(3), null, 1, "Term 2")
        repo.contributeToGoal(goalId, 50_000, cash.id, today)
        val withGoal = repo.snapshot.first()
        assertEquals(50_000L, withGoal.goals.first { it.id == goalId }.allocatedMinor)
        assertEquals(1, withGoal.contributions.count { it.goalId == goalId })

        val ruleId = repo.saveRecurring(
            null,
            RecurringDraft(
                name = "Data",
                type = RecurringType.BILL,
                amountMinor = 10_000,
                categoryId = food,
                walletId = cash.id,
                frequency = Frequency.MONTHLY,
                nextDue = today,
                essential = true,
            ),
        )
        repo.markRecurringPaid(ruleId, today)
        val afterBill = repo.snapshot.first()
        assertTrue(afterBill.transactions.any { it.type == TransactionType.EXPENSE && it.amountMinor == 10_000L })
        assertTrue(afterBill.recurring.first { it.id == ruleId }.nextDueEpochDay > today.toEpochDay())

        val debtId = repo.saveDebt(null, "Phone loan", "Lender", 100_000, 200_000, 20_000, today.plusDays(7), 3.0, "")
        repo.recordDebtPayment(debtId, cash.id, 20_000, today)
        val afterDebt = repo.snapshot.first()
        assertEquals(80_000L, afterDebt.debts.first { it.id == debtId }.currentBalanceMinor)

        repo.deleteTransaction(incomeId)
        assertEquals(6, repo.snapshot.first().transactions.size)
        val tombstone = repo.exportBundle()
        assertTrue(tombstone.transactions.none { it.id == incomeId })

        repo.restoreTransaction(incomeId)
        assertEquals(7, repo.snapshot.first().transactions.size)
    }

    private fun assertRejected(action: suspend () -> Unit, message: String) = runBlocking {
        try {
            action()
            fail("Expected rejection: $message")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Error '${e.message}' should mention '$message'",
                e.message?.contains(message, ignoreCase = true) == true,
            )
        }
    }

    @Test
    fun repositoryWriteBoundaryRejectsInvalidReferences() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val repo = app.container.financeRepository

        repo.createWorkspace("UG", "UGX", "Cash", WalletType.CASH, 0)
        val snap = repo.snapshot.first()
        val cash = snap.wallets.first()
        val food = DefaultCategories.defaultEntityId("category", "food")
        val salary = snap.categories.first { it.type == CategoryType.INCOME }.id

        // Expense using an income-category must be rejected (wrong category type).
        assertRejected(
            {
                repo.saveTransaction(
                    TransactionDraft(
                        type = TransactionType.EXPENSE,
                        amountMinor = 1_000,
                        walletId = cash.id,
                        categoryId = salary,
                    ),
                )
            },
            "expense",
        )

        // Transfer to an archived destination must be rejected.
        val savings = repo.addWallet("Savings", WalletType.SAVINGS, 0, "#000")
        repo.setWalletArchived(savings, true)
        assertRejected(
            {
                repo.saveTransaction(
                    TransactionDraft(
                        type = TransactionType.TRANSFER,
                        amountMinor = 1_000,
                        walletId = cash.id,
                        toWalletId = savings,
                    ),
                )
            },
            "archived",
        )

        // Recurring bill with an income-category must be rejected.
        assertRejected(
            {
                repo.saveRecurring(
                    null,
                    RecurringDraft(
                        name = "Bad bill",
                        type = RecurringType.BILL,
                        amountMinor = 1_000,
                        categoryId = salary,
                        walletId = cash.id,
                        frequency = Frequency.MONTHLY,
                        nextDue = LocalDate.now(),
                    ),
                )
            },
            "bill",
        )

        // Budget line with an income-category must be rejected.
        val month = Periods.monthOf(LocalDate.now())
        assertRejected(
            {
                repo.saveBudget(
                    budgetId = null,
                    name = "Bad plan",
                    periodType = BudgetPeriodType.MONTHLY,
                    start = month.start,
                    end = month.end,
                    lines = listOf(BudgetLineInput(salary, 10_000, 0, true)),
                )
            },
            "budget line",
        )
    }

    @Test
    fun importedReferencesStayUniqueAndCommitmentLinksDoNotDuplicateExpenses() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val repo = app.container.financeRepository
        val today = LocalDate.now()
        repo.createWorkspace("UG", "UGX", "Cash", WalletType.CASH, 10_000)
        val wallet = repo.snapshot.first().wallets.single()
        val category = DefaultCategories.defaultEntityId("category", "education")
        val transactionId = repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 600,
                walletId = wallet.id,
                categoryId = category,
                date = today,
                sourceProvider = "MTN_MOMO",
                sourceReference = "abc-123",
            ),
        )
        assertTrue(repo.hasImportedReference("mtn_momo", "ABC-123"))

        try {
            repo.saveTransaction(
                TransactionDraft(
                    type = TransactionType.EXPENSE,
                    amountMinor = 600,
                    walletId = wallet.id,
                    categoryId = category,
                    date = today,
                    sourceProvider = "mtn_momo",
                    sourceReference = "ABC-123",
                ),
            )
            fail("A duplicate provider reference must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected duplicate rejection.
        }

        val commitmentId = repo.saveCommitment(
            id = null,
            draft = CommitmentDraft(
                name = "School fees",
                targetMinor = 1_000,
                dueDate = today.plusDays(10),
                categoryId = category,
                reminderDaysBefore = 7,
            ),
        )
        repo.linkExpenseToCommitment(commitmentId, transactionId)
        val linked = repo.snapshot.first()
        assertEquals(1, linked.transactions.size)
        assertEquals(600L, linked.commitmentProgress().single().paidMinor)
        assertEquals(400L, linked.commitmentProgress().single().remainingMinor)
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.REFUND,
                amountMinor = 100,
                walletId = wallet.id,
                categoryId = category,
                linkedTransactionId = transactionId,
                date = today,
            ),
        )
        val refunded = repo.snapshot.first()
        assertEquals(500L, refunded.commitmentProgress().single().paidMinor)
        assertEquals(500L, refunded.commitmentProgress().single().remainingMinor)
        val balanceBeforeUnlink = refunded.walletBalances()[wallet.id]

        repo.unlinkCommitmentPayment(refunded.commitmentPayments.single().id)
        val unlinked = repo.snapshot.first()
        assertEquals(2, unlinked.transactions.size)
        assertEquals(balanceBeforeUnlink, unlinked.walletBalances()[wallet.id])
        assertEquals(0L, unlinked.commitmentProgress().single().paidMinor)

        repo.deleteTransaction(transactionId)
        assertTrue(repo.snapshot.first().commitmentPayments.isEmpty())
        repo.restoreTransaction(transactionId)
        assertTrue(repo.snapshot.first().commitmentPayments.isEmpty())
        assertTrue(repo.hasImportedReference("mtn_momo", "ABC-123"))
    }
}
