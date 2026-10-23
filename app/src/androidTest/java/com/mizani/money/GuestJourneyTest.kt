package com.mizani.money

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end guest journey against the real encrypted database. The test clears only test-owned
 * local state before launch, then verifies the ledger through the repository after driving the UI.
 */
@RunWith(AndroidJUnit4::class)
class GuestJourneyTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun resetLocalState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("mizani.db")
        context.getSharedPreferences("mizani_database_key", Context.MODE_PRIVATE)
            .edit().clear().commit()
        File(context.filesDir, "datastore/mizani_settings.preferences_pb").delete()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() {
        scenario.close()
    }

    @Test
    fun guestCanOnboardAndRecordIncomeAndExpense() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Continue without account").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Continue without account").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Continue").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Continue").onFirst().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Continue").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Continue").onFirst().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Start using Mizani").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Start using Mizani").performClick()

        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Activity").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Add transaction").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("25000")
        Espresso.closeSoftKeyboard()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Save expense").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Save expense").performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Activity").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Add transaction").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Income").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Income").onFirst().performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("100000")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save income").performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Activity").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Activity").performClick()
        compose.onNodeWithText("Housing & rent").assertExists()

        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val bundle = runBlocking { app.container.financeRepository.exportBundle() }
        assertEquals(2, bundle.transactions.size)
        val expense = bundle.transactions.first { it.type == TransactionType.EXPENSE }
        val income = bundle.transactions.first { it.type == TransactionType.INCOME }
        assertEquals(25_000L, expense.amountMinor)
        assertEquals(100_000L, income.amountMinor)
        assertEquals(1, bundle.wallets.size)
        val balance = runBlocking { app.container.financeRepository.snapshot.first() }.walletBalances()[bundle.wallets.single().id]
        assertEquals(75_000L, balance)
        assertTrue(bundle.categories.isNotEmpty())
    }
}
