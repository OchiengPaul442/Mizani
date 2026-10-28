package com.mizani.money.feature.insights

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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.mizani.money.ui.components.EmptyState
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
import com.mizani.money.ui.theme.LocalMizaniThemeTokens
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onExportData: () -> Unit,
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
                .padding(bottom = Spacing.bottomFab),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            PeriodMenu(
                selected = selectedPreset,
                customRange = customRange,
                range = periodRange,
                onSelect = { preset -> vm.selectPreset(preset) },
                onCustom = { showRangePicker = true },
            )

            if (snapshot.transactions.isEmpty()) {
                EmptyState(
                    title = "No transactions yet",
                    body = "Add income or expenses to see your cash flow and analysis.",
                    icon = Icons.Rounded.BarChart,
                    illustrationAsset = "financial_data_cuate.svg",
                )
                return@Column
            }

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

            OutlinedButton(
                onClick = onExportData,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Export data")
            }

            val visibleInsights = insights
            if (visibleInsights.isNotEmpty()) {
                Text("What changed", style = MaterialTheme.typography.titleMedium)
                visibleInsights.take(3).forEach { insight ->
                    InsightCard(
                        title = insight.title,
                        body = insight.body,
                        tone = insight.tone,
                        actionLabel = null,
                        onAction = {},
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
            ) {
                if (monthsWithData < 2) {
                    Text(
                        "Add another month of records to see cash-flow trends.",
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
                subtitle = "Spending by category",
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

            MetricCard(
                title = "Income trends",
                subtitle = current.incomeVariability?.let { "${it.monthsWithIncome} months with income" }
                    ?: "Record income to build a monthly trend",
            ) {
                val variability = current.incomeVariability
                if (variability == null) {
                    Text(
                        if (current.incomeMinor > 0) {
                            "Keep recording income across months to compare your income history."
                        } else {
                            "Record income to see your history."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    VariabilityContent(variability, currency)
                }
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
            if (showMoreMetrics) {
                MetricCard(
                    title = "Wallets",
                    subtitle = "${current.walletBalances.size} active wallets",
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
                    title = "Fees",
                    subtitle = "Fees on transactions and transfers",
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
                    subtitle = "Estimate for this period",
                ) {
                    val forecast = current.forecastMinor
                    if (forecast == null) {
                        Text(
                            "Record more expenses to see your estimate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            MoneyFormat.format(forecast, currency),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            "Your spending may finish around this amount.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (current.incomeVariability == null && current.transactionCount == 0) {
                    Text(
                        "Record more transactions to see your insights.",
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
    content: @Composable ColumnScope.() -> Unit,
) {
    MizaniCard {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.md))
        content()
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
    val themeTokens = LocalMizaniThemeTokens.current
    val hasIncomePrevious = metrics.previousIncomeMinor > 0
    val hasExpensePrevious = metrics.previousExpenseMinor > 0
    // Percent change is only an apples-to-apples claim when both periods cover the same
    // number of days (e.g. a month-to-date range compared with the same elapsed days before).
    val showPercentChange = metrics.previousPeriod.days == metrics.period.days
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(themeTokens.heroStart, themeTokens.heroEnd)))
            .padding(20.dp),
    ) {
        Text(
            "Cash flow",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
        Text(
            periodLabel,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
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
                color = Color.White,
            )
            ComparisonLine(
                label = "Income",
                current = metrics.incomeMinor,
                previous = metrics.previousIncomeMinor,
                currency = currency,
                color = Color.White,
                showPercent = showPercentChange,
            )
            ComparisonLine(
                label = "Net spending",
                current = metrics.expenseMinor,
                previous = metrics.previousExpenseMinor,
                currency = currency,
                color = Color.White,
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
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp),
            )
        } else {
            Text(
                "No prior period with recorded activity.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp),
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
            color = Color.White,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
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
