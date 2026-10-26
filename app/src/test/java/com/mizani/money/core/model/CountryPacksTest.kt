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
    fun `device region preselects a supported country and falls back safely`() {
        assertEquals("KE", CountryPacks.forDeviceRegion("ke").code)
        assertEquals("UG", CountryPacks.forDeviceRegion("US").code)
    }
}
