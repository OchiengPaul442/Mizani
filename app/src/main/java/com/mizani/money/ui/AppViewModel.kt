package com.mizani.money.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mizani.money.di.AppContainer
import com.mizani.money.core.time.PeriodPreset
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.Currency
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.Periods
import com.mizani.money.data.repo.AppSettings
import com.mizani.money.data.repo.FinanceRepository
import com.mizani.money.data.repo.ExchangeRateRepository
import com.mizani.money.data.repo.LedgerSnapshot
import com.mizani.money.data.repo.SettingsRepository
import com.mizani.money.domain.analytics.AnalyticsEngine
import com.mizani.money.domain.analytics.DashboardMetrics
import com.mizani.money.domain.analytics.InsightEngine
import com.mizani.money.domain.export.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class AppUiState(
    val loading: Boolean = true,
    val startupError: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val snapshot: LedgerSnapshot = LedgerSnapshot(),
    val locked: Boolean = false,
) {
    val currency: Currency
        get() = Currencies.byCode(snapshot.currencyCode.ifBlank { settings.currencyCode })
}

/** Outcome of a PIN attempt, separating a wrong PIN from an active backoff. */
sealed interface PinAttemptResult {
    object Success : PinAttemptResult
    object WrongPin : PinAttemptResult
    data class Blocked(val remainingMs: Long) : PinAttemptResult
}

/** User-safe message shown while the failed-PIN backoff is active. */
const val PIN_RETRY_MESSAGE = "Too many attempts. Please wait a moment, then try again."

class AppViewModel(private val container: AppContainer) : ViewModel() {

    lateinit var finance: FinanceRepository
        private set
    val exchangeRates: ExchangeRateRepository = container.exchangeRateRepository
    val settingsRepository: SettingsRepository = container.settingsRepository
    lateinit var exporter: ExportManager
        private set

    val today: LocalDate = LocalDate.now()

    private val settingsFlow = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val snapshotFlow = MutableStateFlow<LedgerSnapshot?>(null)
    private val startupError = MutableStateFlow(false)
    private val startupAttempt = MutableStateFlow(0)
    private val unlocked = MutableStateFlow(false)

    /** Timestamp of the last backgrounding; null while foregrounded. */
    var backgroundedAt: Long? = null
        private set

    private val _selectedPreset = MutableStateFlow(PeriodPreset.THIS_MONTH)
    val selectedPreset: StateFlow<PeriodPreset> = _selectedPreset

    private val _customRange = MutableStateFlow<DateRange?>(null)
    val customRange: StateFlow<DateRange?> = _customRange

