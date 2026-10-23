package com.mizani.money.domain.analytics

import com.mizani.money.core.model.WalletType
import com.mizani.money.core.time.DateRange
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.CategoryEntity
import com.mizani.money.data.local.GoalEntity
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.local.WalletEntity
import java.time.LocalDate

data class CategorySlice(
    val categoryId: String,
    val name: String,
    val colorHex: String,
    val amountMinor: Long,
    val share: Double,
    val transactionCount: Int,
)

data class WalletBalance(
    val wallet: WalletEntity,
    val balanceMinor: Long,
)

data class WalletTypeTotal(
    val type: WalletType,
    val totalMinor: Long,
)

data class BudgetLineProgress(
    val line: BudgetLineEntity,
    val category: CategoryEntity?,
    val spentMinor: Long,
    val rolloverMinor: Long,
    val availableMinor: Long,
    val remainingMinor: Long,
    val usedPercent: Double,
    val expectedToDateMinor: Long,
    val paceVarianceMinor: Long,
)

/**
 * Budget progress for the selected period. [safeToSpendDailyMinor] and
 * [flexibleRemainingMinor] are funded-only: they count money that has actually been
 * set aside (funded + rollover − spent on non-essential lines), never planned-only amounts.
 */
data class BudgetProgress(
    val budget: BudgetEntity,
    val lines: List<BudgetLineProgress>,
    val plannedMinor: Long,
    val fundedMinor: Long,
    val spentMinor: Long,
    val availableMinor: Long,
    val remainingMinor: Long,
    val unbudgetedSpendMinor: Long,
    val usedPercent: Double,
    val elapsedFraction: Double,
    val reservedCommitmentsMinor: Long,
    val safeToSpendDailyMinor: Long?,
    val flexibleRemainingMinor: Long,
) {
    val fundedPercent: Double
        get() = if (plannedMinor <= 0) 0.0 else (fundedMinor.toDouble() / plannedMinor * 100.0)
}

data class UpcomingCommitment(
    val id: String,
    val name: String,
    val amountMinor: Long,
    val dueDate: LocalDate,
    val isIncome: Boolean,
    val essential: Boolean,
    val categoryName: String,
    val colorHex: String,
    val walletName: String,
)

data class GoalProgress(
    val goal: GoalEntity,
    val contributedThisPeriodMinor: Long,
    val remainingMinor: Long,
    val progress: Double,
)

data class IncomeVariability(
    val medianMinor: Long,
    val lowestMinor: Long,
    val highestMinor: Long,
    val monthsWithIncome: Int,
    val daysSinceLastIncome: Int?,
)

data class DashboardMetrics(
    val period: DateRange,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val feesMinor: Long,
    val refundsMinor: Long,
    val netMinor: Long,
    val categorySlices: List<CategorySlice>,
    val budget: BudgetProgress?,
    val upcoming: List<UpcomingCommitment>,
    val walletBalances: List<WalletBalance>,
    val walletTotalMinor: Long,
    val walletLiquidMinor: Long,
    val walletByType: List<WalletTypeTotal>,
    val goals: List<GoalProgress>,
    val recentTransactions: List<TransactionEntity>,
    val safeToSpendDailyMinor: Long?,
    val budgetRemainingMinor: Long?,
    val budgetUsedPercent: Int?,
    val forecastMinor: Long?,
    val expectedIncomeMinor: Long,
    val receivedIncomeMinor: Long,
    val previousIncomeMinor: Long,
    val previousExpenseMinor: Long,
    val previousPeriod: DateRange,
    val savingsContributionsMinor: Long,
    val savingsRatePercent: Int?,
    val essentialCoveragePercent: Int?,
    val incomeVariability: IncomeVariability?,
    val daysTracked: Int,
    val transactionCount: Int,
    val unbudgetedSpendMinor: Long,
    val monthlySpendToDateMinor: Long,
)
