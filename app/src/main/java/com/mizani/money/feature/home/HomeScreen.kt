package com.mizani.money.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mizani.money.R
import com.mizani.money.core.model.InsightKind
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.PeriodPreset
import com.mizani.money.core.time.Periods
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.data.repo.LedgerSnapshot
import com.mizani.money.domain.analytics.AnalyticsEngine
import com.mizani.money.domain.analytics.DashboardMetrics
import com.mizani.money.domain.analytics.UpcomingCommitment
import com.mizani.money.domain.engagement.DailyMomentum
import com.mizani.money.domain.engagement.DailyMomentumCalculator
import com.mizani.money.feature.lock.ProtectedBalanceValue
import com.mizani.money.feature.lock.WalletBalanceVisibilityControl
import com.mizani.money.ui.AdaptiveLayout
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateRangePickerDialog
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.InsightCard
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniDonutChart
import com.mizani.money.ui.components.PeriodMenu
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StorysetIllustration
import com.mizani.money.ui.components.TransactionRow
import com.mizani.money.ui.components.WalletAvatar
import com.mizani.money.ui.components.parseHexColor
import com.mizani.money.ui.theme.LocalMizaniThemeTokens
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** Two-column Home content is capped so cards never stretch edge-to-edge on tablets. */
private val HomeContentMaxWidth = 1180.dp

/** Weekday initials for the seven-day spending pulse, Monday first. */
private val WeekdayInitials = listOf("M", "T", "W", "T", "F", "S", "S")

/** Oversized white balance rings bleeding past the hero's top-right and bottom-left edges. */
private fun Modifier.balanceRings(): Modifier = drawBehind {
    val base = maxOf(size.width, size.height)
    drawCircle(
        color = Color.White.copy(alpha = 0.06f),
        radius = base * 0.55f,
        center = Offset(size.width + base * 0.18f, -base * 0.22f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.045f),
        radius = base * 0.38f,
        center = Offset(-base * 0.16f, size.height + base * 0.18f),
    )
}

