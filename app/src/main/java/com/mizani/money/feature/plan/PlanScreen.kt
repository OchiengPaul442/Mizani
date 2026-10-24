package com.mizani.money.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.repo.BudgetLineInput
import com.mizani.money.domain.analytics.AnalyticsEngine
import com.mizani.money.domain.analytics.BudgetLineProgress
import com.mizani.money.domain.analytics.BudgetProgress
import com.mizani.money.domain.analytics.GoalProgress
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.CategoryAvatar
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniCelebration
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.SmallPill
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.MizaniStatCard
import com.mizani.money.ui.components.StatusBanner
import com.mizani.money.ui.components.TransactionRow
import com.mizani.money.ui.components.parseHexColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private enum class PlanTab(val label: String) {
    BUDGET("This month"),
    INCOME("Income"),
    BILLS("Bills"),
    GOALS("Goals"),
}

@Composable
fun PlanScreen(
    vm: AppViewModel,
    state: AppUiState,
    onOpenBudget: (String?) -> Unit,
    onOpenGoal: (String?) -> Unit,
    onOpenDebts: () -> Unit = {},
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val today = vm.today
    val scope = rememberCoroutineScope()

    val engine = remember(snapshot) { AnalyticsEngine(snapshot) }
    val month = remember(today) { Periods.monthOf(today) }
    val budget = remember(snapshot, today) { engine.budgetFor(month) }
    val progress = remember(snapshot, today) { engine.budgetProgress(month, budget, today) }
    val goals = remember(snapshot, today) { engine.goalsProgress(month) }
    val income = remember(snapshot, today) { engine.incomeMinor(month) }
    val expected = remember(snapshot, today) { engine.expectedIncome(month) }
    val bills = remember(snapshot) { snapshot.recurring.filter { !it.archived && it.type == RecurringType.BILL } }
    val incomeRules = remember(snapshot) { snapshot.recurring.filter { !it.archived && it.type == RecurringType.INCOME } }
    val debts = remember(snapshot) { snapshot.debts.filter { !it.archived } }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var editingRule by remember { mutableStateOf<RecurringRuleEntity?>(null) }
    var recurringType by remember { mutableStateOf(RecurringType.BILL) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var fundingLine by remember { mutableStateOf<BudgetLineProgress?>(null) }
    var contributingGoal by remember { mutableStateOf<GoalProgress?>(null) }
    var showCelebration by remember { mutableStateOf(false) }

    val recurringDialogInitial = editingRule
    if (showRecurringDialog) {
        RecurringDialog(
            initial = recurringDialogInitial,
            type = recurringType,
            snapshot = snapshot,
            currency = currency,
            today = today,
            onDismiss = { showRecurringDialog = false; editingRule = null },
            onSave = { id, draft ->
                scope.launch {
                    runCatching { vm.finance.saveRecurring(id, draft) }
                        .onSuccess {
                            showRecurringDialog = false
                            editingRule = null
                            error = null
                        }
                        .onFailure {
                            error = it.message ?: if (recurringType == RecurringType.INCOME) {
                                "Could not save this income schedule. Try again."
                            } else {
                                "Could not save this bill. Try again."
                            }
                        }
                }
            },
            onArchive = { id ->
                scope.launch {
                    runCatching { vm.finance.setRecurringArchived(id, true) }
                        .onSuccess { showRecurringDialog = false; editingRule = null }
                        .onFailure { error = it.message ?: "Could not archive this commitment. Try again." }
                }
            },
        )
    }

    if (showSourceDialog) {
        IncomeSourceDialog(
            onDismiss = { showSourceDialog = false },
            onSave = { name, kind ->
                scope.launch {
                    runCatching { vm.finance.saveIncomeSource(null, name, kind) }
                        .onSuccess { showSourceDialog = false }
                        .onFailure { error = it.message ?: "Could not save this income source. Try again." }
                }
            },
        )
    }

    val funding = fundingLine
    if (funding != null && budget != null) {
        FundLineDialog(
            title = funding.category?.name ?: "this category",
            plannedMinor = funding.line.plannedMinor,
            fundedMinor = funding.line.fundedMinor,
            currency = currency,
            onDismiss = { fundingLine = null },
            onApply = { newFunded ->
                val inputs = progress?.lines?.map { line ->
                    BudgetLineInput(
                        categoryId = line.line.categoryId,
                        plannedMinor = line.line.plannedMinor,
                        fundedMinor = if (line.line.categoryId == funding.line.categoryId) newFunded else line.line.fundedMinor,
                        essential = line.line.essential,
                        rolloverRule = line.line.rolloverRule,
                        priority = line.line.priority,
                        note = line.line.note,
                    )
                } ?: emptyList()
                scope.launch {
                    runCatching {
                        vm.finance.saveBudget(
                            budgetId = budget.id,
                            name = budget.name,
                            periodType = budget.periodType,
                            start = LocalDate.ofEpochDay(budget.startEpochDay),
                            end = LocalDate.ofEpochDay(budget.endEpochDay),
                            lines = inputs,
                        )
                    }.onSuccess {
                            fundingLine = null
                            showCelebration = true
                        }
                        .onFailure { error = it.message ?: "Could not update the funded amount. Try again." }
                }
            },
        )
    }

    val goal = contributingGoal
    if (goal != null) {
        ContributeGoalDialog(
            goalName = goal.goal.name,
            targetMinor = goal.goal.targetMinor,
            allocatedMinor = goal.goal.allocatedMinor,
            wallets = snapshot.activeWallets,
            currency = currency,
            onDismiss = { contributingGoal = null },
            onContribute = { amount, walletId ->
                scope.launch {
                    runCatching { vm.finance.contributeToGoal(goal.goal.id, amount, walletId, today) }
                        .onSuccess { contributingGoal = null }
                        .onFailure { error = it.message ?: "Could not add this contribution. Try again." }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "Plan", subtitle = "Plan income, spending and savings")
        // PrimaryTabRow (Material3 stable) draws the brand emerald pill indicator on the screen background.
        PrimaryTabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            PlanTab.entries.forEachIndexed { index, planTab ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(planTab.label) },
                )
            }
        }
        if (error != null) {
            StatusBanner(
                text = error!!,
                isError = true,
                onDismiss = { error = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
        if (message != null) {
            StatusBanner(
                text = message!!,
                onDismiss = { message = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
        Box(Modifier.weight(1f)) {
            when (PlanTab.entries[tab]) {
                PlanTab.BUDGET -> BudgetTab(
                    budget = budget,
                    progress = progress,
                    currency = currency,
                    engine = engine,
                    month = month,
                    onCreate = { onOpenBudget(null) },
                    onEdit = { onOpenBudget(budget?.id) },
                    onFund = { fundingLine = it },
                )
                PlanTab.INCOME -> IncomeTab(
                    currency = currency,
                    incomeMinor = income,
                    expectedMinor = expected,
                    sources = snapshot.incomeSources,
                    rules = incomeRules,
                    transactions = snapshot.transactions.filter {
                        it.type == TransactionType.INCOME &&
                            it.localDateEpochDay in month.start.toEpochDay()..month.end.toEpochDay()
                    },
                    snapshot = snapshot,
                    onAddSource = { showSourceDialog = true },
                    onAddRule = {
                        editingRule = null
                        recurringType = RecurringType.INCOME
                        showRecurringDialog = true
                    },
                    onEditRule = { rule ->
                        editingRule = rule
                        recurringType = RecurringType.INCOME
                        showRecurringDialog = true
                    },
                    onMarkReceived = { rule ->
                        scope.launch {
                            runCatching { vm.finance.markRecurringPaid(rule.id, today) }
                                .onSuccess {
                                    error = null
                                    message = "Receipt recorded. Your balance and reports are updated."
                                }
                                .onFailure { error = it.message ?: "Could not record this receipt. Try again." }
                        }
                    },
                )
                PlanTab.BILLS -> BillsTab(
                    currency = currency,
                    bills = bills,
                    debts = debts,
                    today = today,
                    snapshot = snapshot,
                    onAdd = {
                        editingRule = null
                        recurringType = RecurringType.BILL
                        showRecurringDialog = true
                    },
                    onEdit = { rule ->
                        editingRule = rule
                        recurringType = RecurringType.BILL
                        showRecurringDialog = true
                    },
                    onMarkPaid = { rule ->
                        scope.launch {
                            runCatching { vm.finance.markRecurringPaid(rule.id, today) }
                                .onSuccess {
                                    error = null
                                    message = "Payment recorded. Your balance and reports are updated."
                                }
                                .onFailure { error = it.message ?: "Could not record this payment. Try again." }
                        }
                    },
                    onOpenDebts = onOpenDebts,
                )
                PlanTab.GOALS -> GoalsTab(
                    currency = currency,
                    goals = goals,
                    onAdd = { onOpenGoal(null) },
                    onOpen = { onOpenGoal(it.goal.id) },
                    onContribute = { contributingGoal = it },
                )
            }
        }
    }
    if (showCelebration) {
        MizaniCelebration(onFinished = { showCelebration = false })
    }
    }
}

@Composable
private fun BudgetTab(
    budget: BudgetEntity?,
    progress: BudgetProgress?,
    currency: Currency,
    engine: AnalyticsEngine,
    month: com.mizani.money.core.time.DateRange,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    onFund: (BudgetLineProgress) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        if (budget == null || progress == null) {
            EmptyState(
                title = "No plan for this month",
                body = "Plan what this money must cover. Mizani keeps scheduled income separate from recorded receipts, " +
                    "and separates funded amounts from planned amounts.",
                iconRes = R.drawable.ic_ph_chart_donut,
                actionLabel = "Create a plan",
                onAction = onCreate,
            )
            return@Column
        }

        MizaniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "${DateText.rangeLabel(DateRange(budget.startEpochDay, budget.endEpochDay))} · ${budget.periodType.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
                TextButton(onClick = onEdit) { Text("Edit") }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                MoneyFormat.format(progress.remainingMinor, currency),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "remaining of ${MoneyFormat.format(progress.availableMinor, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(Spacing.sm))
            ProgressBar(
                fraction = if (progress.availableMinor <= 0) 0f
                else (progress.spentMinor.toDouble() / progress.availableMinor).toFloat(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
            )
            Spacer(Modifier.height(Spacing.sm))
            Row {
                Text(
                    "Spent ${MoneyFormat.format(progress.spentMinor, currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Funded ${MoneyFormat.format(progress.fundedMinor, currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MizaniStatCard(
                label = "Planned",
                value = MoneyFormat.format(progress.plannedMinor, currency),
                modifier = Modifier.weight(1f),
            )
            MizaniStatCard(
                label = "Funded",
                value = MoneyFormat.format(progress.fundedMinor, currency),
                modifier = Modifier.weight(1f),
            )
            MizaniStatCard(
                label = "Unfunded",
                value = MoneyFormat.format((progress.plannedMinor - progress.fundedMinor).coerceAtLeast(0), currency),
                modifier = Modifier.weight(1f),
            )
        }

        if (progress.unbudgetedSpendMinor > 0) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    "${MoneyFormat.format(progress.unbudgetedSpendMinor, currency)} of spending this period is outside the categories in this plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }

        SectionHeader(title = "Categories")
        if (progress.lines.isEmpty()) {
            Text(
                "This plan has no categories yet. Edit it to add planned amounts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        progress.lines
            .sortedWith(compareByDescending<BudgetLineProgress> { it.line.essential }.thenByDescending { it.line.plannedMinor })
            .forEach { line ->
                BudgetLineCard(line = line, currency = currency, onFund = { onFund(line) })
            }
        Text(
            "Available = planned + rollover. Remaining = available − eligible spending. " +
                "Only spending in this plan's categories counts toward its lines.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.bottom))
    }
}

@Composable
private fun BudgetLineCard(
    line: BudgetLineProgress,
    currency: Currency,
    onFund: () -> Unit,
) {
    val color = parseHexColor(line.category?.colorHex ?: "#3D6E8C")
    MizaniCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(
                icon = line.category?.icon ?: "category",
                colorHex = line.category?.colorHex ?: "#3D6E8C",
                size = 36,
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        line.category?.name ?: "Category",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (line.line.essential) {
                        Spacer(Modifier.width(Spacing.sm))
                        SmallPill("Essential")
                    }
                }
                Text(
                    "Planned ${MoneyFormat.format(line.line.plannedMinor, currency)} · Funded ${MoneyFormat.format(line.line.fundedMinor, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    MoneyFormat.format(line.remainingMinor, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (line.remainingMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text("left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        ProgressBar(
            fraction = if (line.availableMinor <= 0) 0f else (line.spentMinor.toDouble() / line.availableMinor).toFloat(),
            color = if (line.remainingMinor < 0) MaterialTheme.colorScheme.error else color,
            height = 7,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Spent ${MoneyFormat.format(line.spentMinor, currency)} of ${MoneyFormat.format(line.availableMinor, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (line.line.fundedMinor < line.line.plannedMinor) {
                TextButton(onClick = onFund) { Text("Fund") }
            }
        }
    }
}

@Composable
private fun IncomeTab(
    currency: Currency,
    incomeMinor: Long,
    expectedMinor: Long,
    sources: List<com.mizani.money.data.local.IncomeSourceEntity>,
    rules: List<RecurringRuleEntity>,
    transactions: List<com.mizani.money.data.local.TransactionEntity>,
    snapshot: com.mizani.money.data.repo.LedgerSnapshot,
    onAddSource: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (RecurringRuleEntity) -> Unit,
    onMarkReceived: (RecurringRuleEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        MizaniCard {
            Text("This month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                Column(Modifier.weight(1f)) {
                Text("Still scheduled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyFormat.format(expectedMinor, currency), style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Received", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyFormat.format(incomeMinor, currency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Scheduled amounts are estimates. Record each receipt when it arrives to update your balance and reports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader(title = "Income sources", actionLabel = "Add", onAction = onAddSource)
        if (sources.isEmpty()) {
            Text(
                "Add salary, business, freelance, remittance or casual-work sources to see how concentrated your income is.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            sources.forEach { source ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Text(source.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(source.kind.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionHeader(title = "Recurring income", actionLabel = "Add", onAction = onAddRule)
        if (rules.isEmpty()) {
            Text(
                "Add salary or other income you receive on a schedule. It appears in your forecast until you record a receipt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rules.forEach { rule ->
                RecurringRow(
                    rule = rule,
                    currency = currency,
                    walletName = snapshot.walletName(rule.walletId),
                    actionLabel = "Record receipt",
                    onAction = { onMarkReceived(rule) },
                    onClick = { onEditRule(rule) },
                )
            }
        }

        SectionHeader(title = "Received this month")
        if (transactions.isEmpty()) {
            Text(
                "No income recorded for this month yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            transactions.take(8).forEach { transaction ->
                TransactionRow(transaction = transaction, snapshot = snapshot, currency = currency)
            }
        }
        Spacer(Modifier.height(Spacing.bottom))
    }
}

@Composable
private fun BillsTab(
    currency: Currency,
    bills: List<RecurringRuleEntity>,
    debts: List<com.mizani.money.data.local.DebtEntity>,
    today: LocalDate,
    snapshot: com.mizani.money.data.repo.LedgerSnapshot,
    onAdd: () -> Unit,
    onEdit: (RecurringRuleEntity) -> Unit,
    onMarkPaid: (RecurringRuleEntity) -> Unit,
    onOpenDebts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SectionHeader(title = "Recurring bills & commitments", actionLabel = "Add", onAction = onAdd)
        if (bills.isEmpty()) {
            Text(
                "Add recurring bills and loan payments. Upcoming amounts are included in your plan; record each payment when it leaves a wallet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            bills.sortedBy { it.nextDueEpochDay }.forEach { bill ->
                RecurringRow(
                    rule = bill,
                    currency = currency,
                    walletName = snapshot.walletName(bill.walletId),
                    actionLabel = "Record payment",
                    onAction = { onMarkPaid(bill) },
                    onClick = { onEdit(bill) },
                )
            }
        }

        SectionHeader(title = "Debts", actionLabel = "Manage", onAction = onOpenDebts)
        if (debts.isEmpty()) {
            Text(
                "Track balances and upcoming payments. Mizani does not lend, score credit or recommend new borrowing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            debts.forEach { debt ->
                MizaniCard(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
                    onClick = onOpenDebts,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(debt.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                debt.lender.ifBlank { "Debt" } + (debt.nextDueEpochDay?.let {
                                    " · next ${DateText.short(LocalDate.ofEpochDay(it))}"
                                } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(MoneyFormat.format(debt.currentBalanceMinor, currency), style = MaterialTheme.typography.titleSmall)
                            Text("balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Text(
            "Marking a bill paid records an actual expense in that bill's category. Skipping a date is " +
                "never punished — you can mark it paid whenever you get to it.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.bottom))
    }
}

@Composable
private fun GoalsTab(
    currency: Currency,
    goals: List<GoalProgress>,
    onAdd: () -> Unit,
    onOpen: (GoalProgress) -> Unit,
    onContribute: (GoalProgress) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Savings goals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add goal")
            }
        }
        if (goals.isEmpty()) {
            EmptyState(
                title = "No goals yet",
                body = "Set aside money for an emergency fund, school fees, rent, stock or a new phone. " +
                    "Transfers into savings are not counted as spending.",
                iconRes = R.drawable.ic_ph_check_circle,
                actionLabel = "Create a goal",
                onAction = onAdd,
            )
        } else {
            goals.forEach { goal ->
                GoalCard(
                    goal = goal,
                    currency = currency,
                    onOpen = { onOpen(goal) },
                    onContribute = { onContribute(goal) },
                )
            }
        }
        Text(
            "Savings contributions are tracked separately from ordinary spending. A transfer between your own wallets " +
                "does not reduce your income or count as an expense.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.bottom))
    }
}

@Composable
private fun GoalCard(
    goal: GoalProgress,
    currency: Currency,
    onOpen: () -> Unit,
    onContribute: () -> Unit,
) {
    MizaniCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    com.mizani.money.ui.components.MizaniIcons.goal(goal.goal.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(goal.goal.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${(goal.progress).roundToInt()}% · ${MoneyFormat.format(goal.goal.allocatedMinor, currency)} of ${MoneyFormat.format(goal.goal.targetMinor, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
        Spacer(Modifier.height(Spacing.sm))
        ProgressBar(fraction = (goal.progress / 100.0).toFloat(), height = 7)
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                goal.goal.targetEpochDay?.let { "Target ${DateText.medium(LocalDate.ofEpochDay(it))}" } ?: "No target date",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onContribute) { Text("Add money") }
        }
    }
}

@Composable
private fun RecurringRow(
    rule: RecurringRuleEntity,
    currency: Currency,
    walletName: String,
    actionLabel: String,
    onAction: () -> Unit,
    onClick: () -> Unit,
) {
    MizaniCard(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (rule.essential) {
                        Spacer(Modifier.width(Spacing.sm))
                        SmallPill("Essential")
                    }
                }
                Text(
                    "${rule.frequency.label} · ${DateText.relative(LocalDate.ofEpochDay(rule.nextDueEpochDay))} · $walletName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(MoneyFormat.format(rule.amountMinor, currency), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun DateRange(startEpochDay: Long, endEpochDay: Long): com.mizani.money.core.time.DateRange =
    com.mizani.money.core.time.DateRange(
        LocalDate.ofEpochDay(startEpochDay),
        LocalDate.ofEpochDay(endEpochDay),
    )
