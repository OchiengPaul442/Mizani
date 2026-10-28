package com.mizani.money

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.DarkThemeStyle
import com.mizani.money.core.model.ThemeMode
import com.mizani.money.core.model.WalletType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end coverage for saved appearance choices and sensitive export/backup confirmation. */
@RunWith(AndroidJUnit4::class)
class AppearanceAndSecurityEndToEndTest {

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
                openingBalanceMinor = 0,
            )
            app.container.settingsRepository.completeOnboarding("UG", "UGX")
            app.container.settingsRepository.recordLegalAcceptance(
                LegalDocuments.TERMS_VERSION,
                LegalDocuments.PRIVACY_VERSION,
                System.currentTimeMillis(),
            )
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForMainNavigation()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test
    fun appearanceOptionsAreAppliedAndPersistAfterActivityRestart() {
        openSettings()

        ThemeMode.entries.forEach { mode ->
            compose.onNodeWithText(mode.label).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                runBlocking { app.container.settingsRepository.settings.first().themeMode == mode }
            }
        }
        compose.onNodeWithText("Choose color").performClick()
        compose.onNodeWithText("Hex color").performTextReplacement("#6B55D4")
        compose.onNodeWithText("Apply color").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                app.container.settingsRepository.settings.first().customAccentColorHex == "#6B55D4"
            }
        }
        DarkThemeStyle.entries.forEach { style ->
            compose.onNodeWithText(style.label).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                runBlocking { app.container.settingsRepository.settings.first().darkThemeStyle == style }
            }
        }

        compose.onNodeWithText(ThemeMode.DARK.label).performClick()
        compose.onNodeWithText(DarkThemeStyle.WARM_SLATE.label).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val settings = app.container.settingsRepository.settings.first()
                settings.themeMode == ThemeMode.DARK &&
                    settings.customAccentColorHex == "#6B55D4" &&
                    settings.darkThemeStyle == DarkThemeStyle.WARM_SLATE
            }
        }

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForMainNavigation()
        openSettings()
        compose.onNodeWithText(ThemeMode.DARK.label).assertIsSelected()
        compose.onNodeWithText("#6B55D4").assertExists()
        check(compose.onAllNodesWithText("Ocean").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText(DarkThemeStyle.WARM_SLATE.label).assertIsSelected()
    }

    @Test
    fun appAndBudgetSharingActionsAreAbsent() {
        compose.onAllNodesWithText("More").onLast().performClick()
        compose.onNodeWithText("About and privacy").performClick()
        compose.onNodeWithText("Share Mizani").assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onAllNodesWithText("More").onLast().performClick()
        compose.onNodeWithText("Export or back up data").performClick()
        compose.onNodeWithText("Share budgets").assertDoesNotExist()
        compose.onNodeWithText("No saved budgets are available to share yet.").assertDoesNotExist()
    }

    @Test
    fun financialExportsAndBackupsRequireFreshIdentityConfirmation() {
        openDataTools()

        compose.onNodeWithText("Download transactions (CSV)").performClick()
        compose.onNodeWithText("Set up a PIN first").assertExists()
        compose.onNodeWithText("CSV export is not encrypted").assertDoesNotExist()
        compose.onNodeWithText("Set up PIN").performClick()
        compose.onNodeWithText("Create a 6-digit PIN").assertExists()
        enterPin("123456")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Confirm your PIN").fetchSemanticsNodes().isNotEmpty()
        }
        enterPin("123456")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("PIN set up", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("CSV export is not encrypted").assertDoesNotExist()

        compose.onNodeWithText("Download transactions (CSV)").performScrollTo().performClick()
        compose.onNodeWithText("Confirm your identity").assertExists()
        enterPin("123456")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("CSV export is not encrypted").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("Download Excel workbook").performScrollTo().performClick()
        compose.onNodeWithText("Confirm your identity").assertExists()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("Create encrypted backup").performScrollTo().performClick()
        compose.onNodeWithText("Confirm your identity").assertExists()
        compose.onNodeWithText("Cancel").performClick()
    }

    private fun openSettings() {
        compose.onAllNodesWithText("More").onLast().performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Appearance").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openDataTools() {
        compose.onAllNodesWithText("More").onLast().performClick()
        compose.onNodeWithText("Export or back up data").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Data export & backup").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun enterPin(value: String) {
        value.forEach { digit -> compose.onNodeWithText(digit.toString()).performClick() }
    }

    private fun waitForMainNavigation() {
        compose.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                compose.onAllNodesWithText("More").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }
}
