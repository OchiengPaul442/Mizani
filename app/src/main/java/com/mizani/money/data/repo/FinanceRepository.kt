package com.mizani.money.data.repo

import androidx.room.withTransaction
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.Ids
import com.mizani.money.core.model.IncomeSourceKind
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.importing.ImportIdentifiers
import com.mizani.money.core.money.MoneyLimits
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.CategoryEntity
import com.mizani.money.data.local.CommitmentEntity
import com.mizani.money.data.local.CommitmentPaymentEntity
import com.mizani.money.data.local.DebtEntity
import com.mizani.money.data.local.GoalContributionEntity
import com.mizani.money.data.local.GoalEntity
import com.mizani.money.data.local.IncomeSourceEntity
import com.mizani.money.data.local.MizaniDatabase
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.data.local.WorkspaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val MAX_COMMITMENT_NAME_LENGTH = 80
private const val MAX_COMMITMENT_REMINDER_DAYS = 30

/**
 * The single write boundary for financial data. Every mutation is validated and executed
 * inside a Room transaction, so a failed write can never leave a half-applied ledger state.
 * The local database is the canonical source for all UI reads.
 */
class FinanceRepository(private val database: MizaniDatabase) {

    private val workspaceDao = database.workspaceDao()
    private val walletDao = database.walletDao()
    private val categoryDao = database.categoryDao()
    private val incomeSourceDao = database.incomeSourceDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val budgetLineDao = database.budgetLineDao()
    private val goalDao = database.goalDao()
    private val contributionDao = database.goalContributionDao()
    private val recurringDao = database.recurringRuleDao()
    private val debtDao = database.debtDao()
    private val commitmentDao = database.commitmentDao()
    private val commitmentPaymentDao = database.commitmentPaymentDao()

    val snapshot: Flow<LedgerSnapshot> = combine(
        combine(
            workspaceDao.observe(),
            walletDao.observe(),
            categoryDao.observe(),
            incomeSourceDao.observe(),
            transactionDao.observe(),
        ) { workspace, wallets, categories, sources, transactions ->
            CorePart(workspace, wallets, categories, sources, transactions)
        },
        combine(
            budgetDao.observe(),
            budgetLineDao.observe(),
            goalDao.observe(),
            contributionDao.observe(),
        ) { budgets, lines, goals, contributions ->
            PlanPart(budgets, lines, goals, contributions)
        },
        combine(
            recurringDao.observe(),
            debtDao.observe(),
            commitmentDao.observe(),
            commitmentPaymentDao.observe(),
        ) { recurring, debts, commitments, payments ->
            OtherPart(recurring, debts, commitments, payments)
        },
    ) { core, plan, other ->
        LedgerSnapshot(
            workspace = core.workspace,
            wallets = core.wallets,
            categories = core.categories,
            incomeSources = core.incomeSources,
            transactions = core.transactions,
            budgets = plan.budgets,
            budgetLines = plan.lines,
            goals = plan.goals,
            contributions = plan.contributions,
            recurring = other.recurring,
            debts = other.debts,
            commitments = other.commitments,
            commitmentPayments = other.commitmentPayments,
        )
    }.distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    suspend fun currentWorkspace(): WorkspaceEntity? = workspaceDao.get()

    suspend fun hasWorkspace(): Boolean = workspaceDao.get() != null

