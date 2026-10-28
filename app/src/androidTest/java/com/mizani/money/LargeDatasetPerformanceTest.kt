package com.mizani.money

import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Exercises the encrypted ledger and high-volume UI paths with a realistic 10,000-row dataset. */
@RunWith(AndroidJUnit4::class)
class LargeDatasetPerformanceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun resetLocalState() {
        TestState.reset()
    }

    @After
    fun clearLocalState() {
        if (::scenario.isInitialized) scenario.close()
        TestState.reset()
    }

    @Test
    fun tenThousandTransactionsKeepCoreFlowsResponsive() {
        val timings = seedLargeLedger()

        val appStart = SystemClock.uptimeMillis()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(timeoutMillis = HOME_TIMEOUT_MS) {
            compose.onAllNodesWithText("Your wallets").fetchSemanticsNodes().isNotEmpty()
        }
        val homeReadyMs = SystemClock.uptimeMillis() - appStart

        val activityStart = SystemClock.uptimeMillis()
        compose.onNode(hasTextAndClick("Activity")).performClick()
        compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) {
            compose.onAllNodesWithText("$RECORD_COUNT transactions").fetchSemanticsNodes().isNotEmpty()
        }
        val activityReadyMs = SystemClock.uptimeMillis() - activityStart

        val searchStart = SystemClock.uptimeMillis()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Stress record 9999")
        compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) {
            compose.onAllNodesWithText("1 transactions").fetchSemanticsNodes().isNotEmpty()
        }
        val searchReadyMs = SystemClock.uptimeMillis() - searchStart

        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) {
            compose.onAllNodesWithText("$RECORD_COUNT transactions").fetchSemanticsNodes().isNotEmpty()
        }

        val insightsStart = SystemClock.uptimeMillis()
        compose.onNode(hasTextAndClick("Insights")).performClick()
        compose.waitUntil(timeoutMillis = SCREEN_TIMEOUT_MS) {
            compose.onAllNodesWithText("Six-month cash flow").fetchSemanticsNodes().isNotEmpty()
        }
        val insightsReadyMs = SystemClock.uptimeMillis() - insightsStart

        Log.i(
            TAG,
            "records=$RECORD_COUNT seedMs=${timings.seedMs} snapshotMs=${timings.snapshotMs} " +
                "exportMs=${timings.exportMs} homeReadyMs=$homeReadyMs activityReadyMs=$activityReadyMs " +
                "searchReadyMs=$searchReadyMs insightsReadyMs=$insightsReadyMs",
        )

        assertTrue("Seed took too long: ${timings.seedMs}ms", timings.seedMs < SEED_LIMIT_MS)
        assertTrue("Snapshot took too long: ${timings.snapshotMs}ms", timings.snapshotMs < SNAPSHOT_LIMIT_MS)
        assertTrue("Export took too long: ${timings.exportMs}ms", timings.exportMs < EXPORT_LIMIT_MS)
        assertTrue("Home took too long: ${homeReadyMs}ms", homeReadyMs < HOME_LIMIT_MS)
        assertTrue("Activity took too long: ${activityReadyMs}ms", activityReadyMs < SCREEN_LIMIT_MS)
        assertTrue("Search took too long: ${searchReadyMs}ms", searchReadyMs < SEARCH_LIMIT_MS)
        assertTrue("Insights took too long: ${insightsReadyMs}ms", insightsReadyMs < SCREEN_LIMIT_MS)
    }

    private fun seedLargeLedger(): Timings {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val seedStarted = SystemClock.uptimeMillis()
        val snapshot = runBlocking {
            app.container.awaitSqlCipherReady()
            app.container.financeRepository.createWorkspace("UG", "UGX", "Cash", WalletType.CASH, 0)
            app.container.settingsRepository.completeOnboarding("UG", "UGX")
            app.container.settingsRepository.recordLegalAcceptance(
                termsVersion = LegalDocuments.TERMS_VERSION,
                privacyVersion = LegalDocuments.PRIVACY_VERSION,
                acceptedAtMillis = now,
            )
            app.container.financeRepository.snapshot.first()
        }
        val wallet = snapshot.wallets.first()
        val expenseCategory = snapshot.categories.first { it.type == CategoryType.EXPENSE }.id
        val incomeCategory = snapshot.categories.first { it.type == CategoryType.INCOME }.id
        val records = (0 until RECORD_COUNT).map { index ->
            val type = if (index % 5 == 0) TransactionType.INCOME else TransactionType.EXPENSE
            val timestamp = now - index * 1_000L
            TransactionEntity(
                id = "stress-$index",
                type = type,
                amountMinor = 10_000L + (index % 31) * 1_000L,
                currencyCode = "UGX",
                walletId = wallet.id,
                categoryId = if (type == TransactionType.INCOME) incomeCategory else expenseCategory,
                note = "Stress record $index",
                payee = "Stress merchant ${index % 50}",
                localDateEpochDay = today.minusDays((index % 730).toLong()).toEpochDay(),
                occurredAt = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }
        runBlocking {
            withContext(Dispatchers.IO) {
                app.container.database.withTransaction {
                    records.forEach { app.container.database.transactionDao().upsert(it) }
                }
            }
        }
        val seedMs = SystemClock.uptimeMillis() - seedStarted

        val snapshotStarted = SystemClock.uptimeMillis()
        val loaded = runBlocking {
            app.container.financeRepository.snapshot.first { it.transactions.size == RECORD_COUNT }
        }
        val snapshotMs = SystemClock.uptimeMillis() - snapshotStarted
        assertEquals(RECORD_COUNT, loaded.transactions.size)

        val exportStarted = SystemClock.uptimeMillis()
        val exported = runBlocking { app.container.financeRepository.exportBundle() }
        val exportMs = SystemClock.uptimeMillis() - exportStarted
        assertEquals(RECORD_COUNT, exported.transactions.size)

        return Timings(seedMs = seedMs, snapshotMs = snapshotMs, exportMs = exportMs)
    }

    private fun hasTextAndClick(text: String) =
        androidx.compose.ui.test.hasText(text) and hasClickAction()

    private data class Timings(
        val seedMs: Long,
        val snapshotMs: Long,
        val exportMs: Long,
    )

    private companion object {
        const val RECORD_COUNT = 10_000
        const val TAG = "LargeDatasetPerformanceTest"
        const val SEED_LIMIT_MS = 30_000L
        const val SNAPSHOT_LIMIT_MS = 10_000L
        const val EXPORT_LIMIT_MS = 30_000L
        const val HOME_LIMIT_MS = 10_000L
        const val SEARCH_LIMIT_MS = 5_000L
        const val SCREEN_LIMIT_MS = 15_000L
        const val HOME_TIMEOUT_MS = 30_000L
        const val SCREEN_TIMEOUT_MS = 30_000L
    }
}
