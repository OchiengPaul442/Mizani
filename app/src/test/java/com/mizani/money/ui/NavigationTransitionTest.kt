package com.mizani.money.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTransitionTest {

    @Test
    fun peerTabChangesAreImmediate() {
        assertFalse(shouldAnimateNavigation("home", "transactions"))
        assertFalse(shouldAnimateNavigation("insights", "more"))
    }

    @Test
    fun nestedNavigationKeepsTheShortSlide() {
        assertTrue(shouldAnimateNavigation("home", "wallets"))
        assertTrue(shouldAnimateNavigation("wallets", "more"))
        assertTrue(shouldAnimateNavigation("wallets", "categories"))
    }
}
