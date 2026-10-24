package com.mizani.money.feature.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Payments
import com.mizani.money.ui.components.MizaniAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.DebtEntity
import com.mizani.money.data.repo.LedgerSnapshot
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.R
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniStatCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DebtsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onBack: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val scope = rememberCoroutineScope()
    val active = snapshot.debts.filter { !it.archived && it.deletedAt == null }
    val archived = snapshot.debts.filter { it.archived && it.deletedAt == null }
    var editing by remember { mutableStateOf<DebtEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (showDialog) {
        DebtDialog(
            initial = editing,
            currency = currency,
            onDismiss = { showDialog = false; editing = null },
            onSave = { name, lender, balance, original, periodic, nextDue, interest, note ->
                scope.launch {
                    runCatching {
                        vm.finance.saveDebt(
                            debtId = editing?.id,
                            name = name,
                            lender = lender,
                            balanceMinor = balance,
                            originalMinor = original,
                            periodicPaymentMinor = periodic,
                            nextDue = nextDue,
                            interestRatePercent = interest,
                            note = note,
                        )
                    }.onSuccess {
                        showDialog = false
                        editing = null
                    }.onFailure { error = it.message ?: "Could not save this debt. Try again." }
                }
            },
            onArchive = { debt ->
                scope.launch {
                    runCatching { vm.finance.setDebtArchived(debt.id, true) }
                        .onSuccess { showDialog = false; editing = null }
                        .onFailure { error = it.message ?: "Could not archive this debt. Try again." }
                }
            },
        )
    }

    val paying = payingDebt
    if (paying != null) {
        PaymentDialog(
            debt = paying,
            snapshot = snapshot,
            currency = currency,
            today = vm.today,
            onDismiss = { payingDebt = null },
            onPay = { walletId, amount, date ->
                scope.launch {
                    runCatching { vm.finance.recordDebtPayment(paying.id, walletId, amount, date) }
                        .onSuccess { payingDebt = null }
                        .onFailure { error = it.message ?: "Could not record this payment. Try again." }
                }
            },
        )
    }

    val totalDebt = active.sumOf { it.currentBalanceMinor }
    val monthlyRequired = active.sumOf { it.periodicPaymentMinor }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = "Debts",
            subtitle = "${active.size} tracked",
            onBack = onBack,
            actions = {
                IconButton(onClick = { editing = null; showDialog = true }) {
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
            if (error != null) {
                StatusBanner(
                    text = error!!,
                    isError = true,
                    onDismiss = { error = null },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MizaniStatCard(
                    label = "Total balance",
                    value = MoneyFormat.format(totalDebt, currency),
                    modifier = Modifier.weight(1f),
                    accent = if (totalDebt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                MizaniStatCard(
                    label = "Required monthly",
                    value = MoneyFormat.format(monthlyRequired, currency),
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                "Debt tracking is for awareness only; Mizani does not provide credit scores, eligibility decisions or " +
                    "borrowing advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (active.isEmpty()) {
                EmptyState(
                    title = "No debts tracked",
                    body = "Add a balance, payment and due date to include debt in your plan.",
                    iconRes = R.drawable.ic_ph_bank,
                    actionLabel = "Add debt",
                    onAction = { editing = null; showDialog = true },
                )
            } else {
                active.forEach { debt ->
                    DebtCard(
                        debt = debt,
                        currency = currency,
                        onEdit = { editing = debt; showDialog = true },
                        onPay = { payingDebt = debt },
                    )
                }
            }

            if (archived.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text("Archived", style = MaterialTheme.typography.titleSmall)
                archived.forEach { debt ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { editing = debt; showDialog = true },
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(Spacing.md))
                            Text(debt.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(MoneyFormat.format(debt.currentBalanceMinor, currency), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtCard(
    debt: DebtEntity,
    currency: Currency,
    onEdit: () -> Unit,
    onPay: () -> Unit,
) {
    val progress = if (debt.originalMinor != null && debt.originalMinor > 0) {
        ((debt.originalMinor - debt.currentBalanceMinor).toDouble() / debt.originalMinor).coerceIn(0.0, 1.0).toFloat()
    } else {
        null
    }
    MizaniCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(debt.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        debt.lender.takeIf { it.isNotBlank() },
                        debt.nextDueEpochDay?.let { "next ${DateText.short(LocalDate.ofEpochDay(it))}" },
                        debt.interestRatePercent?.let { "${it}% interest" },
                    ).joinToString(" · ").ifBlank { "No extra details" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    MoneyFormat.format(debt.currentBalanceMinor, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text("balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(Spacing.sm))
            ProgressBar(fraction = progress, color = MaterialTheme.colorScheme.primary, height = 6)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "${(progress * 100).toInt()}% repaid from ${MoneyFormat.format(debt.originalMinor ?: 0, currency)} original",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Required ${MoneyFormat.format(debt.periodicPaymentMinor, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onPay) {
                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Record payment")
            }
        }
    }
}

@Composable
private fun DebtDialog(
    initial: DebtEntity?,
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long?, Long, LocalDate?, Double?, String) -> Unit,
    onArchive: (DebtEntity) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var lender by rememberSaveable { mutableStateOf(initial?.lender ?: "") }
    var balanceText by rememberSaveable {
        mutableStateOf(initial?.let { MoneyFormat.toInput(it.currentBalanceMinor, currency) } ?: "")
    }
    var originalText by rememberSaveable {
        mutableStateOf(initial?.originalMinor?.let { MoneyFormat.toInput(it, currency) } ?: "")
    }
    var paymentText by rememberSaveable {
        mutableStateOf(initial?.let { MoneyFormat.toInput(it.periodicPaymentMinor, currency) } ?: "")
    }
    var interestText by rememberSaveable { mutableStateOf(initial?.interestRatePercent?.toString() ?: "") }
    var hasDueDate by rememberSaveable { mutableStateOf(initial?.nextDueEpochDay != null) }
    var nextDue by rememberSaveable {
        mutableStateOf(initial?.nextDueEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now())
    }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val balance = MoneyFormat.parse(balanceText, currency) ?: 0L
    val original = MoneyFormat.parse(originalText, currency)
    val payment = MoneyFormat.parse(paymentText, currency) ?: 0L
    val interest = interestText.toDoubleOrNull()

    MizaniAlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(if (initial == null) "Add debt" else "Edit debt") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Debt name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lender,
                    onValueChange = { lender = it },
                    label = { Text("Lender (optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                MizaniAmountField(
                    value = balanceText,
                    onValueChange = { input -> balanceText = input },
                    label = "Current balance",
                    currency = currency,
                )
                MizaniAmountField(
                    value = originalText,
                    onValueChange = { input -> originalText = input },
                    label = "Original amount (optional)",
                    currency = currency,
                )
                MizaniAmountField(
                    value = paymentText,
                    onValueChange = { input -> paymentText = input },
                    label = "Required periodic payment",
                    currency = currency,
                )
                OutlinedTextField(
                    value = interestText,
                    onValueChange = { input -> interestText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Interest rate % (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Next due date", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it })
                }
                if (hasDueDate) {
                    DateField(label = "Due", date = nextDue, onDateSelected = { nextDue = it })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (initial != null) {
                    TextButton(onClick = { onArchive(initial) }, modifier = Modifier.align(Alignment.Start)) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Archive debt")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = "Give this debt a name"
                } else if (balanceText.isNotBlank() && MoneyFormat.parse(balanceText, currency) == null) {
                    error = "Enter a valid balance"
                } else if (balance <= 0) {
                    error = "Enter the current balance"
                } else if (interestText.isNotBlank() && interest == null) {
                    error = "Enter a valid interest rate"
                } else {
                    onSave(
                        name,
                        lender,
                        balance,
                        original,
                        payment,
                        if (hasDueDate) nextDue else null,
                        interest,
                        note,
                    )
                }
            }) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PaymentDialog(
    debt: DebtEntity,
    snapshot: LedgerSnapshot,
    currency: Currency,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPay: (String, Long, LocalDate) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf(MoneyFormat.toInput(debt.periodicPaymentMinor, currency)) }
    var walletId by rememberSaveable { mutableStateOf(snapshot.activeWallets.firstOrNull()?.id) }
    var date by rememberSaveable { mutableStateOf(today) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = MoneyFormat.parse(amountText, currency) ?: 0L

    MizaniAlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Record payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "${debt.name} · balance ${MoneyFormat.format(debt.currentBalanceMinor, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MizaniAmountField(
                    value = amountText,
                    onValueChange = { input -> amountText = input },
                    label = "Payment amount",
                    currency = currency,
                )
                MizaniPicker(
                    selectedText = walletId?.let { snapshot.walletName(it) } ?: "Choose wallet",
                    options = snapshot.activeWallets.map { PickerOption(it.id, it.name) },
                    onSelect = { walletId = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(label = "Paid on", date = date, onDateSelected = { date = it })
                Text(
                    "This records an expense and reduces the debt balance. The category is Debt payments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val wallet = walletId
                if (amount <= 0) error = "Enter a payment amount" else if (wallet == null) error = "Choose a wallet" else onPay(wallet, amount, date)
            }) { Text("Record payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
