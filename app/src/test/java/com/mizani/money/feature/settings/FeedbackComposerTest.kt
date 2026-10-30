package com.mizani.money.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackComposerTest {

    @Test
    fun blankFeedbackIsRejected() {
        val result = FeedbackComposer.validate(FeedbackTopic.SUGGESTION, "   ")

        assertEquals("Write a few words so we know how to help.", result.message)
    }

    @Test
    fun feedbackLongerThanTheBoundIsRejected() {
        val result = FeedbackComposer.validate(
            FeedbackTopic.PROBLEM,
            "x".repeat(FeedbackComposer.MAX_MESSAGE_LENGTH + 1),
        )

        assertEquals(
            "Keep your feedback under ${FeedbackComposer.MAX_MESSAGE_LENGTH} characters.",
            result.message,
        )
    }

    @Test
    fun validFeedbackHasNoValidationError() {
        assertEquals(
            null,
            FeedbackComposer.validate(FeedbackTopic.QUESTION, "How can I add another wallet?").message,
        )
    }

    @Test
    fun emailBodyContainsOnlyFeedbackAndMinimalAppContext() {
        val body = FeedbackComposer.body(
            topic = FeedbackTopic.SUGGESTION,
            message = "  The insights screen is useful.  ",
            appVersion = "1.6.0",
            androidVersion = "16",
        )

        assertTrue(body.startsWith("The insights screen is useful."))
        assertTrue(body.contains("Topic: suggestion"))
        assertTrue(body.contains("Mizani version: 1.6.0"))
        assertTrue(body.contains("Android version: 16"))
        assertTrue(body.contains("Please do not reply with balances"))
        assertFalse(body.contains("amountMinor"))
    }

    @Test
    fun subjectsAreStableAndClearlyIdentified() {
        assertEquals("Mizani feedback: problem report", FeedbackComposer.subject(FeedbackTopic.PROBLEM))
        assertEquals("paul.ochieng.dev@gmail.com", FeedbackComposer.OFFICIAL_EMAIL)
    }
}
