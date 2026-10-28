package com.mizani.money.core.importing

import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.MoneyLimits
import com.mizani.money.core.money.MoneyFormat
import java.time.LocalDate
import java.time.DateTimeException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/** Parsed suggestion only. The original message is never retained by the parser or ledger. */
data class ImportedTransactionCandidate(
    val provider: String,
    val reference: String,
    val type: TransactionType,
    val amountMinor: Long,
    val feeMinor: Long,
    val currencyCode: String,
    val payee: String,
    val date: LocalDate,
)

sealed interface MessageParseResult {
    data class Parsed(val candidate: ImportedTransactionCandidate) : MessageParseResult
    data class Rejected(val reason: Reason) : MessageParseResult

    enum class Reason {
        TOO_LONG,
        UNSUPPORTED_PROVIDER,
        UNSUPPORTED_MESSAGE,
    }
}

/** Bounded, local-only rules for confirmation texts shared or pasted by the user. */
object TransactionMessageParser {
    const val MAX_MESSAGE_LENGTH = 4_096

    /** Common African mobile-money brands. Detection remains local and provider-specific. */
    val providerOptions: List<ProviderOption> = listOf(
        ProviderOption("mtn_momo", "MTN MoMo", Regex("(?i)\\bMTN\\b|MOMO|MO\\s*MO")),
        ProviderOption("airteltigo_money", "AirtelTigo Money", Regex("(?i)AIRTEL\\s*TIGO(?:\\s*MONEY)?")),
        ProviderOption("airtel_money", "Airtel Money", Regex("(?i)AIRTEL(?:\\s*MONEY)?")),
        ProviderOption("mpesa", "M-Pesa", Regex("(?i)M[- ]?PESA|SAFARICOM|VODACOM")),
        ProviderOption("orange_money", "Orange Money", Regex("(?i)ORANGE(?:\\s*MONEY)?")),
        ProviderOption("moov_money", "Moov Money", Regex("(?i)MOOV(?:\\s*MONEY)?")),
        ProviderOption("wave", "Wave", Regex("(?i)\\bWAVE(?:\\s*MONEY)?\\b")),
        ProviderOption("telecel_cash", "Telecel Cash", Regex("(?i)TELECEL(?:\\s+CASH)?")),
        ProviderOption("tigo_pesa", "Tigo Pesa", Regex("(?i)TIGO(?:\\s*PESA)?")),
        ProviderOption("vodafone_cash", "Vodafone Cash", Regex("(?i)VODAFONE(?:\\s+CASH)?")),
        ProviderOption("ecocash", "EcoCash", Regex("(?i)ECO\\s*CASH")),
        ProviderOption("opay", "OPay", Regex("(?i)\\bOPAY\\b")),
        ProviderOption("palmpay", "PalmPay", Regex("(?i)PALM\\s*PAY")),
        ProviderOption("moniepoint", "Moniepoint", Regex("(?i)MONIEPOINT")),
        ProviderOption("halopesa", "HaloPesa", Regex("(?i)HALO\\s*PESA")),
        ProviderOption("tnm_mpamba", "TNM Mpamba", Regex("(?i)TNM(?:\\s+MPAMBA)?")),
    )
    private val currencyAlternation by lazy {
        Currencies.transactionCurrencies.map { Regex.escape(it.code) }.sortedByDescending { it.length }.joinToString("|")
    }
    private val amountPattern by lazy {
        Regex("(?i)\\b($currencyAlternation)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,6})?)")
    }
    private val feePattern by lazy {
        Regex("(?i)(?:transaction\\s+fee|fee|charge)\\s*(?:of|:)?\\s*($currencyAlternation)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,6})?)")
    }
    private val referencePattern = Regex("(?i)\\b(?:ref(?:erence)?|txn(?:\\s*(?:id|no))?|transaction\\s*(?:id|no)|(?:financial\\s+)?id|receipt(?:\\s*(?:id|no|number))?)\\s*[:#-]\\s*([A-Z0-9][A-Z0-9./_-]{2,127})\\b")
    private val outgoingPattern = Regex("(?i)\\b(?:you\\s+have\\s+sent|you\\s+sent|sent\\s+to|payment\\s+of|paid)\\b")
    private val incomingPattern = Regex("(?i)\\b(?:you\\s+have\\s+received|you\\s+received|received\\s+from|deposit\\s+of|credited)\\b")
    private val balanceLabelPattern = Regex("(?i)\\b(?:available\\s+)?balance\\b")
    private const val MAX_ACTION_AMOUNT_GAP = 200
    private const val MAX_PRE_ACTION_AMOUNT_GAP = 32

    data class ProviderOption(val id: String, val label: String, internal val matcher: Regex)

    fun parse(
        message: String,
        today: LocalDate = LocalDate.now(),
        providerHint: String? = null,
    ): MessageParseResult {
        if (message.length > MAX_MESSAGE_LENGTH) return MessageParseResult.Rejected(MessageParseResult.Reason.TOO_LONG)
        val normalized = message.trim()
        if (normalized.isEmpty()) return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)

