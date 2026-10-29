package com.mizani.money

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.InvestmentAssetType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.time.Periods
import com.mizani.money.data.repo.BudgetLineInput
import com.mizani.money.data.repo.CommitmentDraft
import com.mizani.money.data.repo.InvestmentHoldingDraft
import com.mizani.money.data.repo.TransactionDraft
import com.mizani.money.domain.export.BackupException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {

    @Before
    fun reset() {
        TestState.reset()
    }

    @Test
    fun encryptedBackupAndCsvRoundTrip() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val repo = app.container.financeRepository
        val settings = app.container.settingsRepository
        val exporter = app.container.exportManager
        val today = LocalDate.now()
        val month = Periods.monthOf(today)

        repo.createWorkspace("UG", "UGX", "Cash", WalletType.CASH, 0)
        settings.completeOnboarding("UG", "UGX")
        val wallet = repo.snapshot.first().wallets.first()
        val food = DefaultCategories.defaultEntityId("category", "food")

        repo.saveTransaction(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                amountMinor = 25_000,
                walletId = wallet.id,
                categoryId = food,
                payee = "Market",
                note = "Backup fixture",
                date = today,
                sourceProvider = "mtn_momo",
                sourceReference = "BACKUP123",
            ),
        )
        val transactionId = repo.snapshot.first().transactions.single().id
        val commitmentId = repo.saveCommitment(
            id = null,
            draft = CommitmentDraft(
                name = "School fees",
                targetMinor = 50_000,
                dueDate = today.plusDays(10),
                categoryId = food,
                reminderDaysBefore = 7,
            ),
        )
        repo.linkExpenseToCommitment(commitmentId, transactionId)
        repo.setOutlookDailyTarget(5_000)
        repo.saveInvestmentHolding(
            null,
            InvestmentHoldingDraft(
                name = "Treasury fund",
                provider = "Example provider",
                assetType = InvestmentAssetType.FUNDS,
                costBasisMinor = 100_000,
                currentValueMinor = 105_000,
                valuationDate = today,
            ),
        )
        repo.saveBudget(
            budgetId = null,
            name = "Backup budget",
            periodType = BudgetPeriodType.MONTHLY,
            start = month.start,
            end = month.end,
            lines = listOf(BudgetLineInput(food, 300_000, 200_000, true)),
        )

        val csv = exporter.buildCsv()
        assertTrue(csv.contains("25000"))
        assertTrue(csv.contains("Market"))
        assertTrue(csv.contains("cash", ignoreCase = true))
        assertTrue(csv.contains("mtn_momo"))
        assertTrue(csv.contains("BACKUP123"))

        val passphrase = "correct-horse-battery"
        val backup = exporter.buildBackup(passphrase)
        assertTrue(backup.size > 100)
        assertTrue(!String(backup).startsWith("SQLite format 3"))

        val summary = exporter.inspectBackup(backup, passphrase)
        assertEquals(1, summary.transactionCount)
        assertEquals("UGX", summary.currencyCode)
        assertEquals(1, summary.investmentCount)

        try {
            exporter.inspectBackup(backup, "wrong-passphrase")
            fail("Wrong passphrase must not decrypt")
        } catch (_: BackupException) {
            // expected
        }

        repo.clearAllFinancialData()
        assertNull(repo.currentWorkspace())

        exporter.restore(backup, passphrase)
        val restored = repo.exportBundle()
        assertNotNull(restored.workspace)
        assertEquals(1, restored.transactions.size)
        assertEquals(1, restored.budgets.size)
        assertEquals(25_000L, restored.transactions.single().amountMinor)
        assertTrue(restored.categories.isNotEmpty())
        assertEquals("mtn_momo", restored.transactions.single().sourceProvider)
        assertEquals("BACKUP123", restored.transactions.single().sourceReference)
        assertEquals(1, restored.commitments.size)
        assertEquals(1, restored.commitmentPayments.size)
        assertEquals(1, restored.investments.size)
        assertEquals("Treasury fund", restored.investments.single().name)
        assertEquals(5_000L, restored.workspace.outlookDailyTargetMinor)
    }

    @Test
    fun version1EncryptedBackupWithoutNewFieldsRemainsReadable() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val exporter = app.container.exportManager
        val json = org.json.JSONObject().apply {
            put("format", "mizani-backup")
            put("version", 1)
            put(
                "workspace",
                org.json.JSONObject().apply {
                    put("id", "legacy-workspace")
                    put("name", "My money")
                    put("countryCode", "UG")
                    put("currencyCode", "UGX")
                    put("createdAt", 1)
                    put("updatedAt", 1)
                    put("revision", 1)
                    put("deletedAt", org.json.JSONObject.NULL)
                },
            )
            put(
                "wallets",
                org.json.JSONArray().put(
                    org.json.JSONObject().apply {
                        put("id", "legacy-wallet")
                        put("name", "Cash")
                        put("type", "CASH")
                        put("openingBalanceMinor", 0)
                        put("currencyCode", "UGX")
                        put("colorHex", "#087F72")
                        put("icon", "wallet")
                        put("sortOrder", 0)
                        put("archived", false)
                        put("createdAt", 1)
                        put("updatedAt", 1)
                        put("revision", 1)
                        put("deletedAt", org.json.JSONObject.NULL)
                    },
                ),
            )
            listOf(
                "categories", "incomeSources", "transactions", "budgets", "budgetLines", "goals",
                "contributions", "recurring", "debts",
            ).forEach { put(it, org.json.JSONArray()) }
        }.toString().toByteArray(Charsets.UTF_8)
        val backup = com.mizani.money.domain.export.BackupCrypto.encrypt(json, "legacy-backup-passphrase")

        val summary = exporter.inspectBackup(backup, "legacy-backup-passphrase")

        assertEquals(0, summary.transactionCount)
    }
}
