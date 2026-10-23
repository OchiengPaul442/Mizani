package com.mizani.money.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mizani.money.core.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace WHERE deletedAt IS NULL LIMIT 1")
    fun observe(): Flow<WorkspaceEntity?>

    @Query("SELECT * FROM workspace WHERE deletedAt IS NULL LIMIT 1")
    suspend fun get(): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspace")
    suspend fun clear()
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observe(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    suspend fun getAll(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)

    @Query("UPDATE wallets SET archived = :archived, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("UPDATE wallets SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM wallets")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    fun observe(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND deletedAt IS NULL ORDER BY sortOrder, createdAt")
    suspend fun getByType(type: CategoryType): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Query("UPDATE categories SET sortOrder = :order, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun updateSortOrder(id: String, order: Int, now: Long)

    @Query("UPDATE categories SET archived = :archived, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("UPDATE categories SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface IncomeSourceDao {
    @Query("SELECT * FROM income_sources WHERE deletedAt IS NULL ORDER BY name")
    fun observe(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE deletedAt IS NULL ORDER BY name")
    suspend fun getAll(): List<IncomeSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(source: IncomeSourceEntity)

    @Query("UPDATE income_sources SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM income_sources")
    suspend fun clear()
}

@Dao
interface TransactionDao {
    @Query(
        "SELECT * FROM transactions WHERE deletedAt IS NULL " +
            "ORDER BY localDateEpochDay DESC, occurredAt DESC",
    )
    fun observe(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE deletedAt IS NULL " +
            "ORDER BY localDateEpochDay DESC, occurredAt DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY localDateEpochDay DESC, occurredAt DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE transactions SET deletedAt = NULL, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE deletedAt IS NULL ORDER BY startEpochDay DESC")
    fun observe(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE deletedAt IS NULL ORDER BY startEpochDay DESC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("UPDATE budgets SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface BudgetLineDao {
    @Query("SELECT * FROM budget_lines")
    fun observe(): Flow<List<BudgetLineEntity>>

    @Query("SELECT * FROM budget_lines WHERE budgetId = :budgetId ORDER BY priority, id")
    fun observeForBudget(budgetId: String): Flow<List<BudgetLineEntity>>

    @Query("SELECT * FROM budget_lines")
    suspend fun getAll(): List<BudgetLineEntity>

    @Query("SELECT * FROM budget_lines WHERE budgetId = :budgetId")
    suspend fun getForBudget(budgetId: String): List<BudgetLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lines: List<BudgetLineEntity>)

    @Query("DELETE FROM budget_lines WHERE budgetId = :budgetId")
    suspend fun deleteForBudget(budgetId: String)

    @Query("DELETE FROM budget_lines")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY archived, priority, createdAt")
    fun observe(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE deletedAt IS NULL")
    suspend fun getAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("UPDATE goals SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface GoalContributionDao {
    @Query("SELECT * FROM goal_contributions ORDER BY localDateEpochDay DESC")
    fun observe(): Flow<List<GoalContributionEntity>>

    @Query("SELECT * FROM goal_contributions")
    suspend fun getAll(): List<GoalContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contribution: GoalContributionEntity)

    @Query("DELETE FROM goal_contributions")
    suspend fun clear()
}

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules WHERE deletedAt IS NULL ORDER BY archived, nextDueEpochDay")
    fun observe(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE deletedAt IS NULL ORDER BY archived, nextDueEpochDay")
    suspend fun getAll(): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET archived = :archived, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("UPDATE recurring_rules SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE deletedAt IS NULL ORDER BY archived, nextDueEpochDay")
    fun observe(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE deletedAt IS NULL")
    suspend fun getAll(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: String): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debt: DebtEntity)

    @Query("UPDATE debts SET deletedAt = :now, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("DELETE FROM debts")
    suspend fun clear()
}
