package com.mizani.money.data.repo

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mizani.money.core.money.Currency
import com.mizani.money.core.model.ThemeMode
import com.mizani.money.core.model.ColorTheme
import com.mizani.money.core.model.DarkThemeStyle
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.security.PinDefaults
import com.mizani.money.core.security.PinHasher
import com.mizani.money.core.time.DailyReminderSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.mizaniSettings: DataStore<Preferences> by preferencesDataStore(name = "mizani_settings")

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val countryCode: String = "UG",
    val currencyCode: String = "UGX",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.MIZANI,
    val darkThemeStyle: DarkThemeStyle = DarkThemeStyle.DEEP_TEAL,
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderMinuteOfDay: Int = DailyReminderSchedule.DEFAULT_MINUTE_OF_DAY,
    val dailyReminderScheduleVersion: Long = 0L,
    val lastDailyReminderEpochDay: Long? = null,
    val acceptedTermsVersion: String? = null,
    val acceptedPrivacyVersion: String? = null,
    val legalAcceptedAtMillis: Long? = null,
) {
    val currency: Currency
        get() = com.mizani.money.core.money.Currencies.byCode(currencyCode)

    val hasAcceptedCurrentLegalDocuments: Boolean
        get() = LegalDocuments.isCurrentAcceptance(acceptedTermsVersion, acceptedPrivacyVersion)
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val COUNTRY = stringPreferencesKey("country_code")
        val CURRENCY = stringPreferencesKey("currency_code")
        val THEME = stringPreferencesKey("theme_mode")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val DARK_THEME_STYLE = stringPreferencesKey("dark_theme_style")
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        // Persisted failed-PIN backoff state (not schema; plain DataStore entries).
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LAST_FAILURE_AT = longPreferencesKey("pin_last_failure_at")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_MINUTE_OF_DAY = intPreferencesKey("daily_reminder_minute_of_day")
        val DAILY_REMINDER_SCHEDULE_VERSION = longPreferencesKey("daily_reminder_schedule_version")
        val LAST_DAILY_REMINDER_EPOCH_DAY = longPreferencesKey("last_daily_reminder_epoch_day")
        val ACCEPTED_TERMS_VERSION = stringPreferencesKey("accepted_terms_version")
        val ACCEPTED_PRIVACY_VERSION = stringPreferencesKey("accepted_privacy_version")
        val LEGAL_ACCEPTED_AT = longPreferencesKey("legal_accepted_at")
    }

    val settings: Flow<AppSettings> = context.mizaniSettings.data
        .map { preferences ->
            AppSettings(
                onboardingComplete = preferences[Keys.ONBOARDING_COMPLETE] ?: false,
                countryCode = preferences[Keys.COUNTRY] ?: "UG",
                currencyCode = preferences[Keys.CURRENCY] ?: "UGX",
                themeMode = runCatching {
                    ThemeMode.valueOf(preferences[Keys.THEME] ?: ThemeMode.SYSTEM.name)
                }.getOrDefault(ThemeMode.SYSTEM),
                colorTheme = runCatching {
                    ColorTheme.valueOf(preferences[Keys.COLOR_THEME] ?: ColorTheme.MIZANI.name)
                }.getOrDefault(ColorTheme.MIZANI),
                darkThemeStyle = runCatching {
                    DarkThemeStyle.valueOf(preferences[Keys.DARK_THEME_STYLE] ?: DarkThemeStyle.DEEP_TEAL.name)
                }.getOrDefault(DarkThemeStyle.DEEP_TEAL),
                lockEnabled = preferences[Keys.LOCK_ENABLED] ?: false,
                biometricEnabled = preferences[Keys.BIOMETRIC_ENABLED] ?: false,
                hasPin = preferences[Keys.PIN_HASH] != null && preferences[Keys.PIN_SALT] != null,
                dailyReminderEnabled = preferences[Keys.DAILY_REMINDER_ENABLED] ?: false,
                dailyReminderMinuteOfDay = (preferences[Keys.DAILY_REMINDER_MINUTE_OF_DAY]
                    ?: DailyReminderSchedule.DEFAULT_MINUTE_OF_DAY)
                    .coerceIn(0, DailyReminderSchedule.MINUTES_PER_DAY - 1),
                dailyReminderScheduleVersion = preferences[Keys.DAILY_REMINDER_SCHEDULE_VERSION] ?: 0L,
                lastDailyReminderEpochDay = preferences[Keys.LAST_DAILY_REMINDER_EPOCH_DAY],
                acceptedTermsVersion = preferences[Keys.ACCEPTED_TERMS_VERSION],
                acceptedPrivacyVersion = preferences[Keys.ACCEPTED_PRIVACY_VERSION],
                legalAcceptedAtMillis = preferences[Keys.LEGAL_ACCEPTED_AT],
            )
        }

    suspend fun recordLegalAcceptance(termsVersion: String, privacyVersion: String, acceptedAtMillis: Long) {
        context.mizaniSettings.edit { preferences ->
            preferences[Keys.ACCEPTED_TERMS_VERSION] = termsVersion
            preferences[Keys.ACCEPTED_PRIVACY_VERSION] = privacyVersion
            preferences[Keys.LEGAL_ACCEPTED_AT] = acceptedAtMillis
        }
    }

    suspend fun completeOnboarding(countryCode: String, currencyCode: String) {
        context.mizaniSettings.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETE] = true
            preferences[Keys.COUNTRY] = countryCode
            preferences[Keys.CURRENCY] = currencyCode
        }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.mizaniSettings.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setColorTheme(theme: ColorTheme) {
        context.mizaniSettings.edit { it[Keys.COLOR_THEME] = theme.name }
    }

    suspend fun setDarkThemeStyle(style: DarkThemeStyle) {
        context.mizaniSettings.edit { it[Keys.DARK_THEME_STYLE] = style.name }
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.mizaniSettings.edit { preferences ->
            preferences[Keys.DAILY_REMINDER_ENABLED] = enabled
            preferences[Keys.DAILY_REMINDER_SCHEDULE_VERSION] =
                (preferences[Keys.DAILY_REMINDER_SCHEDULE_VERSION] ?: 0L) + 1L
        }
    }

    suspend fun setDailyReminderMinuteOfDay(minuteOfDay: Int) {
        require(minuteOfDay in 0 until DailyReminderSchedule.MINUTES_PER_DAY)
        context.mizaniSettings.edit { preferences ->
            preferences[Keys.DAILY_REMINDER_MINUTE_OF_DAY] = minuteOfDay
            preferences[Keys.DAILY_REMINDER_SCHEDULE_VERSION] =
                (preferences[Keys.DAILY_REMINDER_SCHEDULE_VERSION] ?: 0L) + 1L
        }
    }

    suspend fun bumpDailyReminderScheduleVersion() {
        context.mizaniSettings.edit { preferences ->
            preferences[Keys.DAILY_REMINDER_SCHEDULE_VERSION] =
                (preferences[Keys.DAILY_REMINDER_SCHEDULE_VERSION] ?: 0L) + 1L
        }
    }

    suspend fun markDailyReminderShown(epochDay: Long) {
        context.mizaniSettings.edit { it[Keys.LAST_DAILY_REMINDER_EPOCH_DAY] = epochDay }
    }

    suspend fun setCountry(countryCode: String, currencyCode: String) {
        context.mizaniSettings.edit {
            it[Keys.COUNTRY] = countryCode
            it[Keys.CURRENCY] = currencyCode
        }
    }

    suspend fun setPin(pin: String) {
        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        context.mizaniSettings.edit { preferences ->
            preferences[Keys.PIN_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
            preferences[Keys.PIN_HASH] = Base64.encodeToString(hash, Base64.NO_WRAP)
            preferences[Keys.LOCK_ENABLED] = true
            // A fresh PIN starts with a clean attempt history.
            preferences.remove(Keys.PIN_FAILED_ATTEMPTS)
            preferences.remove(Keys.PIN_LAST_FAILURE_AT)
        }
    }

    /**
     * Verifies the PIN with the persisted failed-attempt backoff enforced.
     * While blocked it returns false immediately without hashing and without
     * counting a new failure. A successful verification clears the failure
     * state; a failed one records the attempt and timestamp. Never logs the
     * PIN or exception details.
     */
    suspend fun verifyPin(pin: String): Boolean {
        val current = context.mizaniSettings.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .first()
        val now = System.currentTimeMillis()
        val failures = current[Keys.PIN_FAILED_ATTEMPTS] ?: 0
        val lastFailureAt = current[Keys.PIN_LAST_FAILURE_AT] ?: 0L
        if (PinDefaults.blockedRemainingMs(failures, lastFailureAt, now) > 0L) return false
        val salt = current[Keys.PIN_SALT]?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = current[Keys.PIN_HASH]?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val ok = withContext(Dispatchers.Default) { PinHasher.verify(pin, salt, expected) }
        if (ok) {
            context.mizaniSettings.edit { preferences ->
                preferences.remove(Keys.PIN_FAILED_ATTEMPTS)
                preferences.remove(Keys.PIN_LAST_FAILURE_AT)
            }
        } else {
            context.mizaniSettings.edit { preferences ->
                preferences[Keys.PIN_FAILED_ATTEMPTS] = failures + 1
                preferences[Keys.PIN_LAST_FAILURE_AT] = now
            }
        }
        return ok
    }

    /** Milliseconds until the next PIN attempt is allowed; 0 when no backoff is active. */
    suspend fun pinBackoffRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        val current = context.mizaniSettings.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .first()
        return PinDefaults.blockedRemainingMs(
            failedAttempts = current[Keys.PIN_FAILED_ATTEMPTS] ?: 0,
            lastFailureAtMs = current[Keys.PIN_LAST_FAILURE_AT] ?: 0L,
            nowMs = nowMs,
        )
    }

    suspend fun disableLock() {
        context.mizaniSettings.edit { preferences ->
            preferences[Keys.LOCK_ENABLED] = false
            preferences[Keys.BIOMETRIC_ENABLED] = false
            preferences.remove(Keys.PIN_HASH)
            preferences.remove(Keys.PIN_SALT)
            preferences.remove(Keys.PIN_FAILED_ATTEMPTS)
            preferences.remove(Keys.PIN_LAST_FAILURE_AT)
        }
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.mizaniSettings.edit { it[Keys.LOCK_ENABLED] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.mizaniSettings.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun resetAll() {
        context.mizaniSettings.edit { it.clear() }
    }
}
