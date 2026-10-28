package com.mizani.money

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.WalletType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Prevents detail-route back transitions from leaving the More screen visually blocked. */
@RunWith(AndroidJUnit4::class)
class NavigationBackRegressionTest {

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
    fun returningFromWalletsRestoresInteractiveMoreScreen() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Wallets").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Wallets").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Manage money").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Categories") and hasClickAction()).performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Categories").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Manage money").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Settings") and hasClickAction()).assertExists()
    }
}
