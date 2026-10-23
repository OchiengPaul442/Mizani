package com.mizani.money.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateRangePickerDialog
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.TransactionRow
import java.time.LocalDate

private enum class DateFilter(val label: String) {
    ALL("All time"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    CUSTOM("Custom"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val today = vm.today
    var search by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var dateFilter by rememberSaveable { mutableStateOf(DateFilter.ALL) }
    var customRange by remember { mutableStateOf<DateRange?>(null) }
    var showRangePicker by remember { mutableStateOf(false) }
    var selectedTypes by remember { mutableStateOf(setOf<TransactionType>()) }
    var walletId by remember { mutableStateOf<String?>(null) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }

    val range: DateRange? = when (dateFilter) {
        DateFilter.ALL -> null
        DateFilter.THIS_MONTH -> Periods.monthOf(today)
        DateFilter.LAST_MONTH -> Periods.monthOf(today.minusMonths(1))
        DateFilter.CUSTOM -> customRange
    }

    val minMinor = MoneyFormat.parse(minAmount, currency)
    val maxMinor = MoneyFormat.parse(maxAmount, currency)

    val filtered = remember(
        snapshot.transactions,
        search,
        range?.start,
        range?.end,
        selectedTypes,
        walletId,
        categoryId,
        minMinor,
        maxMinor,
    ) {
        val query = search.trim().lowercase()
        snapshot.transactions.filter { transaction ->
            if (range != null && !range.contains(LocalDate.ofEpochDay(transaction.localDateEpochDay))) return@filter false
            if (selectedTypes.isNotEmpty() && transaction.type !in selectedTypes) return@filter false
            if (walletId != null && transaction.walletId != walletId && transaction.toWalletId != walletId) return@filter false
            if (categoryId != null && transaction.categoryId != categoryId && transaction.feeCategoryId != categoryId) return@filter false
            if (minMinor != null && transaction.amountMinor < minMinor) return@filter false
            if (maxMinor != null && transaction.amountMinor > maxMinor) return@filter false
            if (query.isNotEmpty()) {
                val haystack = buildString {
                    append(transaction.payee).append(' ')
                    append(transaction.note).append(' ')
                    append(snapshot.categoryName(transaction.categoryId)).append(' ')
                    append(snapshot.walletName(transaction.walletId)).append(' ')
                    append(transaction.type.label).append(' ')
                    append(transaction.amountMinor)
                }.lowercase()
                if (!haystack.contains(query)) return@filter false
            }
            true
        }
    }

    val grouped = filtered.groupBy { LocalDate.ofEpochDay(it.localDateEpochDay) }
        .toSortedMap(compareByDescending { it })
    val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor + it.feeMinor } +
        filtered.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeMinor } -
        filtered.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor } +
        filtered.filter { it.type == TransactionType.REFUND }.sumOf { it.feeMinor }

    if (showRangePicker) {
        DateRangePickerDialog(
            initial = customRange ?: DateRange(Periods.monthOf(today).start, today),
            onDismiss = { showRangePicker = false },
            onConfirm = {
                customRange = it
                dateFilter = DateFilter.CUSTOM
                showRangePicker = false
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = "Activity",
            subtitle = "${filtered.size} transactions",
            actions = {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
                }
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(Icons.Rounded.FilterList, contentDescription = "Filters")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                bottom = Spacing.bottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search payee, note, wallet, category") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    DateFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = dateFilter == filter,
                            onClick = {
                                if (filter == DateFilter.CUSTOM) {
                                    showRangePicker = true
                                } else {
                                    dateFilter = filter
                                }
                            },
                            label = {
                                Text(
                                    if (filter == DateFilter.CUSTOM && customRange != null) {
                                        DateText.rangeLabel(customRange!!)
                                    } else {
                                        filter.label
                                    },
                                )
                            },
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(visible = showFilters) {
                    FilterPanel(
                        snapshot = snapshot,
                        currency = currency,
                        selectedTypes = selectedTypes,
                        onTypesChange = { selectedTypes = it },
                        walletId = walletId,
                        onWalletChange = { walletId = it },
                        categoryId = categoryId,
                        onCategoryChange = { categoryId = it },
                        minAmount = minAmount,
                        maxAmount = maxAmount,
                        onMinChange = { minAmount = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                        onMaxChange = { maxAmount = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                        onClear = {
                            selectedTypes = emptySet()
                            walletId = null
                            categoryId = null
                            minAmount = ""
                            maxAmount = ""
                        },
                    )
                }
            }
            item {
                SummaryRow(
                    count = filtered.size,
                    incomeMinor = income,
                    expenseMinor = expense,
                    currency = currency,
                )
            }
            if (grouped.isEmpty()) {
                item {
                    EmptyState(
                        title = "No matching transactions",
                        body = if (snapshot.transactions.isEmpty()) {
                            "Record your first income or expense. Transfers, refunds and adjustments are supported too."
                        } else {
                            "Try a different search, date range or filter."
                        },
                        actionLabel = if (snapshot.transactions.isEmpty()) "Add transaction" else null,
                        onAction = if (snapshot.transactions.isEmpty()) onAdd else null,
                    )
                }
            } else {
                grouped.forEach { (date, transactions) ->
                    item(key = "header-$date") {
                        DayHeader(date = date, transactions = transactions, snapshot = snapshot, currency = currency)
                    }
                    items(transactions, key = { it.id }) { transaction ->
                        MizaniCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.sm),
                        ) {
                            TransactionRow(
                                transaction = transaction,
                                snapshot = snapshot,
                                currency = currency,
                                onClick = { onEdit(transaction.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    transactions: List<TransactionEntity>,
    snapshot: com.mizani.money.data.repo.LedgerSnapshot,
    currency: Currency,
) {
    val dayExpense = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amountMinor + it.feeMinor } +
        transactions.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeMinor } -
        transactions.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor } +
        transactions.filter { it.type == TransactionType.REFUND }.sumOf { it.feeMinor }
    // One sign only: a net-negative day (refunds exceed spending) is money coming back,
    // so it reads as "+amount"; MoneyFormat.format would otherwise render a second minus.
    val dayExpenseLabel = when {
        dayExpense > 0 -> "−${MoneyFormat.format(dayExpense, currency)}"
        dayExpense < 0 -> "+${MoneyFormat.format(dayExpense, currency).removePrefix("-")}"
        else -> MoneyFormat.format(dayExpense, currency)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            DateText.relative(date, Periods.today()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            dayExpenseLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryRow(
    count: Int,
    incomeMinor: Long,
    expenseMinor: Long,
    currency: Currency,
) {
    MizaniCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("INCOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormat.format(incomeMinor, currency), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("NET EXPENSES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormat.format(expenseMinor, currency), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                "$count shown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterPanel(
    snapshot: com.mizani.money.data.repo.LedgerSnapshot,
    currency: Currency,
    selectedTypes: Set<TransactionType>,
    onTypesChange: (Set<TransactionType>) -> Unit,
    walletId: String?,
    onWalletChange: (String?) -> Unit,
    categoryId: String?,
    onCategoryChange: (String?) -> Unit,
    minAmount: String,
    maxAmount: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    MizaniCard {
        Text("Transaction type", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TransactionType.entries.forEach { type ->
                FilterChip(
                    selected = type in selectedTypes,
                    onClick = {
                        onTypesChange(
                            if (type in selectedTypes) selectedTypes - type else selectedTypes + type,
                        )
                    },
                    label = { Text(type.label) },
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MizaniPicker(
                selectedText = walletId?.let { snapshot.walletName(it) } ?: "Any wallet",
                options = snapshot.wallets.map { PickerOption(it.id, it.name) },
                onSelect = { onWalletChange(it) },
                noneLabel = "Any wallet",
                onSelectNone = { onWalletChange(null) },
                modifier = Modifier.weight(1f),
            )
            MizaniPicker(
                selectedText = categoryId?.let { snapshot.categoryName(it) } ?: "Any category",
                options = snapshot.categories.map { PickerOption(it.id, it.name) },
                onSelect = { onCategoryChange(it) },
                noneLabel = "Any category",
                onSelectNone = { onCategoryChange(null) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MizaniAmountField(
                value = minAmount,
                onValueChange = onMinChange,
                label = "Min amount",
                currency = currency,
                modifier = Modifier.weight(1f),
            )
            MizaniAmountField(
                value = maxAmount,
                onValueChange = onMaxChange,
                label = "Max amount",
                currency = currency,
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) {
            Text("Clear filters")
        }
    }
}
