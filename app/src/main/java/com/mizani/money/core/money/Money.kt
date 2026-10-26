package com.mizani.money.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency as JavaCurrency
import java.util.Locale

/**
 * A currency definition. [minorUnits] is the number of decimal places used for storage.
 * All money is persisted as integer minor units; never as floating point.
 */
data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val minorUnits: Int,
) {
    init {
        require(minorUnits in 0..6) { "Currency minor units must be between zero and six" }
    }

    val scaleFactor: Long
        get() = (0 until minorUnits).fold(1L) { factor, _ -> Math.multiplyExact(factor, 10L) }
}

object Currencies {
    val UGX = Currency("UGX", "USh", "Ugandan Shilling", 0)
    val KES = Currency("KES", "KSh", "Kenyan Shilling", 0)
    val TZS = Currency("TZS", "TSh", "Tanzanian Shilling", 0)
    val RWF = Currency("RWF", "FRw", "Rwandan Franc", 0)
    val NGN = Currency("NGN", "₦", "Nigerian Naira", 2)
    val GHS = Currency("GHS", "GH₵", "Ghanaian Cedi", 2)
    val ZAR = Currency("ZAR", "R", "South African Rand", 2)
    val USD = Currency("USD", "$", "US Dollar", 2)
    val EUR = Currency("EUR", "€", "Euro", 2)
    val GBP = Currency("GBP", "£", "British Pound", 2)

    private val curatedCurrencies = listOf(UGX, KES, TZS, RWF, NGN, GHS, ZAR, USD, EUR, GBP)
    private val africanCurrencyCodes = setOf(
        "AOA", "BIF", "BWP", "CDF", "CVE", "DJF", "DZD", "EGP", "ERN", "ETB", "GMD", "GNF",
        "KMF", "LRD", "LYD", "MAD", "MGA", "MRU", "MUR", "MWK", "MZN", "NAD", "SCR", "SDG",
        "SLE", "SOS", "SSP", "STN", "SZL", "TND", "XAF", "XOF", "ZMW", "ZWG",
    )

    /** Common account currencies for African regions plus the globally common choices. */
    val all: List<Currency> by lazy {
        val knownCodes = curatedCurrencies.mapTo(mutableSetOf()) { it.code }
        curatedCurrencies + transactionCurrencies.filter {
            it.code in africanCurrencyCodes && knownCodes.add(it.code)
        }
    }

    /** Currency codes currently assigned to an ISO country or territory. */
    private val currentCurrencyCodes: Set<String> by lazy {
        Locale.getISOCountries().mapNotNullTo(mutableSetOf()) { region ->
            runCatching {
                JavaCurrency.getInstance(Locale.Builder().setRegion(region).build()).currencyCode
            }.getOrNull()
        }
    }

    /**
     * All currencies known to the runtime are retained for formatting older records. Historical
     * codes are deliberately excluded from new transaction entry.
     */
    private val runtimeCurrencies: List<Currency> by lazy {
        JavaCurrency.getAvailableCurrencies().mapNotNull { javaCurrency ->
            val code = javaCurrency.currencyCode
            val minorUnits = javaCurrency.defaultFractionDigits
            if (code == "XXX" || minorUnits !in 0..6) return@mapNotNull null
            Currency(
                code = code,
                symbol = javaCurrency.getSymbol(Locale.US),
                name = javaCurrency.getDisplayName(Locale.ENGLISH),
                minorUnits = minorUnits,
            )
        }
    }

    /**
     * Foreign currencies supported for transaction entry. Keep this broader than [all]: a
     * workspace still uses the app's verified country currencies, while users can record income
     * and expenses in any current country currency known to the runtime.
     */
    val transactionCurrencies: List<Currency> by lazy {
        val curatedByCode = curatedCurrencies.associateBy { it.code }
        val byCode = (curatedCurrencies + runtimeCurrencies.filter { it.code in currentCurrencyCodes })
            .associateBy { it.code }
        val preferred = curatedCurrencies.mapNotNull { byCode[it.code] }
        preferred + byCode.values.filterNot { it.code in curatedByCode }.sortedBy { it.code }
    }

    private val allCurrenciesByCode: Map<String, Currency> by lazy {
        (curatedCurrencies + runtimeCurrencies).associateBy { it.code }
    }

    fun byCode(code: String): Currency =
        allCurrenciesByCode[code.uppercase(Locale.ROOT)] ?: USD
}

