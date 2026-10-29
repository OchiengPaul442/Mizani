package com.mizani.money.data.repo

import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.CategoryEntity
import com.mizani.money.data.local.DebtEntity
import com.mizani.money.data.local.GoalContributionEntity
import com.mizani.money.data.local.GoalEntity
import com.mizani.money.data.local.IncomeSourceEntity
import com.mizani.money.data.local.InvestmentHoldingEntity
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.data.local.WorkspaceEntity
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.IncomeSourceKind
import com.mizani.money.core.model.InvestmentAssetType
import com.mizani.money.core.model.RecurringType
import com.mizani.money.domain.Fixtures
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BackupValidatorTest {

    private val now = Fixtures.NOW
    private val currency = Fixtures.CURRENCY

    private val workspace = WorkspaceEntity(
        id = "ws", name = "Test", countryCode = "UG", currencyCode = currency,
        createdAt = now, updatedAt = now,
    )
    private val wallet = WalletEntity(
        id = "cash", name = "Cash", type = WalletType.CASH, openingBalanceMinor = 0,
        currencyCode = currency, colorHex = "#000", icon = "wallet", sortOrder = 0,
        createdAt = now, updatedAt = now,
    )
    private val food = CategoryEntity(
        id = "food", name = "Food", type = CategoryType.EXPENSE, icon = "food",
        colorHex = "#000", isDefault = false, sortOrder = 0, createdAt = now, updatedAt = now,
    )

    private fun txn(
        id: String,
        type: TransactionType,
        walletId: String = "cash",
        toWalletId: String? = null,
        categoryId: String? = "food",
        feeCategoryId: String? = null,
        feeMinor: Long = 0L,
        linkedTransactionId: String? = null,
        currencyCode: String = currency,
    ) = TransactionEntity(
        id = id, type = type, amountMinor = 10_000, feeMinor = feeMinor,
        currencyCode = currencyCode, walletId = walletId, toWalletId = toWalletId,
        categoryId = categoryId, feeCategoryId = feeCategoryId, linkedTransactionId = linkedTransactionId,
        localDateEpochDay = LocalDate.of(2026, 10, 3).toEpochDay(),
        occurredAt = now, createdAt = now, updatedAt = now,
    )

    private fun validBundle(transactions: List<TransactionEntity> = listOf(txn("t1", TransactionType.EXPENSE))) =
        RestoreBundle(
            workspace = workspace,
            wallets = listOf(wallet),
            categories = listOf(food),
            incomeSources = emptyList(),
            transactions = transactions,
            budgets = emptyList(),
            budgetLines = emptyList(),
            goals = emptyList(),
            contributions = emptyList(),
            recurring = emptyList(),
            debts = emptyList(),
        )

    @Test
    fun `valid bundle passes`() {
        BackupValidator.validate(validBundle())
    }

    @Test
    fun `rejects mismatched transaction currency`() {
        val bundle = validBundle(listOf(txn("t1", TransactionType.EXPENSE, currencyCode = "USD")))
        assertRejected(bundle, "currency")
    }

    @Test
    fun `rejects mismatched wallet currency`() {
        val bundle = validBundle().copy(wallets = listOf(wallet.copy(currencyCode = "USD")))
        assertRejected(bundle, "currency")
    }

    @Test
    fun `rejects dangling fee category`() {
        val bundle = validBundle(listOf(txn("t1", TransactionType.EXPENSE, feeCategoryId = "ghost-fees")))
        assertRejected(bundle, "fee category")
    }

    @Test
    fun `rejects dangling linked transaction`() {
        val bundle = validBundle(listOf(txn("ref", TransactionType.REFUND, linkedTransactionId = "ghost")))
        assertRejected(bundle, "linked transaction")
    }

    @Test
    fun `rejects dangling income source`() {
        val salary = CategoryEntity(
            id = "salary", name = "Salary", type = CategoryType.INCOME, icon = "income",
            colorHex = "#000", isDefault = false, sortOrder = 1, createdAt = now, updatedAt = now,
        )
        val t = txn("t1", TransactionType.INCOME, categoryId = "salary").copy(incomeSourceId = "ghost-source")
        val bundle = validBundle(listOf(t)).copy(categories = listOf(food, salary))
        assertRejected(bundle, "income source")
    }

    @Test
    fun `accepts valid fee category`() {
        val fees = CategoryEntity(
            id = DefaultCategories.feesId(), name = "Fees", type = CategoryType.EXPENSE,
            icon = "fees", colorHex = "#000", isDefault = false, sortOrder = 1,
            createdAt = now, updatedAt = now,
        )
        val savings = wallet.copy(id = "savings")
        val bundle = validBundle(
            listOf(txn("t1", TransactionType.TRANSFER, toWalletId = "savings", feeCategoryId = fees.id, feeMinor = 500)),
        ).copy(wallets = listOf(wallet, savings), categories = listOf(food, fees))
        BackupValidator.validate(bundle)
    }

    @Test
    fun `rejects transfer to missing wallet`() {
        val t = txn("tr", TransactionType.TRANSFER).copy(toWalletId = "ghost")
        assertRejected(validBundle(listOf(t)), "missing wallet")
    }

    @Test
    fun `rejects budget line with dangling category`() {
        val line = BudgetLineEntity(
            id = "l1", budgetId = "b1", categoryId = "ghost", plannedMinor = 100,
            fundedMinor = 0, essential = true,
        )
        val bundle = validBundle().copy(
            budgets = listOf(BudgetEntity("b1", "B", BudgetPeriodType.MONTHLY, 0L, 1L, now, now)),
            budgetLines = listOf(line),
        )
        assertRejected(bundle, "missing category")
    }

    @Test
    fun `rejects contribution dangling wallet and transfer`() {
        val goal = GoalEntity(id = "g1", name = "G", targetMinor = 100, allocatedMinor = 0, createdAt = now, updatedAt = now)
        val contribution = GoalContributionEntity(
            id = "c1", goalId = "g1", amountMinor = 10, localDateEpochDay = 0L,
            walletId = "ghost", transferTransactionId = "ghost-txn", createdAt = now,
        )
        val bundle = validBundle().copy(goals = listOf(goal), contributions = listOf(contribution))
        assertRejected(bundle, "missing wallet")
    }

    @Test
    fun `rejects recurring dangling category`() {
        val rule = RecurringRuleEntity(
            id = "r1", name = "R", type = RecurringType.BILL, amountMinor = 10,
            categoryId = "ghost", walletId = null, frequency = com.mizani.money.core.model.Frequency.MONTHLY,
            nextDueEpochDay = 0L, createdAt = now, updatedAt = now,
        )
        assertRejected(validBundle().copy(recurring = listOf(rule)), "missing category")
    }

    @Test
    fun `rejects goal dangling linked wallet`() {
        val goal = GoalEntity(
            id = "g1", name = "G", targetMinor = 100, allocatedMinor = 0,
            linkedWalletId = "ghost", createdAt = now, updatedAt = now,
        )
        assertRejected(validBundle().copy(goals = listOf(goal)), "missing wallet")
    }

    @Test
    fun `rejects expense referencing an income category`() {
        val incomeCat = CategoryEntity(
            id = "salary", name = "Salary", type = CategoryType.INCOME, icon = "income",
            colorHex = "#000", isDefault = false, sortOrder = 1, createdAt = now, updatedAt = now,
        )
        val t = txn("t1", TransactionType.EXPENSE, categoryId = "salary")
        val bundle = validBundle(listOf(t)).copy(categories = listOf(food, incomeCat))
        assertRejected(bundle, "expense category")
    }

    @Test
    fun `rejects refund referencing an income category`() {
        val incomeCat = CategoryEntity(
            id = "salary", name = "Salary", type = CategoryType.INCOME, icon = "income",
            colorHex = "#000", isDefault = false, sortOrder = 1, createdAt = now, updatedAt = now,
        )
        val t = txn("r1", TransactionType.REFUND, categoryId = "salary")
        val bundle = validBundle(listOf(t)).copy(categories = listOf(food, incomeCat))
        assertRejected(bundle, "expense category")
    }

    @Test
    fun `rejects income referencing an expense category`() {
        val t = txn("t1", TransactionType.INCOME, categoryId = "food")
        assertRejected(validBundle(listOf(t)), "income category")
    }

    @Test
    fun `rejects fee category that is not an expense category`() {
        val incomeCat = CategoryEntity(
            id = "salary", name = "Salary", type = CategoryType.INCOME, icon = "income",
            colorHex = "#000", isDefault = false, sortOrder = 1, createdAt = now, updatedAt = now,
        )
        val t = txn("t1", TransactionType.EXPENSE, feeCategoryId = "salary", feeMinor = 500)
        val bundle = validBundle(listOf(t)).copy(categories = listOf(food, incomeCat))
        assertRejected(bundle, "expense category")
    }

    @Test
    fun `rejects non-positive transfer amount`() {
        val savings = wallet.copy(id = "savings")
        val t = txn("tr", TransactionType.TRANSFER, toWalletId = "savings").copy(amountMinor = 0L)
        val bundle = validBundle(listOf(t)).copy(wallets = listOf(wallet, savings))
        assertRejected(bundle, "positive")
    }

    @Test
    fun `rejects non-positive refund amount`() {
        val t = txn("r1", TransactionType.REFUND).copy(amountMinor = 0L)
        assertRejected(validBundle(listOf(t)), "positive")
    }

    @Test
    fun `rejects negative debt balance`() {
        val debt = DebtEntity(
            id = "d1", name = "Loan", lender = "Bank", currentBalanceMinor = -100,
            periodicPaymentMinor = 10, createdAt = now, updatedAt = now,
        )
        assertRejected(validBundle().copy(debts = listOf(debt)), "negative")
    }

    @Test
    fun `rejects negative debt payment`() {
        val debt = DebtEntity(
            id = "d1", name = "Loan", lender = "Bank", currentBalanceMinor = 1000,
            periodicPaymentMinor = -10, createdAt = now, updatedAt = now,
        )
        assertRejected(validBundle().copy(debts = listOf(debt)), "negative")
    }

    @Test
    fun `rejects duplicate transaction ids`() = assertRejected(
        validBundle(listOf(txn("t1", TransactionType.EXPENSE), txn("t1", TransactionType.EXPENSE))),
        "duplicate transaction",
    )

    @Test
    fun `rejects duplicate budget ids`() {
        val budget = BudgetEntity("b1", "B", BudgetPeriodType.MONTHLY, 0L, 1L, now, now)
        assertRejected(validBundle().copy(budgets = listOf(budget, budget)), "duplicate budget")
    }

    @Test
    fun `rejects duplicate budget line ids`() {
        val budget = BudgetEntity("b1", "B", BudgetPeriodType.MONTHLY, 0L, 1L, now, now)
        val line = BudgetLineEntity("l1", "b1", "food", 100, 0, essential = true)
        assertRejected(
            validBundle().copy(budgets = listOf(budget), budgetLines = listOf(line, line)),
            "duplicate budget line",
        )
    }

    @Test
    fun `rejects duplicate budget line for the same budget and category`() {
        val budget = BudgetEntity("b1", "B", BudgetPeriodType.MONTHLY, 0L, 1L, now, now)
        val line1 = BudgetLineEntity("l1", "b1", "food", 100, 0, essential = true)
        val line2 = BudgetLineEntity("l2", "b1", "food", 200, 0, essential = true)
        assertRejected(
            validBundle().copy(budgets = listOf(budget), budgetLines = listOf(line1, line2)),
            "duplicate budget lines",
        )
    }

    @Test
    fun `rejects duplicate goal ids`() {
        val goal = GoalEntity(id = "g1", name = "G", targetMinor = 100, allocatedMinor = 0, createdAt = now, updatedAt = now)
        assertRejected(validBundle().copy(goals = listOf(goal, goal)), "duplicate goal")
    }

    @Test
    fun `rejects duplicate contribution ids`() {
        val goal = GoalEntity(id = "g1", name = "G", targetMinor = 100, allocatedMinor = 0, createdAt = now, updatedAt = now)
        val contribution = GoalContributionEntity(
            id = "c1", goalId = "g1", amountMinor = 10, localDateEpochDay = 0L, createdAt = now,
        )
        assertRejected(
            validBundle().copy(goals = listOf(goal), contributions = listOf(contribution, contribution)),
            "duplicate contribution",
        )
    }

    @Test
    fun `rejects duplicate recurring ids`() {
        val rule = RecurringRuleEntity(
            id = "r1", name = "R", type = RecurringType.BILL, amountMinor = 10,
            categoryId = null, walletId = null, frequency = com.mizani.money.core.model.Frequency.MONTHLY,
            nextDueEpochDay = 0L, createdAt = now, updatedAt = now,
        )
        assertRejected(validBundle().copy(recurring = listOf(rule, rule)), "duplicate recurring")
    }

    @Test
    fun `rejects duplicate debt ids`() {
        val debt = DebtEntity(
            id = "d1", name = "Loan", lender = "Bank", currentBalanceMinor = 1000,
            periodicPaymentMinor = 10, createdAt = now, updatedAt = now,
        )
        assertRejected(validBundle().copy(debts = listOf(debt, debt)), "duplicate debt")
    }

    @Test
    fun `rejects duplicate income source ids`() {
        val source = IncomeSourceEntity(
            id = "s1", name = "Salary", kind = IncomeSourceKind.SALARY, createdAt = now, updatedAt = now,
        )
        assertRejected(
            validBundle().copy(incomeSources = listOf(source, source)),
            "duplicate income source",
        )
    }

    @Test
    fun `accepts valid investment holding`() {
        BackupValidator.validate(
            validBundle().copy(
                investments = listOf(
                    InvestmentHoldingEntity(
                        id = "i1",
                        name = "Treasury fund",
                        provider = "Provider",
                        assetType = InvestmentAssetType.FUNDS,
                        costBasisMinor = 100,
                        currentValueMinor = 125,
                        currencyCode = currency,
                        valuationDateEpochDay = LocalDate.now().toEpochDay(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `rejects investment with future valuation`() {
        val investment = InvestmentHoldingEntity(
            id = "i1",
            name = "Future fund",
            provider = "Provider",
            assetType = InvestmentAssetType.FUNDS,
            costBasisMinor = 100,
            currentValueMinor = 125,
            currencyCode = currency,
            valuationDateEpochDay = LocalDate.now().plusDays(1).toEpochDay(),
            createdAt = now,
            updatedAt = now,
        )
        assertRejected(validBundle().copy(investments = listOf(investment)), "future")
    }

    private fun assertRejected(bundle: RestoreBundle, expectedPhrase: String) {
        try {
            BackupValidator.validate(bundle)
            fail("Expected rejection containing '$expectedPhrase'")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Error '${e.message}' should contain '$expectedPhrase'",
                e.message?.contains(expectedPhrase, ignoreCase = true) == true,
            )
        }
    }
}
