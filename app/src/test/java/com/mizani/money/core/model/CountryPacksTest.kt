package com.mizani.money.core.model

import com.mizani.money.core.money.Currencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryPacksTest {

    @Test
    fun `african country packs use their local currency and generic wallet choices`() {
        val senegal = CountryPacks.byCode("SN")

        assertEquals("Senegal", senegal.name)
        assertEquals("XOF", senegal.currencyCode)
        assertTrue(Currencies.all.any { it.code == senegal.currencyCode })
        assertTrue(senegal.walletSuggestions.any { it.type == WalletType.MOBILE_MONEY })
    }

    @Test
    fun `country packs cover global regions with their local currencies`() {
        val expectedCurrencies = mapOf(
            "US" to "USD",
            "GB" to "GBP",
            "BR" to "BRL",
            "IN" to "INR",
            "JP" to "JPY",
            "AU" to "AUD",
        )

        expectedCurrencies.forEach { (countryCode, currencyCode) ->
            val country = CountryPacks.byCode(countryCode)

            assertEquals(countryCode, country.code)
            assertEquals(currencyCode, country.currencyCode)
            assertTrue(Currencies.all.any { it.code == country.currencyCode })
            assertTrue(country.walletSuggestions.any { it.type == WalletType.CASH })
            assertTrue(country.walletSuggestions.any { it.type == WalletType.BANK })
        }
    }

    @Test
    fun `country pack codes are unique and device regions resolve globally`() {
        val codes = CountryPacks.all.map { it.code }

        assertEquals(codes.size, codes.toSet().size)
        assertTrue(codes.size > 200)
        assertEquals("US", CountryPacks.forDeviceRegion("us").code)
        assertEquals("UG", CountryPacks.forDeviceRegion("ZZ").code)
    }

    @Test
    fun `device region preselects a supported country and falls back safely`() {
        assertEquals("KE", CountryPacks.forDeviceRegion("ke").code)
        assertEquals("UG", CountryPacks.forDeviceRegion("").code)
    }
}
