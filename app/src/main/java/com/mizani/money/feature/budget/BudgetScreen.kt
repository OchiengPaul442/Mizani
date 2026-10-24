package com.mizani.money.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryDefaults
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.data.repo.BudgetLineInput
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.CategoryAvatar
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.SmallPill
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.MizaniCelebration
import com.mizani.money.ui.components.StatusBanner
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun BudgetScreen(
    vm: AppViewModel,
    state: AppUiState,
    budgetId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val today = vm.today
    val scope = rememberCoroutineScope()
    val existing = remember(budgetId, snapshot.budgets) {
        budgetId?.let { id -> snapshot.budgets.firstOrNull { it.id == id } }
    }
    val existingLines = remember(budgetId, snapshot.budgetLines) {
        snapshot.budgetLinesFor(budgetId)
    }
    val previousBudget = remember(snapshot.budgets, budgetId) {
        snapshot.budgets.filter { it.id != budgetId }.maxByOrNull { it.startEpochDay }
    }
    val previousLines = remember(previousBudget, snapshot.budgetLines) {
        previousBudget?.let { snapshot.budgetLinesFor(it.id) } ?: emptyList()
    }

    val allCategories = remember(snapshot.categories) {
        snapshot.categories
            .filter { it.type == CategoryType.EXPENSE && it.deletedAt == null }
    }
    val categoryIds = remember(allCategories, existingLines) {
        (allCategories.map { it.id } + existingLines.map { it.categoryId }).distinct()
    }

    var name by rememberSaveable(budgetId) {
        mutableStateOf(existing?.name ?: "${DateText.month(today)} plan")
    }
    var periodType by rememberSaveable(budgetId) {
        mutableStateOf(existing?.periodType ?: BudgetPeriodType.MONTHLY)
    }
    var start by rememberSaveable(budgetId) {
        mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.startEpochDay) } ?: Periods.monthOf(today).start)
    }
    var end by rememberSaveable(budgetId) {
        mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.endEpochDay) } ?: Periods.monthOf(today).end)
    }
    var planned by remember(budgetId) {
        mutableStateOf<Map<String, String>>(
            existingLines.associate { line ->
                line.categoryId to MoneyFormat.toInput(line.plannedMinor, currency)
            },
        )
    }
    var funded by remember(budgetId) {
        mutableStateOf<Map<String, String>>(
            existingLines.associate { line ->
                line.categoryId to MoneyFormat.toInput(line.fundedMinor, currency)
            },
        )
    }
    var essential by remember(budgetId) {
        mutableStateOf<Map<String, Boolean>>(
            allCategories.associate { category ->
                category.id to (existingLines.firstOrNull { it.categoryId == category.id }?.essential
                    ?: CategoryDefaults.isEssential(category.id))
            },
        )
    }
    var rollover by remember(budgetId) {
        mutableStateOf<Map<String, RolloverRule>>(
            allCategories.associate { category ->
                category.id to (existingLines.firstOrNull { it.categoryId == category.id }?.rolloverRule ?: RolloverRule.NONE)
            },
        )
    }
    var priority by remember(budgetId) {
        mutableStateOf<Map<String, Int>>(
            allCategories.associate { category ->
                category.id to (existingLines.firstOrNull { it.categoryId == category.id }?.priority ?: 2)
            },
        )
    }
    var optionsTarget by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }
    // Progressive disclosure: a new budget shows only the essential default pack; editing keeps
    // every existing line (even at zero) plus essentials. The rest is one toggle away.
    var showAllCategories by rememberSaveable(budgetId) { mutableStateOf(false) }
    val essentialPackIds = allCategories
        .filter { CategoryDefaults.isEssential(it.id) }
        .map { it.id }
    val baseVisibleCategoryIds = if (existing != null) {
        (existingLines.map { it.categoryId } + essentialPackIds).distinct()
    } else {
        essentialPackIds.take(6)
    }
    val displayedCategoryIds = if (showAllCategories) categoryIds else baseVisibleCategoryIds

    fun plannedMinor(categoryId: String): Long = MoneyFormat.parse(planned[categoryId].orEmpty(), currency) ?: 0L
    fun fundedMinor(categoryId: String): Long = MoneyFormat.parse(funded[categoryId].orEmpty(), currency) ?: 0L
    val totalPlanned = categoryIds.sumOf { plannedMinor(it) }
    val totalFunded = categoryIds.sumOf { fundedMinor(it) }

    val target = optionsTarget
    if (target != null) {
        val category = snapshot.category(target)
        LineOptionsDialog(
            categoryName = category?.name ?: "Category",
            fundedText = funded[target].orEmpty(),
            plannedMinor = plannedMinor(target),
            essential = essential[target] ?: false,
            rollover = rollover[target] ?: RolloverRule.NONE,
            priority = priority[target] ?: 2,
            currency = currency,
            onDismiss = { optionsTarget = null },
            onApply = { fundedValue, isEssential, rolloverValue, priorityValue ->
                funded = funded + (target to fundedValue)
                essential = essential + (target to isEssential)
                rollover = rollover + (target to rolloverValue)
                priority = priority + (target to priorityValue)
                optionsTarget = null
            },
        )
    }

    fun save() {
        if (saving) return
        error = null
        val inputs = categoryIds
            .mapNotNull { categoryId ->
                val planned = plannedMinor(categoryId)
                val fundedValue = fundedMinor(categoryId)
                if (planned <= 0 && fundedValue <= 0) return@mapNotNull null
                BudgetLineInput(
                    categoryId = categoryId,
                    plannedMinor = planned,
                    fundedMinor = fundedValue,
                    essential = essential[categoryId] ?: false,
                    rolloverRule = rollover[categoryId] ?: RolloverRule.NONE,
                    priority = priority[categoryId] ?: 2,
                )
            }
        if (inputs.isEmpty()) {
            error = "Set a planned amount for at least one category"
            return
        }
        saving = true
        scope.launch {
            runCatching {
                vm.finance.saveBudget(
                    budgetId = existing?.id,
                    name = name,
                    periodType = periodType,
                    start = start,
                    end = end,
                    lines = inputs,
                )
            }.onSuccess {
                // Keep saving = true until the celebration overlay finishes (its
                // onSaved navigates away) so a double-tap cannot save twice.
                showCelebration = true
            }.onFailure {
                saving = false
                error = it.message ?: "Could not save this plan. Try again."
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = if (existing == null) "New plan" else "Edit plan",
            subtitle = DateText.rangeLabel(com.mizani.money.core.time.DateRange(start, end)),
            onBack = onBack,
            actions = {
                TextButton(onClick = ::save, enabled = !saving) { Text(if (saving) "Saving…" else "Save") }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (budgetId != null && existing == null) {
                EmptyState(
                    title = "Plan not found",
                    body = "It may have been deleted. Go back and choose another plan.",
                    iconRes = R.drawable.ic_ph_chart_donut,
                    actionLabel = "Go back",
                    onAction = onBack,
                )
                return@Column
            }

            if (error != null) {
                StatusBanner(
                    text = error!!,
                    isError = true,
                    onDismiss = { error = null },
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Plan name") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Period", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                BudgetPeriodType.entries.forEach { option ->
                    FilterChip(
                        selected = periodType == option,
                        onClick = {
                            periodType = option
                            if (option != BudgetPeriodType.CUSTOM) {
                                val range = Periods.budgetRange(option, today)
                                start = range.start
                                end = range.end
                            }
                        },
                        label = { Text(option.label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DateField(
                    label = "Start",
                    date = start,
                    modifier = Modifier.weight(1f),
                    onDateSelected = {
                        start = it
                        periodType = BudgetPeriodType.CUSTOM
                    },
                )
                DateField(
                    label = "End",
                    date = end,
                    modifier = Modifier.weight(1f),
                    onDateSelected = {
                        end = it
                        periodType = BudgetPeriodType.CUSTOM
                    },
                )
            }
            Text(
                "Reports include only transactions whose local date falls inside these dates. Weekly periods start on Monday.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MizaniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Planned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                        Text(MoneyFormat.format(totalPlanned, currency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Funded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                        Text(MoneyFormat.format(totalFunded, currency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Funding records what you have actually set aside. Planned is what the period needs. " +
                        "The difference is visible as unfunded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Categories", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (previousBudget != null && previousLines.isNotEmpty()) {
                    TextButton(onClick = {
                        planned = previousLines.associate { line ->
                            line.categoryId to MoneyFormat.toInput(line.plannedMinor, currency)
                        }
                        essential = essential + previousLines.associate { it.categoryId to it.essential }
                        rollover = rollover + previousLines.associate { it.categoryId to it.rolloverRule }
                        priority = priority + previousLines.associate { it.categoryId to it.priority }
                    }) { Text("Copy last plan") }
                }
            }
            TextButton(onClick = { showAllCategories = !showAllCategories }) {
                Text(if (showAllCategories) "Hide unused categories" else "Show all categories")
            }
            Text(
                "You can fund categories now or later. Funding is separate from spending.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            displayedCategoryIds.forEach { categoryId ->
                val category = snapshot.category(categoryId) ?: return@forEach
                val archived = category.archived
                val plannedValue = planned[categoryId].orEmpty()
                MizaniCard(
                    containerColor = if (archived) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(category.icon, category.colorHex, size = 36)
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (essential[categoryId] == true) {
                                    Spacer(Modifier.width(Spacing.sm))
                                    SmallPill("Essential")
                                }
                                if (archived) {
                                    Spacer(Modifier.width(Spacing.sm))
                                    SmallPill("Archived", container = MaterialTheme.colorScheme.surfaceContainerHighest)
                                }
                            }
                            Text(
                                "Funded ${MoneyFormat.format(fundedMinor(categoryId), currency)}" +
                                    if ((rollover[categoryId] ?: RolloverRule.NONE) != RolloverRule.NONE) " · rollover on" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { optionsTarget = categoryId }) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Category options")
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    MizaniAmountField(
                        value = plannedValue,
                        onValueChange = { input -> planned = planned + (categoryId to input) },
                        label = "Planned amount",
                        currency = currency,
                    )
                }
            }

            Button(
                onClick = ::save,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(if (saving) "Saving…" else "Save plan")
            }
            Text(
                "Rollover is off by default. Repairs and refunds reduce the original category's eligible " +
                    "spending; transfers never count.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showCelebration) {
        MizaniCelebration(onFinished = { showCelebration = false; onSaved() })
    }
    }
}

@Composable
private fun LineOptionsDialog(
    categoryName: String,
    fundedText: String,
    plannedMinor: Long,
    essential: Boolean,
    rollover: RolloverRule,
    priority: Int,
    currency: com.mizani.money.core.money.Currency,
    onDismiss: () -> Unit,
    onApply: (String, Boolean, RolloverRule, Int) -> Unit,
) {
    var funded by rememberSaveable { mutableStateOf(fundedText) }
    var isEssential by rememberSaveable { mutableStateOf(essential) }
    var rolloverValue by rememberSaveable { mutableStateOf(rollover) }
    var priorityValue by rememberSaveable { mutableStateOf(priority) }
    var error by remember { mutableStateOf<String?>(null) }
    val fundedMinor = MoneyFormat.parse(funded, currency) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("$categoryName options") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    "Planned ${MoneyFormat.format(plannedMinor, currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MizaniAmountField(
                    value = funded,
                    onValueChange = { input -> funded = input },
                    label = "Funded amount",
                    currency = currency,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Essential", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Essential amounts are excluded from the discretionary safe-to-spend pool.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = isEssential, onCheckedChange = { isEssential = it })
                }
                Text("Rollover", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = rolloverValue.label,
                    options = RolloverRule.entries.map { PickerOption(it.name, it.label) },
                    onSelect = { rolloverValue = RolloverRule.valueOf(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf(1 to "High", 2 to "Medium", 3 to "Low").forEach { (value, label) ->
                        FilterChip(
                            selected = priorityValue == value,
                            onClick = { priorityValue = value },
                            label = { Text(label) },
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (funded.isNotBlank() && MoneyFormat.parse(funded, currency) == null) {
                    error = "Enter a valid funded amount"
                } else {
                    onApply(funded, isEssential, rolloverValue, priorityValue)
                }
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
