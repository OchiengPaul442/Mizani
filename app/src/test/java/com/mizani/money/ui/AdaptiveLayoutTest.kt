package com.mizani.money.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun `without a separating feature the full window remains available`() {
        assertEquals(
            SafeWindowPane(0, 0, 1200, 800),
            AdaptiveLayout.largestSafePane(1200, 800, feature = null),
        )
    }

    @Test
    fun `vertical hinge selects the larger uninterrupted pane`() {
        assertEquals(
            SafeWindowPane(leftPx = 580, topPx = 0, widthPx = 620, heightPx = 900),
            AdaptiveLayout.largestSafePane(
                windowWidthPx = 1200,
                windowHeightPx = 900,
                feature = FoldFeatureBounds(560, 0, 580, 900, vertical = true, separating = true),
            ),
        )
    }

    @Test
    fun `equal horizontal panes prefer the leading pane for stable navigation`() {
        assertEquals(
            SafeWindowPane(leftPx = 0, topPx = 0, widthPx = 1000, heightPx = 480),
            AdaptiveLayout.largestSafePane(
                windowWidthPx = 1000,
                windowHeightPx = 1000,
                feature = FoldFeatureBounds(0, 480, 1000, 520, vertical = false, separating = true),
            ),
        )
    }
}
