package com.mizani.money.feature.commitments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.repo.RecurringDraft
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.MizaniAlertDialog
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StorysetIllustration
import java.time.LocalDate

/** The user-facing recurring schedule workspace. Schedules are forecasts until recorded. */
@Composable
internal fun RecurringSchedulesPanel(
    state: AppUiState,
    today: LocalDate,
    busyId: String?,
    message: String?,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onEdit: (RecurringRuleEntity) -> Unit,
    onArchive: (RecurringRuleEntity, Boolean) -> Unit,
    onRecord: (RecurringRuleEntity) -> Unit,
    onDelete: (RecurringRuleEntity) -> Unit,
) {
    val snapshot = state.snapshot
    val active = remember(snapshot.recurring) {
        snapshot.recurring
            .filter { it.deletedAt == null && !it.archived }
            .sortedWith(compareBy<RecurringRuleEntity> { it.nextDueEpochDay }.thenBy { it.name.lowercase() })
    }
    val archived = remember(snapshot.recurring) {
        snapshot.recurring
            .filter { it.deletedAt == null && it.archived }
            .sortedBy { it.name.lowercase() }
    }
    val activeBills = active.count { it.type == RecurringType.BILL }
    val activeIncome = active.count { it.type == RecurringType.INCOME }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(top = Spacing.lg)
            .padding(bottom = Spacing.bottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        MizaniCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Repeating money, made simple", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A schedule is a reminder and forecast. It never changes your balance until you record the actual payment or receipt.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                "$activeBills recurring bill${if (activeBills == 1) "" else "s"} · " +
                    "$activeIncome expected income item${if (activeIncome == 1) "" else "s"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        message?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { contentDescription = it },
            )
        }

        if (active.isEmpty()) {
            MizaniCard {
                StorysetIllustration(
                    assetName = "financial_data_cuate.svg",
                    modifier = Modifier.fillMaxWidth().height(132.dp),
                    contentDescription = "An illustration about planning and understanding money",
                )
                Spacer(Modifier.height(Spacing.sm))
                Text("Nothing repeating yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Add rent, data, school fees, subscriptions, salary or any other money movement that happens on a pattern.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                Button(
                    onClick = onAdd,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Autorenew, contentDescription = null)
                    Spacer(Modifier.size(Spacing.xs))
                    Text("Add repeating item")
                }
            }
        } else {
            active.forEach { rule ->
                RecurringScheduleCard(
                    rule = rule,
                    today = today,
                    currency = state.currency,
                    categoryName = rule.categoryId?.let { snapshot.categoryName(it) },
                    busy = busyId == rule.id,
                    onEdit = { onEdit(rule) },
                    onArchive = { onArchive(rule, true) },
                    onRecord = { onRecord(rule) },
                    onDelete = { onDelete(rule) },
                )
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Rounded.Autorenew, contentDescription = null)
                Spacer(Modifier.size(Spacing.xs))
                Text("Add repeating item")
            }
        }

        if (archived.isNotEmpty()) {
            SectionHeader("Archived repeating items")
            archived.forEach { rule ->
                RecurringScheduleCard(
                    rule = rule,
                    today = today,
                    currency = state.currency,
                    categoryName = rule.categoryId?.let { snapshot.categoryName(it) },
                    busy = busyId == rule.id,
                    onEdit = { onEdit(rule) },
                    onArchive = { onArchive(rule, false) },
                    onRecord = {},
                    onDelete = { onDelete(rule) },
                )
            }
        }
    }
}

@Composable
private fun RecurringScheduleCard(
    rule: RecurringRuleEntity,
    today: LocalDate,
    currency: com.mizani.money.core.money.Currency,
    categoryName: String?,
    busy: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRecord: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = LocalDate.ofEpochDay(rule.nextDueEpochDay)
    val overdue = date.isBefore(today)
    val isIncome = rule.type == RecurringType.INCOME
    val dueLabel = when {
        date == today -> "Due today"
        overdue -> "Overdue · ${DateText.relative(date, today)}"
        else -> "Next ${DateText.relative(date, today)}"
    }
    val typeLabel = if (isIncome) "Expected income" else "Recurring bill"
    val amountLabel = MoneyFormat.format(rule.amountMinor, currency)
    val categoryLabel = categoryName?.let { " · $it" }.orEmpty()

    MizaniCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Rounded.Payments else Icons.Rounded.CalendarMonth,
                contentDescription = typeLabel,
                tint = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "$typeLabel · ${rule.frequency.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$amountLabel$categoryLabel",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
                Text(
                    "$dueLabel · ${DateText.medium(date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit, enabled = !busy) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${rule.name}")
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        if (!rule.archived) {
            Button(
                onClick = onRecord,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(Spacing.xs))
                }
                Text(if (isIncome) "Record income received" else "Record payment")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onArchive, enabled = !busy) {
                Icon(Icons.Rounded.Archive, contentDescription = null)
                Spacer(Modifier.size(Spacing.xs))
                Text(if (rule.archived) "Restore" else "Archive")
            }
            IconButton(onClick = onDelete, enabled = !busy) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete ${rule.name}")
            }
        }
    }
}

