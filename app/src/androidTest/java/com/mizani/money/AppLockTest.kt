package com.mizani.money

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.WalletType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        TestState.reset()
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        runBlocking {
            app.container.financeRepository.createWorkspace(
                countryCode = "UG",
                currencyCode = "UGX",
                walletName = "Cash",
                walletType = WalletType.CASH,
                openingBalanceMinor = 0,
            )
            app.container.settingsRepository.completeOnboarding("UG", "UGX")
            app.container.settingsRepository.recordLegalAcceptance(
                LegalDocuments.TERMS_VERSION,
                LegalDocuments.PRIVACY_VERSION,
                System.currentTimeMillis(),
            )
            app.container.settingsRepository.setPin("123456")
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun wrongPinIsRejectedThenCorrectPinUnlocks() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Welcome back").fetchSemanticsNodes().isNotEmpty()
        }
        repeat(6) { index ->
            compose.onNodeWithText("0").performClick()
            if (index < 5) {
                compose.waitUntil(timeoutMillis = 2_000) {
                    compose.onAllNodesWithContentDescription("PIN entry, ${index + 1} of 6 digits entered")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Incorrect PIN. Try again.").fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(450)
        listOf("1", "2", "3", "4", "5", "6").forEachIndexed { index, digit ->
            compose.onNodeWithText(digit).performClick()
            if (index < 5) {
                compose.waitUntil(timeoutMillis = 2_000) {
                    compose.onAllNodesWithContentDescription("PIN entry, ${index + 1} of 6 digits entered")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
