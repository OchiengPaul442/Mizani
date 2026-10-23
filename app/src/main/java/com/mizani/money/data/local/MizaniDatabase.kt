package com.mizani.money.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        WorkspaceEntity::class,
        WalletEntity::class,
        CategoryEntity::class,
        IncomeSourceEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        BudgetLineEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        RecurringRuleEntity::class,
        DebtEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MizaniDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetLineDao(): BudgetLineDao
    abstract fun goalDao(): GoalDao
    abstract fun goalContributionDao(): GoalContributionDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun debtDao(): DebtDao

    companion object {
        const val NAME = "mizani.db"

        fun build(context: Context, passphrase: ByteArray): MizaniDatabase {
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(context, MizaniDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .build()
        }
    }
}
