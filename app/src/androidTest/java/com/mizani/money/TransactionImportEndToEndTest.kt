package com.mizani.money

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.assertIsEnabled
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the real import screen and verifies review-before-save category behavior. */
@RunWith(AndroidJUnit4::class)
class TransactionImportEndToEndTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var app: MizaniApplication
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        TestState.reset()
        app = ApplicationProvider.getApplicationContext()
        runBlocking {
            app.container.financeRepository.createWorkspace(
                countryCode = "UG",
                currencyCode = "UGX",
                walletName = "MTN MoMo",
                walletType = WalletType.MOBILE_MONEY,
                openingBalanceMinor = 50_000,
            )
            app.container.settingsRepository.completeOnboarding("UG", "UGX")
            app.container.settingsRepository.recordLegalAcceptance(
                LegalDocuments.TERMS_VERSION,
                LegalDocuments.PRIVACY_VERSION,
                System.currentTimeMillis(),
            )
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun incomingVirtualCardMessageRequiresCategoryThenSavesOnce() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Activity").performClick()
        compose.onNodeWithText("Add transaction").performClick()
        compose.onNodeWithText("Import a message").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Import transaction").fetchSemanticsNodes().isNotEmpty()
        }

        val message = "You have received UGX 10000 from MTN MOMO VIRTUAL CARD on 2026-10-28 13:45:16. fee:0. Reason: Card offload. New balance: UGX 37570. ID: 44604050982."
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(message)
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Review transaction").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Review imported transaction").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Suggested from MTN MoMo", substring = true).assertExists()
        compose.onNodeWithText("Confirm what this receipt represents", substring = true).assertExists()

        compose.onNodeWithText("Save income").performClick()
        compose.onNodeWithText("Choose an income category so we can classify this received money").assertExists()
        compose.onNodeWithText("Gifts & other income").performScrollTo().performClick()
        compose.onNodeWithText("Choose an income category so we can classify this received money").assertDoesNotExist()
        compose.onNodeWithText("Save income").assertIsEnabled()
        compose.onNodeWithText("Save income").performClick()

        // Saving shows a short success celebration before the import-review destination pops.
        // Wait for the destination to leave the composition so ActivityScenario teardown never
        // races Navigation Compose's back-stack lifecycle transition.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Review imported transaction").fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
        val snapshot = runBlocking { app.container.financeRepository.snapshot.first() }
        val transaction = snapshot.transactions.single()
        assertEquals(TransactionType.INCOME, transaction.type)
        assertEquals("mtn_momo", transaction.sourceProvider)
        assertEquals("44604050982", transaction.sourceReference)
        assertEquals(10_000L, transaction.amountMinor)
        assertNotNull(transaction.categoryId)
        assertEquals(CategoryType.INCOME, snapshot.category(transaction.categoryId!!)?.type)
    }
}
