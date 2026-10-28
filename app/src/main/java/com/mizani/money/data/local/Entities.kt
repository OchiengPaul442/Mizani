package com.mizani.money.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.IncomeSourceKind
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType

/**
 * Every synchronized entity carries a stable client-generated id, timestamps, a revision
 * and a soft-delete marker. Deletions are retained as tombstones so that a future sync
 * layer can propagate them; all UI and analytics queries exclude rows with [deletedAt].
 */

@Entity(tableName = "workspace")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val currencyCode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
    val outlookDailyTargetMinor: Long? = null,
)

@Entity(
    tableName = "wallets",
    indices = [Index("deletedAt"), Index("archived")],
)
data class WalletEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: WalletType,
    val openingBalanceMinor: Long,
    val currencyCode: String,
    val colorHex: String,
    val icon: String,
    val sortOrder: Int,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "categories",
    indices = [Index("deletedAt"), Index("type"), Index("archived")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val colorHex: String,
    val isDefault: Boolean,
    val archived: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "income_sources",
    indices = [Index("deletedAt")],
)
data class IncomeSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: IncomeSourceKind,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("deletedAt"),
        Index("localDateEpochDay"),
        Index("walletId"),
        Index("toWalletId"),
        Index("categoryId"),
        Index("linkedTransactionId"),
        Index(value = ["sourceProvider", "sourceReference"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    /** Positive for all types except ADJUSTMENT, which is signed. */
    val amountMinor: Long,
    val feeMinor: Long = 0L,
    val currencyCode: String,
    val walletId: String,
    val toWalletId: String? = null,
    val categoryId: String? = null,
    val feeCategoryId: String? = null,
    val incomeSourceId: String? = null,
    val linkedTransactionId: String? = null,
    val note: String = "",
    val payee: String = "",
    val localDateEpochDay: Long,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
    /** Original entered amount when a foreign currency was converted into the workspace currency. */
    val sourceAmountMinor: Long? = null,
    val sourceFeeMinor: Long? = null,
    val sourceCurrencyCode: String? = null,
    /** Decimal text, target major units per source major unit; retained exactly for auditability. */
    val exchangeRate: String? = null,
    val exchangeRateDate: String? = null,
    /** Normalized source identifiers used only for user-initiated import deduplication. */
    val sourceProvider: String? = null,
    val sourceReference: String? = null,
)

@Entity(
    tableName = "budgets",
    indices = [Index("startEpochDay"), Index("endEpochDay"), Index("deletedAt")],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val periodType: BudgetPeriodType,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "budget_lines",
    indices = [
        Index(value = ["budgetId", "categoryId"], unique = true),
        Index("categoryId"),
    ],
)
data class BudgetLineEntity(
    @PrimaryKey val id: String,
    val budgetId: String,
    val categoryId: String,
    val plannedMinor: Long,
    val fundedMinor: Long,
    val rolloverRule: RolloverRule = RolloverRule.NONE,
    val essential: Boolean,
    val priority: Int = 2,
    val note: String = "",
)

@Entity(
    tableName = "goals",
    indices = [Index("deletedAt"), Index("archived")],
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetMinor: Long,
    val allocatedMinor: Long,
    val targetEpochDay: Long? = null,
    val linkedWalletId: String? = null,
    val priority: Int = 2,
    val note: String = "",
    val icon: String = "savings",
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "goal_contributions",
    indices = [Index("goalId"), Index("localDateEpochDay")],
)
data class GoalContributionEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val amountMinor: Long,
    val localDateEpochDay: Long,
    val walletId: String? = null,
    val transferTransactionId: String? = null,
    val note: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "recurring_rules",
    indices = [Index("nextDueEpochDay"), Index("deletedAt"), Index("archived")],
)
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: RecurringType,
    val amountMinor: Long,
    val categoryId: String?,
    val walletId: String?,
    val frequency: Frequency,
    val nextDueEpochDay: Long,
    val autoPost: Boolean = false,
    val essential: Boolean = true,
    val note: String = "",
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "debts",
    indices = [Index("deletedAt"), Index("archived")],
)
data class DebtEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lender: String,
    val currentBalanceMinor: Long,
    val originalMinor: Long? = null,
    val periodicPaymentMinor: Long,
    val nextDueEpochDay: Long? = null,
    val interestRatePercent: Double? = null,
    val note: String = "",
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "commitments",
    indices = [Index("dueEpochDay"), Index("deletedAt"), Index("archived"), Index("categoryId")],
)
data class CommitmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetMinor: Long,
    /** Currency in which the obligation is denominated. */
    val currencyCode: String = "UGX",
    /** Target converted to the workspace currency using the saved reference rate. */
    val targetWorkspaceMinor: Long = targetMinor,
    /** Daily reference rate expressed as commitment currency major units to workspace units. */
    val exchangeRate: String? = null,
    val exchangeRateDate: String? = null,
    val dueEpochDay: Long,
    val categoryId: String?,
    /** 0 disables reminders; otherwise notify this many days before the due date. */
    val reminderDaysBefore: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "commitment_payments",
    indices = [
        Index("commitmentId"),
        // A ledger transaction can fund only one commitment at a time.
        Index(value = ["transactionId"], unique = true),
    ],
)
data class CommitmentPaymentEntity(
    @PrimaryKey val id: String,
    val commitmentId: String,
    val transactionId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1L,
)