/** Concentric rings echoing the balance language behind the empty-state illustration. */
private fun Modifier.concentricBalanceRings(accent: Color): Modifier = drawBehind {
    val center = Offset(size.width / 2f, size.height / 2f)
    val base = size.minDimension
    drawCircle(Color.White.copy(alpha = 0.06f), radius = base * 0.52f, center = center)
    drawCircle(Color.White.copy(alpha = 0.045f), radius = base * 0.36f, center = center)
    drawCircle(
        color = accent,
        radius = 3.dp.toPx(),
        center = Offset(size.width * 0.76f, size.height * 0.24f),
    )
}

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
    onAddTransfer: () -> Unit,
    onImportMessage: () -> Unit,
    onOpenCommitments: () -> Unit,
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
    val analytics = remember(snapshot) { AnalyticsEngine(snapshot) }
    // One seven-day spending pulse feeds both the two-column and single-column layouts.
    val spendingPulse = remember(analytics, today) {
        analytics.dailySpending(7, today)
    }
    // The card looks ahead independently of the selected analytics period so bills at the
    // start of next month are still listed near month-end.
    val upcomingSoon = remember(analytics, today) {
        analytics.upcoming(DateRange(today, today.plusDays(45)), today)
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

    val activeWallets = remember(snapshot) { snapshot.wallets.filter { !it.archived && it.deletedAt == null } }
    val isEmptyLedger = snapshot.transactions.isEmpty()
    val current = metrics

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(top = Spacing.md, bottom = Spacing.bottom),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val twoColumn = maxWidth >= AdaptiveLayout.EXPANDED_CONTENT_WIDTH_DP.dp
            val contentMaxWidth = if (twoColumn) {
                HomeContentMaxWidth
            } else {
                AdaptiveLayout.SINGLE_COLUMN_MAX_WIDTH_DP.dp
            }
            val stackHeader = maxWidth < 380.dp || fontScale > AdaptiveLayout.LARGE_FONT_SCALE

            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth()
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                HomeHeader(
                    workspaceName = snapshot.workspace?.name,
                    today = today,
                    selectedPreset = selectedPreset,
                    customRange = customRange,
                    periodRange = periodRange,
                    stackHeader = stackHeader,
                    onSelect = { preset -> vm.selectPreset(preset) },
                    onCustom = { showRangePicker = true },
                )

                if (isEmptyLedger) {
                    EmptyLedgerHome(
                        vm = vm,
                        state = state,
                        activeWallets = activeWallets,
                        walletBalances = walletBalances,
                        currency = currency,
                        momentum = momentum,
                        onOpenWallets = onOpenWallets,
                        onSetupPin = onSetupPin,
                        onAdd = onAdd,
                        onAddIncome = onAddIncome,
                        onAddExpense = onAddExpense,
                        onAddTransfer = onAddTransfer,
                        onImportMessage = onImportMessage,
                    )
                } else if (current == null) {
                    LoadingBox()
                } else if (twoColumn) {
                    BalanceHero(
                        metrics = current,
                        currency = currency,
                        periodRange = periodRange,
                        today = today,
                        visible = state.walletBalancesVisible,
                        vm = vm,
                        state = state,
                        onSetupPin = onSetupPin,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                        ) {
                            QuickActionsCard(
                                onAddExpense = onAddExpense,
                                onAddIncome = onAddIncome,
                                onAddTransfer = onAddTransfer,
                                onImportMessage = onImportMessage,
                            )
                            RecentActivitySection(
                                metrics = current,
                                snapshot = snapshot,
                                currency = currency,
                                onOpenTransactions = onOpenTransactions,
                                onEditTransaction = onEditTransaction,
                            )
                            WalletsSection(
                                vm = vm,
                                state = state,
                                wallets = activeWallets,
                                balances = walletBalances,
                                currency = currency,
                                onOpenWallets = onOpenWallets,
                                onSetupPin = onSetupPin,
                                showVisibilityControl = false,
                            )
                            InsightSection(
                                insights = insights,
                                onAdd = onAdd,
                                onOpenInsights = onOpenInsights,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                        ) {
                            SpendingSection(
                                metrics = current,
                                currency = currency,
                                pulse = spendingPulse,
                                onOpenInsights = onOpenInsights,
                            )
                            ComingUpSection(
                                upcoming = upcomingSoon,
                                today = today,
                                currency = currency,
                                onOpenCommitments = onOpenCommitments,
                            )
                            DailyMomentumCard(momentum = momentum, onAdd = onAdd)
                        }
                    }
                } else {
                    BalanceHero(
                        metrics = current,
                        currency = currency,
                        periodRange = periodRange,
                        today = today,
                        visible = state.walletBalancesVisible,
                        vm = vm,
                        state = state,
                        onSetupPin = onSetupPin,
                    )
                    QuickActionsCard(
                        onAddExpense = onAddExpense,
                        onAddIncome = onAddIncome,
                        onAddTransfer = onAddTransfer,
                        onImportMessage = onImportMessage,
                    )
                    RecentActivitySection(
                        metrics = current,
                        snapshot = snapshot,
                        currency = currency,
                        onOpenTransactions = onOpenTransactions,
                        onEditTransaction = onEditTransaction,
                    )
                    SpendingSection(
                        metrics = current,
                        currency = currency,
                        pulse = spendingPulse,
                        onOpenInsights = onOpenInsights,
                    )
                    ComingUpSection(
                        upcoming = upcomingSoon,
                        today = today,
                        currency = currency,
                        onOpenCommitments = onOpenCommitments,
                    )
                    WalletsSection(
                        vm = vm,
                        state = state,
                        wallets = activeWallets,
                        balances = walletBalances,
                        currency = currency,
                        onOpenWallets = onOpenWallets,
                        onSetupPin = onSetupPin,
                        showVisibilityControl = false,
                    )
                    DailyMomentumCard(momentum = momentum, onAdd = onAdd)
                    InsightSection(
                        insights = insights,
                        onAdd = onAdd,
                        onOpenInsights = onOpenInsights,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    workspaceName: String?,
    today: LocalDate,
    selectedPreset: PeriodPreset,
    customRange: DateRange?,
    periodRange: DateRange,
    stackHeader: Boolean,
    onSelect: (PeriodPreset) -> Unit,
    onCustom: () -> Unit,
) {
    val greeting = DateText.greeting(LocalTime.now().hour)
    val subtitle = "${DateText.weekday(today)} · ${workspaceName ?: "My money"}"
    if (stackHeader) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            HomeGreeting(greeting = greeting, subtitle = subtitle)
            PeriodMenu(
                selected = selectedPreset,
                customRange = customRange,
                range = periodRange,
                onSelect = onSelect,
                onCustom = onCustom,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HomeGreeting(
                greeting = greeting,
                subtitle = subtitle,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.md))
            PeriodMenu(
                selected = selectedPreset,
                customRange = customRange,
                range = periodRange,
                onSelect = onSelect,
                onCustom = onCustom,
            )
        }
    }
}

