package com.mizani.money.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.text.format.DateFormat
import android.app.TimePickerDialog
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import com.mizani.money.ui.components.MizaniAlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.CountryPacks
import com.mizani.money.core.model.ColorTheme
import com.mizani.money.core.model.DarkThemeStyle
import com.mizani.money.core.model.ThemeMode
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.R
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.domain.export.BackupSummary
import com.mizani.money.domain.export.BackupFileReader
import com.mizani.money.domain.export.BudgetShareText
import com.mizani.money.feature.lock.PinSetupDialog
import com.mizani.money.feature.lock.PinVerifyDialog
import com.mizani.money.feature.lock.SensitiveActionConfirmationDialog
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.PIN_RETRY_MESSAGE
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniConfirmDialog
import com.mizani.money.ui.components.MizaniInfoRow
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniSectionCard
import com.mizani.money.ui.components.MizaniSectionDivider
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner
import com.mizani.money.ui.theme.colorThemePreviewColor
import com.mizani.money.reminder.DailyReminderNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

private enum class SensitiveDataAction(val description: String) {
    CSV_EXPORT("export transactions as a CSV file"),
    EXCEL_EXPORT("export an Excel workbook"),
    ENCRYPTED_BACKUP("create an encrypted backup"),
    BUDGET_SHARE("share this budget"),
}

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onBack: () -> Unit,
    dataToolsOnly: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = state.settings
    val shareableBudgets = state.snapshot.budgets
        .filter { it.deletedAt == null }
        .sortedByDescending { it.startEpochDay }

    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var budgetToShare by remember { mutableStateOf<BudgetEntity?>(null) }
    var pendingSensitiveAction by remember { mutableStateOf<SensitiveDataAction?>(null) }
    var pendingSensitiveBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var authorizedBudgetToShare by remember { mutableStateOf<BudgetEntity?>(null) }
    var showSensitiveActionConfirmation by remember { mutableStateOf(false) }
    var showPinRequiredForSensitiveAction by remember { mutableStateOf(false) }
    var settingUpPinForSensitiveAction by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showDisablePin by remember { mutableStateOf(false) }
    var showVerifyForChange by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showCsvWarning by remember { mutableStateOf(false) }
    var showExcelWarning by remember { mutableStateOf(false) }
    var showBackupPassphrase by remember { mutableStateOf(false) }
    var showRestorePassphrase by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var backupPassphraseConfirm by remember { mutableStateOf("") }
    var pendingBackupPassphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var creatingBackup by remember { mutableStateOf(false) }
    var checkingBackup by remember { mutableStateOf(false) }
    var restoringBackup by remember { mutableStateOf(false) }
    var reminderStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var showReminderNotificationSettings by rememberSaveable { mutableStateOf(false) }
    var notificationSettingsRevision by remember { mutableStateOf(0) }

    fun requestSensitiveAction(action: SensitiveDataAction, budget: BudgetEntity? = null) {
        error = null
        if (!settings.hasPin) {
            if (budget != null) budgetToShare = null
            showPinRequiredForSensitiveAction = true
            return
        }
        if (budget != null) budgetToShare = null
        pendingSensitiveAction = action
        pendingSensitiveBudget = budget
        showSensitiveActionConfirmation = true
    }

    fun shareBudget(budget: BudgetEntity) {
        val shareText = BudgetShareText.build(budget, state.snapshot, state.currency)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mizani budget plan")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, "Share budget"))
        } catch (_: ActivityNotFoundException) {
            error = "No app is available to share this budget."
        }
        budgetToShare = null
    }

    fun clearSensitiveActionConfirmation(restoreBudgetPreview: Boolean = false) {
        val action = pendingSensitiveAction
        val budget = pendingSensitiveBudget
        showSensitiveActionConfirmation = false
        pendingSensitiveAction = null
        pendingSensitiveBudget = null
        if (restoreBudgetPreview && action == SensitiveDataAction.BUDGET_SHARE) {
            budgetToShare = budget
        }
    }

    fun authorizeSensitiveAction() {
        val action = pendingSensitiveAction
        val budget = pendingSensitiveBudget
        clearSensitiveActionConfirmation()
        when (action) {
            SensitiveDataAction.CSV_EXPORT -> showCsvWarning = true
            SensitiveDataAction.EXCEL_EXPORT -> showExcelWarning = true
            SensitiveDataAction.ENCRYPTED_BACKUP -> showBackupPassphrase = true
            SensitiveDataAction.BUDGET_SHARE -> {
                if (budget != null && shareableBudgets.any { it.id == budget.id }) {
                    authorizedBudgetToShare = budget
                } else {
                    error = "This budget is no longer available to share."
                }
            }
            null -> Unit
        }
    }

    LaunchedEffect(authorizedBudgetToShare) {
        authorizedBudgetToShare?.let { budget ->
            shareBudget(budget)
            authorizedBudgetToShare = null
        }
    }

    fun discardBackupPassphrase() {
        showBackupPassphrase = false
        backupPassphrase = ""
        backupPassphraseConfirm = ""
        pendingBackupPassphrase = ""
    }

    fun discardPendingRestore() {
        showRestorePassphrase = false
        showRestoreConfirm = false
        restorePassphrase = ""
        pendingRestoreBytes = null
        pendingSummary = null
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationSettingsRevision += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        onDispose {
            pendingRestoreBytes?.fill(0)
            backupPassphrase = ""
            backupPassphraseConfirm = ""
            pendingBackupPassphrase = ""
            restorePassphrase = ""
        }
    }
    val notificationsAvailable = remember(notificationSettingsRevision) {
        DailyReminderNotifications.canPost(context)
    }

    val enableDailyReminder: () -> Unit = {
        scope.launch {
            vm.setDailyReminderEnabled(true)
                .onSuccess {
                    reminderStatus = null
                    showReminderNotificationSettings = false
                }
                .onFailure {
                    reminderStatus = context.getString(R.string.settings_reminder_schedule_error)
                }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationSettingsRevision += 1
        if (granted) {
            enableDailyReminder()
        } else {
            reminderStatus = context.getString(R.string.settings_reminder_permission_denied)
            showReminderNotificationSettings = true
        }
    }
    val openReminderNotificationSettings: () -> Unit = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(
                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            )
        }
        runCatching { context.startActivity(intent) }
    }
    val onDailyReminderChanged: (Boolean) -> Unit = { enabled ->
        reminderStatus = null
        showReminderNotificationSettings = false
        if (!enabled) {
            scope.launch {
                vm.setDailyReminderEnabled(false)
                    .onFailure { reminderStatus = context.getString(R.string.settings_reminder_schedule_error) }
            }
        } else {
            DailyReminderNotifications.ensureChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (!DailyReminderNotifications.canPost(context)) {
                reminderStatus = context.getString(R.string.settings_reminder_notifications_disabled)
                showReminderNotificationSettings = true
            } else {
                enableDailyReminder()
            }
        }
    }

    val canBiometric = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = withContext(Dispatchers.IO) { vm.exporter.buildCsv() }
                    writeBytes(context, uri, csv.toByteArray(Charsets.UTF_8))
                }.onSuccess {
                    message = "Transactions exported as CSV. This file is not encrypted."
                    error = null
                }.onFailure {
                    error = it.message ?: "Could not write the CSV"
                }
            }
        }
    }

    val createExcel = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                            vm.exporter.writeExcel(output)
                        } ?: throw IOException("Could not open the selected file for writing")
                    }
                }.onSuccess {
                    message = "Excel workbook saved. It contains financial data and is not encrypted."
                    error = null
                }.onFailure {
                    error = it.message ?: "Could not create the Excel workbook"
                }
            }
        }
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val passphrase = pendingBackupPassphrase
        pendingBackupPassphrase = ""
        if (uri == null) {
            backupPassphrase = ""
            backupPassphraseConfirm = ""
        } else {
            backupPassphrase = ""
            backupPassphraseConfirm = ""
            scope.launch {
                creatingBackup = true
                message = "Creating encrypted backup…"
                try {
                    runCatching {
                        if (passphrase.isBlank()) throw IOException("The passphrase was cleared. Please try again.")
                        val bytes = vm.exporter.buildBackup(passphrase)
                        writeBytes(context, uri, bytes)
                    }.onSuccess {
                        message = "Encrypted backup created. Keep your passphrase safe; it cannot be recovered."
                        error = null
                    }.onFailure {
                        message = null
                        error = it.message ?: "Could not create the backup"
                    }
                } finally {
                    creatingBackup = false
                }
            }
        }
    }

    val openRestore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { BackupFileReader.read(it) }
                            ?: throw IOException("Could not open the selected file")
                    }
                }.onSuccess { bytes ->
                    pendingRestoreBytes = bytes
                    restorePassphrase = ""
                    showRestorePassphrase = true
                    error = null
                }.onFailure {
                    discardPendingRestore()
                    error = it.message ?: "Could not read the selected file"
                }
            }
        }
    }

    if (showSensitiveActionConfirmation) {
        val action = pendingSensitiveAction
        if (action != null) {
            SensitiveActionConfirmationDialog(
                actionDescription = action.description,
                allowBiometrics = settings.biometricEnabled && canBiometric,
                verifyPin = { pin -> vm.settingsRepository.verifyPin(pin) },
                pinBackoffRemainingMs = { vm.settingsRepository.pinBackoffRemainingMs() },
                onAuthorized = ::authorizeSensitiveAction,
                onDismiss = { clearSensitiveActionConfirmation(restoreBudgetPreview = true) },
            )
        }
    }

    if (showPinRequiredForSensitiveAction) {
        MizaniAlertDialog(
            onDismissRequest = { showPinRequiredForSensitiveAction = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("Set up a PIN first") },
            text = {
                Text(
                    "A 6-digit app PIN is required to confirm exports, encrypted backup creation and budget sharing. " +
                        "Setting one up also turns on app lock when you reopen Mizani. " +
                        "After setup, choose the action again and confirm with your PIN or enabled biometrics.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPinRequiredForSensitiveAction = false
                    settingUpPinForSensitiveAction = true
                    showPinSetup = true
                }) { Text("Set up PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinRequiredForSensitiveAction = false }) { Text("Cancel") }
            },
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = {
                showPinSetup = false
                settingUpPinForSensitiveAction = false
            },
            onConfirm = { pin ->
                scope.launch {
                    // enableLock returns a Result rather than throwing, so handle it directly:
                    // wrapping it in runCatching would report failures as success.
                    vm.enableLock(pin)
                        .onSuccess {
                            showPinSetup = false
                            message = if (settingUpPinForSensitiveAction) {
                                "PIN set up. Choose the export, backup or budget share again and confirm your identity."
                            } else {
                                "App lock is on."
                            }
                            settingUpPinForSensitiveAction = false
                            error = null
                        }
                        .onFailure {
                            // Close the dialog so the banner behind it is visible, and keep the
                            // message user-safe (no raw exception details).
                            showPinSetup = false
                            settingUpPinForSensitiveAction = false
                            error = "App lock could not be turned on. Your data is unchanged — try again."
                        }
                }
            },
        )
    }

    if (showDisablePin) {
        PinVerifyDialog(
            title = "Enter your PIN to turn off app lock",
            onDismiss = { showDisablePin = false },
            onSubmit = { pin ->
                scope.launch {
                    // verifyPin enforces the same persisted failed-attempt
                    // backoff as the lock screen: while blocked it refuses the
                    // attempt without hashing, so this path cannot be used to
                    // keep guessing either.
                    val ok = vm.settingsRepository.verifyPin(pin)
                    showDisablePin = false
                    if (ok) {
                        vm.disableLock()
                        message = "App lock is off. Your local data is still encrypted."
                        error = null
                    } else if (vm.settingsRepository.pinBackoffRemainingMs() > 0L) {
                        error = PIN_RETRY_MESSAGE
                    } else {
                        error = "Incorrect PIN"
                    }
                }
            },
        )
    }

    if (showVerifyForChange) {
        PinVerifyDialog(
            title = "Confirm your current PIN",
            onDismiss = { showVerifyForChange = false },
            onSubmit = { pin ->
                scope.launch {
                    val ok = vm.settingsRepository.verifyPin(pin)
                    showVerifyForChange = false
                    when {
                        ok -> showPinSetup = true
                        vm.settingsRepository.pinBackoffRemainingMs() > 0L -> error = PIN_RETRY_MESSAGE
                        else -> error = "Incorrect PIN"
                    }
                }
            },
        )
    }

    if (showCountryDialog) {
        CountryCurrencyDialog(
            initialCountry = settings.countryCode,
            workspaceCurrency = state.snapshot.currencyCode,
            onDismiss = { showCountryDialog = false },
            onSave = { countryCode ->
                scope.launch {
                    runCatching { vm.setCountry(countryCode, state.snapshot.currencyCode) }
                        .onSuccess {
                            showCountryDialog = false
                            message = "Country changed. Your amounts were not converted."
                            error = null
                        }
                        .onFailure { error = it.message ?: "Could not update your region. Try again." }
                }
            },
        )
    }

    if (showCsvWarning) {
        MizaniAlertDialog(
            onDismissRequest = { showCsvWarning = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("CSV export is not encrypted") },
            text = {
                Text("This CSV contains financial details and is not encrypted. Save it somewhere private.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showCsvWarning = false
                    createCsv.launch(vm.exporter.suggestedCsvName())
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { showCsvWarning = false }) { Text("Cancel") } },
        )
    }

    if (showExcelWarning) {
        MizaniAlertDialog(
            onDismissRequest = { showExcelWarning = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("Excel workbook is not encrypted") },
            text = {
                Text("This workbook contains your financial records and is not encrypted. Save it somewhere private.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExcelWarning = false
                    createExcel.launch(vm.exporter.suggestedExcelName())
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { showExcelWarning = false }) { Text("Cancel") } },
        )
    }

    if (showBackupPassphrase) {
        MizaniAlertDialog(
            onDismissRequest = ::discardBackupPassphrase,
            shape = MaterialTheme.shapes.large,
            title = { Text("Protect your backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "Encrypted with a key derived from this passphrase. " +
                            "A memorable phrase of 12 or more characters is easier to remember. " +
                            "There is no recovery if you forget it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Passphrase (12+ characters)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = backupPassphraseConfirm,
                        onValueChange = { backupPassphraseConfirm = it },
                        label = { Text("Confirm passphrase") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        backupPassphrase.length < 12 -> error = "Use at least 12 characters"
                        backupPassphrase != backupPassphraseConfirm -> error = "Passphrases do not match"
                        else -> {
                            error = null
                            pendingBackupPassphrase = backupPassphrase
                            backupPassphrase = ""
                            backupPassphraseConfirm = ""
                            showBackupPassphrase = false
                            createBackup.launch(vm.exporter.suggestedBackupName())
                        }
                    }
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = ::discardBackupPassphrase) { Text("Cancel") } },
        )
    }

    if (showRestorePassphrase) {
        MizaniAlertDialog(
            onDismissRequest = { if (!checkingBackup) discardPendingRestore() },
            shape = MaterialTheme.shapes.large,
            title = { Text("Open encrypted backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "Enter the passphrase used to create this backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text("Backup passphrase") },
                        singleLine = true,
                        enabled = !checkingBackup,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!checkingBackup) {
                        val bytes = pendingRestoreBytes
                        val passphrase = restorePassphrase
                        if (bytes == null) {
                            error = "Choose a backup file first"
                        } else {
                            checkingBackup = true
                            scope.launch {
                                try {
                                    val summary = vm.exporter.inspectBackup(bytes, passphrase)
                                    if (showRestorePassphrase && pendingRestoreBytes === bytes) {
                                        pendingSummary = summary
                                        showRestorePassphrase = false
                                        showRestoreConfirm = true
                                        error = null
                                    }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    if (showRestorePassphrase) {
                                        error = failure.message ?: "Could not read this backup"
                                    }
                                } finally {
                                    checkingBackup = false
                                }
                            }
                        }
                    }
                }, enabled = !checkingBackup) {
                    Text(if (checkingBackup) "Checking…" else "Check backup")
                }
            },
            dismissButton = {
                TextButton(onClick = ::discardPendingRestore, enabled = !checkingBackup) { Text("Cancel") }
            },
        )
    }

    val summary = pendingSummary
    if (showRestoreConfirm && summary != null) {
        MizaniAlertDialog(
            onDismissRequest = { if (!restoringBackup) discardPendingRestore() },
            shape = MaterialTheme.shapes.large,
            title = { Text("Replace local data with this backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        "Created ${Instant.ofEpochMilli(summary.createdAt).atZone(ZoneId.systemDefault()).let { DateText.medium(it.toLocalDate()) }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Currency: ${summary.currencyCode}")
                    Text("Wallets: ${summary.walletCount}")
                    Text("Categories: ${summary.categoryCount}")
                    Text("Transactions: ${summary.transactionCount}")
                    Text(
                        "Date range: ${summary.firstDate?.let { DateText.medium(it) } ?: "—"} to ${summary.lastDate?.let { DateText.medium(it) } ?: "—"}",
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "This replaces every wallet, transaction, and other saved record on this phone. " +
                            "Create a backup first if you want to keep your current data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!restoringBackup) {
                        val bytes = pendingRestoreBytes ?: return@TextButton
                        val passphrase = restorePassphrase
                        restorePassphrase = ""
                        restoringBackup = true
                        scope.launch {
                            try {
                                vm.exporter.restore(bytes, passphrase)
                                discardPendingRestore()
                                message = "Backup restored. Your local ledger was replaced."
                                error = null
                                try {
                                    vm.finance.currentWorkspace()?.let { workspace ->
                                        vm.setCountry(workspace.countryCode, workspace.currencyCode)
                                    }
                                } catch (failure: Exception) {
                                    error = "Backup restored, but settings could not refresh. Restart the app to reload them."
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                error = failure.message ?: "Could not restore this backup"
                                discardPendingRestore()
                            } finally {
                                restoringBackup = false
                            }
                        }
                    }
                }, enabled = !restoringBackup) {
                    Text(if (restoringBackup) "Restoring…" else "Restore backup")
                }
            },
            dismissButton = {
                TextButton(onClick = ::discardPendingRestore, enabled = !restoringBackup) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirm) {
        MizaniConfirmDialog(
            title = "Delete all local data?",
            body = "This deletes all wallets, transactions, and other saved records on this phone. Without a backup, you cannot restore them. App lock and settings also reset.",
            confirmLabel = "Delete everything",
            destructive = true,
            onConfirm = {
                scope.launch {
                    vm.deleteAllData()
                        .onSuccess {
                            showDeleteConfirm = false
                            message = "All local data was deleted."
                            error = null
                        }
                        .onFailure {
                            // Close so the failure banner is visible instead of hidden behind the dialog.
                            showDeleteConfirm = false
                            error = it.message ?: "Could not delete local data. Nothing was removed."
                        }
                }
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (dataToolsOnly) {
        Column(Modifier.fillMaxSize()) {
            MizaniTopBar(title = "Data export & backup", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.bottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                message?.let {
                    StatusBanner(text = it, isError = false, onDismiss = { message = null })
                }
                if (error != null && !showBackupPassphrase && !showRestorePassphrase) {
                    StatusBanner(text = error!!, isError = true, onDismiss = { error = null })
                }
                Text(
                    "Download your records for your own analysis, or create an encrypted backup to move them to another phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MizaniSectionCard {
                    MizaniInfoRow(
                        icon = Icons.Rounded.Download,
                        title = "Download transactions (CSV)",
                        subtitle = "Open in a spreadsheet or analysis app",
                        onClick = { requestSensitiveAction(SensitiveDataAction.CSV_EXPORT) },
                    )
                    MizaniSectionDivider()
                    MizaniInfoRow(
                        icon = Icons.Rounded.Download,
                        title = "Download Excel workbook",
                        subtitle = "Transactions and related records in sheets",
                        onClick = { requestSensitiveAction(SensitiveDataAction.EXCEL_EXPORT) },
                    )
                    MizaniSectionDivider()
                    MizaniInfoRow(
                        icon = Icons.Rounded.Backup,
                        title = if (creatingBackup) "Creating encrypted backup…" else "Create encrypted backup",
                        subtitle = if (creatingBackup) "Keep Mizani open while the backup is saved" else "Protect the file with a passphrase",
                        onClick = {
                            if (!creatingBackup) requestSensitiveAction(SensitiveDataAction.ENCRYPTED_BACKUP)
                        },
                    )
                    MizaniSectionDivider()
                    MizaniInfoRow(
                        icon = Icons.Rounded.Restore,
                        title = "Restore or move data",
                        subtitle = "Review backup contents before replacing this ledger",
                        onClick = {
                            discardPendingRestore()
                            error = null
                            openRestore.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
                        },
                    )
                }

                Text("Share budgets", style = MaterialTheme.typography.titleMedium)
                MizaniSectionCard {
                    if (shareableBudgets.isEmpty()) {
                        Text(
                            "No saved budgets are available to share yet.",
                            modifier = Modifier.padding(Spacing.md),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Share one budget as text. The preview shows its name, dates, category names and planned limits. Transactions and wallet balances are not included.",
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        shareableBudgets.forEachIndexed { index, budget ->
                            if (index > 0) MizaniSectionDivider()
                            val plannedMinor = state.snapshot.budgetLinesFor(budget.id)
                                .sumOf { it.plannedMinor }
                            MizaniInfoRow(
                                icon = Icons.Rounded.Share,
                                title = budget.name,
                                subtitle = "${DateText.medium(java.time.LocalDate.ofEpochDay(budget.startEpochDay))} to " +
                                    "${DateText.medium(java.time.LocalDate.ofEpochDay(budget.endEpochDay))} · " +
                                    "${MoneyFormat.format(plannedMinor, state.currency)} planned",
                                onClick = { budgetToShare = budget },
                            )
                        }
                    }
                }

                budgetToShare?.let { budget ->
                    val shareText = remember(budget, state.snapshot, state.currency) {
                        BudgetShareText.build(budget, state.snapshot, state.currency)
                    }
                    MizaniAlertDialog(
                        onDismissRequest = { budgetToShare = null },
                        shape = MaterialTheme.shapes.large,
                        title = { Text("Review before sharing") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Text(
                                    "This text goes only to the app you choose. It contains planned budget details, not transactions or wallet balances.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(shareText, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    requestSensitiveAction(SensitiveDataAction.BUDGET_SHARE, budget)
                                },
                            ) { Text("Share") }
                        },
                        dismissButton = {
                            TextButton(onClick = { budgetToShare = null }) { Text("Cancel") }
                        },
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "Settings", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            message?.let {
                StatusBanner(text = it, isError = false, onDismiss = { message = null })
            }
            if (error != null && !showBackupPassphrase && !showRestorePassphrase) {
                StatusBanner(text = error!!, isError = true, onDismiss = { error = null })
            }

            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                // Labels stay outside their card; +4dp keeps 16dp between groups, 12dp label-to-card.
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniCard {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { scope.launch { vm.setTheme(mode) } },
                            label = { Text(mode.label) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                Text(
                    "Follow your device, or choose light or dark mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(Spacing.lg))
                Text("Color theme", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Choose the accent used for buttons, selections and highlights. Income, spending and transfer colors keep their meaning in every theme.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                ColorTheme.entries.chunked(3).forEach { themes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        themes.forEach { theme ->
                            FilterChip(
                                selected = settings.colorTheme == theme,
                                onClick = { scope.launch { vm.setColorTheme(theme) } },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(colorThemePreviewColor(theme)),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(theme.label, maxLines = 1)
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            )
                        }
                        repeat(3 - themes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                Text("Dark appearance", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Choose the surface style used whenever dark mode is active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    DarkThemeStyle.entries.forEach { style ->
                        FilterChip(
                            selected = settings.darkThemeStyle == style,
                            onClick = { scope.launch { vm.setDarkThemeStyle(style) } },
                            label = { Text(style.label, maxLines = 1) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        )
                    }
                }
            }

            Text(
                "Security",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Lock,
                    title = "App lock",
                    subtitle = if (settings.lockEnabled) "On · 6-digit PIN" else "Off",
                )
                if (settings.hasPin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(onClick = { showVerifyForChange = true }) { Text("Change PIN") }
                        OutlinedButton(onClick = { showDisablePin = true }) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Turn off")
                        }
                        if (settings.lockEnabled) {
                            TextButton(onClick = { vm.lockNow() }) { Text("Lock now") }
                        }
                    }
                } else {
                    OutlinedButton(onClick = { showPinSetup = true }) { Text("Set up app lock") }
                }
                if (settings.lockEnabled && canBiometric) {
                    Spacer(Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text("Biometric unlock", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Use the device fingerprint or face prompt. Mizani never sees biometric data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.biometricEnabled,
                            onCheckedChange = { enabled -> vm.setBiometric(enabled) {} },
                            modifier = Modifier.semantics { contentDescription = "Biometric unlock" },
                        )
                    }
                }
                Text(
                    "PIN lock protects access. Your ledger is encrypted on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                "Region",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Language,
                    title = "Country & primary currency",
                    subtitle = "${CountryPacks.byCode(settings.countryCode).name} · ${state.snapshot.currencyCode}",
                    onClick = { showCountryDialog = true },
                )
                Text(
                    "Country changes suggestions and labels, not your primary currency.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                stringResource(R.string.settings_reminders_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniCard {
                val reminderTimeLabel = formatReminderTime(context, settings.dailyReminderMinuteOfDay)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_daily_reminder_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.settings_daily_reminder_description, reminderTimeLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.dailyReminderEnabled,
                        onCheckedChange = onDailyReminderChanged,
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.settings_daily_reminder_title)
                        },
                    )
                }
                if (settings.dailyReminderEnabled) {
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.settings_reminder_time_label), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        scope.launch {
                                            vm.setDailyReminderMinuteOfDay(hour * 60 + minute)
                                                .onSuccess { reminderStatus = null }
                                                .onFailure {
                                                    reminderStatus = context.getString(R.string.settings_reminder_schedule_error)
                                                }
                                        }
                                    },
                                    settings.dailyReminderMinuteOfDay / 60,
                                    settings.dailyReminderMinuteOfDay % 60,
                                    DateFormat.is24HourFormat(context),
                                ).apply {
                                    window?.setFlags(
                                        WindowManager.LayoutParams.FLAG_SECURE,
                                        WindowManager.LayoutParams.FLAG_SECURE,
                                    )
                                }.show()
                            },
                        ) {
                            Text(reminderTimeLabel)
                        }
                    }
                    Text(
                        stringResource(R.string.settings_reminder_local_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!notificationsAvailable) {
                        Text(
                            stringResource(R.string.settings_reminder_notifications_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = openReminderNotificationSettings) {
                            Text(stringResource(R.string.settings_open_notification_settings))
                        }
                    }
                }
                reminderStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (showReminderNotificationSettings && !settings.dailyReminderEnabled) {
                    TextButton(onClick = openReminderNotificationSettings) {
                        Text(stringResource(R.string.settings_open_notification_settings))
                    }
                }
            }

            Text(
                "Privacy",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Security,
                    title = "No tracking",
                    subtitle = "No ads or usage tracking. Rate checks send currency codes only.",
                )
            }

            Text(
                "Delete data",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Delete all local data",
                    subtitle = "Cannot be undone without a backup",
                    danger = true,
                    onClick = { showDeleteConfirm = true },
                )
            }

        }
    }
}

private fun formatReminderTime(context: Context, minuteOfDay: Int): String {
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
    }
    return DateFormat.getTimeFormat(context).format(time.time)
}

@Composable
private fun CountryCurrencyDialog(
    initialCountry: String,
    workspaceCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var countryCode by rememberSaveable { mutableStateOf(initialCountry) }
    MizaniAlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Country & primary currency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Country", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = CountryPacks.byCode(countryCode).name,
                    options = CountryPacks.all.map { PickerOption(it.code, it.name) },
                    onSelect = { countryCode = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Primary currency", style = MaterialTheme.typography.labelMedium)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        workspaceCurrency,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                    )
                }
                Text(
                        "Primary currency is fixed after setup. Export your data before resetting the app to change it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(countryCode) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private suspend fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
    try {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                output.write(bytes)
                output.flush()
            } ?: throw IOException("Could not open the selected file for writing")
        }
    } finally {
        // The helper consumes a caller-owned buffer so clear both encrypted backups and CSV copies.
        bytes.fill(0)
    }
}
