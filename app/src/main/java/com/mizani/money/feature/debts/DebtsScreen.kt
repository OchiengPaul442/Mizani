package com.mizani.money.feature.debts

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.DebtEntity
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.feature.lock.ProtectedBalanceValue
import com.mizani.money.feature.lock.WalletBalanceVisibilityControl
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniAlertDialog
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniStatCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun DebtsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onBack: () -> Unit,
    onSetupPin: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val active = snapshot.debts.filter { !it.archived && it.deletedAt == null }
    val archived = snapshot.debts.filter { it.archived && it.deletedAt == null }
    val totalOwed = active.fold(0L) { total, debt -> Math.addExact(total, debt.currentBalanceMinor) }
    val plannedPayments = active.fold(0L) { total, debt -> Math.addExact(total, debt.periodicPaymentMinor) }
    val wallets = snapshot.activeWallets.filter { it.type != WalletType.DEBT }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<DebtEntity?>(null) }
    var paymentDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var showDebtDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (showDebtDialog) {
        DebtDialog(
            initial = editing,
            currency = currency,
            balanceVisible = state.walletBalancesVisible,
            saveError = error,
            onDismiss = { showDebtDialog = false; editing = null; error = null },
            onSave = { name, lender, balance, original, payment, dueDate, interest, note ->
                val current = editing
                scope.launch {
                    runCatching {
                        vm.finance.saveDebt(
                            debtId = current?.id,
                            name = name,
                            lender = lender,
                            balanceMinor = balance,
                            originalMinor = original,
                            periodicPaymentMinor = payment,
                            nextDue = dueDate,
                            interestRatePercent = interest,
                            note = note,
                        )
                    }.onSuccess {
                        showDebtDialog = false
                        editing = null
                        error = null
                    }.onFailure { error = it.message ?: "Could not save this debt. Try again." }
                }
            },
            onArchive = { debt, archivedValue ->
                scope.launch {
                    runCatching { vm.finance.setDebtArchived(debt.id, archivedValue) }
                        .onSuccess { showDebtDialog = false; editing = null; error = null }
                        .onFailure { error = it.message ?: "Could not update this debt. Try again." }
                }
            },
        )
    }

    paymentDebt?.let { debt ->
        DebtPaymentDialog(
            debt = debt,
            currency = currency,
            wallets = wallets,
            balanceVisible = state.walletBalancesVisible,
            saveError = error,
            onDismiss = { paymentDebt = null; error = null },
            onSave = { walletId, amount, date ->
                scope.launch {
                    runCatching { vm.finance.recordDebtPayment(debt.id, walletId, amount, date) }
                        .onSuccess { paymentDebt = null; error = null }
                        .onFailure { error = it.message ?: "Could not record this payment. Try again." }
                }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = "Debts",
            subtitle = "${active.size} active ${if (active.size == 1) "debt" else "debts"}",
            onBack = onBack,
            actions = {
                WalletBalanceVisibilityControl(vm, state, onSetupPin)
                IconButton(onClick = { editing = null; error = null; showDebtDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add debt")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MizaniCard {
                Text("See what you owe and how it is changing.", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Add a loan, mortgage or purchase on credit, then record each payment from the wallet you used. Payments become one normal expense in your ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MizaniStatCard(
                    label = "Total owed",
                    value = protectedMoney(totalOwed, currency, state.walletBalancesVisible),
                    valueContentDescription = protectedMoneyDescription("Total debt", totalOwed, currency, state.walletBalancesVisible),
                    modifier = Modifier.weight(1f),
                )
                MizaniStatCard(
                    label = "Planned payments",
                    value = protectedMoney(plannedPayments, currency, state.walletBalancesVisible),
                    caption = "per payment schedule",
                    valueContentDescription = protectedMoneyDescription("Planned debt payments", plannedPayments, currency, state.walletBalancesVisible),
                    modifier = Modifier.weight(1f),
                )
            }

            if (error != null && !showDebtDialog && paymentDebt == null) {
                StatusBanner(text = error!!, isError = true, onDismiss = { error = null })
            }

            if (active.isEmpty()) {
                EmptyState(
                    title = "No debts yet",
                    body = "If you owe money, add it here and record payments as you make them.",
                    icon = Icons.Rounded.AccountBalance,
                    actionLabel = "Add debt",
                    onAction = { editing = null; error = null; showDebtDialog = true },
                )
            } else {
                Text("Active debts", style = MaterialTheme.typography.titleMedium)
                active.forEach { debt ->
                    DebtCard(
                        debt = debt,
                        currency = currency,
                        balanceVisible = state.walletBalancesVisible,
                        onEdit = { editing = debt; error = null; showDebtDialog = true },
                        onRecordPayment = { paymentDebt = debt; error = null },
                    )
                }
            }

            if (archived.isNotEmpty()) {
                Text("Archived", style = MaterialTheme.typography.titleSmall)
                archived.forEach { debt ->
                    DebtCard(
                        debt = debt,
                        currency = currency,
                        balanceVisible = state.walletBalancesVisible,
                        onEdit = { editing = debt; error = null; showDebtDialog = true },
                        onRecordPayment = null,
                    )
                }
            }

            Text(
                "Debt records are kept on this device. Recording a payment creates one expense and reduces the debt balance; it does not create a second ledger entry.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebtCard(
    debt: DebtEntity,
    currency: Currency,
    balanceVisible: Boolean,
    onEdit: () -> Unit,
    onRecordPayment: (() -> Unit)?,
) {
    val original = debt.originalMinor?.takeIf { it > 0L }
    val paid = original?.let { (it - debt.currentBalanceMinor).coerceIn(0L, it) }
    val progress = if (original != null) (paid!!.toDouble() / original.toDouble()).toFloat() else null
    MizaniCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(debt.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (debt.lender.isNotBlank()) {
                    Text(debt.lender, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Edit ${debt.name}")
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text("Current balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProtectedBalanceValue(
            amountMinor = debt.currentBalanceMinor,
            currency = currency,
            visible = balanceVisible,
            style = MaterialTheme.typography.headlineSmall,
            color = if (debt.currentBalanceMinor > 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            amountLabel = "Current debt balance",
        )
        if (progress != null) {
            Spacer(Modifier.height(Spacing.md))
            ProgressBar(fraction = progress)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                if (balanceVisible) "${(progress * 100).roundToInt()}% of the original balance paid" else "Payment progress hidden",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            DebtDetail(
                label = "Regular payment",
                value = protectedMoney(debt.periodicPaymentMinor, currency, balanceVisible),
                modifier = Modifier.weight(1f),
            )
            DebtDetail(
                label = "Next due",
                value = debt.nextDueEpochDay?.let { DateText.short(LocalDate.ofEpochDay(it)) } ?: "Not set",
                modifier = Modifier.weight(1f),
            )
        }
        if (debt.interestRatePercent != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Interest rate ${formatPercent(debt.interestRatePercent)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (debt.archived) {
            Spacer(Modifier.height(Spacing.xs))
            Text("Archived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onRecordPayment != null && debt.currentBalanceMinor > 0L) {
            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = onRecordPayment,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Record payment")
            }
        }
    }
}

@Composable
private fun DebtDetail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DebtDialog(
    initial: DebtEntity?,
    currency: Currency,
    balanceVisible: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long?, Long, LocalDate?, Double?, String) -> Unit,
    onArchive: (DebtEntity, Boolean) -> Unit,
) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var lender by rememberSaveable(initial?.id) { mutableStateOf(initial?.lender.orEmpty()) }
    var balanceText by rememberSaveable(initial?.id) {
        mutableStateOf(if (initial != null && !balanceVisible) "" else initial?.let { MoneyFormat.toInput(it.currentBalanceMinor, currency) }.orEmpty())
    }
    var originalText by rememberSaveable(initial?.id) {
        mutableStateOf(if (initial != null && !balanceVisible) "" else initial?.originalMinor?.let { MoneyFormat.toInput(it, currency) }.orEmpty())
    }
    var paymentText by rememberSaveable(initial?.id) {
        mutableStateOf(if (initial != null && !balanceVisible) "" else initial?.let { MoneyFormat.toInput(it.periodicPaymentMinor, currency) }.orEmpty())
    }
    var dueDate by remember(initial?.id) { mutableStateOf(initial?.nextDueEpochDay?.let(LocalDate::ofEpochDay)) }
    var hasDueDate by rememberSaveable(initial?.id) { mutableStateOf(dueDate != null) }
    var interestText by rememberSaveable(initial?.id) { mutableStateOf(initial?.interestRatePercent?.toString().orEmpty()) }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    MizaniAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add debt" else "Edit debt") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    "Track the balance separately from your wallet. A payment will be recorded as one expense and reduce this debt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80); validationError = null },
                    label = { Text("Debt name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lender,
                    onValueChange = { lender = it.take(80); validationError = null },
                    label = { Text("Lender (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initial != null && !balanceVisible) {
                    Text(
                        "Balance values are hidden. Confirm with your PIN before changing them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MizaniAmountField(
                        value = balanceText,
                        onValueChange = { balanceText = it; validationError = null },
                        label = "Current balance",
                        currency = currency,
                    )
                    MizaniAmountField(
                        value = originalText,
                        onValueChange = { originalText = it; validationError = null },
                        label = "Original amount (optional)",
                        currency = currency,
                    )
                    MizaniAmountField(
                        value = paymentText,
                        onValueChange = { paymentText = it; validationError = null },
                        label = "Regular payment (optional)",
                        currency = currency,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasDueDate,
                        onCheckedChange = { checked ->
                            hasDueDate = checked
                            if (checked && dueDate == null) dueDate = LocalDate.now()
                        },
                    )
                    Text("Set next payment date", style = MaterialTheme.typography.bodyMedium)
                }
                if (hasDueDate && dueDate != null) {
                    DateField(
                        label = "Next payment",
                        date = dueDate!!,
                        onDateSelected = { dueDate = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = interestText,
                    onValueChange = { interestText = it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(8) },
                    label = { Text("Interest rate (optional)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(240) },
                    label = { Text("Note (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (initial != null) {
                    TextButton(
                        onClick = { onArchive(initial, !initial.archived) },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(
                            if (initial.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(if (initial.archived) "Restore debt" else "Archive debt")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val balance = if (initial != null && !balanceVisible) initial.currentBalanceMinor else MoneyFormat.parse(balanceText, currency)
                val original = if (initial != null && !balanceVisible) initial.originalMinor else originalText.takeIf { it.isNotBlank() }?.let { MoneyFormat.parse(it, currency) }
                val payment = if (initial != null && !balanceVisible) {
                    initial.periodicPaymentMinor
                } else if (paymentText.isBlank()) {
                    0L
                } else {
                    MoneyFormat.parse(paymentText, currency)
                }
                val interest = interestText.replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()
                when {
                    name.isBlank() -> validationError = "Enter a name for this debt."
                    balance == null || balance <= 0L -> validationError = "Enter a current balance greater than zero."
                    original == null && originalText.isNotBlank() -> validationError = "Enter a valid original amount."
                    original != null && original < 0L -> validationError = "Original amount cannot be negative."
                    payment == null || payment < 0L -> validationError = "Enter a valid regular payment."
                    interestText.isNotBlank() && (interest == null || interest < 0.0) -> validationError = "Enter a valid non-negative interest rate."
                    else -> onSave(name, lender, balance, original, payment ?: 0L, if (hasDueDate) dueDate else null, interest, note)
                }
            }) { Text(if (initial == null) "Add debt" else "Save changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DebtPaymentDialog(
    debt: DebtEntity,
    currency: Currency,
    wallets: List<WalletEntity>,
    balanceVisible: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (String, Long, LocalDate) -> Unit,
) {
    var amountText by rememberSaveable(debt.id) {
        mutableStateOf(if (debt.periodicPaymentMinor > 0L) MoneyFormat.toInput(debt.periodicPaymentMinor, currency) else "")
    }
    var walletId by remember(debt.id) { mutableStateOf(wallets.firstOrNull()?.id) }
    var date by remember(debt.id) { mutableStateOf(LocalDate.now()) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val wallet = wallets.firstOrNull { it.id == walletId }

    MizaniAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record debt payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "This records one expense from the selected wallet and reduces ${debt.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MizaniAmountField(
                    value = amountText,
                    onValueChange = { amountText = it; validationError = null },
                    label = "Payment amount",
                    currency = currency,
                )
                MizaniPicker(
                    selectedText = wallet?.name ?: "Choose a wallet",
                    options = wallets.map { PickerOption(it.id, it.name) },
                    onSelect = { walletId = it; validationError = null },
                    enabled = wallets.isNotEmpty(),
                    fieldLabel = "Payment wallet",
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(
                    label = "Payment date",
                    date = date,
                    onDateSelected = { date = it; validationError = null },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (balanceVisible) "Remaining before this payment: ${MoneyFormat.format(debt.currentBalanceMinor, currency)}."
                    else "Debt balance is hidden while the app is locked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = MoneyFormat.parse(amountText, currency)
                when {
                    wallets.isEmpty() -> validationError = "Add a non-debt wallet before recording a payment."
                    walletId == null -> validationError = "Choose the wallet used for this payment."
                    amount == null || amount <= 0L -> validationError = "Enter a payment greater than zero."
                    amount > debt.currentBalanceMinor -> validationError = "Payment cannot be greater than the current debt balance."
                    date.isAfter(LocalDate.now()) -> validationError = "Payment date cannot be in the future."
                    else -> onSave(walletId!!, amount, date)
                }
            }) {
                Text("Record payment")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun protectedMoney(value: Long, currency: Currency, visible: Boolean): String =
    if (visible) MoneyFormat.format(value, currency) else "••••••"

private fun protectedMoneyDescription(label: String, value: Long, currency: Currency, visible: Boolean): String =
    if (visible) "$label ${MoneyFormat.format(value, currency)}" else "$label hidden"

private fun formatPercent(value: Double): String =
    when {
        !value.isFinite() || value < 0.0 -> "—"
        value % 1.0 == 0.0 -> value.toInt().toString()
        else -> "%.2f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
    }