@Composable
internal fun RecurringEditorDialog(
    initial: RecurringRuleEntity?,
    state: AppUiState,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (RecurringDraft) -> Unit,
) {
    val currency = state.currency
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: RecurringType.BILL) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember(initial?.id) {
        mutableStateOf(initial?.let { MoneyFormat.toInput(it.amountMinor, currency) }.orEmpty())
    }
    var frequency by remember(initial?.id) { mutableStateOf(initial?.frequency ?: Frequency.MONTHLY) }
    var nextDue by remember(initial?.id) {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.nextDueEpochDay) } ?: LocalDate.now())
    }
    var categoryId by remember(initial?.id) { mutableStateOf(initial?.categoryId) }
    var walletId by remember(initial?.id) { mutableStateOf(initial?.walletId) }
    var validationError by remember(initial?.id) { mutableStateOf<String?>(null) }

    val categories = state.snapshot.categories.filter {
        val expected = if (type == RecurringType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        it.type == expected && (!it.archived || it.id == initial?.categoryId)
    }
    val wallets = state.snapshot.wallets.filter { !it.archived && it.deletedAt == null }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val selectedWallet = wallets.firstOrNull { it.id == walletId }

    MizaniAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (initial == null) "Add repeating item" else "Edit repeating item") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    "Use this for a pattern, not a transaction. Mizani changes the schedule after you record what actually happened.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("What repeats?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = type == RecurringType.BILL,
                        onClick = { type = RecurringType.BILL; categoryId = null; validationError = null },
                        label = { Text("Bill / expense") },
                    )
                    FilterChip(
                        selected = type == RecurringType.INCOME,
                        onClick = { type = RecurringType.INCOME; categoryId = null; validationError = null },
                        label = { Text("Expected income") },
                    )
                }
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80); validationError = null },
                    label = { Text("Name") },
                    supportingText = { Text("For example: Rent, internet, salary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MizaniAmountField(
                    value = amount,
                    onValueChange = { amount = it; validationError = null },
                    label = "Amount (${currency.code})",
                    currency = currency,
                    supportingText = "Enter the usual amount; adjust it before recording if it changes.",
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
                MizaniPicker(
                    selectedText = frequency.label,
                    options = Frequency.entries.map { PickerOption(it.name, it.label) },
                    onSelect = { frequency = Frequency.valueOf(it); validationError = null },
                    fieldLabel = "Repeats",
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "After you record an occurrence, the next date advances by this interval. Monthly and yearly dates clamp to the last valid day of shorter months.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DateField(
                    label = "Next expected date",
                    date = nextDue,
                    onDateSelected = { nextDue = it; validationError = null },
                    modifier = Modifier.fillMaxWidth(),
                )
                MizaniPicker(
                    selectedText = selectedCategory?.name ?: "No category",
                    options = categories.map { PickerOption(it.id, it.name) },
                    onSelect = { categoryId = it; validationError = null },
                    noneLabel = "No category",
                    onSelectNone = { categoryId = null; validationError = null },
                    searchable = categories.size > 6,
                    searchHint = "Search categories",
                    fieldLabel = "Category",
                    modifier = Modifier.fillMaxWidth(),
                )
                MizaniPicker(
                    selectedText = selectedWallet?.name ?: "No wallet chosen",
                    options = wallets.map { PickerOption(it.id, it.name) },
                    onSelect = { walletId = it; validationError = null },
                    noneLabel = "No wallet chosen",
                    onSelectNone = { walletId = null; validationError = null },
                    searchable = wallets.size > 6,
                    searchHint = "Search wallets",
                    fieldLabel = "Wallet",
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Wallet is optional. If you leave it blank, Mizani uses the first active wallet when you record it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    val parsedAmount = MoneyFormat.parse(amount, currency)
                    when {
                        name.isBlank() -> validationError = "Enter a name for this repeating item."
                        parsedAmount == null || parsedAmount <= 0L -> validationError = "Enter an amount greater than zero."
                        else -> onSave(
                            RecurringDraft(
                                name = name,
                                type = type,
                                amountMinor = parsedAmount,
                                categoryId = categoryId,
                                walletId = walletId,
                                frequency = frequency,
                                nextDue = nextDue,
                                essential = initial?.essential ?: true,
                                note = initial?.note.orEmpty(),
                            ),
                        )
                    }
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(Spacing.xs))
                }
                Text(if (initial == null) "Save repeating item" else "Save changes")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
