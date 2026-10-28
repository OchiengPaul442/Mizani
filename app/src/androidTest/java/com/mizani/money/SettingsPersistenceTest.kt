package com.mizani.money

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.ColorTheme
import com.mizani.money.core.model.ThemeMode
import com.mizani.money.data.repo.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The app keeps its settings in one stable DataStore across app object recreation. */
@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    private lateinit var app: MizaniApplication

    @Before
    fun setUp() {
        TestState.reset()
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        TestState.reset()
    }

    @Test
    fun appLockPreferencesAndLegalAcceptanceSurviveRepositoryRecreation() = runBlocking {
        val original = app.container.settingsRepository
        original.completeOnboarding("UG", "UGX")
        original.setTheme(ThemeMode.DARK)
        original.setColorTheme(ColorTheme.OCEAN)
        original.setPin("123456")
        original.recordLegalAcceptance(
            LegalDocuments.TERMS_VERSION,
            LegalDocuments.PRIVACY_VERSION,
            acceptedAtMillis = 1234L,
        )

        val restored = SettingsRepository(app)
        val settings = restored.settings.first()

        assertTrue(settings.onboardingComplete)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(ColorTheme.OCEAN, settings.colorTheme)
        assertTrue(settings.lockEnabled)
        assertTrue(settings.hasPin)
        assertTrue(settings.hasAcceptedCurrentLegalDocuments)
        assertTrue(restored.verifyPin("123456"))
    }
}
