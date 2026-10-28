package com.mizani.money.feature.commitments

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.CommitmentEntity
import com.mizani.money.data.repo.CommitmentDraft
import com.mizani.money.data.repo.CommitmentProgress
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniAlertDialog
import com.mizani.money.ui.components.MizaniConfirmDialog
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CommitmentsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onRecordExpense: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<CommitmentEntity?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf<String?>(null) }
    var savingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<CommitmentEntity?>(null) }
    var pendingDelete by remember { mutableStateOf(false) }

    val progress = remember(snapshot.commitments, snapshot.commitmentPayments, snapshot.transactions) {
        snapshot.commitmentProgress().sortedWith(
            compareBy<CommitmentProgress> { it.commitment.archived }
                .thenBy { it.commitment.dueEpochDay }
                .thenBy { it.commitment.name.lowercase() },
        )
    }
    val active = progress.filterNot { it.commitment.archived }
    val archived = progress.filter { it.commitment.archived }
    val alreadyLinkedIds = remember(snapshot.commitmentPayments) {
        snapshot.commitmentPayments.mapTo(mutableSetOf()) { it.transactionId }
    }
    val linkedTransactionsById = remember(snapshot.transactions) { snapshot.transactions.associateBy { it.id } }
    val unlinkedExpenses = remember(snapshot.transactions, alreadyLinkedIds) {
        snapshot.transactions
            .filter { it.type == TransactionType.EXPENSE && it.id !in alreadyLinkedIds }
            .sortedWith(compareByDescending<com.mizani.money.data.local.TransactionEntity> { it.localDateEpochDay }
                .thenByDescending { it.occurredAt })
    }

    if (editorOpen) {
        CommitmentEditorDialog(
            initial = editor,
            state = state,
            busy = savingId == "__editor__",
            error = editError,
            onDismiss = {
                if (savingId != "__editor__") {
                    editorOpen = false
                    editor = null
                    editError = null
                }
            },
            onSave = { draft ->
                savingId = "__editor__"
                editError = null
                scope.launch {
                    runCatching { vm.finance.saveCommitment(editor?.id, draft) }
                        .onSuccess {
                            editorOpen = false
                            editor = null
                            editError = null
                        }
                        .onFailure { editError = it.message ?: "Could not save this commitment." }
                    savingId = null
                }
            },
        )
    }

    if (deleteTarget != null) {
        MizaniConfirmDialog(
            title = "Delete ${deleteTarget?.name}?",
            body = "This removes the commitment and its payment links. The linked expenses stay in your activity and balances.",
            confirmLabel = "Delete commitment",
            destructive = true,
            dismissLabel = "Keep it",
            onDismiss = { if (!pendingDelete) deleteTarget = null },
            onConfirm = {
                val selected = deleteTarget ?: return@MizaniConfirmDialog
                pendingDelete = true
                scope.launch {
                    runCatching { vm.finance.deleteCommitment(selected.id) }
                        .onSuccess { deleteTarget = null; error = null }
                        .onFailure { error = it.message ?: "Could not delete this commitment." }
                    pendingDelete = false
                }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = "Bills & commitments",
            subtitle = "Track one-off bills and payment progress",
            actions = {
                if (active.isNotEmpty()) {
                    IconButton(onClick = { editor = null; editError = null; editorOpen = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add commitment")
                    }
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                "Plan one-off costs with a target and due date. Record each payment as an expense, then link it here to track progress without duplicating it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (error != null) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (active.isEmpty()) {
                MizaniCard {
                    Text(
                        if (archived.isEmpty()) "No bills or commitments yet"
                        else "No active bills or commitments",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        if (archived.isEmpty()) {
                            "Add a one-off bill or commitment to keep its due date and payment progress in one place."
                        } else {
                            "Archived items are saved below. Add a new one when you need to plan another due date."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Button(onClick = { editor = null; editError = null; editorOpen = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 2.dp))
                        Text("Add a one-off bill")
                    }
                }
            } else {
                active.forEach { item ->
                    CommitmentCard(
                        item = item,
                        state = state,
                        linkedTransactions = linkedTransactionsById,
                        unlinkedExpenses = unlinkedExpenses,
                        busy = savingId == item.commitment.id,
                        onRecordExpense = onRecordExpense,
                        onEdit = { editor = item.commitment; editError = null; editorOpen = true },
                        onArchive = { archivedValue ->
                            savingId = item.commitment.id
                            scope.launch {
                                runCatching { vm.finance.setCommitmentArchived(item.commitment.id, archivedValue) }
                                    .onFailure { error = it.message ?: "Could not update this commitment." }
                                savingId = null
                            }
                        },
                        onDelete = { deleteTarget = item.commitment },
                        onLink = { transactionId ->
                            savingId = item.commitment.id
                            error = null
                            scope.launch {
                                runCatching { vm.finance.linkExpenseToCommitment(item.commitment.id, transactionId) }
                                    .onFailure { error = it.message ?: "Could not link this expense." }
                                savingId = null
                            }
                        },
                        onUnlink = { paymentId ->
                            savingId = item.commitment.id
                            error = null
                            scope.launch {
                                runCatching { vm.finance.unlinkCommitmentPayment(paymentId) }
                                    .onFailure { error = it.message ?: "Could not unlink this expense." }
                                savingId = null
                            }
                        },
                    )
                }
            }
            if (archived.isNotEmpty()) {
                SectionHeader("Archived")
                archived.forEach { item ->
                    CommitmentCard(
                        item = item,
                        state = state,
                        linkedTransactions = linkedTransactionsById,
                        unlinkedExpenses = emptyList(),
                        busy = savingId == item.commitment.id,
                        onRecordExpense = onRecordExpense,
                        onEdit = { editor = item.commitment; editError = null; editorOpen = true },
                        onArchive = { archivedValue ->
                            savingId = item.commitment.id
                            scope.launch {
                                runCatching { vm.finance.setCommitmentArchived(item.commitment.id, archivedValue) }
                                    .onFailure { error = it.message ?: "Could not update this commitment." }
                                savingId = null
                            }
                        },
                        onDelete = { deleteTarget = item.commitment },
                        onLink = {},
                        onUnlink = { paymentId ->
                            savingId = item.commitment.id
                            scope.launch {
                                runCatching { vm.finance.unlinkCommitmentPayment(paymentId) }
                                    .onFailure { error = it.message ?: "Could not unlink this expense." }
                                savingId = null
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitmentCard(
    item: CommitmentProgress,
    state: AppUiState,
    linkedTransactions: Map<String, com.mizani.money.data.local.TransactionEntity>,
    unlinkedExpenses: List<com.mizani.money.data.local.TransactionEntity>,
    busy: Boolean,
    onRecordExpense: () -> Unit,
    onEdit: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onLink: (String) -> Unit,
    onUnlink: (String) -> Unit,
) {
    val commitment = item.commitment
    val currency = state.currency
    val categoryName = state.snapshot.categoryName(commitment.categoryId)
    val date = LocalDate.ofEpochDay(commitment.dueEpochDay)
    val fraction = if (commitment.targetMinor > 0L) {
        (item.paidMinor.toDouble() / commitment.targetMinor.toDouble()).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val links = state.snapshot.commitmentPayments.filter { it.commitmentId == commitment.id }

    MizaniCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(commitment.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Due ${DateText.medium(date)} · $categoryName${if (commitment.reminderDaysBefore > 0) " · ${commitment.reminderDaysBefore}-day reminder" else ""}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onEdit, enabled = !busy) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${commitment.name}")
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormat.format(item.paidMinor, currency), style = MaterialTheme.typography.titleSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormat.format(item.remainingMinor, currency), style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        ProgressBar(
            fraction = fraction,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    contentDescription = "${(fraction * 100).toInt()} percent paid toward ${commitment.name}"
                },
        )
        Text(
            "${MoneyFormat.format(item.paidMinor, currency)} of ${MoneyFormat.format(commitment.targetMinor, currency)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (links.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Linked expenses", style = MaterialTheme.typography.labelLarge)
            links.forEach { link ->
                val transaction = linkedTransactions[link.transactionId] ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            transaction.payee.ifBlank { state.snapshot.categoryName(transaction.categoryId) },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${DateText.medium(LocalDate.ofEpochDay(transaction.localDateEpochDay))} · ${MoneyFormat.format(transaction.amountMinor, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onUnlink(link.id) }, enabled = !busy) { Text("Unlink") }
                }
            }
        }
        if (!commitment.archived && item.remainingMinor > 0L && unlinkedExpenses.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            MizaniPicker(
                selectedText = if (busy) "Updating…" else "Link a recorded expense",
                options = unlinkedExpenses.map { expense ->
                    val label = expense.payee.ifBlank { state.snapshot.categoryName(expense.categoryId) }
                    PickerOption(
                        id = expense.id,
                        label = "$label · ${MoneyFormat.format(expense.amountMinor, currency)} · ${DateText.short(LocalDate.ofEpochDay(expense.localDateEpochDay))}",
                    )
                },
                onSelect = onLink,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (!commitment.archived && item.remainingMinor > 0L) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "Record the payment as an expense first, then link it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRecordExpense, enabled = !busy) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Record expense")
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            TextButton(onClick = { onArchive(!commitment.archived) }, enabled = !busy) {
                Icon(Icons.Rounded.Archive, contentDescription = null)
                Text(if (commitment.archived) "Restore" else "Archive")
            }
            TextButton(onClick = onDelete, enabled = !busy) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CommitmentEditorDialog(
    initial: CommitmentEntity?,
    state: AppUiState,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (CommitmentDraft) -> Unit,
) {
    val currency = state.currency
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember(initial?.id) {
        mutableStateOf(initial?.targetMinor?.let { MoneyFormat.toInput(it, currency) }.orEmpty())
    }
    var dueDate by remember(initial?.id) {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.dueEpochDay) } ?: LocalDate.now())
    }
    var categoryId by remember(initial?.id) { mutableStateOf(initial?.categoryId) }
    var reminderDays by remember(initial?.id) {
        mutableStateOf(initial?.reminderDaysBefore?.toString() ?: "0")
    }
    var validationError by remember(initial?.id) { mutableStateOf<String?>(null) }
    val nameFocusRequester = remember(initial?.id) { FocusRequester() }
    val amountFocusRequester = remember(initial?.id) { FocusRequester() }
    val reminderFocusRequester = remember(initial?.id) { FocusRequester() }
    val nameError = validationError?.takeIf { it.startsWith("Enter a name") }
    val amountError = validationError?.takeIf { it.startsWith("Enter an amount") }
    val reminderError = validationError?.takeIf { it.startsWith("Choose a reminder") }

    MizaniAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (initial == null) "Add commitment" else "Edit commitment") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    "Payments must already be recorded as expenses. Linking them here will not create another transaction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80); validationError = null },
                    label = { Text("Name") },
                    isError = nameError != null,
                    supportingText = { nameError?.let { message ->
                        Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocusRequester),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; validationError = null },
                    label = { Text("Target (${currency.code})") },
                    isError = amountError != null,
                    supportingText = { amountError?.let { message ->
                        Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocusRequester),
                )
                DateField(label = "Due date", date = dueDate, onDateSelected = { dueDate = it }, modifier = Modifier.fillMaxWidth())
                val categories = state.snapshot.categories.filter {
                    it.type == CategoryType.EXPENSE && !it.archived || it.id == initial?.categoryId
                }
                val selectedCategory = categories.firstOrNull { it.id == categoryId }
                MizaniPicker(
                    selectedText = selectedCategory?.name ?: "No category",
                    options = categories.map { PickerOption(it.id, it.name) },
                    onSelect = { categoryId = it; validationError = null },
                    modifier = Modifier.fillMaxWidth(),
                    noneLabel = "No category",
                    onSelectNone = { categoryId = null; validationError = null },
                    searchable = categories.size > 6,
                    searchHint = "Search expense categories",
                    fieldLabel = "Category",
                )
                OutlinedTextField(
                    value = reminderDays,
                    onValueChange = { value -> reminderDays = value.filter(Char::isDigit).take(2); validationError = null },
                    label = { Text("Reminder days before") },
                    isError = reminderError != null,
                    supportingText = {
                        Text(
                            reminderError ?: "Enter 0 to turn reminders off (maximum 30 days).",
                            modifier = if (reminderError != null) Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            } else Modifier,
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(reminderFocusRequester),
                )
                error?.let { message ->
                    Text(
                        message,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    val parsedAmount = MoneyFormat.parse(amount, currency)
                    val days = reminderDays.toIntOrNull()
                    when {
                        name.isBlank() -> {
                            validationError = "Enter a name for this commitment."
                            nameFocusRequester.requestFocus()
                        }
                        parsedAmount == null || parsedAmount <= 0L -> {
                            validationError = "Enter an amount greater than zero."
                            amountFocusRequester.requestFocus()
                        }
                        days == null || days !in 0..30 -> {
                            validationError = "Choose a reminder from 0 to 30 days."
                            reminderFocusRequester.requestFocus()
                        }
                        else -> {
                            validationError = null
                            onSave(
                                CommitmentDraft(
                                    name = name,
                                    targetMinor = parsedAmount,
                                    dueDate = dueDate,
                                    categoryId = categoryId,
                                    reminderDaysBefore = days,
                                ),
                            )
                        }
                    }
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp).semantics {
                            contentDescription = "Saving commitment"
                        },
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text("Saving...")
                } else Text(if (initial == null) "Save commitment" else "Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        },
    )
}
