package com.mizani.money.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Boundary tests for the failed-PIN backoff policy. Pure and time-injected,
 * so results are deterministic; no DataStore or Android dependencies here.
 */
class PinBackoffPolicyTest {

    @Test
    fun `no backoff below the free attempt threshold`() {
        assertEquals(0L, PinDefaults.backoffMs(0))
        assertEquals(0L, PinDefaults.backoffMs(1))
        assertEquals(0L, PinDefaults.backoffMs(PinDefaults.FREE_ATTEMPTS - 1))
    }

    @Test
    fun `backoff starts at the base delay exactly on the threshold`() {
        assertEquals(PinDefaults.BACKOFF_BASE_MS, PinDefaults.backoffMs(PinDefaults.FREE_ATTEMPTS))
    }

    @Test
    fun `backoff doubles with each additional failure`() {
        assertEquals(10_000L, PinDefaults.backoffMs(6))
        assertEquals(20_000L, PinDefaults.backoffMs(7))
        assertEquals(40_000L, PinDefaults.backoffMs(8))
    }

    @Test
    fun `backoff is capped so the wait stays bounded at any failure count`() {
        assertEquals(PinDefaults.BACKOFF_MAX_MS, PinDefaults.backoffMs(9))
        assertEquals(PinDefaults.BACKOFF_MAX_MS, PinDefaults.backoffMs(1_000))
        assertEquals(PinDefaults.BACKOFF_MAX_MS, PinDefaults.backoffMs(Int.MAX_VALUE))
        // Bounded, never "forever": the maximum wait is a finite window.
        assertTrue(PinDefaults.BACKOFF_MAX_MS in 1L until Long.MAX_VALUE)
    }

    @Test
    fun `remaining block counts down to zero and then allows attempts`() {
        val now = 1_700_000_000_000L
        val failures = 6 // 10s window
        assertEquals(10_000L, PinDefaults.blockedRemainingMs(failures, now, now))
        assertEquals(4_000L, PinDefaults.blockedRemainingMs(failures, now, now + 6_000))
        assertEquals(1L, PinDefaults.blockedRemainingMs(failures, now, now + 9_999))
        assertEquals(0L, PinDefaults.blockedRemainingMs(failures, now, now + 10_000))
        assertEquals(0L, PinDefaults.blockedRemainingMs(failures, now, now + 60_000))
    }

    @Test
    fun `attempts below the threshold are never blocked`() {
        val now = 1_700_000_000_000L
        assertEquals(0L, PinDefaults.blockedRemainingMs(PinDefaults.FREE_ATTEMPTS - 1, now - 1L, now))
    }

    @Test
    fun `missing or invalid failure state is treated as not blocked`() {
        // No recorded failure timestamp.
        assertEquals(0L, PinDefaults.blockedRemainingMs(9, 0L, Long.MAX_VALUE))
        // Clock reading before the recorded failure (skew/reset) — not blocked.
        assertEquals(0L, PinDefaults.blockedRemainingMs(9, 5_000L, 4_000L))
        // Negative/absurd counters cannot create an open-ended block.
        assertEquals(0L, PinDefaults.blockedRemainingMs(-1, 1_000L, 1_001L))
    }

    @Test
    fun `policy constants keep the first wait short and the cap bounded`() {
        assertTrue(PinDefaults.FREE_ATTEMPTS > 0)
        assertTrue(PinDefaults.BACKOFF_BASE_MS in 1L..PinDefaults.BACKOFF_MAX_MS)
        assertTrue(PinDefaults.BACKOFF_MAX_MS < 5 * 60_000L)
    }
}
