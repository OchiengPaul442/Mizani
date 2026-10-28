package com.mizani.money.data.repo

import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.Ids
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.CategoryEntity
import com.mizani.money.data.local.CommitmentEntity
import com.mizani.money.data.local.CommitmentPaymentEntity
import com.mizani.money.data.local.DebtEntity
import com.mizani.money.data.local.GoalContributionEntity
import com.mizani.money.data.local.GoalEntity
import com.mizani.money.data.local.IncomeSourceEntity
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.data.local.WorkspaceEntity
import java.time.LocalDate

data class LedgerSnapshot(
    val workspace: WorkspaceEntity? = null,
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val incomeSources: List<IncomeSourceEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val budgetLines: List<BudgetLineEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val contributions: List<GoalContributionEntity> = emptyList(),
    val recurring: List<RecurringRuleEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val commitments: List<CommitmentEntity> = emptyList(),
    val commitmentPayments: List<CommitmentPaymentEntity> = emptyList(),
) {
    val currencyCode: String get() = workspace?.currencyCode ?: "UGX"

    val activeWallets: List<WalletEntity>
        get() = wallets.filter { !it.archived && it.deletedAt == null }

    val activeCategories: List<CategoryEntity>
        get() = categories.filter { !it.archived && it.deletedAt == null }

    fun wallet(id: String?): WalletEntity? = id?.let { key -> wallets.firstOrNull { it.id == key } }

    fun category(id: String?): CategoryEntity? = id?.let { key -> categories.firstOrNull { it.id == key } }

    fun incomeSource(id: String?): IncomeSourceEntity? =
        id?.let { key -> incomeSources.firstOrNull { it.id == key } }

    fun walletName(id: String?): String = wallet(id)?.name ?: "Wallet"

    fun categoryName(id: String?): String = category(id)?.name ?: "Uncategorised"

    fun incomeSourceName(id: String?): String = incomeSource(id)?.name ?: "Income"

    fun budgetLinesFor(budgetId: String?): List<BudgetLineEntity> =
        budgetId?.let { id -> budgetLines.filter { it.budgetId == id } } ?: emptyList()

    /** Balances derived from a single canonical ledger; transfers preserve total net worth. */
    fun walletBalances(): Map<String, Long> {
        val balances = wallets.associate { it.id to it.openingBalanceMinor }.toMutableMap()
        for (transaction in transactions) {
            if (transaction.deletedAt != null) continue
            when (transaction.type) {
                TransactionType.INCOME -> add(balances, transaction.walletId, transaction.amountMinor)
                TransactionType.EXPENSE -> add(
                    balances,
                    transaction.walletId,
                    -(transaction.amountMinor + transaction.feeMinor),
                )
                TransactionType.TRANSFER -> {
                    add(
                        balances,
                        transaction.walletId,
                        -(transaction.amountMinor + transaction.feeMinor),
                    )
                    transaction.toWalletId?.let { add(balances, it, transaction.amountMinor) }
                }
                TransactionType.REFUND -> add(
                    balances,
                    transaction.walletId,
                    transaction.amountMinor - transaction.feeMinor,
                )
                TransactionType.ADJUSTMENT -> add(balances, transaction.walletId, transaction.amountMinor)
            }
        }
        return balances
    }

    fun liquidBalance(): Long {
        val balances = walletBalances()
        return wallets
            .filter { it.type != WalletType.DEBT && !it.archived && it.deletedAt == null }
            .sumOf { balances[it.id] ?: 0L }
    }

    /** Paid totals are derived from linked, active expense transactions; links never duplicate ledger rows. */
    fun commitmentProgress(): List<CommitmentProgress> {
        val transactionsById = transactions.associateBy { it.id }
        val refundsByExpenseId = transactions.asSequence()
            .filter { it.type == TransactionType.REFUND && it.linkedTransactionId != null }
            .groupingBy { it.linkedTransactionId!! }
            .fold(0L) { total, refund -> Math.addExact(total, refund.amountMinor) }
        val paymentsByCommitment = commitmentPayments.asSequence()
            .groupBy { it.commitmentId }

        return commitments.asSequence()
            .filter { it.deletedAt == null }
            .map { commitment ->
                val paid = paymentsByCommitment[commitment.id].orEmpty().fold(0L) { total, payment ->
                    val transaction = transactionsById[payment.transactionId]
                    if (transaction == null || transaction.type != TransactionType.EXPENSE) {
                        total
                    } else {
                        val netPayment = (transaction.amountMinor - (refundsByExpenseId[transaction.id] ?: 0L))
                            .coerceAtLeast(0L)
                        Math.addExact(total, netPayment)
                    }
                }
                CommitmentProgress(
                    commitment = commitment,
                    paidMinor = paid,
                    remainingMinor = (commitment.targetMinor - paid).coerceAtLeast(0L),
                )
            }
            .toList()
    }

    private fun add(map: MutableMap<String, Long>, key: String, delta: Long) {
        map[key] = (map[key] ?: 0L) + delta
    }
}

