package com.mizani.money.core.money

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats zero-decimal currency`() {
        assertEquals("USh 25,000", MoneyFormat.format(25_000, Currencies.UGX))
        assertEquals("USh 0", MoneyFormat.format(0, Currencies.UGX))
        assertEquals("-USh 1,500", MoneyFormat.format(-1_500, Currencies.UGX))
    }

    @Test
    fun `formats two-decimal currency`() {
        assertEquals("$ 1,234.50", MoneyFormat.format(123_450, Currencies.USD))
        assertEquals("₦ 10.05", MoneyFormat.format(1_005, Currencies.NGN))
    }

    @Test
    fun `formatBare drops the symbol but keeps the minus sign`() {
        assertEquals("25,000", MoneyFormat.formatBare(25_000, Currencies.UGX))
        assertEquals("-1,500", MoneyFormat.formatBare(-1_500, Currencies.UGX))
        assertEquals("1,234.50", MoneyFormat.formatBare(123_450, Currencies.USD))
        assertEquals("-1,234.50", MoneyFormat.formatBare(-123_450, Currencies.USD))
        assertEquals("0", MoneyFormat.formatBare(0, Currencies.UGX))
    }

    @Test
    fun `parses grouped and decimal input`() {
        assertEquals(1_500_000L, MoneyFormat.parse("1,500,000", Currencies.UGX))
        assertEquals(1_500_000L, MoneyFormat.parse(" 1 500 000 ", Currencies.UGX))
        assertEquals(123_450L, MoneyFormat.parse("1234.50", Currencies.USD))
        assertEquals(5L, MoneyFormat.parse(".05", Currencies.USD))
    }

    @Test
    fun `rejects malformed input`() {
        assertNull(MoneyFormat.parse("", Currencies.UGX))
        assertNull(MoneyFormat.parse("abc", Currencies.UGX))
        assertNull(MoneyFormat.parse("12.3.4", Currencies.UGX))
        assertNull(MoneyFormat.parse("999999999999999999999999", Currencies.UGX))
    }

    @Test
    fun `rounds half up to the nearest minor unit`() {
        assertEquals(1235L, MoneyFormat.parse("12.349", Currencies.USD))
    }

    @Test
    fun `input round trip preserves value`() {
        val currency = Currencies.GHS
        listOf(0L, 1L, 999L, 123_456L).forEach { value ->
            val input = MoneyFormat.toInput(value, currency)
            assertEquals(value, MoneyFormat.parse(input.ifBlank { "0" }, currency))
        }
    }

    @Test
    fun `money arithmetic is exact`() {
        val a = Money(10_000, "UGX")
        val b = Money(2_500, "UGX")
        assertEquals(Money(12_500, "UGX"), a + b)
        assertEquals(Money(7_500, "UGX"), a - b)
        assertEquals(Money(30_000, "UGX"), a * 3)
    }

    @Test
    fun `currency mismatch is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(100, "UGX") + Money(100, "KES")
        }
    }

    @Test
    fun `zero-decimal currency rounds fractional input half-up`() {
        // UGX has no minor units; a fractional major amount is rounded to the nearest whole
        // unit using HALF_UP. This is the documented behaviour for zero-decimal currencies.
        assertEquals(1_235L, MoneyFormat.parse("1234.50", Currencies.UGX))
        assertEquals(1_234L, MoneyFormat.parse("1234.49", Currencies.UGX))
        assertEquals(1_235L, MoneyFormat.parse("1234.5", Currencies.UGX))
    }

    @Test
    fun `two-decimal currency preserves fractional input exactly`() {
        assertEquals(1_234_500L, MoneyFormat.parse("12345.00", Currencies.USD))
        assertEquals(1_234_567L, MoneyFormat.parse("12345.67", Currencies.USD))
    }

    @Test
    fun `foreign currency metadata uses ISO minor units`() {
        assertEquals(0, Currencies.byCode("JPY").minorUnits)
        assertEquals(3, Currencies.byCode("BHD").minorUnits)
        assertEquals(1_000L, Currencies.byCode("BHD").scaleFactor)
        assertTrue(Currencies.transactionCurrencies.any { it.code == "CAD" })
    }

    @Test
    fun `transaction currency choices omit discontinued codes but retain them for old records`() {
        val selectableCodes = Currencies.transactionCurrencies.map { it.code }.toSet()

        assertTrue("ADP" !in selectableCodes)
        assertTrue("AFA" !in selectableCodes)
        assertTrue("ALK" !in selectableCodes)
        assertEquals("ADP", Currencies.byCode("ADP").code)
        assertEquals("AFA", Currencies.byCode("AFA").code)
    }

    @Test
    fun `conversion handles zero and three decimal currencies`() {
        val yen = CurrencyConversion.convertMinorUnits(
            amountMinor = 10_000L,
            source = Currencies.byCode("JPY"),
            target = Currencies.USD,
            rate = BigDecimal("0.0067"),
        )
        val dinar = CurrencyConversion.convertMinorUnits(
            amountMinor = 1_000L,
            source = Currencies.byCode("BHD"),
            target = Currencies.USD,
            rate = BigDecimal("2.65"),
        )

        assertEquals(6_700L, yen)
        assertEquals(265L, dinar)
    }

    @Test
    fun `overflow is explicit rather than silent`() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE, "UGX") + Money(1, "UGX")
        }
    }
}
