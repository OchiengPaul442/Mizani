package com.mizani.money

import android.content.Intent
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.WalletType
import com.mizani.money.feature.settings.FeedbackComposer
import com.mizani.money.feature.settings.FeedbackTopic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator coverage for feedback placement, validation, and the email handoff contract. */
@RunWith(AndroidJUnit4::class)
class FeedbackEndToEndTest {

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
                walletName = "Feedback test wallet",
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
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("More").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
        TestState.reset()
    }

    @Test
    fun feedbackIsReachableFromMoreAndValidatesEmptyMessage() {
        compose.onAllNodesWithText("More").onLast().performClick()
        compose.onNodeWithText("Send feedback").performClick()
        compose.onNodeWithText("Help shape Mizani").assertExists()
        compose.onNodeWithText(
            "Tell us what is working, what could be clearer, or where you got stuck. " +
                "Your feedback goes to the Mizani team.",
        ).assertExists()
        assertTrue(
            compose.onAllNodesWithText(FeedbackComposer.OFFICIAL_EMAIL).fetchSemanticsNodes().isEmpty(),
        )
        compose.onNodeWithText("Open email app").assertIsEnabled().performClick()
        compose.onNodeWithText("Write a few words so we know how to help.").assertExists()

        compose.onNodeWithText("Your feedback").performTextInput("The feedback form is easy to find.")
        compose.onNodeWithText("Open email app").assertIsEnabled()
    }

    @Test
    fun emailIntentUsesOfficialRecipientAndDoesNotAttachFiles() {
        val intent = FeedbackComposer.createIntent(
            topic = FeedbackTopic.PROBLEM,
            message = "A test problem description.",
            appVersion = "test-version",
            androidVersion = "test-android",
        )

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data?.scheme)
        assertEquals("", intent.data?.schemeSpecificPart)
        assertArrayEquals(
            arrayOf(FeedbackComposer.OFFICIAL_EMAIL),
            intent.getStringArrayExtra(Intent.EXTRA_EMAIL),
        )
        assertEquals("Mizani feedback: problem report", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertTrue(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().contains("A test problem description."))
        assertTrue(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().contains("Mizani version: test-version"))
        assertTrue(!intent.hasExtra(Intent.EXTRA_STREAM))
    }
}
