package com.mizani.money.data.repo

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class CommitmentCurrencyRulesTest {

    private val rateDate = LocalDate.now(ZoneOffset.UTC).toString()

    @Test
    fun validatesForeignTargetAndConvertsBackToCommitmentCurrency() {
        CommitmentCurrencyRules.validate(
            workspaceCurrencyCode = "UGX",
            currencyCode = "USD",
            targetMinor = 10_000,
            targetWorkspaceMinor = 360_000,
            exchangeRate = "3600",
            exchangeRateDate = rateDate,
        )

        assertEquals(
            10_000L,
            CommitmentCurrencyRules.workspaceToCommitmentMinor(
                amountMinor = 360_000,
                workspaceCurrencyCode = "UGX",
                commitmentCurrencyCode = "USD",
                exchangeRate = BigDecimal("3600"),
            ),
        )
    }

    @Test
    fun rejectsAConvertedTargetThatDoesNotMatchTheSavedRate() {
        try {
            CommitmentCurrencyRules.validate(
                workspaceCurrencyCode = "UGX",
                currencyCode = "USD",
                targetMinor = 10_000,
                targetWorkspaceMinor = 350_000,
                exchangeRate = "3600",
                exchangeRateDate = rateDate,
            )
            fail("Expected a mismatched converted target to be rejected")
        } catch (error: IllegalArgumentException) {
            assertEquals("Converted commitment amount does not match the saved exchange rate", error.message)
        }
    }
}
