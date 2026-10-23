package com.mizani.money.domain

import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.CategoryEntity
import com.mizani.money.data.local.GoalEntity
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.data.repo.LedgerSnapshot
import java.time.LocalDate
import java.time.ZoneId

object Fixtures {

    const val CURRENCY = "UGX"
    const val NOW = 1_760_000_000_000L

    fun wallet(
        id: String,
        name: String = id,
        type: WalletType = WalletType.MOBILE_MONEY,
        opening: Long = 0L,
        archived: Boolean = false,
    ) = WalletEntity(
        id = id,
        name = name,
        type = type,
        openingBalanceMinor = opening,
        currencyCode = CURRENCY,
        colorHex = "#2F6B5E",
        icon = "wallet",
        sortOrder = 0,
        archived = archived,
        createdAt = NOW,
        updatedAt = NOW,
    )

    fun category(
        id: String,
        name: String = id,
        type: CategoryType = CategoryType.EXPENSE,
    ) = CategoryEntity(
        id = id,
        name = name,
        type = type,
        icon = "category",
        colorHex = "#3D6E8C",
        isDefault = false,
        sortOrder = 0,
        createdAt = NOW,
        updatedAt = NOW,
    )

    fun transaction(
        id: String,
        type: TransactionType,
        amount: Long,
        walletId: String,
        date: LocalDate,
        toWalletId: String? = null,
        categoryId: String? = null,
        fee: Long = 0L,
        feeCategoryId: String? = null,
        note: String = "",
        linkedTransactionId: String? = null,
        deleted: Boolean = false,
    ) = TransactionEntity(
        id = id,
        type = type,
        amountMinor = amount,
        feeMinor = fee,
        currencyCode = CURRENCY,
        walletId = walletId,
        toWalletId = toWalletId,
        categoryId = categoryId,
        feeCategoryId = feeCategoryId,
        note = note,
        payee = note,
        localDateEpochDay = date.toEpochDay(),
        occurredAt = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        createdAt = NOW,
        updatedAt = NOW,
        deletedAt = if (deleted) NOW else null,
        linkedTransactionId = linkedTransactionId,
    )

    fun budget(
        id: String,
        start: LocalDate,
        end: LocalDate,
        type: BudgetPeriodType = BudgetPeriodType.MONTHLY,
    ) = BudgetEntity(
        id = id,
        name = "Test budget",
        periodType = type,
        startEpochDay = start.toEpochDay(),
        endEpochDay = end.toEpochDay(),
        createdAt = NOW,
        updatedAt = NOW,
    )

    fun line(
        id: String,
        budgetId: String,
        categoryId: String,
        planned: Long,
        funded: Long = planned,
        essential: Boolean = false,
        rollover: RolloverRule = RolloverRule.NONE,
    ) = BudgetLineEntity(
        id = id,
        budgetId = budgetId,
        categoryId = categoryId,
        plannedMinor = planned,
        fundedMinor = funded,
        rolloverRule = rollover,
        essential = essential,
        priority = 2,
    )

    fun recurring(
        id: String,
        type: RecurringType,
        amount: Long,
        nextDue: LocalDate,
        essential: Boolean = true,
        categoryId: String? = null,
        walletId: String? = null,
        frequency: Frequency = Frequency.MONTHLY,
        archived: Boolean = false,
    ) = RecurringRuleEntity(
        id = id,
        name = id,
        type = type,
        amountMinor = amount,
        categoryId = categoryId,
        walletId = walletId,
        frequency = frequency,
        nextDueEpochDay = nextDue.toEpochDay(),
        essential = essential,
        archived = archived,
        createdAt = NOW,
        updatedAt = NOW,
    )

    fun goal(
        id: String,
        target: Long,
        allocated: Long = 0L,
        targetDate: LocalDate? = null,
    ) = GoalEntity(
        id = id,
        name = id,
        targetMinor = target,
        allocatedMinor = allocated,
        targetEpochDay = targetDate?.toEpochDay(),
        createdAt = NOW,
        updatedAt = NOW,
    )

    fun snapshot(
        wallets: List<WalletEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList(),
        transactions: List<TransactionEntity> = emptyList(),
        budgets: List<BudgetEntity> = emptyList(),
        lines: List<BudgetLineEntity> = emptyList(),
        recurring: List<RecurringRuleEntity> = emptyList(),
        goals: List<GoalEntity> = emptyList(),
        contributions: List<com.mizani.money.data.local.GoalContributionEntity> = emptyList(),
        debts: List<com.mizani.money.data.local.DebtEntity> = emptyList(),
    ) = LedgerSnapshot(
        wallets = wallets,
        categories = categories,
        transactions = transactions,
        budgets = budgets,
        budgetLines = lines,
        recurring = recurring,
        goals = goals,
        contributions = contributions,
        debts = debts,
    )
}