@Composable
private fun HomeGreeting(greeting: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(greeting, style = MaterialTheme.typography.titleLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Announced so the wait reads as loading, not an empty screen.
        val loadingLabel = stringResource(R.string.home_loading)
        CircularProgressIndicator(
            modifier = Modifier
                .size(28.dp)
                .semantics { contentDescription = loadingLabel },
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun EmptyLedgerHome(
    vm: AppViewModel,
    state: AppUiState,
    activeWallets: List<WalletEntity>,
    walletBalances: Map<String, Long>,
    currency: Currency,
    momentum: DailyMomentum,
    onOpenWallets: () -> Unit,
    onSetupPin: () -> Unit,
    onAdd: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onImportMessage: () -> Unit,
) {
    GetStartedHero()
    QuickActionsCard(
        onAddExpense = onAddExpense,
        onAddIncome = onAddIncome,
        onAddTransfer = onAddTransfer,
        onImportMessage = onImportMessage,
    )
    WalletsSection(
        vm = vm,
        state = state,
        wallets = activeWallets,
        balances = walletBalances,
        currency = currency,
        onOpenWallets = onOpenWallets,
        onSetupPin = onSetupPin,
        showVisibilityControl = true,
    )
    DailyMomentumCard(momentum = momentum, onAdd = onAdd)
}

@Composable
private fun GetStartedHero() {
    val themeTokens = LocalMizaniThemeTokens.current
    val illustrationSize = 104.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(themeTokens.heroStart, themeTokens.heroEnd)))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(illustrationSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .concentricBalanceRings(themeTokens.heroAccent),
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
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(R.string.home_get_started_body),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BalanceHero(
    metrics: DashboardMetrics,
    currency: Currency,
    periodRange: DateRange,
    today: LocalDate,
    visible: Boolean,
    vm: AppViewModel,
    state: AppUiState,
    onSetupPin: () -> Unit,
) {
    val themeTokens = LocalMizaniThemeTokens.current
    val progress = periodProgress(periodRange, today)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(themeTokens.heroStart, themeTokens.heroEnd)))
            .balanceRings()
            .padding(20.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val wide = maxWidth >= 520.dp && fontScale <= AdaptiveLayout.LARGE_FONT_SCALE
            val amountStyle = if (maxWidth < 300.dp) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.headlineMedium
            }
            val incomeLabel = stringResource(R.string.home_hero_income)
            val spendingLabel = stringResource(R.string.home_hero_spending)
            val netLabel = stringResource(R.string.home_hero_net)
            val periodLabel = DateText.rangeLabel(periodRange)
            val acrossWalletsLabel = stringResource(R.string.home_hero_across_wallets)

            if (wide) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(0.45f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                acrossWalletsLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            WalletBalanceVisibilityControl(
                                vm = vm,
                                state = state,
                                onSetupPin = onSetupPin,
                                showLabel = false,
                                tint = Color.White,
                            )
                        }
                        ProtectedBalanceValue(
                            amountMinor = metrics.walletTotalMinor,
                            currency = currency,
                            visible = visible,
                            style = amountStyle,
                            color = Color.White,
                            maxLines = 1,
                            amountLabel = "Across wallets",
                        )
                        Text(
                            periodLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                        if (progress != null) {
                            PeriodProgressTrack(progress)
                        }
                    }
                    Spacer(Modifier.width(Spacing.lg))
                    Row(
                        modifier = Modifier
                            .weight(0.55f)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HeroStat(
                            label = incomeLabel,
                            value = MoneyFormat.format(metrics.incomeMinor, currency),
                            modifier = Modifier.weight(1f),
                        )
                        HeroDivider()
                        HeroStat(
                            label = spendingLabel,
                            value = MoneyFormat.format(metrics.expenseMinor, currency),
                            modifier = Modifier.weight(1f),
                        )
                        HeroDivider()
                        HeroStat(
                            label = netLabel,
                            value = signedNet(metrics.netMinor, currency),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            acrossWalletsLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        WalletBalanceVisibilityControl(
                            vm = vm,
                            state = state,
                            onSetupPin = onSetupPin,
                            showLabel = false,
                            tint = Color.White,
                        )
                    }
                    ProtectedBalanceValue(
                        amountMinor = metrics.walletTotalMinor,
                        currency = currency,
                        visible = visible,
                        style = amountStyle,
                        color = Color.White,
                        maxLines = 1,
                        amountLabel = "Across wallets",
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                    Text(
                        periodLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    if (progress != null) {
                        PeriodProgressTrack(progress)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        HeroStat(
                            label = incomeLabel,
                            value = MoneyFormat.format(metrics.incomeMinor, currency),
                            modifier = Modifier.weight(1f),
                        )
                        HeroStat(
                            label = spendingLabel,
                            value = MoneyFormat.format(metrics.expenseMinor, currency),
                            modifier = Modifier.weight(1f),
                        )
                        HeroStat(
                            label = netLabel,
                            value = signedNet(metrics.netMinor, currency),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodProgressTrack(progress: PeriodProgress) {
    val themeTokens = LocalMizaniThemeTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(R.string.home_hero_day_progress, progress.elapsedDays, progress.totalDays),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.fraction)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.70f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(themeTokens.heroAccent),
                )
            }
        }
    }
}

private data class PeriodProgress(val elapsedDays: Int, val totalDays: Int) {
    val fraction: Float
        get() = if (totalDays <= 0) 0f else (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f)
}

/** Null when today is outside the period or the period is a single day. */
private fun periodProgress(range: DateRange, today: LocalDate): PeriodProgress? {
    if (today.isBefore(range.start) || today.isAfter(range.end)) return null
    val total = ChronoUnit.DAYS.between(range.start, range.end) + 1
    if (total <= 1L) return null
    val elapsed = (ChronoUnit.DAYS.between(range.start, today) + 1).coerceIn(0L, total)
    return PeriodProgress(elapsed.toInt(), total.toInt())
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.12f)),
    )
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.5.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun signedNet(minorUnits: Long, currency: Currency): String {
    if (minorUnits == 0L) return MoneyFormat.format(0L, currency)
    val sign = if (minorUnits > 0) "+" else "−"
    val absolute = if (minorUnits == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minorUnits)
    return sign + MoneyFormat.format(absolute, currency)
}

