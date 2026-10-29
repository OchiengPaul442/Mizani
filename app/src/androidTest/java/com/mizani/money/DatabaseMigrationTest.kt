package com.mizani.money

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mizani.money.data.local.MizaniDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MizaniDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun version2MigrationPreservesLedgerAndAddsNewOptionalFeatures() {
        helper.createDatabase(DATABASE_V2, 2).apply {
            seedWorkspaceAndWallet("v2")
            execSQL(
                "INSERT INTO transactions (id,type,amountMinor,feeMinor,currencyCode,walletId,toWalletId," +
                    "categoryId,feeCategoryId,incomeSourceId,linkedTransactionId,note,payee,localDateEpochDay," +
                    "occurredAt,createdAt,updatedAt,revision,deletedAt,sourceAmountMinor,sourceFeeMinor," +
                    "sourceCurrencyCode,exchangeRate,exchangeRateDate) VALUES " +
                    "('txn-v2','EXPENSE',25000,0,'UGX','wallet-v2',NULL,NULL,NULL,NULL,NULL,'note','shop'," +
                    "20000,1,1,1,1,NULL,NULL,NULL,NULL,NULL,NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE_V2, 3, true, MizaniDatabase.MIGRATION_2_3).use { database ->
            database.query("SELECT amountMinor, sourceProvider, sourceReference FROM transactions WHERE id = 'txn-v2'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(25_000L, cursor.getLong(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
            }
            database.query("SELECT outlookDailyTargetMinor FROM workspace WHERE id = 'workspace-v2'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            database.query("SELECT COUNT(*) FROM commitments").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun version1ThroughVersion3MigrationPreservesExistingTransaction() {
        helper.createDatabase(DATABASE_V1, 1).apply {
            seedWorkspaceAndWallet("v1")
            execSQL(
                "INSERT INTO transactions (id,type,amountMinor,feeMinor,currencyCode,walletId,toWalletId," +
                    "categoryId,feeCategoryId,incomeSourceId,linkedTransactionId,note,payee,localDateEpochDay," +
                    "occurredAt,createdAt,updatedAt,revision,deletedAt) VALUES " +
                    "('txn-v1','EXPENSE',40000,100,'UGX','wallet-v1',NULL,NULL,NULL,NULL,NULL,'old','market'," +
                    "20000,1,1,1,1,NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_V1,
            3,
            true,
            MizaniDatabase.MIGRATION_1_2,
            MizaniDatabase.MIGRATION_2_3,
        ).use { database ->
            database.query("SELECT amountMinor, feeMinor, note FROM transactions WHERE id = 'txn-v1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(40_000L, cursor.getLong(0))
                assertEquals(100L, cursor.getLong(1))
                assertEquals("old", cursor.getString(2))
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_sourceProvider_sourceReference'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertFalse(cursor.isNull(0))
            }
        }
    }

    @Test
    fun version3ToVersion4BackfillsCommitmentCurrencyFromWorkspace() {
        helper.createDatabase(DATABASE_V3, 3).apply {
            seedWorkspaceAndWallet("v3", "KES")
            execSQL(
                "INSERT INTO commitments (id,name,targetMinor,dueEpochDay,categoryId,reminderDaysBefore," +
                    "archived,createdAt,updatedAt,revision,deletedAt) VALUES " +
                    "('commitment-v3','School fees',100000,20000,NULL,0,0,1,1,1,NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE_V3, 4, true, MizaniDatabase.MIGRATION_3_4).use { database ->
            database.query(
                "SELECT currencyCode,targetWorkspaceMinor,exchangeRate,exchangeRateDate " +
                    "FROM commitments WHERE id = 'commitment-v3'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("KES", cursor.getString(0))
                assertEquals(100_000L, cursor.getLong(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
            }
        }
    }

    @Test
    fun version4ToVersion5AddsInvestmentsWithoutChangingExistingLedgerRows() {
        helper.createDatabase(DATABASE_V4, 4).apply {
            seedWorkspaceAndWallet("v4")
            execSQL(
                "INSERT INTO transactions (id,type,amountMinor,feeMinor,currencyCode,walletId,toWalletId," +
                    "categoryId,feeCategoryId,incomeSourceId,linkedTransactionId,note,payee,localDateEpochDay," +
                    "occurredAt,createdAt,updatedAt,revision,deletedAt,sourceAmountMinor,sourceFeeMinor," +
                    "sourceCurrencyCode,exchangeRate,exchangeRateDate,sourceProvider,sourceReference) VALUES " +
                    "('txn-v4','EXPENSE',75000,250,'UGX','wallet-v4',NULL,NULL,NULL,NULL,NULL,'old','market'," +
                    "20000,1,1,1,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE_V4, 5, true, MizaniDatabase.MIGRATION_4_5).use { database ->
            database.query("SELECT amountMinor, feeMinor FROM transactions WHERE id = 'txn-v4'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(75_000L, cursor.getLong(0))
                assertEquals(250L, cursor.getLong(1))
            }
            database.query("SELECT COUNT(*) FROM investment_holdings").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' " +
                    "AND name = 'index_investment_holdings_provider'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    private fun SupportSQLiteDatabase.seedWorkspaceAndWallet(suffix: String, currency: String = "UGX") {
        execSQL(
            "INSERT INTO workspace (id,name,countryCode,currencyCode,createdAt,updatedAt,revision,deletedAt) " +
                "VALUES ('workspace-$suffix','My money','UG','$currency',1,1,1,NULL)",
        )
        execSQL(
            "INSERT INTO wallets (id,name,type,openingBalanceMinor,currencyCode,colorHex,icon,sortOrder,archived," +
                "createdAt,updatedAt,revision,deletedAt) VALUES " +
                "('wallet-$suffix','Cash','CASH',100000,'$currency','#087F72','wallet',0,0,1,1,1,NULL)",
        )
    }

    private companion object {
        const val DATABASE_V1 = "mizani_migration_v1_test.db"
        const val DATABASE_V2 = "mizani_migration_v2_test.db"
        const val DATABASE_V3 = "mizani_migration_v3_test.db"
        const val DATABASE_V4 = "mizani_migration_v4_test.db"
    }
}
