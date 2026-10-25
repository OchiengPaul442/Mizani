package com.mizani.money.data.repo

import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.CurrencyConversion
import java.time.LocalDate
import java.time.ZoneOffset

/** Shared validation for live transaction writes and restored encrypted backups. */
internal object TransactionCurrencyRules {
    fun validate(
        workspaceCurrencyCode: String,
        type: TransactionType,
        amountMinor: Long,
        feeMinor: Long,
        sourceAmountMinor: Long?,
        sourceFeeMinor: Long?,
        sourceCurrencyCode: String?,
        exchangeRate: String?,
        exchangeRateDate: String?,
    ) {
        val hasMetadata = sourceAmountMinor != null || sourceFeeMinor != null || sourceCurrencyCode != null ||
            exchangeRate != null || exchangeRateDate != null
        if (!hasMetadata) return

        require(type in setOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.REFUND)) {
            "Currency conversion is not supported for this transaction type"
        }
        require(sourceAmountMinor != null && sourceAmountMinor > 0L) { "Original amount is missing or invalid" }
        require(sourceFeeMinor == null || sourceFeeMinor >= 0L) { "Original fee cannot be negative" }
        require(sourceCurrencyCode != null && sourceCurrencyCode != workspaceCurrencyCode) {
            "Original currency must differ from the workspace currency"
        }
        require(exchangeRate != null && exchangeRate.length <= MAX_RATE_LENGTH) { "Exchange rate is missing or invalid" }
        require(exchangeRateDate != null) { "Exchange-rate date is missing" }

        val sourceCurrency = Currencies.transactionCurrencies.firstOrNull { it.code == sourceCurrencyCode }
            ?: throw IllegalArgumentException("Original currency is not supported")
        val workspaceCurrency = Currencies.all.firstOrNull { it.code == workspaceCurrencyCode }
            ?: throw IllegalArgumentException("Workspace currency is not supported")
        val rate = exchangeRate.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Exchange rate is invalid")
        require(rate.signum() > 0 && rate.precision() <= MAX_RATE_PRECISION) { "Exchange rate is invalid" }
        val date = runCatching { LocalDate.parse(exchangeRateDate) }
            .getOrElse { throw IllegalArgumentException("Exchange-rate date is invalid") }
        require(!date.isAfter(LocalDate.now(ZoneOffset.UTC))) { "Exchange-rate date cannot be in the future" }

        val convertedAmount = CurrencyConversion.convertMinorUnits(sourceAmountMinor, sourceCurrency, workspaceCurrency, rate)
        require(convertedAmount == amountMinor) { "Converted amount does not match the saved exchange rate" }
        val convertedFee = CurrencyConversion.convertMinorUnits(sourceFeeMinor ?: 0L, sourceCurrency, workspaceCurrency, rate)
        require(convertedFee == feeMinor) { "Converted fee does not match the saved exchange rate" }
    }

    private const val MAX_RATE_LENGTH = 64
    private const val MAX_RATE_PRECISION = 36
}
