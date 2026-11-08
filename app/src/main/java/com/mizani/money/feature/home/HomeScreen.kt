package com.mizani.money.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mizani.money.R
import com.mizani.money.core.model.InsightKind
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.Periods
import com.mizani.money.domain.analytics.DashboardMetrics
import com.mizani.money.domain.engagement.DailyMomentum
import com.mizani.money.domain.engagement.DailyMomentumCalculator
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.feature.lock.ProtectedBalanceValue
import com.mizani.money.feature.lock.WalletBalanceVisibilityControl
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.AdaptiveLayout
import com.mizani.money.ui.components.DateRangePickerDialog
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.InsightCard
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniDonutChart
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.PeriodMenu
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StorysetIllustration
import com.mizani.money.ui.components.TransactionRow
import com.mizani.money.ui.components.WalletAvatar
import com.mizani.money.ui.components.parseHexColor
import com.mizani.money.ui.theme.LocalMizaniThemeTokens
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: AppViewModel,
    state: AppUiState,
    onOpenTransactions: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onOpenInsights: () -> Unit,
    onOpenWallets: () -> Unit,
    onSetupPin: () -> Unit,
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
    val walletBalances = remember(snapshot) { snapshot.walletBalances() }
    val today by vm.today.collectAsStateWithLifecycle()
    val momentum = remember(snapshot.transactions, today) {
        DailyMomentumCalculator.calculate(snapshot.transactions, today)
    }
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

        val activeWallets = snapshot.wallets.filter { !it.archived && it.deletedAt == null }

        if (snapshot.transactions.isEmpty()) {
            GetStartedHero(onAddIncome = onAddIncome, onAddExpense = onAddExpense)
            if (activeWallets.isNotEmpty()) {
                WalletShortcutsSection(
                    vm = vm,
                    state = state,
                    wallets = activeWallets,
                    balances = walletBalances,
                    currency = currency,
                    onOpenWallets = onOpenWallets,
                    onSetupPin = onSetupPin,
                )
            }
            DailyMomentumCard(momentum = momentum, onAdd = onAdd)
            return@Column
        }

        if (activeWallets.isNotEmpty()) {
            WalletShortcutsSection(
                vm = vm,
                state = state,
                wallets = activeWallets,
                balances = walletBalances,
                currency = currency,
                onOpenWallets = onOpenWallets,
                onSetupPin = onSetupPin,
            )
        }
        DailyMomentumCard(momentum = momentum, onAdd = onAdd)

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
                TransactionRow(
                    transaction = transaction,
                    snapshot = snapshot,
                    currency = currency,
                    onClick = { onEditTransaction(transaction.id) },
                )
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

