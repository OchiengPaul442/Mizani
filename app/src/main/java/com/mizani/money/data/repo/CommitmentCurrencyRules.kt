package com.mizani.money.data.repo

import com.mizani.money.core.money.CurrencyConversion
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.MoneyLimits
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset

/** Shared validation and deterministic conversion for commitment targets. */
internal object CommitmentCurrencyRules {

    fun validate(
        workspaceCurrencyCode: String,
        currencyCode: String,
        targetMinor: Long,
        targetWorkspaceMinor: Long,
        exchangeRate: String?,
        exchangeRateDate: String?,
    ) {
        require(targetMinor in 1..MoneyLimits.MAX_MINOR_AMOUNT) { "Enter a valid commitment amount" }
        require(targetWorkspaceMinor in 1..MoneyLimits.MAX_MINOR_AMOUNT) {
            "The converted commitment amount is invalid"
        }
        require(Currencies.transactionCurrencies.any { it.code == currencyCode }) {
            "Commitment currency is not supported"
        }
        require(Currencies.all.any { it.code == workspaceCurrencyCode }) {
            "Workspace currency is not supported"
        }
        if (currencyCode == workspaceCurrencyCode) {
            require(targetWorkspaceMinor == targetMinor) {
                "A workspace-currency commitment cannot have a converted target"
            }
            require(exchangeRate == null && exchangeRateDate == null) {
                "A workspace-currency commitment cannot have an exchange rate"
            }
            return
        }

        require(exchangeRate != null && exchangeRate.length <= MAX_RATE_LENGTH) {
            "Exchange rate is missing or invalid"
        }
        require(exchangeRateDate != null) { "Exchange-rate date is missing" }
        val rate = exchangeRate.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Exchange rate is invalid")
        require(rate.signum() > 0 && rate.precision() <= MAX_RATE_PRECISION) {
            "Exchange rate is invalid"
        }
        val date = runCatching { LocalDate.parse(exchangeRateDate) }
            .getOrElse { throw IllegalArgumentException("Exchange-rate date is invalid") }
        require(!date.isAfter(LocalDate.now(ZoneOffset.UTC))) {
            "Exchange-rate date cannot be in the future"
        }
        val converted = commitmentToWorkspaceMinor(
            amountMinor = targetMinor,
            workspaceCurrencyCode = workspaceCurrencyCode,
            commitmentCurrencyCode = currencyCode,
            exchangeRate = rate,
        )
        require(converted == targetWorkspaceMinor) {
            "Converted commitment amount does not match the saved exchange rate"
        }
    }

    fun commitmentToWorkspaceMinor(
        amountMinor: Long,
        workspaceCurrencyCode: String,
        commitmentCurrencyCode: String,
        exchangeRate: BigDecimal?,
    ): Long {
        if (workspaceCurrencyCode == commitmentCurrencyCode) return amountMinor
        val rate = exchangeRate ?: error("Exchange rate is required for a foreign commitment")
        return CurrencyConversion.convertMinorUnits(
            amountMinor,
            Currencies.byCode(commitmentCurrencyCode),
            Currencies.byCode(workspaceCurrencyCode),
            rate,
        )
    }

    fun workspaceToCommitmentMinor(
        amountMinor: Long,
        workspaceCurrencyCode: String,
        commitmentCurrencyCode: String,
        exchangeRate: BigDecimal?,
    ): Long {
        if (workspaceCurrencyCode == commitmentCurrencyCode) return amountMinor
        val rate = exchangeRate ?: error("Exchange rate is required for a foreign commitment")
        val inverse = BigDecimal.ONE.divide(rate, INVERSE_SCALE, RoundingMode.HALF_UP)
        return CurrencyConversion.convertMinorUnits(
            amountMinor,
            Currencies.byCode(workspaceCurrencyCode),
            Currencies.byCode(commitmentCurrencyCode),
            inverse,
        )
    }

    private const val MAX_RATE_LENGTH = 64
    private const val MAX_RATE_PRECISION = 36
    private const val INVERSE_SCALE = 48
}
