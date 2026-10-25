package com.mizani.money.data.repo

import com.mizani.money.core.model.TransactionType
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionCurrencyRulesTest {

    @Test
    fun `accepts foreign currency income with three decimal source units`() {
        TransactionCurrencyRules.validate(
            workspaceCurrencyCode = "USD",
            type = TransactionType.INCOME,
            amountMinor = 265L,
            feeMinor = 0L,
            sourceAmountMinor = 1_000L,
            sourceFeeMinor = null,
            sourceCurrencyCode = "BHD",
            exchangeRate = "2.65",
            exchangeRateDate = LocalDate.now(ZoneOffset.UTC).toString(),
        )
    }

    @Test
    fun `rejects foreign income whose saved workspace amount disagrees with the rate`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionCurrencyRules.validate(
                workspaceCurrencyCode = "USD",
                type = TransactionType.INCOME,
                amountMinor = 266L,
                feeMinor = 0L,
                sourceAmountMinor = 1_000L,
                sourceFeeMinor = null,
                sourceCurrencyCode = "BHD",
                exchangeRate = "2.65",
                exchangeRateDate = LocalDate.now(ZoneOffset.UTC).toString(),
            )
        }
    }
}
