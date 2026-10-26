package com.mizani.money.feature.home

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mizani.money.R
import com.mizani.money.core.model.InsightKind
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.domain.analytics.DashboardMetrics
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateRangePickerDialog
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.InsightCard
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniDonutChart
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.PeriodMenu
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.TransactionRow
import com.mizani.money.ui.components.parseHexColor
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: AppViewModel,
    state: AppUiState,
    onOpenTransactions: () -> Unit,
    onOpenInsights: () -> Unit,
    onAdd: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(top = Spacing.md, bottom = Spacing.bottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MizaniMark(size = 38)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    DateText.greeting(LocalTime.now().hour),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${DateText.weekday(today)} · ${snapshot.workspace?.name ?: "My money"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        PeriodMenu(
            selected = selectedPreset,
            customRange = customRange,
            range = periodRange,
            onSelect = { preset -> vm.selectPreset(preset) },
            onCustom = { showRangePicker = true },
        )

        if (snapshot.transactions.isEmpty()) {
            GetStartedHero(onAddIncome = onAddIncome, onAddExpense = onAddExpense)
            return@Column
        }

        val current = metrics
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Announced so the wait reads as loading, not an empty screen.
                val loadingLabel = stringResource(R.string.home_loading)
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp).semantics { contentDescription = loadingLabel },
                    strokeWidth = 2.dp,
                )
            }
            return@Column
        }

        SummaryCard(metrics = current, currency = currency)

        QuickAddActions(onAddIncome = onAddIncome, onAddExpense = onAddExpense)

        SectionBlock(title = "Recent activity", actionLabel = "See all", onAction = onOpenTransactions) {
            snapshot.transactions.take(4).forEach { transaction ->
                TransactionRow(transaction = transaction, snapshot = snapshot, currency = currency)
            }
        }

        // Refund-heavy periods can leave negative category totals. A negative slice distorts
        // the donut and its shares, so only positive net spending is charted and shared out.
        val positiveSlices = current.categorySlices.filter { it.amountMinor > 0 }
        SectionBlock(title = "Where your money went", actionLabel = "Insights", onAction = onOpenInsights) {
            if (positiveSlices.isEmpty()) {
                EmptyState(
                    title = "No spending yet",
                    body = "Add an expense to see spending by category.",
                    iconRes = R.drawable.ic_ph_chart_donut,
                    compact = true,
                )
            } else {
                val positiveTotal = positiveSlices.sumOf { it.amountMinor }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MizaniDonutChart(
                        slices = positiveSlices,
                        totalMinor = positiveTotal,
                        currency = currency,
                        modifier = Modifier.weight(0.9f).aspectRatio(1f),
                    )
                    Spacer(Modifier.width(Spacing.lg))
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        val top = positiveSlices.take(4)
                        val rest = positiveSlices.drop(4)
                        top.forEach { slice -> SliceLegend(slice, currency) }
                        if (rest.isNotEmpty()) {
                            val restTotal = rest.sumOf { it.amountMinor }
                            val restShare = restTotal.toDouble() / positiveTotal
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(9.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outline),
                                )
                                Spacer(Modifier.width(Spacing.sm))
                                Text(
                                    "Other",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${(restShare * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(Spacing.sm))
                                Text(
                                    MoneyFormat.format(restTotal, currency),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

    val insight = insights.firstOrNull()
        if (insight != null) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader(title = "Insight")
                InsightCard(
                    title = insight.title,
                    body = insight.body,
                    tone = insight.tone,
                    actionLabel = insight.actionLabel,
                    onAction = {
                        when (insight.kind) {
                            InsightKind.DATA_COMPLETENESS -> onAdd()
                            else -> onOpenInsights()
                        }
                    },
                )
            }
        }

    }
}

/**
 * A titled block whose section header lives outside the card, so the Home screen reads
 * as a hierarchy of sections rather than a stack of self-contained cards.
 */
@Composable
private fun SectionBlock(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SectionHeader(title = title, actionLabel = actionLabel, onAction = onAction)
        MizaniCard(content = content)
    }
}

/**
 * The empty-ledger state: exactly one primary action, with one short reassurance card after it.
 */
@Composable
private fun GetStartedHero(onAddIncome: () -> Unit, onAddExpense: () -> Unit) {
    // Gradient capped at Emerald: opaque white reads at >=4.79:1, comfortably clearing
    // the 4.5:1 threshold for normal text on the lightest stop.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(DeepTeal, Emerald)))
            .padding(20.dp),
    ) {
        Column {
            // Compact illustration cluster: wallet + coins + chart donut, solid white on the gradient.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Spacing.md),
            ) {
                listOf(
                    R.drawable.ic_ph_wallet to 36,
                    R.drawable.ic_ph_coins to 30,
                    R.drawable.ic_ph_chart_donut to 36,
                ).forEachIndexed { index, (res, size) ->
                    if (index > 0) Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .size((size + 16).dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(res),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size.dp),
                        )
                    }
                }
            }
            Text("Get started", style = MaterialTheme.typography.labelMedium, color = Color.White)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.home_get_started_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(R.string.home_get_started_body),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
            Spacer(Modifier.height(Spacing.lg))
            QuickAddActions(
                onAddIncome = onAddIncome,
                onAddExpense = onAddExpense,
                onDarkBackground = true,
            )
        }
    }
}

@Composable
private fun QuickAddActions(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onDarkBackground: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val colors = if (onDarkBackground) {
            ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepTeal)
        } else {
            ButtonDefaults.buttonColors()
        }
        Button(
            onClick = onAddExpense,
            modifier = Modifier.weight(1f).height(Spacing.buttonHeight),
            shape = MaterialTheme.shapes.small,
            colors = colors,
        ) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add expense", maxLines = 1)
        }
        OutlinedButton(
            onClick = onAddIncome,
            modifier = Modifier.weight(1f).height(Spacing.buttonHeight),
            shape = MaterialTheme.shapes.small,
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = if (onDarkBackground) Color.White else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add income", maxLines = 1)
        }
    }
}

@Composable
private fun SummaryCard(metrics: DashboardMetrics, currency: Currency) {
    MizaniCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryColumn(
                label = "Income",
                value = MoneyFormat.format(metrics.incomeMinor, currency),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            SummaryDivider()
            SummaryColumn(
                label = "Net expenses",
                value = MoneyFormat.format(metrics.expenseMinor, currency),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            SummaryDivider()
            SummaryColumn(
                label = "Net",
                value = MoneyFormat.format(metrics.netMinor, currency),
                valueColor = if (metrics.netMinor >= 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowScope.SummaryColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .height(34.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun SliceLegend(
    slice: com.mizani.money.domain.analytics.CategorySlice,
    currency: Currency,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(9.dp).clip(CircleShape).background(parseHexColor(slice.colorHex)),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            slice.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${(slice.share * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
