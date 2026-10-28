package com.mizani.money

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.model.ThemeMode
import com.mizani.money.ui.theme.MizaniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun splashShowsCenteredBrandMarkWithoutLoaderCopy() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MizaniTheme(themeMode = ThemeMode.LIGHT) {
                SplashScreen()
            }
        }

        check(compose.onAllNodesWithContentDescription("Mizani").fetchSemanticsNodes().isNotEmpty())
        check(compose.onAllNodesWithText("Loading your data…").fetchSemanticsNodes().isEmpty())
        check(compose.onAllNodesWithText("Your money, in balance.").fetchSemanticsNodes().isEmpty())
    }
}
