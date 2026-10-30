package com.mizani.money.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mizani.money.di.AppContainer
import com.mizani.money.core.time.PeriodPreset
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.model.ColorTheme
import com.mizani.money.core.model.DarkThemeStyle
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.Currency
import com.mizani.money.core.importing.ImportedTransactionCandidate
import com.mizani.money.core.importing.MessageParseResult
import com.mizani.money.core.importing.TransactionMessageParser
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
    val walletBalancesVisible: Boolean = false,
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

private sealed interface SettingsReadState {
    object Loading : SettingsReadState
    data class Ready(val settings: AppSettings) : SettingsReadState
    object Failed : SettingsReadState
}

class AppViewModel(private val container: AppContainer) : ViewModel() {

    lateinit var finance: FinanceRepository
        private set
    val exchangeRates: ExchangeRateRepository = container.exchangeRateRepository
    val settingsRepository: SettingsRepository = container.settingsRepository
    lateinit var exporter: ExportManager
        private set

    val today: LocalDate = LocalDate.now()

    private val settingsAttempt = MutableStateFlow(0)
    private val settingsReadState = MutableStateFlow<SettingsReadState>(SettingsReadState.Loading)
    private val snapshotFlow = MutableStateFlow<LedgerSnapshot?>(null)
    private val startupError = MutableStateFlow(false)
    private val startupAttempt = MutableStateFlow(0)
    private val unlocked = MutableStateFlow(false)
    private val _walletBalancesVisible = MutableStateFlow(false)
    private val _pendingImport = MutableStateFlow<ImportedTransactionCandidate?>(null)
    val pendingImport: StateFlow<ImportedTransactionCandidate?> = _pendingImport
    private val _sharedImportReady = MutableStateFlow(false)
    val sharedImportReady: StateFlow<Boolean> = _sharedImportReady
    private val _sharedImportNotice = MutableStateFlow<String?>(null)
    val sharedImportNotice: StateFlow<String?> = _sharedImportNotice

    /** Timestamp of the last backgrounding; null while foregrounded. */
    var backgroundedAt: Long? = null
        private set

    private val _selectedPreset = MutableStateFlow(PeriodPreset.THIS_MONTH)
    val selectedPreset: StateFlow<PeriodPreset> = _selectedPreset

    private val _customRange = MutableStateFlow<DateRange?>(null)
    val customRange: StateFlow<DateRange?> = _customRange

