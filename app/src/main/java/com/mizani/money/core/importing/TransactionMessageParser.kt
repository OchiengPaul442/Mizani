package com.mizani.money.core.importing

import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.MoneyLimits
import com.mizani.money.core.money.MoneyFormat
import java.time.LocalDate
import java.time.DateTimeException
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

    private val amountPattern = Regex("(?i)\\b(UGX|KES|TZS|RWF|NGN|GHS|ZAR)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,6})?)")
    private val feePattern = Regex("(?i)(?:transaction\\s+fee|fee|charge)\\s*(?:of|:)?\\s*(?:(UGX|KES|TZS|RWF|NGN|GHS|ZAR)\\s*)?([0-9][0-9,]*(?:\\.[0-9]{1,6})?)")
    private val referencePattern = Regex("(?i)\\b(?:ref(?:erence)?|txn(?:\\s+id)?|transaction\\s+id)\\s*[:#-]\\s*([A-Z0-9][A-Z0-9./_-]{2,127})\\b")
    private val outgoingPattern = Regex("(?i)\\b(?:you\\s+have\\s+sent|you\\s+sent|sent\\s+to|payment\\s+of|paid)\\b")
    private val incomingPattern = Regex("(?i)\\b(?:you\\s+have\\s+received|you\\s+received|received\\s+from|deposit\\s+of)\\b")
    private val balanceLabelPattern = Regex("(?i)\\b(?:available\\s+)?balance\\b")
    private const val MAX_ACTION_AMOUNT_GAP = 200
    private const val MAX_PRE_ACTION_AMOUNT_GAP = 32

    fun parse(
        message: String,
        today: LocalDate = LocalDate.now(),
        providerHint: String? = null,
    ): MessageParseResult {
        if (message.length > MAX_MESSAGE_LENGTH) return MessageParseResult.Rejected(MessageParseResult.Reason.TOO_LONG)
        val normalized = message.trim()
        if (normalized.isEmpty()) return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)

        val provider = when {
            Regex("(?i)\\bMTN\\b|MOMO|MO\\s*MO").containsMatchIn(normalized) -> "mtn_momo"
            Regex("(?i)AIRTEL(?:\\s*MONEY)?").containsMatchIn(normalized) -> "airtel_money"
            providerHint?.let { Regex("(?i)\\bMTN\\b|MOMO|MO\\s*MO").containsMatchIn(it) } == true -> "mtn_momo"
            providerHint?.let { Regex("(?i)AIRTEL(?:\\s*MONEY)?").containsMatchIn(it) } == true -> "airtel_money"
            else -> return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_PROVIDER)
        }
        val type = when {
            outgoingPattern.containsMatchIn(normalized) -> TransactionType.EXPENSE
            incomingPattern.containsMatchIn(normalized) -> TransactionType.INCOME
            else -> return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
        }
        val reference = referencePattern.find(normalized)?.groupValues?.getOrNull(1)
            ?.uppercase(Locale.ROOT)
            ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
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
        val amountMinor = MoneyFormat.parse(amountMatch.groupValues[2], currency)
            ?.takeIf { it in 1..MoneyLimits.MAX_MINOR_AMOUNT }
            ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)

        val feeMatch = feeMatches.firstOrNull()
        val feeMinor = feeMatch?.let { match ->
            val feeCurrencyCode = match.groupValues[1].ifBlank { currencyCode }.uppercase(Locale.ROOT)
            if (feeCurrencyCode != currencyCode) return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
            MoneyFormat.parse(match.groupValues[2], currency)
                ?.takeIf { it in 0..MoneyLimits.MAX_MINOR_AMOUNT }
                ?: return MessageParseResult.Rejected(MessageParseResult.Reason.UNSUPPORTED_MESSAGE)
        } ?: 0L

        val payee = if (type == TransactionType.EXPENSE) {
            Regex("(?i)\\bto\\s+([\\p{L}][\\p{L} .'’-]{0,79}?)(?=\\s*(?:\\.|,|;|$|transaction\\s+fee|fee|ref(?:erence)?|txn))")
                .find(normalized)?.groupValues?.getOrNull(1)?.trim()?.take(80).orEmpty()
        } else {
            Regex("(?i)\\bfrom\\s+([\\p{L}][\\p{L} .'’-]{0,79}?)(?=\\s*(?:\\.|,|;|$|ref(?:erence)?|txn))")
                .find(normalized)?.groupValues?.getOrNull(1)?.trim()?.take(80).orEmpty()
        }

        return MessageParseResult.Parsed(
            ImportedTransactionCandidate(
                provider = provider,
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
        val match = Regex("(?<!\\d)(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})(?!\\d)").find(message) ?: return null
        val (first, second, year) = match.destructured
        return try {
            LocalDate.of(year.toInt(), second.toInt(), first.toInt())
        } catch (_: DateTimeException) {
            null
        }
    }
}
