package com.mizani.money

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.WalletType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Wallet amounts stay concealed until a fresh credential confirms the reveal. */
@RunWith(AndroidJUnit4::class)
class WalletBalancePrivacyEndToEndTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var app: MizaniApplication

    @Before
    fun setUp() {
        TestState.reset()
        app = ApplicationProvider.getApplicationContext()
        runBlocking {
            app.container.financeRepository.createWorkspace(
                countryCode = "UG",
                currencyCode = "UGX",
                walletName = "Private wallet label",
                walletType = WalletType.CASH,
                openingBalanceMinor = 275_000,
            )
            app.container.financeRepository.setOutlookDailyTarget(20_000)
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
        if (::scenario.isInitialized) scenario.close()
        TestState.reset()
    }

    @Test
    fun moneyOutlookMasksDerivedCoverageButKeepsDailyTargetVisible() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Your wallets").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Insights") and hasClickAction()).performClick()
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Money Outlook").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(compose.onAllNodesWithText("••••••").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithContentDescription("Estimated coverage hidden").assertExists()
        compose.onNodeWithContentDescription("Daily target UGX 20,000").assertExists()
        compose.onAllNodesWithText("UGX 275,000").assertCountEquals(0)
    }

    @Test
    fun homeWalletShortcutStaysMaskedUntilPinIsConfirmed() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Your wallets").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Private wallet label").assertExists()
        compose.onAllNodesWithContentDescription("Balance hidden").assertCountEquals(1)

        compose.onNodeWithContentDescription("Set up an app PIN to view wallet balances").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Set up app lock").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Set up app lock").performClick()
        enterPin("123456")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Confirm your PIN").fetchSemanticsNodes().isNotEmpty()
        }
        enterPin("123456")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("On · 6-digit PIN").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Change PIN").assertExists()

        scenario.recreate()
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Your wallets").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Terms of Service").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Balance hidden").assertCountEquals(1)

        compose.onNodeWithContentDescription(
            "Show wallet balances; confirm with your app PIN or enabled biometrics",
        ).performClick()
        compose.onNodeWithText("Confirm your identity").assertExists()
        enterPin("123456")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription("Balance UGX 275,000")
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("See all").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Wallets").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("Balance hidden").assertCountEquals(1)
        compose.onNodeWithText("Private wallet label").performClick()
        compose.onNodeWithText(
            "Opening balance hidden. Confirm with your PIN before viewing or changing it.",
        ).assertExists()
        compose.onAllNodesWithText("UGX 275,000").assertCountEquals(0)
    }

    private fun enterPin(value: String) {
        value.forEach { digit -> compose.onNodeWithText(digit.toString()).performClick() }
    }
}
