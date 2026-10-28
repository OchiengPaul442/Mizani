package com.mizani.money

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.WalletType
import com.mizani.money.reminder.DailyReminderNotifications
import com.mizani.money.reminder.DailyReminderScheduler
import com.mizani.money.reminder.DailyTransactionReminderWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                app.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
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
    fun settingsBackPathsAndDailyReminderWorkerPostsGenericNotification() {
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

        val targetDay = ZonedDateTime.now().toLocalDate().toEpochDay()
        runBlocking {
            app.container.settingsRepository.setDailyReminderMinuteOfDay(0)
        }
        val reminderToggle = compose.onNodeWithContentDescription("Daily log reminder")
        reminderToggle.performScrollTo()
        reminderToggle.assertIsOff()
        reminderToggle.performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            runBlocking { app.container.settingsRepository.settings.first().dailyReminderEnabled }
        }
        reminderToggle.assertIsOn()

        val workManager = WorkManager.getInstance(app)
        val queueDeadline = System.currentTimeMillis() + 10_000L
        var reminderWorkQueued = false
        var latestWorkInfos = emptyList<WorkInfo>()
        while (System.currentTimeMillis() < queueDeadline) {
            latestWorkInfos = workManager.getWorkInfosByTag(DailyReminderScheduler.WORK_TAG)
                .get(5, TimeUnit.SECONDS)
            reminderWorkQueued = latestWorkInfos.any { it.state == WorkInfo.State.ENQUEUED }
            if (reminderWorkQueued) break
            Thread.sleep(100)
        }
        assertTrue("Enabling the reminder must enqueue tagged work", reminderWorkQueued)

        val settings = runBlocking { app.container.settingsRepository.settings.first() }
        val reminderInputData = workDataOf(
            DailyReminderScheduler.INPUT_REMINDER_MINUTE_OF_DAY to settings.dailyReminderMinuteOfDay,
            DailyReminderScheduler.INPUT_TARGET_EPOCH_DAY to targetDay,
            DailyReminderScheduler.INPUT_SCHEDULE_VERSION to settings.dailyReminderScheduleVersion,
        )
        val worker = TestListenableWorkerBuilder<DailyTransactionReminderWorker>(app)
            .setInputData(reminderInputData)
            .build()
        val notifications = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifications.cancelAll()

        runBlocking { worker.doWork() }
        assertTrue(
            "The due reminder should be recorded after its generic notification is posted",
            runBlocking { app.container.settingsRepository.settings.first().lastDailyReminderEpochDay == targetDay },
        )
        val postedNotifications = notifications.activeNotifications.filter { it.packageName == app.packageName }
        assertEquals(1, postedNotifications.size)
        val postedNotification = postedNotifications.single().notification
        assertEquals(
            app.getString(R.string.notification_daily_reminder_title),
            postedNotification.extras.getString(Notification.EXTRA_TITLE),
        )
        assertEquals(
            app.getString(R.string.notification_daily_reminder_body),
            postedNotification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
        assertEquals(Notification.VISIBILITY_PRIVATE, postedNotification.visibility)

        val duplicateWorker = TestListenableWorkerBuilder<DailyTransactionReminderWorker>(app)
            .setInputData(reminderInputData)
            .build()
        runBlocking { duplicateWorker.doWork() }
        assertEquals(
            "Repeated work for the same day must not duplicate the reminder notification",
            1,
            notifications.activeNotifications.count { it.packageName == app.packageName },
        )
    }
}
