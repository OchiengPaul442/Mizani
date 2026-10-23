package com.mizani.money

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.DefaultCategories
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.time.Periods
import com.mizani.money.data.repo.BudgetLineInput
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

        val passphrase = "correct-horse-battery"
        val backup = exporter.buildBackup(passphrase)
        assertTrue(backup.size > 100)
        assertTrue(!String(backup).startsWith("SQLite format 3"))

        val summary = exporter.inspectBackup(backup, passphrase)
        assertEquals(1, summary.transactionCount)
        assertEquals("UGX", summary.currencyCode)

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
    }
}