@Composable
private fun WalletShortcutsSection(
    vm: AppViewModel,
    state: AppUiState,
    wallets: List<WalletEntity>,
    balances: Map<String, Long>,
    currency: Currency,
    onOpenWallets: () -> Unit,
    onSetupPin: () -> Unit,
) {
    val visibleWallets = wallets.take(3)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactHeader = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.15f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Your wallets",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                WalletBalanceVisibilityControl(vm, state, onSetupPin, showLabel = !compactHeader)
                TextButton(onClick = onOpenWallets) {
                    Text("See all", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        MizaniCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenWallets,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
        ) {
            visibleWallets.forEachIndexed { index, wallet ->
                WalletShortcutRow(
                    wallet = wallet,
                    balance = balances[wallet.id] ?: 0L,
                    currency = currency,
                    balanceVisible = state.walletBalancesVisible,
                )
                if (index < visibleWallets.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletShortcutRow(
    wallet: WalletEntity,
    balance: Long,
    currency: Currency,
    balanceVisible: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletShortcutIdentity(wallet, Modifier.weight(1f))
        Spacer(Modifier.width(Spacing.md))
        WalletShortcutBalance(
            wallet = wallet,
            balance = balance,
            currency = currency,
            visible = balanceVisible,
            horizontalAlignment = Alignment.End,
        )
    }
}

@Composable
private fun WalletShortcutIdentity(wallet: WalletEntity, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        WalletAvatar(wallet.type, wallet.colorHex, size = 38)
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                wallet.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!wallet.name.trim().equals(wallet.type.label, ignoreCase = true)) {
                Text(
                    wallet.type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WalletShortcutBalance(
    wallet: WalletEntity,
    balance: Long,
    currency: Currency,
    visible: Boolean,
    horizontalAlignment: Alignment.Horizontal,
) {
    val label = if (wallet.type == WalletType.DEBT) "Amount owed" else "Balance"
    Column(
        modifier = Modifier.widthIn(max = 140.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProtectedBalanceValue(
            amountMinor = balance,
            currency = currency,
            visible = visible,
            style = MaterialTheme.typography.titleMedium,
            color = if (visible && balance < 0L) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            amountLabel = label,
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

/** A compact empty-ledger introduction with direct income and expense actions. */
@Composable
private fun GetStartedHero(onAddIncome: () -> Unit, onAddExpense: () -> Unit) {
    val themeTokens = LocalMizaniThemeTokens.current
    val illustrationSize = if (LocalDensity.current.fontScale > AdaptiveLayout.LARGE_FONT_SCALE) 104.dp else 144.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(themeTokens.heroStart, themeTokens.heroEnd)))
            .padding(20.dp),
    ) {
        Column {
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .size(illustrationSize).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                StorysetIllustration(
                    assetName = "finance_cuate.svg",
                    modifier = Modifier.fillMaxSize().padding(Spacing.xs),
                )
            }
            Spacer(Modifier.height(Spacing.md))
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
    val themeTokens = LocalMizaniThemeTokens.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackActions = maxWidth < AdaptiveLayout.STACKED_CONTENT_WIDTH_DP.dp ||
            LocalDensity.current.fontScale > AdaptiveLayout.LARGE_FONT_SCALE
        val colors = if (onDarkBackground) {
            ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = themeTokens.heroStart)
        } else {
            ButtonDefaults.buttonColors()
        }
        if (stackActions) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(
                    onClick = onAddExpense,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = colors,
                ) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add expense")
                }
                OutlinedButton(
                    onClick = onAddIncome,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = if (onDarkBackground) Color.White else MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add income")
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = colors,
                ) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add expense", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAddIncome,
                    modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
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
    }
}

@Composable
private fun SummaryCard(metrics: DashboardMetrics, currency: Currency) {
    val themeTokens = LocalMizaniThemeTokens.current
    MizaniCard {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackSummary = maxWidth < AdaptiveLayout.STACKED_CONTENT_WIDTH_DP.dp ||
                LocalDensity.current.fontScale > AdaptiveLayout.LARGE_FONT_SCALE
            val netColor = if (metrics.netMinor >= 0) themeTokens.income else MaterialTheme.colorScheme.error
            if (stackSummary) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SummaryColumn("Income", MoneyFormat.format(metrics.incomeMinor, currency), themeTokens.income, Modifier.fillMaxWidth())
                    SummaryColumn("Net expenses", MoneyFormat.format(metrics.expenseMinor, currency), MaterialTheme.colorScheme.onSurface, Modifier.fillMaxWidth())
                    SummaryColumn("Net", MoneyFormat.format(metrics.netMinor, currency), netColor, Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SummaryColumn("Income", MoneyFormat.format(metrics.incomeMinor, currency), themeTokens.income, Modifier.weight(1f))
                    SummaryDivider()
                    SummaryColumn("Net expenses", MoneyFormat.format(metrics.expenseMinor, currency), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    SummaryDivider()
                    SummaryColumn("Net", MoneyFormat.format(metrics.netMinor, currency), netColor, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DailyMomentumCard(
    momentum: DailyMomentum,
    onAdd: () -> Unit,
) {
    val next = momentum.nextTier
    val progress = if (next == null) {
        1f
    } else {
        val range = (next.minimumTrackedDays - momentum.tier.minimumTrackedDays).coerceAtLeast(1)
        (momentum.trackedDays - momentum.tier.minimumTrackedDays).toFloat() / range
    }
    MizaniCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(Spacing.lg),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackStats = maxWidth < AdaptiveLayout.COMPACT_NAVIGATION_WIDTH_DP.dp

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_momentum_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.home_momentum_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    ) {
                        Text(
                            stringResource(R.string.home_momentum_points, momentum.points),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    if (stackStats) {
                        Column(
                            modifier = Modifier.padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            MomentumStat(momentum.trackedDays, R.string.home_momentum_days_tracked)
                            MomentumStat(momentum.currentStreak, R.string.home_momentum_day_streak)
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MomentumStat(
                                value = momentum.trackedDays,
                                labelRes = R.string.home_momentum_days_tracked,
                                modifier = Modifier.weight(1f),
                            )
                            SummaryDivider()
                            MomentumStat(
                                value = momentum.currentStreak,
                                labelRes = R.string.home_momentum_day_streak,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (momentum.todayTracked) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                stringResource(R.string.home_momentum_today_done),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                } else if (stackStats) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            stringResource(R.string.home_momentum_today_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = onAdd,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(stringResource(R.string.home_momentum_record_action))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.home_momentum_today_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = onAdd,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(stringResource(R.string.home_momentum_record_action))
                        }
                    }
                }

                ProgressBar(progress, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        stringResource(R.string.home_momentum_milestone, momentum.tier.title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (next != null) {
                        val daysToNext = momentum.trackedDaysToNextTier ?: 0
                        Text(
                            if (daysToNext == 1) {
                                stringResource(R.string.home_momentum_next_one, next.title)
                            } else {
                                stringResource(R.string.home_momentum_next_many, next.title, daysToNext)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            stringResource(R.string.home_momentum_all_milestones),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentumStat(
    value: Int,
    labelRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
