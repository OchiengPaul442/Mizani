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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mizani.money.feature.home.components.UpcomingList
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateRangePickerDialog
import com.mizani.money.ui.components.InsightCard
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniDonutChart
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.PeriodMenu
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.parseHexColor
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald
import com.mizani.money.ui.theme.EmeraldBright
import com.mizani.money.ui.theme.Mint
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: AppViewModel,
    state: AppUiState,
    onOpenTransactions: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenInsights: () -> Unit,
    onAdd: () -> Unit,
    onOpenBudget: (String?) -> Unit,
    onOpenSettings: () -> Unit = {},
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
    var showSafeExplanation by remember { mutableStateOf(false) }

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
            Spacer(Modifier.width(Spacing.sm))
            // 48dp: meets the design-system minimum touch target (was 40dp).
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            ) {
                IconButton(onClick = onAdd, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.home_add_transaction),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
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
            // One clear first step: a single hero with one action, nothing competing with it.
            GetStartedHero(onAdd = onAdd)
            MizaniCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Text(
                    "You can add a transaction and create a budget from Plan whenever you choose.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

        BudgetHero(current = current, currency = currency, onOpenBudget = onOpenBudget)

        if (current.budget != null) {
            SafeToSpendCard(
                metrics = current,
                currency = currency,
                expanded = showSafeExplanation,
                onToggleExplanation = { showSafeExplanation = !showSafeExplanation },
                onOpenBudget = { onOpenBudget(current.budget?.budget?.id) },
            )
        }

        SummaryCard(metrics = current, currency = currency)

        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().height(Spacing.buttonHeight),
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.home_add_transaction))
        }

        if (current.upcoming.isNotEmpty()) {
            SectionBlock(title = "Upcoming", actionLabel = "See all", onAction = onOpenPlan) {
                UpcomingList(
                    upcoming = current.upcoming.take(3),
                    currency = currency,
                    today = today,
                )
            }
        } else {
            SectionBlock(title = "Upcoming", actionLabel = "Add a bill", onAction = onOpenPlan) {
                Text(
                    stringResource(R.string.home_upcoming_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Refund-heavy periods can leave negative category totals. A negative slice distorts
        // the donut and its shares, so only positive net spending is charted and shared out.
        val positiveSlices = current.categorySlices.filter { it.amountMinor > 0 }
        SectionBlock(title = "Where your money went", actionLabel = "Insights", onAction = onOpenInsights) {
            if (positiveSlices.isEmpty()) {
                Text(
                    "No net spending recorded in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    explanation = insight.explanation,
                    tone = insight.tone,
                    actionLabel = insight.actionLabel,
                    onAction = {
                        when (insight.kind) {
                            InsightKind.DATA_COMPLETENESS -> onAdd()
                            InsightKind.BUDGET_PACE, InsightKind.UNFUNDED_PRIORITY -> onOpenPlan()
                            else -> onOpenInsights()
                        }
                    },
                )
            }
        }

        Text(
            "Based on the information you entered. Safe-to-spend is guidance, not a guarantee.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
private fun GetStartedHero(onAdd: () -> Unit) {
    val white70 = Color.White.copy(alpha = 0.70f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(DeepTeal, Emerald, EmeraldBright)))
            .padding(20.dp),
    ) {
        Column {
            Text("GET STARTED", style = MaterialTheme.typography.labelSmall, color = white70)
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
                color = white70,
            )
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DeepTeal,
                ),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.home_add_transaction))
            }
        }
    }
}

@Composable
private fun BudgetHero(
    current: DashboardMetrics,
    currency: Currency,
    onOpenBudget: (String?) -> Unit,
) {
    val budget = current.budget
    if (budget == null) {
        EmptyBudgetHero(onCreateBudget = { onOpenBudget(null) })
        return
    }

    val remaining = budget.remainingMinor
    val planned = budget.plannedMinor
    val unfunded = (planned - budget.fundedMinor).coerceAtLeast(0)
    // Always white on the emerald gradient: gold text fails WCAG contrast for the
    // most-read figure, so gold is reserved for non-text accents elsewhere.
    val remainingColor = Color.White
    val white70 = Color.White.copy(alpha = 0.70f)
    val white65 = Color.White.copy(alpha = 0.65f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(DeepTeal, Emerald, EmeraldBright)))
            .padding(20.dp),
    ) {
        Column {
            Text(
                budget.budget.name.ifBlank { "This period" }.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = white70,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.lg))
            Text("Remaining", style = MaterialTheme.typography.labelMedium, color = white65)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    currency.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = white70,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    MoneyFormat.formatBare(remaining, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = remainingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics {
                        contentDescription = "Budget remaining ${MoneyFormat.format(remaining, currency)}"
                    },
                )
            }
            Text(
                "of ${MoneyFormat.format(planned, currency)} planned",
                style = MaterialTheme.typography.bodySmall,
                color = white65,
            )
            Spacer(Modifier.height(Spacing.lg))
            ProgressBar(
                fraction = if (budget.availableMinor <= 0) {
                    0f
                } else {
                    (budget.spentMinor.toDouble() / budget.availableMinor).toFloat()
                },
                color = Mint,
                trackColor = Color.White.copy(alpha = 0.20f),
                height = 6,
            )
            Spacer(Modifier.height(Spacing.sm))
            Row {
                Text(
                    "Spent ${MoneyFormat.format(budget.spentMinor, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = white70,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${budget.usedPercent.roundToInt()}% used",
                    style = MaterialTheme.typography.bodySmall,
                    color = white70,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Funded ${MoneyFormat.format(budget.fundedMinor, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = white70,
                    )
                    Text(
                        "Unfunded ${MoneyFormat.format(unfunded, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = white70,
                    )
                }
                TextButton(
                    onClick = { onOpenBudget(budget.budget.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Mint),
                ) {
                    Text(stringResource(R.string.home_open_budget))
                }
            }
        }
    }
}

@Composable
private fun EmptyBudgetHero(onCreateBudget: () -> Unit) {
    val white70 = Color.White.copy(alpha = 0.70f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(DeepTeal, Emerald, EmeraldBright)))
            .padding(20.dp),
    ) {
        Column {
            Text("PLAN THIS MONTH", style = MaterialTheme.typography.labelSmall, color = white70)
            Spacer(Modifier.height(Spacing.sm))
            Text("No budget yet", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Set planned amounts for the categories that matter and mark what you have funded.",
                style = MaterialTheme.typography.bodySmall,
                color = white70,
            )
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onCreateBudget,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DeepTeal,
                ),
            ) {
                Text("Create a budget")
            }
        }
    }
}

@Composable
private fun SafeToSpendCard(
    metrics: DashboardMetrics,
    currency: Currency,
    expanded: Boolean,
    onToggleExplanation: () -> Unit,
    onOpenBudget: () -> Unit,
) {
    val daily = metrics.safeToSpendDailyMinor
    MizaniCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.TipsAndUpdates,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SAFE TO SPEND / DAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onToggleExplanation,
                        // 48dp touch target; the description states the current state.
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = stringResource(
                                if (expanded) R.string.home_safe_info_hide else R.string.home_safe_info_show,
                            ),
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (daily != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            MoneyFormat.format(daily, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            " / day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "through ${DateText.short(metrics.period.end)} · from your funded flexible budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Not available",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Fund your flexible budget to see a daily amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onOpenBudget) { Text(stringResource(R.string.home_open_budget)) }
                }
                if (expanded) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "Safe-to-spend = funded flexible budget remaining minus bills due before the period ends, " +
                            "divided by the days left. It uses only the information you entered; it is guidance, not a guarantee.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
                label = "Net spending",
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
