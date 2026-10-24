package com.mizani.money.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.IncomeSourceKind
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.repo.LedgerSnapshot
import com.mizani.money.data.repo.RecurringDraft
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniConfirmDialog
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import java.time.LocalDate

@Composable
fun RecurringDialog(
    initial: RecurringRuleEntity?,
    type: RecurringType,
    snapshot: LedgerSnapshot,
    currency: Currency,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String?, RecurringDraft) -> Unit,
    onArchive: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var amountText by rememberSaveable {
        mutableStateOf(initial?.let { MoneyFormat.toInput(it.amountMinor, currency) } ?: "")
    }
    var nextDue by rememberSaveable {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.nextDueEpochDay) } ?: today)
    }
    var frequency by rememberSaveable { mutableStateOf(initial?.frequency ?: Frequency.MONTHLY) }
    var essential by rememberSaveable { mutableStateOf(initial?.essential ?: true) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId) }
    var walletId by rememberSaveable { mutableStateOf(initial?.walletId) }
    var error by remember { mutableStateOf<String?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    val desiredCategoryType = if (type == RecurringType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    val categories = snapshot.activeCategories.filter { it.type == desiredCategoryType }
    val wallets = snapshot.activeWallets

    val amount = MoneyFormat.parse(amountText, currency) ?: 0L

    if (showArchiveConfirm && initial != null) {
        MizaniConfirmDialog(
            title = if (type == RecurringType.BILL) "Archive this bill?" else "Archive this expected income?",
            body = "It stops appearing in ${if (type == RecurringType.BILL) "bills" else "expected income"} " +
                "and in planning. You can add it again later.",
            confirmLabel = "Archive",
            destructive = true,
            onConfirm = {
                showArchiveConfirm = false
                onArchive(initial.id)
            },
            onDismiss = { showArchiveConfirm = false },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(if (initial == null) "Add ${if (type == RecurringType.BILL) "a bill" else "expected income"}" else "Edit ${if (type == RecurringType.BILL) "bill" else "expected income"}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (type == RecurringType.BILL) "Bill name" else "Income name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                MizaniAmountField(
                    value = amountText,
                    onValueChange = { input -> amountText = input },
                    label = "Amount",
                    currency = currency,
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = categoryId?.let { snapshot.categoryName(it) } ?: "Choose category",
                    options = categories.map { PickerOption(it.id, it.name) },
                    onSelect = { categoryId = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Pay from wallet (optional)", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = walletId?.let { snapshot.walletName(it) } ?: "No default wallet",
                    options = wallets.map { PickerOption(it.id, it.name) },
                    onSelect = { walletId = it },
                    noneLabel = "No default wallet",
                    onSelectNone = { walletId = null },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Repeats", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = frequency.label,
                    options = Frequency.entries.map { PickerOption(it.name, it.label) },
                    onSelect = { frequency = Frequency.valueOf(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(label = "Next date", date = nextDue, onDateSelected = { nextDue = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Essential commitment", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Reserve money for this before flexible spending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = essential, onCheckedChange = { essential = it })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initial != null) {
                    TextButton(
                        onClick = { showArchiveConfirm = true },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Archive")
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        error = "Give this a name"
                    } else if (amount <= 0) {
                        error = "Enter an amount greater than zero"
                    } else {
                        onSave(
                            initial?.id,
                            RecurringDraft(
                                name = name,
                                type = type,
                                amountMinor = amount,
                                categoryId = categoryId,
                                walletId = walletId,
                                frequency = frequency,
                                nextDue = nextDue,
                                essential = essential,
                                note = note,
                            ),
                        )
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun IncomeSourceDialog(
    onDismiss: () -> Unit,
    onSave: (String, IncomeSourceKind) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(IncomeSourceKind.SALARY) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Add an income source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (for example: Shop sales)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Type", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = kind.label,
                    options = IncomeSourceKind.entries.map { PickerOption(it.name, it.label) },
                    onSelect = { kind = IncomeSourceKind.valueOf(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) error = "Enter a name" else onSave(name, kind)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun FundLineDialog(
    title: String,
    plannedMinor: Long,
    fundedMinor: Long,
    currency: Currency,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val addition = MoneyFormat.parse(amountText, currency) ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Fund $title") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "Planned ${MoneyFormat.format(plannedMinor, currency)} · already funded ${MoneyFormat.format(fundedMinor, currency)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MizaniAmountField(
                    value = amountText,
                    onValueChange = { input -> amountText = input },
                    label = "Add to funded amount",
                    currency = currency,
                )
                Text(
                    "Funding records what you have set aside. It does not create a transaction or count as spending.",
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
                if (addition <= 0) error = "Enter an amount greater than zero" else onApply(fundedMinor + addition)
            }) { Text("Mark funded") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ContributeGoalDialog(
    goalName: String,
    targetMinor: Long,
    allocatedMinor: Long,
    wallets: List<com.mizani.money.data.local.WalletEntity>,
    currency: Currency,
    onDismiss: () -> Unit,
    onContribute: (Long, String?) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var walletId by rememberSaveable { mutableStateOf(wallets.firstOrNull { it.type == com.mizani.money.core.model.WalletType.SAVINGS }?.id) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = MoneyFormat.parse(amountText, currency) ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Add to $goalName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "${MoneyFormat.format(allocatedMinor, currency)} of ${MoneyFormat.format(targetMinor, currency)} set aside.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MizaniAmountField(
                    value = amountText,
                    onValueChange = { input -> amountText = input },
                    label = "Contribution",
                    currency = currency,
                )
                Text("Move money from (optional)", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = walletId?.let { id -> wallets.firstOrNull { it.id == id }?.name } ?: "Do not move money",
                    options = wallets.map { PickerOption(it.id, it.name) },
                    onSelect = { walletId = it },
                    noneLabel = "Do not move money",
                    onSelectNone = { walletId = null },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "If you choose a wallet, the money moves from it as a transfer. Transfers are not spending.",
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
                if (amount <= 0) error = "Enter an amount greater than zero" else onContribute(amount, walletId)
            }) { Text("Add contribution") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