        val provider = providerOptions.firstOrNull { it.matcher.containsMatchIn(normalized) }
            ?: providerHint?.let { hint ->
                providerOptions.firstOrNull { option ->
                    option.id.equals(hint.trim(), ignoreCase = true) ||
                        option.label.equals(hint.trim(), ignoreCase = true) ||
                        option.matcher.containsMatchIn(hint)
                }
            }
            ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_PROVIDER)
        val type = when {
            outgoingPattern.containsMatchIn(normalized) -> TransactionType.EXPENSE
            incomingPattern.containsMatchIn(normalized) -> TransactionType.INCOME
            else -> return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
        }
        // Prefer a provider reference. If a provider omits one, hash the normalized message
        // locally so exact re-imports stay idempotent without persisting raw SMS text.
        val reference = referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?.uppercase(Locale.ROOT)
            ?: generatedMessageReference(provider.id, normalized)
        val actionMatch = (if (type == TransactionType.EXPENSE) outgoingPattern else incomingPattern)
            .find(normalized)
            ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
        val feeMatches = feePattern.findAll(normalized).toList()
        val transactionAmounts = amountPattern.findAll(normalized)
            .filterNot { amount -> feeMatches.any { fee -> amount.range.first in fee.range } }
        val actionEnd = actionMatch.range.last + 1
        fun isSeparatedByBalance(amount: MatchResult): Boolean {
            val connector = if (amount.range.first >= actionEnd) {
                normalized.substring(actionEnd, amount.range.first)
            } else {
                normalized.substring(amount.range.last + 1, actionMatch.range.first)
            }
            return balanceLabelPattern.containsMatchIn(connector)
        }
        val amountMatch = transactionAmounts.firstOrNull { amount ->
            amount.range.first >= actionEnd && amount.range.first - actionEnd <= MAX_ACTION_AMOUNT_GAP &&
                !isSeparatedByBalance(amount)
        } ?: transactionAmounts
            .filter { amount -> amount.range.last < actionMatch.range.first && !isSeparatedByBalance(amount) }
            .minByOrNull { amount -> actionMatch.range.first - amount.range.last }
            ?.takeIf { amount -> actionMatch.range.first - amount.range.last <= MAX_PRE_ACTION_AMOUNT_GAP }
            ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
        val currencyCode = amountMatch.groupValues[1].uppercase(Locale.ROOT)
        val currency = Currencies.byCode(currencyCode)
        val amountMinor = parseMessageMoney(amountMatch.groupValues[2], currency)
            ?.takeIf { it in 1..MoneyLimits.MAX_MINOR_AMOUNT }
            ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)

        val feeMatch = feeMatches.firstOrNull()
        val feeMinor = feeMatch?.let { match ->
            val feeCurrencyCode = match.groupValues[1].ifBlank { currencyCode }.uppercase(Locale.ROOT)
            if (feeCurrencyCode != currencyCode) return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
            parseMessageMoney(match.groupValues[2], currency)
                ?.takeIf { it in 0..MoneyLimits.MAX_MINOR_AMOUNT }
                ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
        } ?: 0L

        val parsedPayee = if (type == TransactionType.EXPENSE) {
            Regex("(?i)\\bto\\s+([\\p{L}][\\p{L} .'’-]{0,79}?)(?=\\s*(?:\\.|,|;|$|transaction\\s+fee|fee|ref(?:erence)?|txn))")
                .find(normalized)?.groupValues?.getOrNull(1)?.trim()?.take(80).orEmpty()
        } else {
            Regex("(?i)\\bfrom\\s+([\\p{L}][\\p{L} .'’-]{0,79}?)(?=\\s*(?:\\.|,|;|$|ref(?:erence)?|txn))")
                .find(normalized)?.groupValues?.getOrNull(1)?.trim()?.take(80).orEmpty()
        }
        val robustIncomingPayee = if (type == TransactionType.INCOME) {
            Regex("(?i)\\bfrom\\s+([A-Za-z][A-Za-z0-9 .'-]{0,79}?)(?=\\s+(?:on|at|fee|reason|new\\s+balance|id)\\b|[.,;]|$)")
                .find(normalized)?.groupValues?.getOrNull(1).orEmpty()
        } else {
            ""
        }
        val payee = Regex("(?is)\\s+(?:on|at|reason|new\\s+balance|fee|id)\\b.*$")
            .replace(robustIncomingPayee.ifBlank { parsedPayee }, "")
            .trim()

        return MessageParseResult.Parsed(
            ImportedTransactionCandidate(
                provider = provider.id,
                reference = reference,
                type = type,
                amountMinor = amountMinor,
                feeMinor = feeMinor,
                currencyCode = currencyCode,
                payee = payee,
                date = parseDate(normalized) ?: today,
            ),
        )
    }

    private fun parseDate(message: String): LocalDate? {
        Regex("(?<!\\d)(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})(?!\\d)").find(message)?.let { match ->
            val (year, month, day) = match.destructured
            try {
                return LocalDate.of(year.toInt(), month.toInt(), day.toInt())
            } catch (_: DateTimeException) {
                return null
            }
        }
        val match = Regex("(?<!\\d)(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})(?!\\d)").find(message) ?: return null
        val (first, second, year) = match.destructured
        return try {
            LocalDate.of(year.toInt(), second.toInt(), first.toInt())
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun parseMessageMoney(raw: String, currency: Currency): Long? {
        val fractional = raw.substringAfter('.', missingDelimiterValue = "")
        if (currency.minorUnits == 0 && fractional.any { it != '0' }) return null
        return MoneyFormat.parse(raw, currency)
    }

    private fun generatedMessageReference(provider: String, message: String): String {
        val normalized = message.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$provider|$normalized".toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte) }
        return "MSG-${digest.uppercase(Locale.ROOT)}"
    }
}
