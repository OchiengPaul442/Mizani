package com.mizani.money.core.security

/** App-lock PIN policy shared by setup, unlock and settings flows. */
object PinDefaults {
    const val LENGTH = 6

    /** Consecutive wrong PINs allowed before any wait is imposed. */
    const val FREE_ATTEMPTS = 5

    /** First wait once [FREE_ATTEMPTS] is exceeded; doubles per extra failure. */
    const val BACKOFF_BASE_MS = 5_000L

    /** Hard cap on any single wait, so a legitimate owner is never locked out forever. */
    const val BACKOFF_MAX_MS = 60_000L

    /**
     * Milliseconds to wait after [failedAttempts] consecutive failures.
     * Zero below the free-attempt threshold, doubles per extra failure,
     * capped at [BACKOFF_MAX_MS]; pure and deterministic for tests.
     */
    fun backoffMs(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        var delayMs = BACKOFF_BASE_MS
        var step = FREE_ATTEMPTS
        while (step < failedAttempts && delayMs < BACKOFF_MAX_MS) {
            delayMs = (delayMs * 2).coerceAtMost(BACKOFF_MAX_MS)
            step++
        }
        return delayMs
    }

    /**
     * Milliseconds remaining until another attempt is allowed; 0 when not blocked.
     * The block is purely time-based on [lastFailureAtMs], so it always expires.
     * Ignores invalid state (no recorded failure or a clock before it).
     */
    fun blockedRemainingMs(failedAttempts: Int, lastFailureAtMs: Long, nowMs: Long): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        if (lastFailureAtMs <= 0L || nowMs < lastFailureAtMs) return 0L
        val backoff = backoffMs(failedAttempts)
        return (backoff - (nowMs - lastFailureAtMs)).coerceAtLeast(0L)
    }
}
