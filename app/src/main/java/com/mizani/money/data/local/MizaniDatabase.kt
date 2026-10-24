package com.mizani.money.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN sourceAmountMinor INTEGER")
                database.execSQL("ALTER TABLE transactions ADD COLUMN sourceFeeMinor INTEGER")
                database.execSQL("ALTER TABLE transactions ADD COLUMN sourceCurrencyCode TEXT")
                database.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRate TEXT")
                database.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRateDate TEXT")
            }
        }

        fun build(context: Context, passphrase: ByteArray): MizaniDatabase {
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(context, MizaniDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
