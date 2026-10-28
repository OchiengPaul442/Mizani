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
        CommitmentEntity::class,
        CommitmentPaymentEntity::class,
    ],
    version = 3,
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
    abstract fun commitmentDao(): CommitmentDao
    abstract fun commitmentPaymentDao(): CommitmentPaymentDao

    companion object {
        const val NAME = "mizani.db"

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceAmountMinor INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceFeeMinor INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceCurrencyCode TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRate TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRateDate TEXT")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace ADD COLUMN outlookDailyTargetMinor INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceProvider TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceReference TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_sourceProvider_sourceReference` " +
                        "ON `transactions` (`sourceProvider`, `sourceReference`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `commitments` (" +
                        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `targetMinor` INTEGER NOT NULL, " +
                        "`dueEpochDay` INTEGER NOT NULL, `categoryId` TEXT, `reminderDaysBefore` INTEGER NOT NULL, " +
                        "`archived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "`revision` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commitments_dueEpochDay` ON `commitments` (`dueEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commitments_deletedAt` ON `commitments` (`deletedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commitments_archived` ON `commitments` (`archived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commitments_categoryId` ON `commitments` (`categoryId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `commitment_payments` (" +
                        "`id` TEXT NOT NULL, `commitmentId` TEXT NOT NULL, `transactionId` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_commitment_payments_commitmentId` " +
                        "ON `commitment_payments` (`commitmentId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_commitment_payments_transactionId` " +
                        "ON `commitment_payments` (`transactionId`)",
                )
            }
        }

        fun build(context: Context, passphrase: ByteArray): MizaniDatabase {
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(context, MizaniDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
