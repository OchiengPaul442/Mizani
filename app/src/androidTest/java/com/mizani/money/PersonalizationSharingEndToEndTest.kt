package com.mizani.money

import android.accessibilityservice.AccessibilityService
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.ColorTheme
import com.mizani.money.core.model.DarkThemeStyle
import com.mizani.money.core.model.ThemeMode
import com.mizani.money.core.model.WalletType
import com.mizani.money.data.repo.BudgetLineInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import java.time.LocalDate

/** End-to-end coverage for saved appearance choices and the app/budget Android share sheets. */
@RunWith(AndroidJUnit4::class)
class PersonalizationSharingEndToEndTest {

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

            val snapshot = app.container.financeRepository.snapshot.first()
            val housing = snapshot.categories.single {
                it.type == CategoryType.EXPENSE && it.name == "Housing & rent"
            }
            val start = LocalDate.now().withDayOfMonth(1)
            app.container.financeRepository.saveBudget(
                budgetId = null,
                name = "Rent plan",
                periodType = BudgetPeriodType.MONTHLY,
                start = start,
                end = start.plusMonths(1).minusDays(1),
                lines = listOf(BudgetLineInput(categoryId = housing.id, plannedMinor = 125_000, essential = true)),
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
        ColorTheme.entries.forEach { theme ->
            compose.onNodeWithText(theme.label).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                runBlocking { app.container.settingsRepository.settings.first().colorTheme == theme }
            }
        }
        DarkThemeStyle.entries.forEach { style ->
            compose.onNodeWithText(style.label).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                runBlocking { app.container.settingsRepository.settings.first().darkThemeStyle == style }
            }
        }

        compose.onNodeWithText(ThemeMode.DARK.label).performClick()
        compose.onNodeWithText(ColorTheme.OCEAN.label).performClick()
        compose.onNodeWithText(DarkThemeStyle.OLED_BLACK.label).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val settings = app.container.settingsRepository.settings.first()
                settings.themeMode == ThemeMode.DARK &&
                    settings.colorTheme == ColorTheme.OCEAN &&
                    settings.darkThemeStyle == DarkThemeStyle.OLED_BLACK
            }
        }

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForMainNavigation()
        openSettings()
        compose.onNodeWithText(ThemeMode.DARK.label).assertIsSelected()
        compose.onNodeWithText(ColorTheme.OCEAN.label).assertIsSelected()
        compose.onNodeWithText(DarkThemeStyle.OLED_BLACK.label).assertIsSelected()
    }

    @Test
    fun budgetSharingShowsPlanOnlyPreviewBeforeOpeningAndroidShareSheet() {
        openDataTools()
        compose.onNodeWithText("Share budgets").performScrollTo().assertExists()
        compose.onNodeWithText("Rent plan").performScrollTo().performClick()

        compose.onNodeWithText("Review before sharing").assertExists()
        compose.onNodeWithText("This text goes only to the app you choose", substring = true).assertExists()
        compose.onNodeWithText("Mizani budget: Rent plan", substring = true).assertExists()
        compose.onNodeWithText("Housing & rent", substring = true).assertExists()
        compose.onNodeWithText("Private wallet label").assertDoesNotExist()

        compose.onNodeWithText("Share").performClick()
        dismissAndroidShareSheet("Data export & backup")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Share budgets").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun financialExportsBackupAndBudgetShareRequireFreshIdentityConfirmation() {
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

        compose.onNodeWithText("Rent plan").performScrollTo().performClick()
        compose.onNodeWithText("Review before sharing").assertExists()
        compose.onNodeWithText("Share").performClick()
        compose.onNodeWithText("Confirm your identity").assertExists()
        compose.onNodeWithText("Review before sharing").assertDoesNotExist()
        enterPin("123456")
        compose.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                compose.onAllNodesWithText("Confirm your identity").fetchSemanticsNodes().isEmpty()
            }.getOrDefault(true)
        }
        awaitAndroidShareSheetOpened()
    }

    @Test
    fun appSharingOpensAndroidShareSheet() {
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("About and privacy").performClick()
        compose.onNodeWithText("Share Mizani").performClick()

        dismissAndroidShareSheet("About & privacy")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("About & privacy").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSettings() {
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Appearance").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openDataTools() {
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Export or back up data").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Data export & backup").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun enterPin(value: String) {
        value.forEach { digit -> compose.onNodeWithText(digit.toString()).performClick() }
    }

    private fun dismissAndroidShareSheet(returningScreen: String) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var relevantActivities = ""
        fun isShareSheetOpen(): Boolean {
            val activityDump = ParcelFileDescriptor.AutoCloseInputStream(
                uiAutomation.executeShellCommand("dumpsys activity activities"),
            ).bufferedReader().use { it.readText() }
            val windowDump = ParcelFileDescriptor.AutoCloseInputStream(
                uiAutomation.executeShellCommand("dumpsys window windows"),
            ).bufferedReader().use { it.readText() }
            relevantActivities = activityDump.lineSequence()
                .filter {
                    it.contains("topResumedActivity") ||
                        it.contains("mResumedActivity") ||
                        it.contains("com.android.intentresolver") ||
                        it.contains("ChooserActivity") ||
                        it.contains("ResolverActivity") ||
                        it.contains("com.mizani.money")
                }
                .toList()
                .takeLast(20)
                .joinToString("\n")
            return activityDump.contains("ChooserActivity") ||
                activityDump.contains("ResolverActivity") ||
                activityDump.contains("com.android.intentresolver") ||
                windowDump.contains("ChooserActivity") ||
                windowDump.contains("com.android.intentresolver")
        }
        val deadline = SystemClock.uptimeMillis() + 30_000
        var shareSheetOpen = false
        while (!shareSheetOpen && SystemClock.uptimeMillis() < deadline) {
            shareSheetOpen = isShareSheetOpen()
            if (!shareSheetOpen) Thread.sleep(100)
        }
        if (!shareSheetOpen) {
            val confirmationDialogs = runCatching {
                compose.onAllNodesWithText("Confirm your identity").fetchSemanticsNodes().size
            }.getOrDefault(-1)
            val budgetPreviews = runCatching {
                compose.onAllNodesWithText("Review before sharing").fetchSemanticsNodes().size
            }.getOrDefault(-1)
            val pinErrors = listOf(
                "Incorrect PIN. Try again.",
                "Could not verify the PIN. Try again.",
                "Too many attempts. Please wait a moment, then try again.",
            ).associateWith { text ->
                runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().size }.getOrDefault(-1)
            }
            val fullPinEntries = runCatching {
                compose.onAllNodesWithContentDescription("PIN entry, 6 of 6 digits entered")
                    .fetchSemanticsNodes().size
            }.getOrDefault(-1)
            val verifyingPin = runCatching {
                compose.onAllNodesWithContentDescription("Verifying PIN").fetchSemanticsNodes().size
            }.getOrDefault(-1)
            val checkingBackoff = runCatching {
                compose.onAllNodesWithContentDescription("Checking PIN retry status").fetchSemanticsNodes().size
            }.getOrDefault(-1)
            assertTrue(
                "The Android share sheet was not opened. Confirmation dialogs: $confirmationDialogs; " +
                    "budget previews: $budgetPreviews; PIN messages: $pinErrors; full PIN entries: $fullPinEntries; " +
                    "PIN verifications running: $verifyingPin; retry check running: $checkingBackoff. " +
                    "Activities:\n$relevantActivities",
                shareSheetOpen,
            )
        }
        assertTrue(uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(returningScreen).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitAndroidShareSheetOpened() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var activityDump = ""
        var windowDump = ""
        fun isShareSheetOpen(): Boolean {
            activityDump = ParcelFileDescriptor.AutoCloseInputStream(
                uiAutomation.executeShellCommand("dumpsys activity activities"),
            ).bufferedReader().use { it.readText() }
            windowDump = ParcelFileDescriptor.AutoCloseInputStream(
                uiAutomation.executeShellCommand("dumpsys window windows"),
            ).bufferedReader().use { it.readText() }
            return activityDump.contains("ChooserActivity") ||
                activityDump.contains("ResolverActivity") ||
                activityDump.contains("com.android.intentresolver") ||
                windowDump.contains("ChooserActivity") ||
                windowDump.contains("com.android.intentresolver")
        }
        val deadline = SystemClock.uptimeMillis() + 45_000
        var shareSheetOpen = false
        while (!shareSheetOpen && SystemClock.uptimeMillis() < deadline) {
            shareSheetOpen = isShareSheetOpen()
            if (!shareSheetOpen) Thread.sleep(100)
        }
        if (!shareSheetOpen) {
            val pinProgress = runCatching {
                compose.onAllNodesWithContentDescription("PIN entry, 6 of 6 digits entered")
                    .fetchSemanticsNodes().size
            }.getOrDefault(-1)
            val verification = runCatching {
                compose.onAllNodesWithContentDescription("Verifying PIN").fetchSemanticsNodes().size
            }.getOrDefault(-1)
            val retryCheck = runCatching {
                compose.onAllNodesWithContentDescription("Checking PIN retry status").fetchSemanticsNodes().size
            }.getOrDefault(-1)
            assertTrue(
                "PIN-authorized budget sharing did not open Android's share sheet " +
                    "(full PIN entry=$pinProgress, verification=$verification, retry check=$retryCheck, " +
                    "activity state=${activityDump.lineSequence().filter { it.contains("topResumedActivity") || it.contains("ChooserActivity") || it.contains("MainActivity") }.toList().takeLast(8).joinToString(" | ")}, " +
                    "window state=${windowDump.lineSequence().filter { it.contains("mCurrentFocus") || it.contains("ChooserActivity") || it.contains("MainActivity") }.toList().takeLast(8).joinToString(" | ")})",
                shareSheetOpen,
            )
        }
    }

    private fun waitForMainNavigation() {
        compose.waitUntil(timeoutMillis = 20_000) {
            runCatching {
                compose.onAllNodesWithText("More").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }
}
