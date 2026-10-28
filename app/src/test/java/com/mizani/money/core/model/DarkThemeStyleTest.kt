package com.mizani.money.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DarkThemeStyleTest {
    @Test
    fun legacyAndUnknownDarkStylesResolveToSoftCharcoal() {
        assertEquals(DarkThemeStyle.SOFT_CHARCOAL, DarkThemeStyle.fromStoredValue("DEEP_TEAL"))
        assertEquals(DarkThemeStyle.SOFT_CHARCOAL, DarkThemeStyle.fromStoredValue("OLED_BLACK"))
        assertEquals(DarkThemeStyle.SOFT_CHARCOAL, DarkThemeStyle.fromStoredValue("future-style"))
        assertEquals(DarkThemeStyle.SOFT_CHARCOAL, DarkThemeStyle.fromStoredValue(null))
    }

    @Test
    fun currentDarkStylesRemainSelectableFromSavedPreferences() {
        DarkThemeStyle.entries.forEach { style ->
            assertEquals(style, DarkThemeStyle.fromStoredValue(style.name))
        }
    }
}