/** Converts minor units using a decimal rate expressed as target major units per source major unit. */
object CurrencyConversion {
    fun convertMinorUnits(
        amountMinor: Long,
        source: Currency,
        target: Currency,
        rate: BigDecimal,
    ): Long {
        require(amountMinor >= 0L) { "Amount cannot be negative" }
        require(rate.signum() > 0) { "Exchange rate must be positive" }

        return BigDecimal.valueOf(amountMinor)
            .multiply(rate)
            .multiply(BigDecimal.valueOf(target.scaleFactor))
            .divide(BigDecimal.valueOf(source.scaleFactor), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}

/**
 * Immutable money value. Arithmetic requires matching currencies, and overflow is
 * handled explicitly rather than silently wrapping around.
 */
data class Money(val minorUnits: Long, val currencyCode: String) : Comparable<Money> {

    private fun requireSame(other: Money) {
        require(currencyCode == other.currencyCode) {
            "Currency mismatch: $currencyCode vs ${other.currencyCode}"
        }
    }

    operator fun plus(other: Money): Money {
        requireSame(other)
        return Money(Math.addExact(minorUnits, other.minorUnits), currencyCode)
    }

    operator fun minus(other: Money): Money {
        requireSame(other)
        return Money(Math.subtractExact(minorUnits, other.minorUnits), currencyCode)
    }

    operator fun times(factor: Long): Money =
        Money(Math.multiplyExact(minorUnits, factor), currencyCode)

    operator fun unaryMinus(): Money = Money(Math.negateExact(minorUnits), currencyCode)

    fun abs(): Money = Money(Math.abs(minorUnits), currencyCode)

    fun isZero(): Boolean = minorUnits == 0L

    fun isNegative(): Boolean = minorUnits < 0L

    fun isPositive(): Boolean = minorUnits > 0L

    fun format(currency: Currency = Currencies.byCode(currencyCode)): String =
        MoneyFormat.format(minorUnits, currency)

    override fun compareTo(other: Money): Int {
        requireSame(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    companion object {
        fun zero(currencyCode: String): Money = Money(0L, currencyCode)

        fun ofMinor(minorUnits: Long, currencyCode: String): Money = Money(minorUnits, currencyCode)

        fun ofMajor(major: Long, currencyCode: String): Money {
            val currency = Currencies.byCode(currencyCode)
            return Money(Math.multiplyExact(major, currency.scaleFactor), currencyCode)
        }
    }
}

object MoneyFormat {

    /** Formats minor units with the currency symbol and stable grouping (e.g. "USh 1,250,000"). */
    fun format(minorUnits: Long, currency: Currency): String {
        val negative = minorUnits < 0
        // Avoid Long.MIN_VALUE overflow when negating.
        val absolute = if (negative) {
            if (minorUnits == Long.MIN_VALUE) Long.MAX_VALUE else -minorUnits
        } else {
            minorUnits
        }
        val whole = absolute / currency.scaleFactor
        val fraction = (absolute % currency.scaleFactor).toInt()
        val grouping = NumberFormat.getIntegerInstance(Locale.US).apply {
            isGroupingUsed = true
        }.format(whole)
        val fractionPart = if (currency.minorUnits > 0) {
            "." + fraction.toString().padStart(currency.minorUnits, '0')
        } else {
            ""
        }
        val sign = if (negative) "-" else ""
        return "$sign${currency.symbol} $grouping$fractionPart"
    }

    /** Formatting without symbol, used in inputs and compact chart labels. Keeps the minus sign. */
    fun formatBare(minorUnits: Long, currency: Currency): String {
        val sign = if (minorUnits < 0) "-" else ""
        val magnitude = format(if (minorUnits == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minorUnits), currency)
        return sign + magnitude.removePrefix(currency.symbol).trim()
    }

    /**
     * Parses free-form user input into minor units. Accepts grouping commas, spaces and
     * a single decimal point. Returns null for malformed or out-of-range input.
     */
    fun parse(input: String, currency: Currency): Long? {
        val cleaned = input.trim()
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace(",", "")
            .replace(currency.symbol, "")
        if (cleaned.isEmpty() || cleaned == ".") return null
        if (!Regex("^\\d{0,15}(\\.\\d{0,6})?$").matches(cleaned)) return null
        return try {
            val scaled = BigDecimal(cleaned)
                .movePointRight(currency.minorUnits)
                .setScale(0, RoundingMode.HALF_UP)
            if (scaled > BigDecimal.valueOf(9_000_000_000_000_000L)) return null
            scaled.toLong()
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Converts minor units to a plain editable decimal string. */
    fun toInput(minorUnits: Long, currency: Currency): String {
        if (minorUnits == 0L) return ""
        val negative = minorUnits < 0
        val absolute = if (negative) -minorUnits else minorUnits
        val whole = absolute / currency.scaleFactor
        val fraction = absolute % currency.scaleFactor
        val base = if (currency.minorUnits == 0) {
            whole.toString()
        } else {
            whole.toString() + "." + fraction.toString().padStart(currency.minorUnits, '0')
        }
        return if (negative) "-$base" else base
    }
}