    val uiState: StateFlow<AppUiState> = combine(
        settingsFlow,
        snapshotFlow,
        unlocked,
        startupError,
    ) { settings, snapshot, isUnlocked, hasStartupError ->
        if (hasStartupError) {
            AppUiState(loading = false, startupError = true)
        } else if (settings == null || snapshot == null) {
            AppUiState(loading = true)
        } else {
            AppUiState(
                loading = false,
                settings = settings,
                snapshot = snapshot,
                locked = settings.lockEnabled && settings.hasPin && !isUnlocked,
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    init {
        // Keystore access and encrypted Room initialization can be slow on entry-level devices.
        // Keep both off the main thread so the splash appears promptly and stays animated.
        viewModelScope.launch {
            startupAttempt.collectLatest {
                snapshotFlow.value = null
                startupError.value = false
                try {
                    val (repository, exportManager) = withContext(Dispatchers.IO) {
                        container.awaitSqlCipherReady()
                        container.financeRepository to container.exportManager
                    }
                    finance = repository
                    exporter = exportManager
                    repository.snapshot.collect { snapshotFlow.value = it }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: LinkageError) {
                    snapshotFlow.value = null
                    startupError.value = true
                } catch (_: Exception) {
                    snapshotFlow.value = null
                    startupError.value = true
                }
            }
        }
    }

    fun retryStartup() {
        startupAttempt.value += 1
    }

    val periodRange: StateFlow<DateRange> = combine(_selectedPreset, _customRange) { preset, custom ->
        if (preset == PeriodPreset.CUSTOM && custom != null) {
            custom
        } else {
            Periods.preset(preset, today)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Periods.monthOf(LocalDate.now()))

    val metrics: StateFlow<DashboardMetrics?> = combine(snapshotFlow, periodRange) { snapshot, range ->
        snapshot?.let { AnalyticsEngine(it).dashboard(range, today) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val insights: StateFlow<List<com.mizani.money.core.model.Insight>> =
        combine(snapshotFlow, metrics) { snapshot, dashboard ->
            if (snapshot == null || dashboard == null) {
                emptyList()
            } else {
                InsightEngine(
                    data = snapshot,
                    engine = AnalyticsEngine(snapshot),
                    currency = Currencies.byCode(snapshot.currencyCode),
                ).generate(dashboard, today)
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectPreset(preset: PeriodPreset) {
        _selectedPreset.value = preset
        if (preset != PeriodPreset.CUSTOM) {
            _customRange.value = null
        }
    }

    fun setCustomRange(range: DateRange) {
        _customRange.value = range
        _selectedPreset.value = PeriodPreset.CUSTOM
    }

    suspend fun completeOnboarding(
        countryCode: String,
        currencyCode: String,
        walletName: String,
        walletType: WalletType,
        openingBalanceMinor: Long,
    ): Result<Unit> = runCatching {
        finance.createWorkspace(
            countryCode = countryCode,
            currencyCode = currencyCode,
            walletName = walletName,
            walletType = walletType,
            openingBalanceMinor = openingBalanceMinor,
        )
        settingsRepository.completeOnboarding(countryCode, currencyCode)
        unlocked.value = true
    }

    suspend fun acceptCurrentLegalDocuments(): Result<Unit> = runCatching {
        settingsRepository.recordLegalAcceptance(
            termsVersion = LegalDocuments.TERMS_VERSION,
            privacyVersion = LegalDocuments.PRIVACY_VERSION,
            acceptedAtMillis = System.currentTimeMillis(),
        )
    }

    /**
     * Verifies the PIN through the repository's persisted backoff and reports
     * whether the attempt succeeded, was wrong, or was refused because a
     * backoff is still active (in which case no hashing took place).
     */
    suspend fun attemptUnlockWithPin(pin: String): PinAttemptResult {
        val ok = settingsRepository.verifyPin(pin)
        if (ok) {
            unlocked.value = true
            return PinAttemptResult.Success
        }
        val remaining = settingsRepository.pinBackoffRemainingMs()
        return if (remaining > 0L) PinAttemptResult.Blocked(remaining) else PinAttemptResult.WrongPin
    }

    /** Kept for compatibility: true only when the PIN verifies and unlock succeeds. */
    suspend fun unlockWithPin(pin: String): Boolean =
        attemptUnlockWithPin(pin) is PinAttemptResult.Success

    /** Milliseconds until a PIN attempt is allowed again; 0 when not blocked. */
    suspend fun pinBackoffRemainingMs(): Long = settingsRepository.pinBackoffRemainingMs()

    fun onBiometricUnlocked() {
        unlocked.value = true
    }

    fun onBackgrounded() {
        backgroundedAt = System.currentTimeMillis()
    }

    /** Re-entering the foreground keeps the session only within the grace period. */
    fun onForeground() {
        val at = backgroundedAt ?: return
        backgroundedAt = null
        if (System.currentTimeMillis() - at > LOCK_GRACE_PERIOD_MS) {
            unlocked.value = false
        }
    }

    fun lockNow() {
        backgroundedAt = null
        unlocked.value = false
    }

    suspend fun enableLock(pin: String): Result<Unit> = runCatching {
        settingsRepository.setPin(pin)
        unlocked.value = true
    }

    suspend fun disableLock() {
        settingsRepository.disableLock()
    }

    suspend fun setTheme(mode: com.mizani.money.core.model.ThemeMode) {
        settingsRepository.setTheme(mode)
    }

    suspend fun setCountry(countryCode: String, currencyCode: String) {
        settingsRepository.setCountry(countryCode, currencyCode)
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean): Result<Unit> = runCatching {
        settingsRepository.setDailyReminderEnabled(enabled)
        container.dailyReminderScheduler.update(settingsRepository.settings.first())
    }

    suspend fun setDailyReminderMinuteOfDay(minuteOfDay: Int): Result<Unit> = runCatching {
        settingsRepository.setDailyReminderMinuteOfDay(minuteOfDay)
        val updatedSettings = settingsRepository.settings.first()
        if (updatedSettings.dailyReminderEnabled) {
            container.dailyReminderScheduler.update(updatedSettings)
        }
    }

    suspend fun deleteAllData(): Result<Unit> = runCatching {
        finance.clearAllFinancialData()
        settingsRepository.resetAll()
        // Clearing preferences disables the worker even if cancellation is delayed.
        runCatching { container.dailyReminderScheduler.cancel() }
        unlocked.value = false
    }

    fun setBiometric(enabled: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
            onDone()
        }
    }

    companion object {
        /** Time in background after which the app re-locks (e.g. long file-picker sessions). */
        const val LOCK_GRACE_PERIOD_MS = 15_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(container) as T
                }
            }
    }
}
