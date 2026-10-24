package com.mizani.money.feature.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.CountryPacks
import com.mizani.money.core.model.ThemeMode
import com.mizani.money.core.time.DateText
import com.mizani.money.domain.export.BackupSummary
import com.mizani.money.feature.lock.PinSetupDialog
import com.mizani.money.feature.lock.PinVerifyDialog
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = state.settings

    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
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
    var restorePassphrase by remember { mutableStateOf("") }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingSummary by remember { mutableStateOf<BackupSummary?>(null) }

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
        if (uri != null) {
            val passphrase = backupPassphrase
            scope.launch {
                runCatching {
                    val bytes = vm.exporter.buildBackup(passphrase)
                    writeBytes(context, uri, bytes)
                }.onSuccess {
                    message = "Encrypted backup created. Keep the passphrase safe — it cannot be recovered."
                    error = null
                    backupPassphrase = ""
                    backupPassphraseConfirm = ""
                }.onFailure {
                    error = it.message ?: "Could not create the backup"
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
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw IOException("Could not open the selected file")
                    }
                }.onSuccess { bytes ->
                    pendingRestoreBytes = bytes
                    restorePassphrase = ""
                    showRestorePassphrase = true
                    error = null
                }.onFailure {
                    error = it.message ?: "Could not read the selected file"
                }
            }
        }
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onConfirm = { pin ->
                scope.launch {
                    // enableLock returns a Result rather than throwing, so handle it directly:
                    // wrapping it in runCatching would report failures as success.
                    vm.enableLock(pin)
                        .onSuccess {
                            showPinSetup = false
                            message = "App lock is on."
                            error = null
                        }
                        .onFailure {
                            // Close the dialog so the banner behind it is visible, and keep the
                            // message user-safe (no raw exception details).
                            showPinSetup = false
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
                            message = "Region updated. Country affects suggestions and labels; amounts were not converted."
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
                Text("This CSV contains transaction dates, amounts, wallets, categories, payees and notes. Save it somewhere private.")
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
                Text("The workbook includes transactions, wallets, budgets, goals, debts and notes. Anyone with the file may be able to read your financial data, so save it somewhere private.")
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
            onDismissRequest = { showBackupPassphrase = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("Protect your backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "Encrypted with a key derived from this passphrase. " +
                            "There is no recovery if you forget it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Passphrase (8+ characters)") },
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
                        backupPassphrase.length < 8 -> error = "Use at least 8 characters"
                        backupPassphrase != backupPassphraseConfirm -> error = "Passphrases do not match"
                        else -> {
                            error = null
                            showBackupPassphrase = false
                            createBackup.launch(vm.exporter.suggestedBackupName())
                        }
                    }
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { showBackupPassphrase = false }) { Text("Cancel") } },
        )
    }

    if (showRestorePassphrase) {
        MizaniAlertDialog(
            onDismissRequest = { showRestorePassphrase = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("Open encrypted backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "Enter the passphrase used when this .mizani backup was created.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text("Backup passphrase") },
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
                    val bytes = pendingRestoreBytes
                    if (bytes == null) {
                        error = "Choose a backup file first"
                    } else {
                        scope.launch {
                            runCatching { vm.exporter.inspectBackup(bytes, restorePassphrase) }
                                .onSuccess { summary ->
                                    pendingSummary = summary
                                    showRestorePassphrase = false
                                    showRestoreConfirm = true
                                    error = null
                                }
                                .onFailure { error = it.message ?: "Could not read this backup" }
                        }
                    }
                }) { Text("Inspect") }
            },
            dismissButton = { TextButton(onClick = { showRestorePassphrase = false }) { Text("Cancel") } },
        )
    }

    val summary = pendingSummary
    if (showRestoreConfirm && summary != null) {
        MizaniAlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
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
                        "This replaces every wallet, transaction, plan, goal, bill and debt on this phone. " +
                            "Validation runs first, so a failed import does not partially overwrite your ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val bytes = pendingRestoreBytes ?: return@TextButton
                    scope.launch {
                        runCatching { vm.exporter.restore(bytes, restorePassphrase) }
                            .onSuccess {
                                val workspace = vm.finance.currentWorkspace()
                                if (workspace != null) {
                                    vm.setCountry(workspace.countryCode, workspace.currencyCode)
                                }
                                showRestoreConfirm = false
                                pendingSummary = null
                                pendingRestoreBytes = null
                                message = "Backup restored. Your local ledger was replaced."
                                error = null
                            }
                            .onFailure {
                                error = it.message ?: "Could not restore this backup"
                                showRestoreConfirm = false
                            }
                    }
                }) { Text("Replace and restore") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showDeleteConfirm) {
        MizaniConfirmDialog(
            title = "Delete all local data?",
            body = "Every wallet, transaction, plan, goal, bill and debt on this phone will be deleted. " +
                "This cannot be undone without an encrypted backup; app lock and settings also reset.",
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
                        )
                    }
                }
                Text(
                    "Mizani uses its own calm palette rather than wallpaper-based dynamic colour, so amounts and " +
                        "statuses keep predictable contrast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    "Your PIN is stored only as a salted hash and is never saved in plain text. The database key " +
                        "is protected by Android Keystore.",
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
                    "The primary currency is fixed for this workspace. Country only affects suggestions and labels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                "Data & backups",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Download,
                    title = "Export transactions (CSV)",
                    subtitle = "Transactions only · unencrypted",
                    onClick = { showCsvWarning = true },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Download,
                    title = "Export Excel workbook",
                    subtitle = "Multiple sheets for analysis · unencrypted",
                    onClick = { showExcelWarning = true },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Backup,
                    title = "Create encrypted backup",
                    subtitle = "Versioned .mizani file with passphrase",
                    onClick = { showBackupPassphrase = true },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Restore,
                    title = "Restore backup",
                    subtitle = "Validated first, applied in one transaction",
                    onClick = { openRestore.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) },
                )
            }

            Text(
                "Privacy",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Info,
                    title = "About & privacy",
                    subtitle = "Where your data stays and its limits",
                    onClick = onOpenAbout,
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Security,
                    title = "No tracking",
                    subtitle = "No ads or financial analytics; currency-pair lookups power conversion",
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

            Text(
                "Your data is stored locally and encrypted on this device.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
                    "The primary currency is fixed after setup. Export your data, then reset or restore " +
                        "a backup to change it.",
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
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "w")?.use { output -> output.write(bytes) }
            ?: throw IOException("Could not open the selected file for writing")
    }
}
