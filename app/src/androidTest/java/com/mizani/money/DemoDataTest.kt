package com.mizani.money

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.IncomeSourceKind
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.time.Periods
import com.mizani.money.data.repo.BudgetLineInput
import com.mizani.money.data.repo.RecurringDraft
import com.mizani.money.data.repo.TransactionDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Test-only seeding for visual verification. Not shipped in release behavior: this class lives in
 * `androidTest` and writes through the same validated repository as the UI.
 */
@RunWith(AndroidJUnit4::class)
class DemoDataTest {

    @Before
    fun clearLocalState() {
        TestState.reset()
    }

    @Test
    fun seedDemoWorkspace() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val repo = app.container.financeRepository
        val settings = app.container.settingsRepository
        val today = LocalDate.now()
        val month = Periods.monthOf(today)

        repo.createWorkspace("UG", "UGX", "Cash", WalletType.CASH, 0)
        settings.completeOnboarding("UG", "UGX")

        val cash = repo.snapshot.first().wallets.first()
        val momo = repo.addWallet("MTN MoMo", WalletType.MOBILE_MONEY, 200_000, "#245B8A")
        val savings = repo.addWallet("Savings", WalletType.SAVINGS, 50_000, "#0B5D63")
        val salarySource = repo.saveIncomeSource(null, "Salary", IncomeSourceKind.SALARY)
        val businessSource = repo.saveIncomeSource(null, "Shop sales", IncomeSourceKind.BUSINESS)

        fun id(key: String) = DefaultCategories.defaultEntityId("category", key)
        fun day(offset: Long): LocalDate = maxOf(month.start, today.minusDays(offset))

        val salaryCategory = repo.snapshot.first().categories.first { it.type == CategoryType.INCOME }.id
        val housing = id("housing")
        val food = id("food")
        val transport = id("transport")
        val mobile = id("mobile_data")
        val family = id("family")
        val utilities = id("utilities")

        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.INCOME,
                amountMinor = 1_200_000,
                walletId = cash.id,
                categoryId = salaryCategory,
                incomeSourceId = salarySource,
                payee = "Employer",
                note = "Monthly salary",
                date = day(20),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.INCOME,
                amountMinor = 250_000,
                walletId = momo,
                categoryId = salaryCategory,
                incomeSourceId = businessSource,
                payee = "Shop customers",
                note = "Weekend sales",
                date = day(6),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 450_000,
                walletId = cash.id,
                categoryId = housing,
                payee = "Landlord",
                note = "October rent",
                date = day(18),
            ),
        )
        val foodExpense = repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 120_000,
                walletId = cash.id,
                categoryId = food,
                payee = "Market",
                note = "Groceries",
                date = day(4),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.REFUND,
                amountMinor = 10_000,
                walletId = cash.id,
                categoryId = food,
                linkedTransactionId = foodExpense,
                payee = "Market",
                note = "Returned item",
                date = day(3),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 60_000,
                walletId = cash.id,
                categoryId = transport,
                payee = "Boda",
                note = "Weekly transport",
                date = day(3),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 25_000,
                walletId = momo,
                categoryId = mobile,
                payee = "MTN",
                note = "Data bundle",
                date = day(2),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 80_000,
                walletId = momo,
                categoryId = family,
                payee = "Family",
                note = "School support",
                date = day(5),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 45_000,
                walletId = cash.id,
                categoryId = utilities,
                payee = "Umeme",
                note = "Prepaid power",
                date = day(1),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.TRANSFER,
                amountMinor = 100_000,
                feeMinor = 1_500,
                walletId = momo,
                toWalletId = cash.id,
                note = "Withdrawal",
                payee = "MTN MoMo",
                date = day(2),
            ),
        )
        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.ADJUSTMENT,
                amountMinor = 5_000,
                walletId = cash.id,
                note = "Reconciled cash count",
                date = day(1),
            ),
        )

        val goalId = repo.saveGoal(
            goalId = null,
            name = "Emergency fund",
            targetMinor = 1_000_000,
            targetDate = today.plusMonths(5),
            linkedWalletId = savings,
            priority = 1,
            note = "Three months of essentials",
            icon = "savings",
        )
        repo.contributeToGoal(goalId, 150_000, cash.id, day(1), "Monthly saving")

        repo.saveRecurring(
            id = null,
            draft = RecurringDraft(
                name = "Rent",
                type = RecurringType.BILL,
                amountMinor = 450_000,
                categoryId = housing,
                walletId = cash.id,
                frequency = Frequency.MONTHLY,
                nextDue = today.plusMonths(1),
                essential = true,
            ),
        )
        repo.saveRecurring(
            id = null,
            draft = RecurringDraft(
                name = "Data bundle",
                type = RecurringType.BILL,
                amountMinor = 25_000,
                categoryId = mobile,
                walletId = momo,
                frequency = Frequency.MONTHLY,
                nextDue = today.plusDays(3),
                essential = true,
            ),
        )
        repo.saveRecurring(
            id = null,
            draft = RecurringDraft(
                name = "Salary",
                type = RecurringType.INCOME,
                amountMinor = 1_200_000,
                categoryId = salaryCategory,
                walletId = cash.id,
                frequency = Frequency.MONTHLY,
                nextDue = today.plusDays(20),
                essential = true,
            ),
        )

        repo.saveDebt(
            debtId = null,
            name = "Phone loan",
            lender = "Wakandi",
            balanceMinor = 300_000,
            originalMinor = 600_000,
            periodicPaymentMinor = 50_000,
            nextDue = today.plusDays(12),
            interestRatePercent = 4.0,
            note = "Installment plan",
        )

        repo.saveBudget(
            budgetId = null,
            name = "October plan",
            periodType = BudgetPeriodType.MONTHLY,
            start = month.start,
            end = month.end,
            lines = listOf(
                BudgetLineInput(housing, 450_000, 450_000, true, RolloverRule.NONE, 1),
                BudgetLineInput(food, 300_000, 300_000, true, RolloverRule.POSITIVE, 1),
                BudgetLineInput(transport, 150_000, 150_000, false, RolloverRule.NONE, 2),
                BudgetLineInput(mobile, 50_000, 50_000, true, RolloverRule.NONE, 2),
                BudgetLineInput(family, 100_000, 100_000, false, RolloverRule.NONE, 2),
                BudgetLineInput(utilities, 100_000, 100_000, true, RolloverRule.NONE, 1),
            ),
        )

        val bundle = repo.exportBundle()
        assertTrue(bundle.transactions.size >= 10)
        assertTrue(bundle.categories.isNotEmpty())
        assertTrue(bundle.budgets.isNotEmpty())
        assertTrue(bundle.goals.isNotEmpty())
        assertTrue(bundle.debts.isNotEmpty())
    }
}