    suspend fun createWorkspace(
        countryCode: String,
        currencyCode: String,
        walletName: String,
        walletType: WalletType,
        openingBalanceMinor: Long,
        name: String = "My money",
    ): String = database.withTransaction {
        workspaceDao.get()?.let { return@withTransaction it.id }
        val now = System.currentTimeMillis()
        val workspace = WorkspaceEntity(
            id = Ids.newId(),
            name = name,
            countryCode = countryCode,
            currencyCode = currencyCode,
            createdAt = now,
            updatedAt = now,
        )
        workspaceDao.insert(workspace)

        DefaultCategories.all.forEachIndexed { index, category ->
            categoryDao.upsert(
                CategoryEntity(
                    id = DefaultCategories.defaultEntityId("category", category.key),
                    name = category.name,
                    type = category.type,
                    icon = category.icon,
                    colorHex = category.colorHex,
                    isDefault = true,
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        walletDao.upsert(
            WalletEntity(
                id = Ids.newId(),
                name = walletName.ifBlank { "Cash" },
                type = walletType,
                openingBalanceMinor = openingBalanceMinor,
                currencyCode = currencyCode,
                colorHex = "#087F72",
                icon = "wallet",
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        workspace.id
    }

    suspend fun renameWorkspace(name: String) {
        database.withTransaction {
            val current = workspaceDao.get() ?: return@withTransaction
            workspaceDao.insert(current.copy(name = name.trim(), updatedAt = System.currentTimeMillis(), revision = current.revision + 1))
        }
    }

    // ---------------------------------------------------------------- wallets

    suspend fun addWallet(
        name: String,
        type: WalletType,
        openingBalanceMinor: Long,
        colorHex: String,
        icon: String = "wallet",
    ): String = database.withTransaction {
        val workspace = workspaceDao.get() ?: error("No workspace")
        val now = System.currentTimeMillis()
        val id = Ids.newId()
        val order = walletDao.getAll().size
        walletDao.upsert(
            WalletEntity(
                id = id,
                name = name.trim().ifBlank { type.label },
                type = type,
                openingBalanceMinor = openingBalanceMinor,
                currencyCode = workspace.currencyCode,
                colorHex = colorHex,
                icon = icon,
                sortOrder = order,
                createdAt = now,
                updatedAt = now,
            ),
        )
        id
    }

    suspend fun updateWallet(
        id: String,
        name: String,
        type: WalletType,
        openingBalanceMinor: Long,
        colorHex: String,
        icon: String,
    ) {
        database.withTransaction {
            val wallet = walletDao.getById(id) ?: error("Wallet not found")
            walletDao.upsert(
                wallet.copy(
                    name = name.trim().ifBlank { type.label },
                    type = type,
                    openingBalanceMinor = openingBalanceMinor,
                    colorHex = colorHex,
                    icon = icon,
                    updatedAt = System.currentTimeMillis(),
                    revision = wallet.revision + 1,
                ),
            )
        }
    }

    suspend fun setWalletArchived(id: String, archived: Boolean) {
        if (archived) {
            val active = walletDao.getAll().filter { !it.archived && it.deletedAt == null }
            require(active.size > 1) { "Keep at least one active wallet" }
        }
        walletDao.setArchived(id, archived, System.currentTimeMillis())
    }

    suspend fun deleteWallet(id: String) {
        database.withTransaction {
            val used = transactionDao.getAll().any {
                it.walletId == id || it.toWalletId == id
            }
            require(!used) { "This wallet has transactions. Archive it instead." }
            walletDao.softDelete(id, System.currentTimeMillis())
        }
    }

    // ------------------------------------------------------------- categories

    suspend fun addCategory(
        name: String,
        type: CategoryType,
        icon: String,
        colorHex: String,
    ): String = database.withTransaction {
        val now = System.currentTimeMillis()
        val id = Ids.newId()
        categoryDao.upsert(
            CategoryEntity(
                id = id,
                name = name.trim().ifBlank { "New category" },
                type = type,
                icon = icon,
                colorHex = colorHex,
                isDefault = false,
                sortOrder = categoryDao.getAll().size,
                createdAt = now,
                updatedAt = now,
            ),
        )
        id
    }

    suspend fun updateCategory(id: String, name: String, icon: String, colorHex: String) {
        database.withTransaction {
            val current = categoryDao.getById(id) ?: error("Category not found")
            categoryDao.upsert(
                current.copy(
                    name = name.trim().ifBlank { current.name },
                    icon = icon,
                    colorHex = colorHex,
                    updatedAt = System.currentTimeMillis(),
                    revision = current.revision + 1,
                ),
            )
        }
    }

    suspend fun setCategoryArchived(id: String, archived: Boolean) {
        if (archived) {
            val category = categoryDao.getById(id) ?: return
            val active = categoryDao.getAll().filter {
                it.type == category.type && !it.archived && it.deletedAt == null
            }
            require(active.size > 1) { "Keep at least one active category of this type" }
        }
        categoryDao.setArchived(id, archived, System.currentTimeMillis())
    }

    /**
     * Moves a category one step up (-1) or down (+1) within its type by swapping
     * sortOrder with the adjacent sibling. No-op at the bounds. Display-only ordering:
     * category ids, analytics and transactions are untouched.
     */
    suspend fun moveCategory(categoryId: String, direction: Int) {
        if (direction == 0) return
        database.withTransaction {
            val category = categoryDao.getById(categoryId) ?: return@withTransaction
            if (category.deletedAt != null) return@withTransaction
            val siblings = categoryDao.getByType(category.type).filter { !it.archived }
            val index = siblings.indexOfFirst { it.id == categoryId }
            val target = index + direction
            if (index < 0 || target !in siblings.indices) return@withTransaction
            val adjacent = siblings[target]
            val now = System.currentTimeMillis()
            categoryDao.updateSortOrder(category.id, adjacent.sortOrder, now)
            categoryDao.updateSortOrder(adjacent.id, category.sortOrder, now)
        }
    }

    // ----------------------------------------------------------- transactions

    suspend fun saveTransaction(draft: TransactionDraft): String = database.withTransaction {
        val workspace = workspaceDao.get() ?: error("No workspace yet")
        val existing = transactionDao.getById(draft.id)
        val wallet = walletDao.getById(draft.walletId) ?: error("Wallet not found")
        require(!wallet.archived) { "Wallet is archived" }
        require(draft.feeMinor >= 0) { "Fee cannot be negative" }
        require(wallet.currencyCode == workspace.currencyCode) { "Currency mismatch" }

        val sourceProvider = draft.sourceProvider?.let(ImportIdentifiers::normalizeProvider)?.takeIf { it.isNotEmpty() }
        val sourceReference = draft.sourceReference?.let(ImportIdentifiers::normalizeReference)?.takeIf { it.isNotEmpty() }
        require((sourceProvider == null) == (sourceReference == null)) {
            "Import source and reference must be provided together"
        }
        if (sourceProvider != null && sourceReference != null) {
            require(ImportIdentifiers.isValidProvider(sourceProvider)) {
                "Import source is invalid"
            }
            require(ImportIdentifiers.isValidReference(sourceReference)) {
                "Import reference is invalid"
            }
            val duplicate = transactionDao.findBySourceReference(sourceProvider, sourceReference)
            require(duplicate == null || duplicate.id == draft.id) {
                "This imported transaction reference is already saved"
            }
        }

        commitmentPaymentDao.getForTransaction(draft.id)?.let { payment ->
            require(draft.type == TransactionType.EXPENSE) {
                "A linked commitment payment must remain an expense"
            }
            val commitment = commitmentDao.getById(payment.commitmentId)
            if (commitment != null) {
                val paidWithoutThisExpense = commitmentPaidWorkspaceMinor(commitment.id, excludingTransactionId = draft.id)
                require(draft.amountMinor <= commitment.targetWorkspaceMinor - paidWithoutThisExpense) {
                    "This edit would pay more than the remaining commitment"
                }
            }
        }

        TransactionCurrencyRules.validate(
            workspaceCurrencyCode = workspace.currencyCode,
            type = draft.type,
            amountMinor = draft.amountMinor,
            feeMinor = draft.feeMinor,
            sourceAmountMinor = draft.sourceAmountMinor,
            sourceFeeMinor = draft.sourceFeeMinor,
            sourceCurrencyCode = draft.sourceCurrencyCode,
            exchangeRate = draft.exchangeRate,
            exchangeRateDate = draft.exchangeRateDate,
        )

        var categoryId = draft.categoryId
        var feeCategoryId = draft.feeCategoryId

        when (draft.type) {
            TransactionType.EXPENSE -> {
                require(draft.amountMinor > 0) { "Amount must be greater than zero" }
                categoryId = categoryId ?: categoryDao.getAll()
                    .firstOrNull { it.type == CategoryType.EXPENSE && it.name == "Other" }?.id
                    ?: categoryDao.getAll().firstOrNull { it.type == CategoryType.EXPENSE }?.id
            }
            TransactionType.INCOME -> {
                require(draft.amountMinor > 0) { "Amount must be greater than zero" }
                categoryId = categoryId ?: categoryDao.getAll()
                    .firstOrNull { it.type == CategoryType.INCOME && !it.archived }?.id
                if (draft.incomeSourceId != null) {
                    require(incomeSourceDao.getAll().any { it.id == draft.incomeSourceId }) {
                        "Income source not found"
                    }
                }
            }
            TransactionType.TRANSFER -> {
                require(draft.amountMinor > 0) { "Transfer amount must be greater than zero" }
                val destination = draft.toWalletId
                require(destination != null) { "Choose a destination wallet" }
                require(destination != draft.walletId) { "Choose a different destination wallet" }
                val destinationWallet = walletDao.getById(destination) ?: error("Destination wallet not found")
                require(!destinationWallet.archived) { "Destination wallet is archived" }
                require(destinationWallet.currencyCode == workspace.currencyCode) { "Currency mismatch" }
                categoryId = null
            }
            TransactionType.REFUND -> {
                require(draft.amountMinor > 0) { "Refund amount must be greater than zero" }
                val original = draft.linkedTransactionId?.let { transactionDao.getById(it) }
                if (original != null) {
                    require(original.type == TransactionType.EXPENSE) { "Refunds link to an expense" }
                    require(original.deletedAt == null) { "The original expense was deleted" }
                    categoryId = categoryId ?: original.categoryId
                }
                require(categoryId != null) { "Choose the category being refunded" }
            }
            TransactionType.ADJUSTMENT -> {
                require(draft.amountMinor != 0L) { "Adjustment cannot be zero" }
                categoryId = null
            }
        }

        // Validate category type matches the transaction type.
        when (draft.type) {
            TransactionType.EXPENSE, TransactionType.REFUND ->
                requireCategoryType(
                    categoryId,
                    CategoryType.EXPENSE,
                    if (draft.type == TransactionType.REFUND) "Refund" else "Expense",
                )
            TransactionType.INCOME -> requireCategoryType(categoryId, CategoryType.INCOME, "Income")
            TransactionType.TRANSFER, TransactionType.ADJUSTMENT -> Unit
        }

        if (draft.feeMinor > 0L) {
            feeCategoryId = feeCategoryId ?: DefaultCategories.feesId()
            requireCategoryType(feeCategoryId, CategoryType.EXPENSE, "Fee")
        }

        val now = System.currentTimeMillis()
        val occurredAt = if (draft.date == LocalDate.now()) {
            now
        } else {
            draft.date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val entity = TransactionEntity(
            id = existing?.id ?: draft.id,
            type = draft.type,
            amountMinor = draft.amountMinor,
            feeMinor = draft.feeMinor,
            currencyCode = workspace.currencyCode,
            walletId = draft.walletId,
            toWalletId = draft.toWalletId,
            categoryId = categoryId,
            feeCategoryId = feeCategoryId,
            incomeSourceId = draft.incomeSourceId,
            linkedTransactionId = draft.linkedTransactionId,
            note = draft.note.trim(),
            payee = draft.payee.trim(),
            localDateEpochDay = draft.date.toEpochDay(),
            occurredAt = occurredAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            revision = (existing?.revision ?: 0L) + 1L,
            sourceAmountMinor = draft.sourceAmountMinor,
            sourceFeeMinor = draft.sourceFeeMinor,
            sourceCurrencyCode = draft.sourceCurrencyCode,
            exchangeRate = draft.exchangeRate,
            exchangeRateDate = draft.exchangeRateDate,
            sourceProvider = sourceProvider,
            sourceReference = sourceReference,
        )
        transactionDao.upsert(entity)
        entity.id
    }

    suspend fun deleteTransaction(id: String) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            transactionDao.softDelete(id, now)
            commitmentPaymentDao.getForTransaction(id)?.let { commitmentPaymentDao.delete(it.id) }
        }
    }

    suspend fun restoreTransaction(id: String) {
        database.withTransaction {
            val transaction = transactionDao.getById(id) ?: return@withTransaction
            val payment = commitmentPaymentDao.getForTransaction(id)
            if (payment != null && transaction.type != TransactionType.EXPENSE) {
                error("A linked commitment payment must remain an expense")
            }
            transactionDao.restore(id, System.currentTimeMillis())
        }
    }

    // ----------------------------------------------------------------- budgets

    suspend fun saveBudget(
        budgetId: String?,
        name: String,
        periodType: BudgetPeriodType,
        start: LocalDate,
        end: LocalDate,
        lines: List<BudgetLineInput>,
    ): String = database.withTransaction {
        require(!end.isBefore(start)) { "End date must not be before start" }
        if (budgetId == null) {
            val startDay = start.toEpochDay()
            val endDay = end.toEpochDay()
            val duplicate = budgetDao.getAll().any { budget ->
                budget.deletedAt == null &&
                    budget.startEpochDay == startDay &&
                    budget.endEpochDay == endDay
            }
            require(!duplicate) { "A budget already exists for this period" }
        }
        val existing = budgetId?.let { budgetDao.getById(it) }
        val now = System.currentTimeMillis()
        val id = existing?.id ?: Ids.newId()
        val budget = BudgetEntity(
            id = id,
            name = name.trim().ifBlank { "${periodType.label} · ${DateText.month(start)}" },
            periodType = periodType,
            startEpochDay = start.toEpochDay(),
            endEpochDay = end.toEpochDay(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            revision = (existing?.revision ?: 0L) + 1L,
        )
        budgetDao.upsert(budget)
        budgetLineDao.deleteForBudget(id)
        val clean = lines
            .filter { it.plannedMinor >= 0 && it.fundedMinor >= 0 }
            .distinctBy { it.categoryId }
        clean.forEach { requireCategoryType(it.categoryId, CategoryType.EXPENSE, "Budget line") }
        budgetLineDao.upsertAll(
            clean.map { line ->
                BudgetLineEntity(
                    id = Ids.newId(),
                    budgetId = id,
                    categoryId = line.categoryId,
                    plannedMinor = line.plannedMinor,
                    fundedMinor = line.fundedMinor,
                    rolloverRule = line.rolloverRule,
                    essential = line.essential,
                    priority = line.priority.coerceIn(1, 3),
                    note = line.note.trim(),
                )
            },
        )
        id
    }

    suspend fun deleteBudget(id: String) {
        budgetDao.softDelete(id, System.currentTimeMillis())
    }

    // -------------------------------------------------------------- repeating

    suspend fun saveRecurring(id: String?, draft: RecurringDraft): String = database.withTransaction {
        require(draft.amountMinor > 0) { "Amount must be greater than zero" }
        val expectedCategoryType = if (draft.type == RecurringType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        requireCategoryType(draft.categoryId, expectedCategoryType, draft.type.label)
        if (draft.walletId != null) {
            val wallet = walletDao.getById(draft.walletId)
            require(wallet != null) { "Wallet not found" }
            require(!wallet.archived) { "Wallet is archived" }
        }
        val existing = id?.let { recurringDao.getAll().firstOrNull { rule -> rule.id == it } }
        val now = System.currentTimeMillis()
        val entity = RecurringRuleEntity(
            id = existing?.id ?: Ids.newId(),
            name = draft.name.trim().ifBlank { draft.type.label },
            type = draft.type,
            amountMinor = draft.amountMinor,
            categoryId = draft.categoryId,
            walletId = draft.walletId,
            frequency = draft.frequency,
            nextDueEpochDay = draft.nextDue.toEpochDay(),
            autoPost = false,
            essential = draft.essential,
            note = draft.note.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            revision = (existing?.revision ?: 0L) + 1L,
        )
        recurringDao.upsert(entity)
        entity.id
    }

    suspend fun setRecurringArchived(id: String, archived: Boolean) {
        recurringDao.setArchived(id, archived, System.currentTimeMillis())
    }

    suspend fun markRecurringPaid(id: String, date: LocalDate = LocalDate.now()): String =
        database.withTransaction {
            val rule = recurringDao.getAll().firstOrNull { it.id == id } ?: error("Rule not found")
            val workspace = workspaceDao.get() ?: error("No workspace")
            val fallbackWallet = walletDao.getAll().firstOrNull { !it.archived && it.deletedAt == null }
                ?: error("Create a wallet first")
            val walletId = rule.walletId?.takeIf { walletDao.getById(it) != null } ?: fallbackWallet.id
            val type = if (rule.type == RecurringType.BILL) TransactionType.EXPENSE else TransactionType.INCOME
            val now = System.currentTimeMillis()
            val transactionId = Ids.newId()
            transactionDao.upsert(
                TransactionEntity(
                    id = transactionId,
                    type = type,
                    amountMinor = rule.amountMinor,
                    currencyCode = workspace.currencyCode,
                    walletId = walletId,
                    categoryId = rule.categoryId,
                    note = rule.name,
                    payee = rule.name,
                    localDateEpochDay = date.toEpochDay(),
                    occurredAt = now,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            // Every user-confirmed receipt/payment advances exactly one scheduled occurrence.
            // This also handles early recording without leaving the same cycle scheduled again;
            // overdue cycles remain visible for the user to reconcile one by one.
            val next = Periods.nextOccurrence(LocalDate.ofEpochDay(rule.nextDueEpochDay), rule.frequency)
            recurringDao.upsert(
                rule.copy(
                    nextDueEpochDay = next.toEpochDay(),
                    updatedAt = now,
                    revision = rule.revision + 1,
                ),
            )
            transactionId
        }

    // ------------------------------------------------------------------- goals

    suspend fun saveGoal(
        goalId: String?,
        name: String,
        targetMinor: Long,
        targetDate: LocalDate?,
        linkedWalletId: String?,
        priority: Int,
        note: String,
        icon: String = "savings",
    ): String = database.withTransaction {
        require(targetMinor > 0) { "Target must be greater than zero" }
        if (linkedWalletId != null) {
            val wallet = walletDao.getById(linkedWalletId)
            require(wallet != null && wallet.deletedAt == null) { "Linked wallet is missing" }
            require(!wallet.archived) { "Linked wallet is archived" }
        }
        val existing = goalId?.let { goalDao.getById(it) }
        val now = System.currentTimeMillis()
        val entity = GoalEntity(
            id = existing?.id ?: Ids.newId(),
            name = name.trim().ifBlank { "Savings goal" },
            targetMinor = targetMinor,
            allocatedMinor = existing?.allocatedMinor ?: 0L,
            targetEpochDay = targetDate?.toEpochDay(),
            linkedWalletId = linkedWalletId,
            priority = priority.coerceIn(1, 3),
            note = note.trim(),
            icon = icon,
            archived = existing?.archived ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            revision = (existing?.revision ?: 0L) + 1L,
        )
        goalDao.upsert(entity)
        entity.id
    }

    suspend fun contributeToGoal(
        goalId: String,
        amountMinor: Long,
        fromWalletId: String?,
        date: LocalDate = LocalDate.now(),
        note: String = "",
    ) {
        require(amountMinor > 0) { "Contribution must be greater than zero" }
        database.withTransaction {
            val goal = goalDao.getById(goalId) ?: error("Goal not found")
            if (goal.deletedAt != null || goal.archived) { error("Goal is no longer available") }
            if (fromWalletId != null) {
                val fromWallet = walletDao.getById(fromWalletId) ?: error("Wallet not found")
                require(!fromWallet.archived) { "Wallet is archived" }
            }
            val workspace = workspaceDao.get() ?: error("No workspace")
            val now = System.currentTimeMillis()
            var transferId: String? = null
            val linked = goal.linkedWalletId
            if (linked != null && fromWalletId != null && fromWalletId != linked) {
                val linkedWallet = walletDao.getById(linked) ?: error("Linked savings wallet not found")
                require(!linkedWallet.archived) { "Linked savings wallet is archived" }
                transferId = Ids.newId()
                transactionDao.upsert(
                    TransactionEntity(
                        id = transferId,
                        type = TransactionType.TRANSFER,
                        amountMinor = amountMinor,
                        currencyCode = workspace.currencyCode,
                        walletId = fromWalletId,
                        toWalletId = linked,
                        note = "Savings: ${goal.name}",
                        payee = goal.name,
                        localDateEpochDay = date.toEpochDay(),
                        occurredAt = now,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            contributionDao.upsert(
                GoalContributionEntity(
                    id = Ids.newId(),
                    goalId = goalId,
                    amountMinor = amountMinor,
                    localDateEpochDay = date.toEpochDay(),
                    walletId = fromWalletId,
                    transferTransactionId = transferId,
                    note = note.trim(),
                    createdAt = now,
                ),
            )
            goalDao.upsert(
                goal.copy(
                    allocatedMinor = goal.allocatedMinor + amountMinor,
                    updatedAt = now,
                    revision = goal.revision + 1,
                ),
            )
        }
    }

    suspend fun setGoalArchived(id: String, archived: Boolean) {
        database.withTransaction {
            val goal = goalDao.getById(id) ?: return@withTransaction
            goalDao.upsert(
                goal.copy(
                    archived = archived,
                    updatedAt = System.currentTimeMillis(),
                    revision = goal.revision + 1,
                ),
            )
        }
    }

    // ------------------------------------------------------------------- debts

    suspend fun saveDebt(
        debtId: String?,
        name: String,
        lender: String,
        balanceMinor: Long,
        originalMinor: Long?,
        periodicPaymentMinor: Long,
        nextDue: LocalDate?,
        interestRatePercent: Double?,
        note: String,
    ): String = database.withTransaction {
        require(balanceMinor >= 0) { "Balance cannot be negative" }
        val existing = debtId?.let { debtDao.getById(it) }
        val now = System.currentTimeMillis()
        val entity = DebtEntity(
            id = existing?.id ?: Ids.newId(),
            name = name.trim().ifBlank { "Debt" },
            lender = lender.trim(),
            currentBalanceMinor = balanceMinor,
            originalMinor = originalMinor,
            periodicPaymentMinor = periodicPaymentMinor.coerceAtLeast(0),
            nextDueEpochDay = nextDue?.toEpochDay(),
            interestRatePercent = interestRatePercent,
            note = note.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            revision = (existing?.revision ?: 0L) + 1L,
        )
        debtDao.upsert(entity)
        entity.id
    }

    suspend fun recordDebtPayment(
        debtId: String,
        walletId: String,
        amountMinor: Long,
        date: LocalDate = LocalDate.now(),
    ) {
        require(amountMinor > 0) { "Payment must be greater than zero" }
        database.withTransaction {
            val debt = debtDao.getById(debtId) ?: error("Debt not found")
            if (debt.deletedAt != null || debt.archived) { error("Debt is no longer available") }
            val wallet = walletDao.getById(walletId) ?: error("Wallet not found")
            require(!wallet.archived) { "Wallet is archived" }
            val workspace = workspaceDao.get() ?: error("No workspace")
            val category = categoryDao.getById(DefaultCategories.defaultEntityId("category", "debt"))
                ?: categoryDao.getAll().firstOrNull { it.type == CategoryType.EXPENSE }
            val now = System.currentTimeMillis()
            transactionDao.upsert(
                TransactionEntity(
                    id = Ids.newId(),
                    type = TransactionType.EXPENSE,
                    amountMinor = amountMinor,
                    currencyCode = workspace.currencyCode,
                    walletId = walletId,
                    categoryId = category?.id,
                    note = "Debt payment: ${debt.name}",
                    payee = debt.lender.ifBlank { debt.name },
                    localDateEpochDay = date.toEpochDay(),
                    occurredAt = now,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            debtDao.upsert(
                debt.copy(
                    currentBalanceMinor = (debt.currentBalanceMinor - amountMinor).coerceAtLeast(0),
                    nextDueEpochDay = debt.nextDueEpochDay?.let {
                        Periods.addMonthsClamped(LocalDate.ofEpochDay(it), 1).toEpochDay()
                    },
                    updatedAt = now,
                    revision = debt.revision + 1,
                ),
            )
        }
    }

    suspend fun setDebtArchived(id: String, archived: Boolean) {
        database.withTransaction {
            val debt = debtDao.getById(id) ?: return@withTransaction
            debtDao.upsert(
                debt.copy(
                    archived = archived,
                    updatedAt = System.currentTimeMillis(),
                    revision = debt.revision + 1,
                ),
            )
        }
    }

    // ----------------------------------------------------------- commitments

    suspend fun saveCommitment(id: String?, draft: CommitmentDraft): String = database.withTransaction {
        val workspace = workspaceDao.get() ?: error("No workspace")
        val name = draft.name.trim()
        require(name.isNotEmpty()) { "Enter a commitment name" }
        require(name.length <= MAX_COMMITMENT_NAME_LENGTH) { "Commitment name is too long" }
        require(draft.reminderDaysBefore in 0..MAX_COMMITMENT_REMINDER_DAYS) {
            "Choose a reminder between 0 and $MAX_COMMITMENT_REMINDER_DAYS days before the due date"
        }
        requireCategoryType(draft.categoryId, CategoryType.EXPENSE, "Commitment")

        val existing = id?.let { commitmentDao.getById(it) }
        // Older callers that edit a commitment without the new optional currency fields keep the
        // existing denomination and saved rate instead of silently resetting it to the workspace.
        val preserveExistingCurrency = draft.currencyCode == null && existing != null
        val currencyCode = (draft.currencyCode ?: existing?.currencyCode ?: workspace.currencyCode).uppercase()
        val targetWorkspaceMinor = draft.targetWorkspaceMinor ?: if (preserveExistingCurrency) {
            existing!!.targetWorkspaceMinor
        } else {
            draft.targetMinor
        }
        val exchangeRate = draft.exchangeRate ?: if (preserveExistingCurrency) existing?.exchangeRate else null
        val exchangeRateDate = draft.exchangeRateDate?.toString()
            ?: if (preserveExistingCurrency) existing?.exchangeRateDate else null
        CommitmentCurrencyRules.validate(
            workspaceCurrencyCode = workspace.currencyCode,
            currencyCode = currencyCode,
            targetMinor = draft.targetMinor,
            targetWorkspaceMinor = targetWorkspaceMinor,
            exchangeRate = exchangeRate,
            exchangeRateDate = exchangeRateDate,
        )

        val paidWorkspace = existing?.let { commitmentPaidWorkspaceMinor(it.id) } ?: 0L
        require(targetWorkspaceMinor >= paidWorkspace) { "Target cannot be less than payments already linked" }
        val now = System.currentTimeMillis()
        val commitment = CommitmentEntity(
            id = existing?.id ?: Ids.newId(),
            name = name,
            targetMinor = draft.targetMinor,
            currencyCode = currencyCode,
            targetWorkspaceMinor = targetWorkspaceMinor,
            exchangeRate = exchangeRate,
            exchangeRateDate = exchangeRateDate,
            dueEpochDay = draft.dueDate.toEpochDay(),
            categoryId = draft.categoryId,
            reminderDaysBefore = draft.reminderDaysBefore,
            archived = existing?.archived ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            revision = (existing?.revision ?: 0L) + 1L,
        )
        commitmentDao.upsert(commitment)
        commitment.id
    }

    suspend fun setCommitmentArchived(id: String, archived: Boolean) {
        database.withTransaction {
            val commitment = commitmentDao.getById(id) ?: return@withTransaction
            commitmentDao.upsert(
                commitment.copy(
                    archived = archived,
                    updatedAt = System.currentTimeMillis(),
                    revision = commitment.revision + 1L,
                ),
            )
        }
    }

    suspend fun deleteCommitment(id: String) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            commitmentPaymentDao.deleteForCommitment(id)
            commitmentDao.softDelete(id, now)
        }
    }

    /** Links an existing expense; this never creates or duplicates a ledger transaction. */
    suspend fun linkExpenseToCommitment(commitmentId: String, transactionId: String): String =
        database.withTransaction {
            val commitment = commitmentDao.getById(commitmentId) ?: error("Commitment not found")
            require(!commitment.archived) { "Commitment is archived" }
            val transaction = transactionDao.getById(transactionId) ?: error("Transaction not found")
            require(transaction.deletedAt == null) { "Deleted transactions cannot be linked" }
            require(transaction.type == TransactionType.EXPENSE) { "Only expenses can be linked as payments" }
            val workspace = workspaceDao.get() ?: error("No workspace")
            require(transaction.currencyCode == workspace.currencyCode) { "Currency mismatch" }
            require(commitmentPaymentDao.getForTransaction(transactionId) == null) {
                "This expense is already linked to a commitment"
            }
            val netPayment = netCommitmentPaymentMinor(transaction)
            require(netPayment > 0L) { "This expense has no remaining value after refunds" }
            val alreadyPaid = commitmentPaidWorkspaceMinor(commitmentId)
            require(netPayment <= commitment.targetWorkspaceMinor - alreadyPaid) {
                "Payment is greater than the remaining commitment"
            }
            val now = System.currentTimeMillis()
            val payment = CommitmentPaymentEntity(
                id = Ids.newId(),
                commitmentId = commitmentId,
                transactionId = transactionId,
                createdAt = now,
                updatedAt = now,
            )
            commitmentPaymentDao.insert(payment)
            payment.id
        }

    /** Unlinks the association only; the original expense remains in the ledger. */
    suspend fun unlinkCommitmentPayment(paymentId: String) {
        database.withTransaction { commitmentPaymentDao.delete(paymentId) }
    }

    suspend fun hasImportedReference(provider: String, reference: String): Boolean {
        val normalizedProvider = ImportIdentifiers.normalizeProvider(provider)
        val normalizedReference = ImportIdentifiers.normalizeReference(reference)
        if (normalizedProvider.isEmpty() || normalizedReference.isEmpty()) return false
        return transactionDao.findBySourceReference(normalizedProvider, normalizedReference) != null
    }

    suspend fun setOutlookDailyTarget(targetMinor: Long?) {
        require(targetMinor == null || targetMinor in 1..MoneyLimits.MAX_MINOR_AMOUNT) {
            "Daily target must be greater than zero"
        }
        database.withTransaction {
            val workspace = workspaceDao.get() ?: error("No workspace")
            workspaceDao.updateOutlookDailyTarget(workspace.id, targetMinor, System.currentTimeMillis())
        }
    }

    private suspend fun commitmentPaidWorkspaceMinor(
        commitmentId: String,
        excludingTransactionId: String? = null,
    ): Long {
        return commitmentPaymentDao.getForCommitment(commitmentId).fold(0L) { total, payment ->
            if (payment.transactionId == excludingTransactionId) return@fold total
            val transaction = transactionDao.getById(payment.transactionId)
            if (transaction == null || transaction.deletedAt != null || transaction.type != TransactionType.EXPENSE) {
                total
            } else {
                Math.addExact(total, netCommitmentPaymentMinor(transaction))
            }
        }
    }

    private suspend fun netCommitmentPaymentMinor(transaction: TransactionEntity): Long =
        (transaction.amountMinor - transactionDao.refundedAmountFor(transaction.id)).coerceAtLeast(0L)

    // ----------------------------------------------------------- income sources

    suspend fun saveIncomeSource(id: String?, name: String, kind: IncomeSourceKind): String =
        database.withTransaction {
            val existing = id?.let { incomeSourceDao.getAll().firstOrNull { source -> source.id == it } }
            val now = System.currentTimeMillis()
            val entity = IncomeSourceEntity(
                id = existing?.id ?: Ids.newId(),
                name = name.trim().ifBlank { kind.label },
                kind = kind,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                revision = (existing?.revision ?: 0L) + 1L,
            )
            incomeSourceDao.upsert(entity)
            entity.id
        }

    // --------------------------------------------------------------- backup I/O

    suspend fun exportBundle(): RestoreBundle = database.withTransaction {
        val workspace = workspaceDao.get() ?: error("No workspace")
        RestoreBundle(
            workspace = workspace,
            wallets = walletDao.getAll(),
            categories = categoryDao.getAll(),
            incomeSources = incomeSourceDao.getAll(),
            transactions = transactionDao.getAll(),
            budgets = budgetDao.getAll(),
            budgetLines = budgetLineDao.getAll(),
            goals = goalDao.getAll(),
            contributions = contributionDao.getAll(),
            recurring = recurringDao.getAll(),
            debts = debtDao.getAll(),
            commitments = commitmentDao.getAll(),
            commitmentPayments = commitmentPaymentDao.getAll(),
        )
    }

    suspend fun restoreBundle(bundle: RestoreBundle) {
        BackupValidator.validate(bundle)
        database.withTransaction {
            commitmentPaymentDao.clear()
            commitmentDao.clear()
            transactionDao.clear()
            contributionDao.clear()
            budgetLineDao.clear()
            budgetDao.clear()
            goalDao.clear()
            recurringDao.clear()
            debtDao.clear()
            incomeSourceDao.clear()
            walletDao.clear()
            categoryDao.clear()
            workspaceDao.clear()

            workspaceDao.insert(bundle.workspace)
            bundle.categories.forEach { categoryDao.upsert(it) }
            bundle.incomeSources.forEach { incomeSourceDao.upsert(it) }
            bundle.wallets.forEach { walletDao.upsert(it) }
            bundle.transactions.forEach { transactionDao.upsert(it) }
            bundle.budgets.forEach { budgetDao.upsert(it) }
            bundle.budgetLines.forEach { budgetLineDao.upsertAll(listOf(it)) }
            bundle.goals.forEach { goalDao.upsert(it) }
            bundle.contributions.forEach { contributionDao.upsert(it) }
            bundle.recurring.forEach { recurringDao.upsert(it) }
            bundle.debts.forEach { debtDao.upsert(it) }
            bundle.commitments.forEach { commitmentDao.upsert(it) }
            bundle.commitmentPayments.forEach { commitmentPaymentDao.insert(it) }
        }
    }

    /** Ensures a referenced category exists, is not tombstoned, and has the expected type. */
    private suspend fun requireCategoryType(categoryId: String?, expected: CategoryType, label: String) {
        if (categoryId == null) return
        val category = categoryDao.getById(categoryId)
        require(category != null) { "$label category not found" }
        require(category.deletedAt == null) { "$label category is no longer available" }
        require(category.type == expected) { "$label must use an ${expected.label.lowercase()} category" }
    }

    suspend fun clearAllFinancialData() {
        database.withTransaction {
            commitmentPaymentDao.clear()
            commitmentDao.clear()
            transactionDao.clear()
            contributionDao.clear()
            budgetLineDao.clear()
            budgetDao.clear()
            goalDao.clear()
            recurringDao.clear()
            debtDao.clear()
            incomeSourceDao.clear()
            walletDao.clear()
            categoryDao.clear()
            workspaceDao.clear()
        }
    }
}
