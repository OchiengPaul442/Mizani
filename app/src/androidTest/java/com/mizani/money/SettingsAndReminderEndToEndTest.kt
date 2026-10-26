package com.mizani.money

import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.time.DailyReminderSchedule
import com.mizani.money.reminder.DailyReminderNotifications
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

/** Runs only in a disposable Android user; TestState.reset() clears that user's app data. */
@RunWith(AndroidJUnit4::class)
class SettingsAndReminderEndToEndTest {

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
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::app.isInitialized) {
            runBlocking {
                app.container.dailyReminderScheduler.cancel()
                app.container.settingsRepository.setDailyReminderEnabled(false)
            }
        }
        if (::scenario.isInitialized) scenario.close()
    }

    @Test
    fun settingsBackPathsAndDailyReminderDeliveryWorkEndToEnd() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNodeWithText("What would you like to record?").assertExists()
        Espresso.pressBack()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Manage money").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            compose.onAllNodesWithText("What would you like to record?").fetchSemanticsNodes().isEmpty(),
        )

        compose.onNodeWithText("Settings").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Reminders").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Manage money").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Settings").assertExists()

        compose.onNodeWithText("Settings").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Reminders").fetchSemanticsNodes().isNotEmpty()
        }
        Espresso.pressBack()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Manage money").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("About and privacy").assertExists()
        compose.onNodeWithText("Settings").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Reminders").fetchSemanticsNodes().isNotEmpty()
        }

        DailyReminderNotifications.ensureChannel(app)
        assertTrue("Notification permission must be enabled in the test user", DailyReminderNotifications.canPost(app))

        val now = ZonedDateTime.now()
        val minuteOfDay = (now.hour * 60 + now.minute + 2) % DailyReminderSchedule.MINUTES_PER_DAY
        val targetDay = DailyReminderSchedule.nextOccurrence(now, minuteOfDay).toLocalDate().toEpochDay()

        runBlocking {
            app.container.settingsRepository.setDailyReminderMinuteOfDay(minuteOfDay)
        }
        compose.onNodeWithContentDescription("Daily log reminder").performClick()

        val deadline = System.currentTimeMillis() + 240_000L
        var reminderMarkedShown = false
        while (System.currentTimeMillis() < deadline) {
            reminderMarkedShown = runBlocking {
                app.container.settingsRepository.settings.first().lastDailyReminderEpochDay == targetDay
            }
            if (reminderMarkedShown) break
            Thread.sleep(500)
        }

        assertTrue("The due WorkManager reminder did not complete within 240 seconds", reminderMarkedShown)
        val notifications = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(
            "The worker should post Mizani's generic daily reminder",
            notifications.activeNotifications.any { it.packageName == app.packageName },
        )
    }
}
