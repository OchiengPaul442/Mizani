package com.mizani.money

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.WalletType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso

/** Drives the manual investment flow against the real encrypted repository. */
@RunWith(AndroidJUnit4::class)
class InvestmentEndToEndTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        TestState.reset()
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        runBlocking {
            app.container.awaitSqlCipherReady()
            app.container.financeRepository.createWorkspace("UG", "UGX", "Cash", WalletType.CASH, 0)
            app.container.settingsRepository.completeOnboarding("UG", "UGX")
            app.container.settingsRepository.recordLegalAcceptance(
                termsVersion = LegalDocuments.TERMS_VERSION,
                privacyVersion = LegalDocuments.PRIVACY_VERSION,
                acceptedAtMillis = System.currentTimeMillis(),
            )
            app.container.financeRepository.snapshot.first()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        TestState.reset()
    }

    @Test
    fun userCanCreateAndArchiveManualInvestmentWithoutLedgerActivity() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Investments").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("No investments yet").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Add investment").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Add investment").fetchSemanticsNodes().size >= 1
        }
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Unit trust")
        fields[1].performTextInput("Example provider")
        fields[2].performTextInput("100000")
        fields[3].performTextInput("105000")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Unit trust").fetchSemanticsNodes().isNotEmpty()
        }
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        val snapshot = runBlocking { app.container.financeRepository.snapshot.first() }
        assertEquals(1, snapshot.activeInvestments.size)
        assertEquals(0, snapshot.transactions.size)
        assertEquals(105_000L, snapshot.activeInvestments.single().currentValueMinor)

        compose.onNodeWithText("Unit trust").performClick()
        compose.onNodeWithText("Archive investment").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Archived").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(runBlocking { app.container.financeRepository.snapshot.first() }.activeInvestments.isEmpty())
    }
}
