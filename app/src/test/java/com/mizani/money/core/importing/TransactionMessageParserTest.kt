package com.mizani.money.core.importing

import com.mizani.money.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionMessageParserTest {

    @Test
    fun `parses outgoing MTN confirmation and fee into editable expense suggestion`() {
        val result = TransactionMessageParser.parse(
            "MTN Mobile Money: You have sent UGX 50,000 to JOHN DOE. Transaction fee UGX 500. Ref: ABC123.",
            today = LocalDate.of(2026, 10, 28),
        )

        assertTrue(result is MessageParseResult.Parsed)
        val candidate = (result as MessageParseResult.Parsed).candidate
        assertEquals("mtn_momo", candidate.provider)
        assertEquals("ABC123", candidate.reference)
        assertEquals(TransactionType.EXPENSE, candidate.type)
        assertEquals(50_000L, candidate.amountMinor)
        assertEquals(500L, candidate.feeMinor)
        assertEquals("JOHN DOE", candidate.payee)
        assertEquals(LocalDate.of(2026, 10, 28), candidate.date)
    }

    @Test
    fun `parses principal amount when fee appears first`() {
        val result = TransactionMessageParser.parse(
            "MTN MoMo: Transaction fee UGX 500. You have sent UGX 50,000 to JOHN DOE. Ref: FEEFIRST9.",
        )

        assertTrue(result is MessageParseResult.Parsed)
        val candidate = (result as MessageParseResult.Parsed).candidate
        assertEquals(50_000L, candidate.amountMinor)
        assertEquals(500L, candidate.feeMinor)
    }

    @Test
    fun `parses principal amount immediately before sent to wording`() {
        val result = TransactionMessageParser.parse(
            "MTN MoMo: UGX 7,500 sent to LOCAL SHOP. Ref: BEFORE9.",
        )

        assertTrue(result is MessageParseResult.Parsed)
        val candidate = (result as MessageParseResult.Parsed).candidate
        assertEquals(7_500L, candidate.amountMinor)
        assertEquals(0L, candidate.feeMinor)
    }

    @Test
    fun `does not mistake a balance for a missing transaction amount`() {
        val result = TransactionMessageParser.parse(
            "MTN MoMo: You have sent to JOHN DOE. Your balance is UGX 20,000. Ref: BALANCE9.",
        )

        assertEquals(
            MessageParseResult.Reason.UNSUPPORTED_MESSAGE,
            (result as MessageParseResult.Rejected).reason,
        )
    }

    @Test
    fun `parses Airtel incoming confirmation without assuming a transfer`() {
        val result = TransactionMessageParser.parse(
            "Airtel Money. You have received UGX 125,000 from ALICE. Ref: ZXC998.",
        )

        assertTrue(result is MessageParseResult.Parsed)
        val candidate = (result as MessageParseResult.Parsed).candidate
        assertEquals("airtel_money", candidate.provider)
        assertEquals(TransactionType.INCOME, candidate.type)
        assertEquals(125_000L, candidate.amountMinor)
        assertEquals(0L, candidate.feeMinor)
        assertEquals("ALICE", candidate.payee)
    }

    @Test
    fun `uses provider hint when shared message text omits the sender`() {
        val result = TransactionMessageParser.parse(
            "You have sent UGX 8,000 to SHOP. Ref: KLM456.",
            providerHint = "MTN MoMo",
        )

        assertTrue(result is MessageParseResult.Parsed)
        assertEquals("mtn_momo", (result as MessageParseResult.Parsed).candidate.provider)
    }

    @Test
    fun `rejects unrecognized provider and overlong message`() {
        val noProvider = TransactionMessageParser.parse("You have sent UGX 8,000 to SHOP. Ref: KLM456.")
        val tooLong = TransactionMessageParser.parse("x".repeat(TransactionMessageParser.MAX_MESSAGE_LENGTH + 1))

        assertEquals(MessageParseResult.Reason.UNSUPPORTED_PROVIDER, (noProvider as MessageParseResult.Rejected).reason)
        assertEquals(MessageParseResult.Reason.TOO_LONG, (tooLong as MessageParseResult.Rejected).reason)
    }

    @Test
    fun `outgoing mobile money transfer wording stays an expense suggestion`() {
        val result = TransactionMessageParser.parse(
            "MTN MoMo: You have sent UGX 20,000 to MY OTHER WALLET. Ref: TRANSFER9.",
        )

        assertTrue(result is MessageParseResult.Parsed)
        assertEquals(TransactionType.EXPENSE, (result as MessageParseResult.Parsed).candidate.type)
    }
}