private data class QuickAction(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val iconContainer: Color,
    val iconTint: Color,
    val allowEllipsis: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun QuickActionsCard(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
    onImportMessage: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val actions = listOf(
        QuickAction(
            label = stringResource(R.string.home_quick_add_expense),
            description = stringResource(R.string.home_quick_add_expense_description),
            icon = Icons.Rounded.ArrowUpward,
            iconContainer = colorScheme.secondaryContainer,
            iconTint = colorScheme.onSecondaryContainer,
            allowEllipsis = false,
            onClick = onAddExpense,
        ),
        QuickAction(
            label = stringResource(R.string.home_quick_add_income),
            description = stringResource(R.string.home_quick_add_income_description),
            icon = Icons.Rounded.ArrowDownward,
            iconContainer = colorScheme.primaryContainer,
            iconTint = colorScheme.onPrimaryContainer,
            allowEllipsis = false,
            onClick = onAddIncome,
        ),
        QuickAction(
            label = stringResource(R.string.home_quick_add_transfer),
            description = stringResource(R.string.home_quick_add_transfer_description),
            icon = Icons.Rounded.SwapHoriz,
            iconContainer = colorScheme.tertiaryContainer,
            iconTint = colorScheme.onTertiaryContainer,
            onClick = onAddTransfer,
        ),
        QuickAction(
            label = stringResource(R.string.home_quick_add_import),
            description = stringResource(R.string.home_quick_add_import_description),
            icon = Icons.Rounded.ContentPaste,
            iconContainer = colorScheme.surfaceContainerHighest,
            iconTint = colorScheme.onSurface,
            onClick = onImportMessage,
        ),
    )
    MizaniCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                stringResource(R.string.home_quick_add_title),
                style = MaterialTheme.typography.titleMedium,
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Keep all four tiles on one row from 300dp content width upward; collapse to 2x2
                // only for narrower panes or large type so the labels never clip horizontally.
                val twoRows = maxWidth < 300.dp || LocalDensity.current.fontScale > AdaptiveLayout.LARGE_FONT_SCALE
                if (twoRows) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            actions.take(2).forEach { action ->
                                QuickActionTile(action = action, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            actions.drop(2).forEach { action ->
                                QuickActionTile(action = action, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions.forEachIndexed { index, action ->
                            if (index > 0) {
                                SummaryDivider()
                            }
                            QuickActionTile(action = action, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(action: QuickAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = action.onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (action.description != action.label) {
                    contentDescription = action.description
                }
            }
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(action.iconContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = action.iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            action.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = if (action.allowEllipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpendingSection(
    metrics: DashboardMetrics,
    currency: Currency,
    pulse: List<Pair<LocalDate, Long>>,
    onOpenInsights: () -> Unit,
) {
    // Refund-heavy periods can leave negative category totals. A negative slice distorts
    // the donut and its shares, so only positive net spending is charted and shared out.
    val positiveSlices = metrics.categorySlices.filter { it.amountMinor > 0 }
    val positiveTotal = positiveSlices.sumOf { it.amountMinor }
    SectionHeader(
        title = stringResource(R.string.home_spending_title),
        actionLabel = "Insights",
        onAction = onOpenInsights,
        accent = true,
    )
    MizaniCard(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stack = maxWidth < 320.dp || LocalDensity.current.fontScale > AdaptiveLayout.LARGE_FONT_SCALE
            if (positiveSlices.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.home_spending_empty_title),
                    body = stringResource(R.string.home_spending_empty_body),
                    iconRes = R.drawable.ic_ph_chart_donut,
                    compact = true,
                )
            } else if (stack) {
                Column {
                    MizaniDonutChart(
                        slices = positiveSlices,
                        totalMinor = positiveTotal,
                        currency = currency,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .widthIn(max = 140.dp)
                            .aspectRatio(1f),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SliceLegendList(positiveSlices = positiveSlices, positiveTotal = positiveTotal, currency = currency)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MizaniDonutChart(
                        slices = positiveSlices,
                        totalMinor = positiveTotal,
                        currency = currency,
                        modifier = Modifier
                            .weight(0.9f)
                            .widthIn(max = 140.dp)
                            .aspectRatio(1f),
                    )
                    Spacer(Modifier.width(Spacing.lg))
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        SliceLegendList(positiveSlices = positiveSlices, positiveTotal = positiveTotal, currency = currency)
                    }
                }
            }
        }
        if (pulse.any { it.second > 0 }) {
            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.home_spending_pulse_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            SevenDayPulse(pulse = pulse, currency = currency)
        }
    }
}

@Composable
private fun SevenDayPulse(pulse: List<Pair<LocalDate, Long>>, currency: Currency) {
    val max = pulse.maxOf { it.second }
    val summary = pulse.joinToString(", ") { (date, amount) ->
        "${DateText.short(date)} ${MoneyFormat.format(amount, currency)}"
    }
    val summaryDescription =
        stringResource(R.string.home_spending_pulse_description, summary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = summaryDescription
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        pulse.forEachIndexed { index, (date, amount) ->
            val isToday = index == pulse.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(3.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(maxOf(2f, if (max > 0) amount.toFloat() / max * 28f else 0f).dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                },
                            ),
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    WeekdayInitials[date.dayOfWeek.value - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SliceLegendList(
    positiveSlices: List<com.mizani.money.domain.analytics.CategorySlice>,
    positiveTotal: Long,
    currency: Currency,
) {
    val top = positiveSlices.take(4)
    val rest = positiveSlices.drop(4)
    top.forEach { slice -> SliceLegend(slice, currency) }
    if (rest.isNotEmpty()) {
        val restTotal = rest.sumOf { it.amountMinor }
        val restShare = restTotal.toDouble() / positiveTotal
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                stringResource(R.string.home_spending_other),
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

@Composable
private fun RecentActivitySection(
    metrics: DashboardMetrics,
    snapshot: LedgerSnapshot,
    currency: Currency,
    onOpenTransactions: () -> Unit,
    onEditTransaction: (String) -> Unit,
) {
    val recent = metrics.recentTransactions.take(4)
    if (recent.isEmpty()) return
    SectionHeader(
        title = stringResource(R.string.home_recent_activity_title),
        actionLabel = "See all",
        onAction = onOpenTransactions,
        accent = true,
    )
    MizaniCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
    ) {
        recent.forEachIndexed { index, transaction ->
            TransactionRow(
                transaction = transaction,
                snapshot = snapshot,
                currency = currency,
                onClick = { onEditTransaction(transaction.id) },
            )
            if (index < recent.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ComingUpSection(
    upcoming: List<UpcomingCommitment>,
    today: LocalDate,
    currency: Currency,
    onOpenCommitments: () -> Unit,
) {
    if (upcoming.isEmpty()) return
    val shown = upcoming.take(3)
    // Overdue rows stay visible but are excluded from the "due in the next 7 days" total,
    // which now covers today through six days ahead.
    val dueWithin7Days = upcoming
        .filter { !it.isIncome }
        .filter {
            val daysUntil = ChronoUnit.DAYS.between(today, it.dueDate)
            daysUntil in 0L..6L
        }
        .sumOf { it.amountMinor }
    SectionHeader(
        title = stringResource(R.string.home_coming_up_title),
        actionLabel = "Bills",
        onAction = onOpenCommitments,
        accent = true,
    )
    MizaniCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            shown.forEachIndexed { index, item ->
                ComingUpRow(item = item, today = today, currency = currency)
                if (index < shown.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.home_coming_up_due_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    MoneyFormat.format(dueWithin7Days, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ComingUpRow(item: UpcomingCommitment, today: LocalDate, currency: Currency) {
    val themeTokens = LocalMizaniThemeTokens.current
    val accent = parseHexColor(item.colorHex)
    val amountColor = if (item.isIncome) themeTokens.income else MaterialTheme.colorScheme.onSurface
    val amountPrefix = if (item.isIncome) "+" else "−"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val badgeColor = if (item.isIncome) themeTokens.income else accent
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(badgeColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    item.dueDate.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    item.dueDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    if (item.isIncome) {
                        stringResource(R.string.home_coming_up_expected_income)
                    } else {
                        stringResource(R.string.home_coming_up_bill)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${item.categoryName} · ${DateText.relative(item.dueDate, today)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            "$amountPrefix${MoneyFormat.format(kotlin.math.abs(item.amountMinor), currency)}",
            style = MaterialTheme.typography.titleSmall,
            color = amountColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun WalletsSection(
    vm: AppViewModel,
    state: AppUiState,
    wallets: List<WalletEntity>,
    balances: Map<String, Long>,
    currency: Currency,
    onOpenWallets: () -> Unit,
    onSetupPin: () -> Unit,
    showVisibilityControl: Boolean,
) {
    if (wallets.isEmpty()) return
    val visibleWallets = wallets.take(3)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                stringResource(R.string.home_wallets_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showVisibilityControl) {
                WalletBalanceVisibilityControl(
                    vm = vm,
                    state = state,
                    onSetupPin = onSetupPin,
                    showLabel = false,
                )
            }
            TextButton(onClick = onOpenWallets) {
                Text(stringResource(R.string.home_wallets_action), style = MaterialTheme.typography.labelLarge)
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

@Composable
private fun InsightSection(
    insights: List<com.mizani.money.core.model.Insight>,
    onAdd: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    val insight = insights.firstOrNull() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(
            title = stringResource(R.string.home_insight_title),
            accent = true,
        )
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
            val stackStats = maxWidth < 300.dp || LocalDensity.current.fontScale > AdaptiveLayout.LARGE_FONT_SCALE

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
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(parseHexColor(slice.colorHex)),
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