data class TransactionDraft(
    val id: String = Ids.newId(),
    val type: TransactionType,
    val amountMinor: Long,
    val feeMinor: Long = 0L,
    val walletId: String,
    val toWalletId: String? = null,
    val categoryId: String? = null,
    val feeCategoryId: String? = null,
    val incomeSourceId: String? = null,
    val linkedTransactionId: String? = null,
    val note: String = "",
    val payee: String = "",
    val date: LocalDate = LocalDate.now(),
    val sourceAmountMinor: Long? = null,
    val sourceFeeMinor: Long? = null,
    val sourceCurrencyCode: String? = null,
    val exchangeRate: String? = null,
    val exchangeRateDate: String? = null,
    val sourceProvider: String? = null,
    val sourceReference: String? = null,
)

data class CommitmentDraft(
    val name: String,
    val targetMinor: Long,
    val dueDate: LocalDate,
    val categoryId: String? = null,
    val reminderDaysBefore: Int = 0,
)

data class CommitmentProgress(
    val commitment: CommitmentEntity,
    val paidMinor: Long,
    val remainingMinor: Long,
)

data class BudgetLineInput(
    val categoryId: String,
    val plannedMinor: Long,
    val fundedMinor: Long = 0L,
    val essential: Boolean,
    val rolloverRule: RolloverRule = RolloverRule.NONE,
    val priority: Int = 2,
    val note: String = "",
)

data class RecurringDraft(
    val name: String,
    val type: RecurringType,
    val amountMinor: Long,
    val categoryId: String?,
    val walletId: String?,
    val frequency: Frequency,
    val nextDue: LocalDate,
    val essential: Boolean = true,
    val note: String = "",
)

data class RestoreBundle(
    val workspace: WorkspaceEntity,
    val wallets: List<WalletEntity>,
    val categories: List<CategoryEntity>,
    val incomeSources: List<IncomeSourceEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val budgetLines: List<BudgetLineEntity>,
    val goals: List<GoalEntity>,
    val contributions: List<GoalContributionEntity>,
    val recurring: List<RecurringRuleEntity>,
    val debts: List<DebtEntity>,
    val commitments: List<CommitmentEntity> = emptyList(),
    val commitmentPayments: List<CommitmentPaymentEntity> = emptyList(),
)

internal data class CorePart(
    val workspace: WorkspaceEntity?,
    val wallets: List<WalletEntity>,
    val categories: List<CategoryEntity>,
    val incomeSources: List<IncomeSourceEntity>,
    val transactions: List<TransactionEntity>,
)

internal data class PlanPart(
    val budgets: List<BudgetEntity>,
    val lines: List<BudgetLineEntity>,
    val goals: List<GoalEntity>,
    val contributions: List<GoalContributionEntity>,
)

internal data class OtherPart(
    val recurring: List<RecurringRuleEntity>,
    val debts: List<DebtEntity>,
    val commitments: List<CommitmentEntity>,
    val commitmentPayments: List<CommitmentPaymentEntity>,
)
