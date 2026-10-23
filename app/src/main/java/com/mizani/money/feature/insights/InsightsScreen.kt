package com.mizani.money.feature.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mizani.money.R
import com.mizani.money.core.model.InsightKind
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.domain.analytics.AnalyticsEngine
import com.mizani.money.domain.analytics.DashboardMetrics
import com.mizani.money.domain.analytics.IncomeVariability
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateRangePickerDialog
import com.mizani.money.ui.components.InsightCard
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniCashFlowBars
import com.mizani.money.ui.components.MizaniDonutChart
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.MonthBar
import com.mizani.money.ui.components.PeriodMenu
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.parseHexColor
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald
import com.mizani.money.ui.theme.EmeraldBright
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onOpenPlan: () -> Unit,
) {
    val metrics by vm.metrics.collectAsStateWithLifecycle()
    val insights by vm.insights.collectAsStateWithLifecycle()
    val selectedPreset by vm.selectedPreset.collectAsStateWithLifecycle()
    val customRange by vm.customRange.collectAsStateWithLifecycle()
    val periodRange by vm.periodRange.collectAsStateWithLifecycle()
    val snapshot = state.snapshot
    val currency = state.currency
    val today = vm.today
    var showRangePicker by remember { mutableStateOf(false) }

    // Secondary metrics stay behind a "More metrics" toggle. rememberSaveable keeps the
    // choice across scrolling, tab switches and process death within the screen session.
    var showMoreMetrics by rememberSaveable { mutableStateOf(false) }

    if (showRangePicker) {
        DateRangePickerDialog(
            initial = customRange ?: DateRange(Periods.monthOf(today).start, today),
            onDismiss = { showRangePicker = false },
            onConfirm = {
                vm.setCustomRange(it)
                showRangePicker = false
            },
        )
    }

    val engine = remember(snapshot) { AnalyticsEngine(snapshot) }
    val currentMonth = remember(today) { Periods.monthOf(today) }
    val previousMonth = remember(today) { Periods.monthOf(today.minusMonths(1)) }
    val feeNow = remember(snapshot, today) { engine.feesMinor(currentMonth) }
    val feeBefore = remember(snapshot, today) { engine.feesMinor(previousMonth) }
    val monthly = remember(snapshot, today) {
        (5L downTo 0L).map { back ->
            val month = YearMonth.from(today).minusMonths(back)
            val range = DateRange(month.atDay(1), month.atEndOfMonth())
            MonthFlow(month, engine.incomeMinor(range), engine.expenseMinor(range))
        }
    }
    val incomeSources = remember(snapshot, today) {
        snapshot.transactions
            .filter {
                it.type == TransactionType.INCOME && it.deletedAt == null &&
                    it.localDateEpochDay in currentMonth.start.toEpochDay()..currentMonth.end.toEpochDay()
            }
            .groupBy { it.incomeSourceId ?: it.categoryId ?: "other" }
            .map { (key, list) ->
                val label = snapshot.incomeSources.firstOrNull { it.id == key }?.name
                    ?: snapshot.categories.firstOrNull { it.id == key }?.name
                    ?: "Other income"
                label to list.sumOf { it.amountMinor }
            }
            .sortedByDescending { it.second }
    }
    val feeTransactions = remember(snapshot, today) {
        snapshot.transactions
            .filter {
                it.feeMinor > 0 &&
                    it.localDateEpochDay in currentMonth.start.toEpochDay()..currentMonth.end.toEpochDay()
            }
            .sortedByDescending { it.feeMinor }
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "Insights", subtitle = stringResource(R.string.insights_subtitle))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            PeriodMenu(
                selected = selectedPreset,
                customRange = customRange,
                range = periodRange,
                onSelect = { preset -> vm.selectPreset(preset) },
                onCustom = { showRangePicker = true },
            )

            val current = metrics
            if (current == null) {
                Text(
                    stringResource(R.string.insights_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    // Loading is a status change: announce it instead of going visually blank.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                return@Column
            }

            CashFlowHero(
                metrics = current,
                periodLabel = DateText.rangeLabel(current.period),
                previousLabel = DateText.rangeLabel(current.previousPeriod),
                currency = currency,
            )

            if (snapshot.transactions.isEmpty()) {
                Text(
                    "Insights appear after you record real transactions. Mizani never estimates your spending to fill a chart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (insights.isNotEmpty()) {
                Text("What changed", style = MaterialTheme.typography.titleMedium)
                insights.take(3).forEach { insight ->
                    InsightCard(
                        title = insight.title,
                        body = insight.body,
                        explanation = insight.explanation,
                        tone = insight.tone,
                        actionLabel = insight.actionLabel,
                        onAction = {
                            when (insight.kind) {
                                InsightKind.BUDGET_PACE, InsightKind.UNFUNDED_PRIORITY -> onOpenPlan()
                                else -> Unit
                            }
                        },
                    )
                }
            }

            val monthlyBars = remember(monthly) {
                monthly.map { flow ->
                    MonthBar(
                        label = flow.month.month.name.take(3),
                        incomeMinor = flow.incomeMinor,
                        expenseMinor = flow.expenseMinor,
                    )
                }
            }
            val monthsWithData = monthly.count { it.incomeMinor > 0 || it.expenseMinor > 0 }
            MetricCard(
                title = "Six-month cash flow",
                subtitle = "Income versus expenses",
                info = "Each bar is the total of posted transactions whose local date falls in that month. " +
                    "Refunds reduce expenses; transfers are excluded. Months with no records show zero, not an estimate.",
            ) {
                if (monthsWithData < 2) {
                    Text(
                        "Cash-flow trends appear after your second month with records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // Bars, month labels, legend and the screen-reader summary all come from
                    // the shared MPAndroidChart-backed component.
                    MizaniCashFlowBars(months = monthlyBars, currency = currency)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Highest recorded income month: ${monthly.maxByOrNull { it.incomeMinor }?.let { "${it.month.month.name} ${it.month.year} (${MoneyFormat.format(it.incomeMinor, currency)})" } ?: "none yet"}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        // Wraps normally below the legend; the card's 16dp content padding keeps it unclipped.
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            MetricCard(
                title = "Where it went",
                subtitle = "Category share, refund-adjusted",
                info = "Each category's net expense = expenses in that category + attributed fees − refunds linked to that category. " +
                    "Share = category net expense ÷ total eligible spending. Transfers are excluded.",
            ) {
                // Refund-heavy periods can leave negative category totals; only positive
                // net spending is charted so shares stay comparable and never negative.
                val positiveSlices = current.categorySlices.filter { it.amountMinor > 0 }
                if (positiveSlices.isEmpty()) {
                    Text(
                        "No net spending recorded in this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val positiveTotal = positiveSlices.sumOf { it.amountMinor }
                    MizaniDonutChart(
                        slices = positiveSlices,
                        totalMinor = positiveTotal,
                        currency = currency,
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    positiveSlices.forEach { slice ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(parseHexColor(slice.colorHex)),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(slice.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${(slice.share * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = Spacing.sm),
                            )
                            Text(
                                MoneyFormat.format(slice.amountMinor, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            val budgetNow = current.budget
            if (budgetNow != null) {
                MetricCard(
                    title = "Budget pace",
                    subtitle = budgetNow.budget.name,
                    info = "Planned spend to date = available category budget × elapsed share of the budget period. " +
                        "Pace variance = actual spending − planned spend to date. A positive variance means spending is ahead of plan.",
                ) {
                    val lines = budgetNow.lines.filter { it.line.plannedMinor > 0 }.sortedByDescending { it.paceVarianceMinor }.take(5)
                    if (lines.isEmpty()) {
                        Text("No planned category amounts yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    lines.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                line.category?.name ?: "Category",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (line.paceVarianceMinor > 0) {
                                    "${MoneyFormat.format(line.paceVarianceMinor, currency)} ahead"
                                } else {
                                    "${MoneyFormat.format(abs(line.paceVarianceMinor), currency)} below"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (line.paceVarianceMinor > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        ProgressBar(
                            fraction = if (line.availableMinor <= 0) 0f else (line.spentMinor.toDouble() / line.availableMinor).toFloat(),
                            color = parseHexColor(line.category?.colorHex ?: "#3D6E8C"),
                            height = 6,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }
            }

            if (showMoreMetrics) {
                MetricCard(
                    title = "Wallets",
                    subtitle = "${current.walletBalances.size} active wallets",
                    info = "Wallet balance = opening balance + income − expenses and fees + transfers in − transfers out " +
                        "+ refunds + adjustments. Debt wallets are excluded from the liquid total; no exchange rates are invented.",
                ) {
                    Row {
                        MetricValue("Total tracked", MoneyFormat.format(current.walletTotalMinor, currency), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        MetricValue("Liquid", MoneyFormat.format(current.walletLiquidMinor, currency), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    val positiveTotal = current.walletByType.sumOf { it.totalMinor }.coerceAtLeast(1)
                    current.walletByType.forEach { total ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(total.type.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(
                                MoneyFormat.format(total.totalMinor, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ProgressBar(
                            fraction = total.totalMinor.toDouble().coerceAtLeast(0.0).toFloat() / positiveTotal.toFloat(),
                            height = 5,
                            color = if (total.type == WalletType.DEBT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                    }
                }

                MetricCard(
                    title = "Income history",
                    subtitle = current.incomeVariability?.let { "${it.monthsWithIncome} months with income" } ?: "Not enough history",
                    info = "Median, lowest and highest are calculated from the last six calendar months with recorded income. " +
                        "Income variability is descriptive; Mizani does not assign a stability score.",
                ) {
                    val variability = current.incomeVariability
                    if (variability == null) {
                        Text(
                            "Record income to see your typical range. No score is invented from too little data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        VariabilityContent(variability, currency)
                        if (incomeSources.isNotEmpty()) {
                            Spacer(Modifier.height(Spacing.md))
                            Text("This month by source", style = MaterialTheme.typography.labelLarge)
                            val total = incomeSources.sumOf { it.second }.coerceAtLeast(1)
                            incomeSources.take(5).forEach { (label, amount) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${(amount.toDouble() / total * 100).roundToInt()}% · ${MoneyFormat.format(amount, currency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                MetricCard(
                    title = "Fees",
                    subtitle = "Fees on transactions and transfers",
                    info = "Fees = the fee amounts recorded on transactions in the month. A fee is counted as spending; " +
                        "the transfer or expense amount it belongs to is not double-counted. The denominator here is recorded fees, " +
                        "not a fee percentage.",
                ) {
                    Row {
                        MetricValue("This month", MoneyFormat.format(feeNow, currency), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        MetricValue(
                            "Last month",
                            MoneyFormat.format(feeBefore, currency),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        if (feeBefore > 0) {
                            if (feeNow > feeBefore) "Up ${MoneyFormat.format(feeNow - feeBefore, currency)} from last month."
                            else if (feeNow < feeBefore) "Down ${MoneyFormat.format(feeBefore - feeNow, currency)} from last month."
                            else "The same as last month."
                        } else if (feeNow > 0) {
                            "No fees were recorded last month, so no percentage change is shown."
                        } else {
                            "No fees recorded this month. Add the fee separately when a provider charges you."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (feeTransactions.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.md))
                        Text("Largest fees this month", style = MaterialTheme.typography.labelLarge)
                        feeTransactions.take(4).forEach { transaction ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    transaction.payee.ifBlank { snapshot.categoryName(transaction.categoryId) },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    MoneyFormat.format(transaction.feeMinor, currency),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                MetricCard(
                    title = "Forecast",
                    subtitle = "A simple, explainable estimate",
                    info = "Projected period spend = spending to date + average daily spending from recorded spending " +
                        "in the last 14 days × days remaining. The estimate appears once that window covers at least " +
                        "7 days of recorded spending, uses recorded spending only and is not a promise or an AI prediction.",
                ) {
                    val forecast = current.forecastMinor
                    if (forecast == null) {
                        Text(
                            "Not enough recent spending to forecast this period. The estimate appears after spending is recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            MoneyFormat.format(forecast, currency),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            "At your recent daily spending pace, you may finish around this amount. Basis: last 14 days of recorded spending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "Safe to spend: ${current.safeToSpendDailyMinor?.let { "${MoneyFormat.format(it, currency)} per day" } ?: "not available"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Safe-to-spend uses the funded flexible budget remaining minus bills due before the period ends, divided by days left.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                MetricCard(
                    title = "Savings",
                    subtitle = "Contributions and rate",
                    info = "Savings rate = goal contributions in this period ÷ income received × 100. " +
                        "It is unavailable when income received is zero. Transfers between owned wallets are not expenses.",
                ) {
                    Row {
                        MetricValue("Set aside", MoneyFormat.format(current.savingsContributionsMinor, currency), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        MetricValue(
                            "Savings rate",
                            current.savingsRatePercent?.let { "$it%" } ?: "Not available",
                            MaterialTheme.colorScheme.onSurface,
                            Modifier.weight(1f),
                        )
                    }
                    if (current.goals.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.md))
                        current.goals.take(3).forEach { goal ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.goal.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${(goal.progress).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            ProgressBar(fraction = (goal.progress / 100.0).toFloat(), height = 5)
                        }
                    }
                }

                MetricCard(
                    title = "Essential coverage",
                    subtitle = "Received income versus essential plans",
                    info = "Essential coverage = income actually received in this period ÷ essential planned amounts in the budget × 100. " +
                        "Expected income is not included. Below 100% is a planning signal, not a prediction.",
                ) {
                    val coverage = current.essentialCoveragePercent
                    if (coverage == null) {
                        Text(
                            "Add a budget with essential categories to see how far received income covers them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("$coverage%", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        ProgressBar(fraction = (coverage / 100f).coerceIn(0f, 1f), height = 8)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "Income received so far covers $coverage% of the essential expenses planned for this period.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (current.incomeVariability == null && current.transactionCount == 0) {
                    Text(
                        "Mizani does not manufacture conclusions from missing data. Record a few transactions and the metrics " +
                            "will become available without any cloud service.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TextButton(
                onClick = { showMoreMetrics = !showMoreMetrics },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showMoreMetrics) "Show fewer metrics" else "More metrics")
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

private data class MonthFlow(val month: YearMonth, val incomeMinor: Long, val expenseMinor: Long)

@Composable
private fun MetricCard(
    title: String,
    subtitle: String,
    info: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    MizaniCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = { expanded = !expanded },
                // 48dp touch target per the design system (was 44dp).
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.Info,
                    contentDescription = stringResource(
                        if (expanded) R.string.how_calculated_hide else R.string.how_calculated_show,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        content()
        AnimatedVisibility(visible = expanded) {
            Text(
                info,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }
}

@Composable
private fun MetricValue(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Period cash-flow hero on the brand gradient. It replaces the old white "Cash flow" metric
 * card: same values and the same percent-change maths, but the comparison is only claimed as
 * a percentage when both periods cover an equal number of days, and the footer states exactly
 * which rows have recorded prior-period data.
 */
@Composable
private fun CashFlowHero(
    metrics: DashboardMetrics,
    periodLabel: String,
    previousLabel: String,
    currency: Currency,
) {
    var showInfo by remember { mutableStateOf(false) }
    val hasIncomePrevious = metrics.previousIncomeMinor > 0
    val hasExpensePrevious = metrics.previousExpenseMinor > 0
    // Percent change is only an apples-to-apples claim when both periods cover the same
    // number of days (e.g. a month-to-date range compared with the same elapsed days before).
    val showPercentChange = metrics.previousPeriod.days == metrics.period.days
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(DeepTeal, Emerald, EmeraldBright)))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "CASH FLOW",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showInfo = !showInfo },
                // 48dp touch target per the design system (was 44dp).
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    if (showInfo) Icons.Rounded.ExpandLess else Icons.Rounded.Info,
                    contentDescription = stringResource(
                        if (showInfo) R.string.how_calculated_hide else R.string.how_calculated_show,
                    ),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            periodLabel,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.65f),
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val netMinor = metrics.netMinor
            // Exactly one sign, always before the symbol: a negative net used to render
            // as "$-12,000", and a zero net as "+$0".
            val netSign = when {
                netMinor > 0 -> "+"
                netMinor < 0 -> "−"
                else -> ""
            }
            if (netSign.isNotEmpty()) {
                Text(
                    netSign,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
            Text(
                currency.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(
                MoneyFormat.formatBare(if (netMinor == Long.MIN_VALUE) Long.MAX_VALUE else abs(netMinor), currency),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroStat(
                label = "Income",
                value = MoneyFormat.format(metrics.incomeMinor, currency),
                modifier = Modifier.weight(1f).padding(end = Spacing.lg),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            HeroStat(
                label = "Net spending",
                value = MoneyFormat.format(metrics.expenseMinor, currency),
                modifier = Modifier.weight(1f).padding(start = Spacing.lg),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        if (hasIncomePrevious || hasExpensePrevious) {
            Text(
                "Previous period $previousLabel (${metrics.previousPeriod.days} days)",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
            ComparisonLine(
                label = "Income",
                current = metrics.incomeMinor,
                previous = metrics.previousIncomeMinor,
                currency = currency,
                color = Color.White.copy(alpha = 0.65f),
                showPercent = showPercentChange,
            )
            ComparisonLine(
                label = "Net spending",
                current = metrics.expenseMinor,
                previous = metrics.previousExpenseMinor,
                currency = currency,
                color = Color.White.copy(alpha = 0.65f),
                showPercent = showPercentChange,
            )
            Text(
                when {
                    hasIncomePrevious && hasExpensePrevious ->
                        "Income and net spending are both compared with recorded prior-period data."
                    hasIncomePrevious ->
                        "Income has a recorded prior value; net spending has no recorded prior value."
                    else ->
                        "Net spending has a recorded prior value; income has no recorded prior value."
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp),
            )
        } else {
            Text(
                "No prior period with recorded activity.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        AnimatedVisibility(visible = showInfo) {
            Text(
                "Income = posted income transactions in this period. Net spending = expenses plus fees, minus refunds. " +
                    "Transfers between your own wallets are excluded. Net = income − net spending.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f),
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 2,
        )
    }
}

@Composable
private fun ComparisonLine(
    label: String,
    current: Long,
    previous: Long,
    currency: Currency,
    color: Color,
    showPercent: Boolean = true,
) {
    val change = if (previous > 0) ((current - previous).toDouble() / previous * 100.0) else null
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.weight(1f))
        Text(
            when {
                change == null -> "not recorded last period"
                // Unequal period lengths get absolute prior values, never a percent claim.
                !showPercent ->
                    "prior ${MoneyFormat.format(previous, currency)} · ${MoneyFormat.format(current, currency)}"
                else ->
                    (if (change >= 0) "+" else "") + "${change.roundToInt()}% · ${MoneyFormat.format(current, currency)}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun VariabilityContent(variability: IncomeVariability, currency: Currency) {
    Row {
        MetricValue("Median", MoneyFormat.format(variability.medianMinor, currency), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        MetricValue("Lowest", MoneyFormat.format(variability.lowestMinor, currency), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        MetricValue("Highest", MoneyFormat.format(variability.highestMinor, currency), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        variability.daysSinceLastIncome?.let { "Last recorded income: $it days ago." } ?: "No income recorded yet.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
