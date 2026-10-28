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
import androidx.compose.runtime.LaunchedEffect
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
import com.mizani.money.core.money.CurrencyConversion
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.CommitmentEntity
import com.mizani.money.data.repo.CommitmentDraft
import com.mizani.money.data.repo.CommitmentProgress
import com.mizani.money.data.repo.ExchangeRate
import com.mizani.money.data.repo.ExchangeRateUnavailableException
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
import kotlinx.coroutines.CancellationException
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
    var actionError by remember { mutableStateOf<Pair<String, String>?>(null) }
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
    val netExpenseMinorById = remember(snapshot.transactions) { snapshot.netExpenseMinorById() }
    val unlinkedExpenseCandidates = remember(snapshot.transactions, alreadyLinkedIds) {
        snapshot.transactions
            .filter { it.type == TransactionType.EXPENSE && it.id !in alreadyLinkedIds }
    }
    val unlinkedExpenses = remember(snapshot.transactions, alreadyLinkedIds, snapshot.currencyCode) {
        snapshot.transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.id !in alreadyLinkedIds &&
                    it.currencyCode == snapshot.currencyCode &&
                    (netExpenseMinorById[it.id] ?: 0L) > 0L
            }
            .sortedWith(compareByDescending<com.mizani.money.data.local.TransactionEntity> { it.localDateEpochDay }
                .thenByDescending { it.occurredAt })
    }
    val linkableExpensesByCommitment = remember(active, unlinkedExpenses, netExpenseMinorById) {
        active.associate { item ->
            item.commitment.id to unlinkedExpenses.filter { expense ->
                (netExpenseMinorById[expense.id] ?: 0L) <= item.remainingWorkspaceMinor
            }
        }
    }

    if (editorOpen) {
        CommitmentEditorDialog(
            initial = editor,
            state = state,
            vm = vm,
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
                "Keep a planned amount and due date in one place. Record the expense when you pay, then link it here to update the bill without adding a second transaction.",
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
                        linkableExpenses = linkableExpensesByCommitment[item.commitment.id].orEmpty(),
                        hasRecordedExpenses = unlinkedExpenseCandidates.isNotEmpty(),
                        netExpenseMinorById = netExpenseMinorById,
                        actionError = actionError?.takeIf { it.first == item.commitment.id }?.second,
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
                            actionError = null
                            scope.launch {
                                runCatching { vm.finance.linkExpenseToCommitment(item.commitment.id, transactionId) }
                                    .onFailure {
                                        actionError = item.commitment.id to commitmentActionMessage(
                                            it,
                                            "Could not link this expense. Please try again.",
                                        )
                                    }
                                savingId = null
                            }
                        },
                        onUnlink = { paymentId ->
                            savingId = item.commitment.id
                            error = null
                            actionError = null
                            scope.launch {
                                runCatching { vm.finance.unlinkCommitmentPayment(paymentId) }
                                    .onFailure {
                                        actionError = item.commitment.id to commitmentActionMessage(
                                            it,
                                            "Could not unlink this expense. Please try again.",
                                        )
                                    }
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
                        linkableExpenses = emptyList(),
                        hasRecordedExpenses = false,
                        netExpenseMinorById = netExpenseMinorById,
                        actionError = actionError?.takeIf { it.first == item.commitment.id }?.second,
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
                            actionError = null
                            scope.launch {
                                runCatching { vm.finance.unlinkCommitmentPayment(paymentId) }
                                    .onFailure {
                                        actionError = item.commitment.id to commitmentActionMessage(
                                            it,
                                            "Could not unlink this expense. Please try again.",
                                        )
                                    }
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
    linkableExpenses: List<com.mizani.money.data.local.TransactionEntity>,
    hasRecordedExpenses: Boolean,
    netExpenseMinorById: Map<String, Long>,
    actionError: String?,
    busy: Boolean,
    onRecordExpense: () -> Unit,
    onEdit: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onLink: (String) -> Unit,
    onUnlink: (String) -> Unit,
) {
    val commitment = item.commitment
    val workspaceCurrency = state.currency
    val currency = Currencies.byCode(commitment.currencyCode)
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
                    "Due ${DateText.medium(date)} · $categoryName · ${currency.code}${if (commitment.reminderDaysBefore > 0) " · ${commitment.reminderDaysBefore}-day reminder" else ""}",
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
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormat.format(item.paidMinor, currency), style = MaterialTheme.typography.titleSmall)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormat.format(item.remainingMinor, currency), style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
            if (commitment.currencyCode != workspaceCurrency.code) {
                Text(
                    "Target saved at 1 ${commitment.currencyCode} = ${MoneyFormat.formatBare(commitment.targetWorkspaceMinor, workspaceCurrency)} ${workspaceCurrency.code} · ${commitment.exchangeRateDate ?: "reference rate"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                            "${DateText.medium(LocalDate.ofEpochDay(transaction.localDateEpochDay))} · ${MoneyFormat.format(transaction.amountMinor, workspaceCurrency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onUnlink(link.id) }, enabled = !busy) { Text("Unlink") }
                }
            }
        }
        if (!commitment.archived && item.remainingMinor > 0L && linkableExpenses.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Choose a recorded expense to add this payment to the bill.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MizaniPicker(
                selectedText = if (busy) "Updating..." else "Choose expense to link",
                options = linkableExpenses.map { expense ->
                    val label = expense.payee.ifBlank { state.snapshot.categoryName(expense.categoryId) }
                    PickerOption(
                        id = expense.id,
                        label = "$label - ${MoneyFormat.format(netExpenseMinorById[expense.id] ?: expense.amountMinor, workspaceCurrency)} - ${DateText.short(LocalDate.ofEpochDay(expense.localDateEpochDay))}",
                    )
                },
                onSelect = onLink,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (!commitment.archived && item.remainingMinor > 0L) {
            Spacer(Modifier.height(Spacing.xs))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    if (hasRecordedExpenses) {
                        "No recorded expense matches the remaining ${MoneyFormat.format(item.remainingMinor, currency)}. Record the payment in ${currency.code}, or choose an expense within the converted target."
                    } else {
                        "Record the payment as an expense first, then link it here."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRecordExpense, enabled = !busy) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Record expense")
                }
            }
        }
        actionError?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
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

private fun commitmentActionMessage(error: Throwable, fallback: String): String = when (error.message) {
    "Payment is greater than the remaining commitment" ->
        "This expense is greater than the amount still due. Choose a smaller expense or increase the target."
    "This expense has no remaining value after refunds" ->
        "This expense has already been fully refunded and cannot be linked."
    "Currency mismatch" ->
        "This expense uses a different currency and cannot be linked to this bill."
    "This expense is already linked to a commitment" ->
        "This expense is already linked to another bill."
    else -> fallback
}

private fun commitmentRateFailureMessage(error: Exception, hasCachedRate: Boolean): String = when {
    hasCachedRate -> "Saved rate in use. Refresh failed."
    error is ExchangeRateUnavailableException -> "No reference rate is available for this currency pair."
    else -> "No rate available. Check your connection."
}

@Composable
private fun CommitmentEditorDialog(
    initial: CommitmentEntity?,
    state: AppUiState,
    vm: AppViewModel,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (CommitmentDraft) -> Unit,
) {
    val workspaceCurrency = state.currency
    var targetCurrencyCode by remember(initial?.id) {
        mutableStateOf(initial?.currencyCode ?: workspaceCurrency.code)
    }
    val targetCurrency = Currencies.byCode(targetCurrencyCode)
    val isForeignCurrency = targetCurrency.code != workspaceCurrency.code
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember(initial?.id) {
        mutableStateOf(initial?.targetMinor?.let { MoneyFormat.toInput(it, targetCurrency) }.orEmpty())
    }
    var dueDate by remember(initial?.id) {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.dueEpochDay) } ?: LocalDate.now())
    }
    var categoryId by remember(initial?.id) { mutableStateOf(initial?.categoryId) }
    var reminderDays by remember(initial?.id) {
        mutableStateOf(initial?.reminderDaysBefore?.toString() ?: "0")
    }
    var exchangeRate by remember(initial?.id) { mutableStateOf<ExchangeRate?>(null) }
    var isRateLoading by remember(initial?.id) { mutableStateOf(false) }
    var rateError by remember(initial?.id) { mutableStateOf<String?>(null) }
    var validationError by remember(initial?.id) { mutableStateOf<String?>(null) }
    val nameFocusRequester = remember(initial?.id) { FocusRequester() }
    val amountFocusRequester = remember(initial?.id) { FocusRequester() }
    val reminderFocusRequester = remember(initial?.id) { FocusRequester() }
    val nameError = validationError?.takeIf { it.startsWith("Enter a name") }
    val amountError = validationError?.takeIf { it.startsWith("Enter an amount") }
    val reminderError = validationError?.takeIf { it.startsWith("Choose a reminder") }

    LaunchedEffect(targetCurrency.code, workspaceCurrency.code, initial?.id) {
        if (!isForeignCurrency) {
            exchangeRate = null
            rateError = null
            isRateLoading = false
            return@LaunchedEffect
        }

        val savedRate = initial?.takeIf {
            it.currencyCode == targetCurrency.code && it.exchangeRate != null && it.exchangeRateDate != null
        }?.let { commitment ->
            runCatching {
                ExchangeRate(
                    commitment.currencyCode,
                    workspaceCurrency.code,
                    commitment.exchangeRate!!,
                    LocalDate.parse(commitment.exchangeRateDate!!),
                )
            }.getOrNull()
        }
        if (savedRate != null) {
            exchangeRate = savedRate
            rateError = null
            isRateLoading = false
            return@LaunchedEffect
        }

        exchangeRate = null
        rateError = null
        isRateLoading = true
        try {
            exchangeRate = vm.exchangeRates.cachedRate(targetCurrency.code, workspaceCurrency.code)
            val shouldRefresh = vm.exchangeRates.shouldRefresh(targetCurrency.code, workspaceCurrency.code)
            if (shouldRefresh) {
                try {
                    exchangeRate = vm.exchangeRates.refreshRate(targetCurrency.code, workspaceCurrency.code)
                    rateError = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    rateError = commitmentRateFailureMessage(failure, exchangeRate != null)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            rateError = commitmentRateFailureMessage(failure, exchangeRate != null)
        } finally {
            isRateLoading = false
        }
    }

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
                    label = { Text("Target amount (${targetCurrency.code})") },
                    isError = amountError != null,
                    supportingText = { amountError?.let { message ->
                        Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocusRequester),
                )
                MizaniPicker(
                    selectedText = "${targetCurrency.code} · ${targetCurrency.name}",
                    options = Currencies.transactionCurrencies.map { option ->
                        PickerOption(option.code, "${option.code} · ${option.name}")
                    },
                    onSelect = {
                        if (it != targetCurrencyCode) {
                            // Do not silently reinterpret an amount under a new currency.
                            // Clear it so the user enters a value with the new currency label.
                            amount = ""
                        }
                        targetCurrencyCode = it
                        validationError = null
                        rateError = null
                    },
                    searchable = true,
                    searchHint = "Search currencies",
                    fieldLabel = "Target currency",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isForeignCurrency) {
                    val rate = exchangeRate
                    Text(
                        when {
                            isRateLoading && rate == null -> "Getting the latest daily reference rate..."
                            rate != null -> buildString {
                                append("1 ${rate.baseCurrencyCode} = ${rate.rate} ${rate.quoteCurrencyCode} · ${DateText.medium(rate.date)}. The target will be saved using this rate.")
                                rateError?.let { append(" $it") }
                            }
                            else -> rateError ?: "No reference rate is available for this currency pair."
                        },
                        color = if (rate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics {
                            if (rate == null) liveRegion = LiveRegionMode.Polite
                        },
                    )
                }
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
                    val parsedAmount = MoneyFormat.parse(amount, targetCurrency)
                    val conversionRate = exchangeRate?.rate?.toBigDecimalOrNull()
                    val convertedTargetWorkspace = parsedAmount?.let { value ->
                        if (!isForeignCurrency) value else conversionRate?.let { rate ->
                            runCatching {
                                CurrencyConversion.convertMinorUnits(
                                    value,
                                    targetCurrency,
                                    workspaceCurrency,
                                    rate,
                                )
                            }.getOrNull()
                        }
                    }
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
                        isForeignCurrency && isRateLoading -> {
                            validationError = "Wait for the latest exchange rate before saving."
                        }
                        isForeignCurrency && conversionRate == null -> {
                            validationError = rateError ?: "No reference rate is available for this currency pair."
                        }
                        convertedTargetWorkspace == null || convertedTargetWorkspace <= 0L -> {
                            validationError = "This amount is too small to convert into ${workspaceCurrency.code}."
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
                                    currencyCode = targetCurrency.code,
                                    targetWorkspaceMinor = convertedTargetWorkspace,
                                    exchangeRate = exchangeRate?.rate,
                                    exchangeRateDate = exchangeRate?.date,
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