    val uiState: StateFlow<AppUiState> = combine(
        settingsReadState,
        snapshotFlow,
        unlocked,
        startupError,
        _walletBalancesVisible,
    ) { settingsState, snapshot, isUnlocked, hasStartupError, showWalletBalances ->
        if (hasStartupError || settingsState is SettingsReadState.Failed) {
            AppUiState(loading = false, startupError = true)
        } else if (settingsState !is SettingsReadState.Ready || snapshot == null) {
            AppUiState(loading = true)
        } else {
            val settings = settingsState.settings
            AppUiState(
                loading = false,
                settings = settings,
                snapshot = snapshot,
                locked = settings.lockEnabled && settings.hasPin && !isUnlocked,
                walletBalancesVisible = showWalletBalances && !(settings.lockEnabled && settings.hasPin && !isUnlocked),
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    init {
        viewModelScope.launch {
            settingsAttempt.collectLatest {
                settingsReadState.value = SettingsReadState.Loading
                try {
                    settingsRepository.settings.collect { settings ->
                        settingsReadState.value = SettingsReadState.Ready(settings)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Never substitute empty settings: that could silently disable the app lock.
                    settingsReadState.value = SettingsReadState.Failed
                }
            }
        }

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
                    repository.snapshot.collect { snapshot ->
                        snapshotFlow.value = snapshot
                        try {
                            container.commitmentReminderScheduler.reconcile(snapshot)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Reminder repair must never prevent access to the encrypted ledger.
                        }
                    }
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
        settingsAttempt.value += 1
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
        hideWalletBalances()
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
        hideWalletBalances()
    }

    fun revealWalletBalances() {
        val currentSettings = (settingsReadState.value as? SettingsReadState.Ready)?.settings
        if (currentSettings?.hasPin == true) _walletBalancesVisible.value = true
    }

    fun hideWalletBalances() {
        _walletBalancesVisible.value = false
    }

    suspend fun prepareTransactionImport(
        message: String,
        providerHint: String? = null,
    ): Result<ImportedTransactionCandidate> = runCatching {
        require(message.length <= TransactionMessageParser.MAX_MESSAGE_LENGTH) {
            "This message is too long to import. Paste only the transaction confirmation."
        }
        val candidate = when (val parsed = TransactionMessageParser.parse(message, today, providerHint)) {
            is MessageParseResult.Parsed -> parsed.candidate
            is MessageParseResult.Rejected -> throw IllegalArgumentException(
                when (parsed.reason) {
                    MessageParseResult.Reason.TOO_LONG -> "This message is too long to import."
                    MessageParseResult.Reason.UNSUPPORTED_PROVIDER -> "This message is not from a supported mobile money provider."
                    MessageParseResult.Reason.UNSUPPORTED_MESSAGE -> "We couldn't identify a complete transaction confirmation."
                },
            )
        }
        val workspace = finance.currentWorkspace() ?: error("Set up a workspace before importing transactions")
        require(!finance.hasImportedReference(candidate.provider, candidate.reference)) {
            "This transaction reference has already been imported."
        }
        _sharedImportNotice.value = null
        _sharedImportReady.value = false
        _pendingImport.value = candidate
        candidate
    }

    suspend fun stageSharedTransaction(message: String, providerHint: String? = null) {
        val current = uiState.first { !it.loading }
        // Don't hold incoming financial text while onboarding, legal consent, or the app lock is active.
        if (current.startupError || current.locked || !current.settings.onboardingComplete ||
            !current.settings.hasAcceptedCurrentLegalDocuments || current.snapshot.workspace == null
        ) return
        prepareTransactionImport(message, providerHint)
            .onSuccess { _sharedImportReady.value = true }
            .onFailure {
                _sharedImportNotice.value = it.message ?: "We couldn't read this transaction message."
            }
    }

    fun setSharedImportNotice(message: String) {
        _sharedImportNotice.value = message.take(160)
        _sharedImportReady.value = false
    }

    fun clearPendingImport() {
        _pendingImport.value = null
        _sharedImportNotice.value = null
        _sharedImportReady.value = false
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

    suspend fun setColorTheme(theme: ColorTheme) {
        settingsRepository.setColorTheme(theme)
    }

    suspend fun setCustomAccentColor(hex: String?) {
        settingsRepository.setCustomAccentColor(hex)
    }

    suspend fun resetAccentColor() {
        settingsRepository.resetAccentColor()
    }

    suspend fun setDarkThemeStyle(style: DarkThemeStyle) {
        settingsRepository.setDarkThemeStyle(style)
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
        if (updatedSettings.dailyReminderEnabled || updatedSettings.habitNotificationsEnabled) {
            container.dailyReminderScheduler.update(updatedSettings)
        }
    }

    suspend fun setHabitNotificationsEnabled(enabled: Boolean): Result<Unit> = runCatching {
        settingsRepository.setHabitNotificationsEnabled(enabled)
        container.dailyReminderScheduler.update(settingsRepository.settings.first())
    }

    suspend fun setHabitSpendingNotificationsEnabled(enabled: Boolean): Result<Unit> =
        updateHabitNotificationCategory { settingsRepository.setHabitSpendingNotificationsEnabled(enabled) }

    suspend fun setHabitIncomeNotificationsEnabled(enabled: Boolean): Result<Unit> =
        updateHabitNotificationCategory { settingsRepository.setHabitIncomeNotificationsEnabled(enabled) }

    suspend fun setHabitInvestmentNotificationsEnabled(enabled: Boolean): Result<Unit> =
        updateHabitNotificationCategory { settingsRepository.setHabitInvestmentNotificationsEnabled(enabled) }

    suspend fun setHabitDebtNotificationsEnabled(enabled: Boolean): Result<Unit> =
        updateHabitNotificationCategory { settingsRepository.setHabitDebtNotificationsEnabled(enabled) }

    suspend fun setHabitGoalNotificationsEnabled(enabled: Boolean): Result<Unit> =
        updateHabitNotificationCategory { settingsRepository.setHabitGoalNotificationsEnabled(enabled) }

    private suspend fun updateHabitNotificationCategory(update: suspend () -> Unit): Result<Unit> =
        runCatching {
            update()
            container.dailyReminderScheduler.update(settingsRepository.settings.first())
        }

    suspend fun restoreBackup(bytes: ByteArray, passphrase: String): Result<Unit> = runCatching {
        stopReminderDelivery()
        try {
            exporter.restore(bytes, passphrase)
        } finally {
            restoreReminderDelivery()
        }
    }

    private suspend fun stopReminderDelivery() {
        container.dailyReminderScheduler.cancelAll()
        container.commitmentReminderScheduler.cancelAll()
    }

    private suspend fun restoreReminderDelivery() {
        container.dailyReminderScheduler.ensureScheduled(settingsRepository.settings.first())
        container.financeRepository.snapshot.first().let { snapshot ->
            container.commitmentReminderScheduler.reconcile(snapshot)
        }
    }

    suspend fun deleteAllData(): Result<Unit> = runCatching {
        stopReminderDelivery()
        container.exchangeRateRepository.clearCache()
        finance.clearAllFinancialData()
        settingsRepository.resetAll()
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
