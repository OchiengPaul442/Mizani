package com.mizani.money.feature.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.model.InvestmentAssetType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.InvestmentHoldingEntity
import com.mizani.money.data.repo.InvestmentHoldingDraft
import com.mizani.money.feature.lock.WalletBalanceVisibilityControl
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniAlertDialog
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniStatCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun InvestmentsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onBack: () -> Unit,
    onSetupPin: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val active = snapshot.investments.filter { !it.archived && it.deletedAt == null }
    val archived = snapshot.investments.filter { it.archived && it.deletedAt == null }
    val summary = snapshot.investmentSummary()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<InvestmentHoldingEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (showDialog) {
        InvestmentDialog(
            initial = editing,
            currency = currency,
            balanceVisible = state.walletBalancesVisible,
            saveError = error,
            onDismiss = { showDialog = false; editing = null },
            onSave = { name, provider, assetType, costBasis, currentValue, valuationDate, note ->
                val current = editing
                scope.launch {
                    runCatching {
                        vm.finance.saveInvestmentHolding(
                            current?.id,
                            InvestmentHoldingDraft(
                                name = name,
                                provider = provider,
                                assetType = assetType,
                                costBasisMinor = costBasis,
                                currentValueMinor = currentValue,
                                valuationDate = valuationDate,
                                note = note,
                            ),
                        )
                    }.onSuccess {
                        showDialog = false
                        editing = null
                        error = null
                    }.onFailure { error = it.message ?: "Could not save this investment. Try again." }
                }
            },
            onArchive = { investment, archivedValue ->
                scope.launch {
                    runCatching { vm.finance.setInvestmentHoldingArchived(investment.id, archivedValue) }
                        .onSuccess { showDialog = false; editing = null }
                        .onFailure { error = it.message ?: "Could not update this investment. Try again." }
                }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = "Investments",
            subtitle = "${active.size} active holding${if (active.size == 1) "" else "s"}",
            onBack = onBack,
            actions = {
                WalletBalanceVisibilityControl(vm, state, onSetupPin)
                IconButton(onClick = { error = null; editing = null; showDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add investment")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MizaniCard {
                Text("Keep a private record of where your money is invested.", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Add the provider, asset category and your latest value. This is a manual snapshot, not a brokerage connection or investment advice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MizaniStatCard(
                    label = "Current value",
                    value = protectedMoney(summary.currentValueMinor, currency, state.walletBalancesVisible),
                    valueContentDescription = protectedMoneyDescription("Current investment value", summary.currentValueMinor, currency, state.walletBalancesVisible),
                    modifier = Modifier.weight(1f),
                )
                MizaniStatCard(
                    label = "Gain / loss",
                    value = protectedMoney(summary.gainLossMinor, currency, state.walletBalancesVisible),
                    valueContentDescription = protectedMoneyDescription("Investment gain or loss", summary.gainLossMinor, currency, state.walletBalancesVisible),
                    accent = if (summary.gainLossMinor >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    infoTitle = stringResource(R.string.info_investment_gain_title),
                    infoBody = stringResource(R.string.info_investment_gain_body),
                    modifier = Modifier.weight(1f),
                )
            }

            if (error != null) {
                StatusBanner(text = error!!, isError = true, onDismiss = { error = null })
            }

            if (active.isEmpty()) {
                EmptyState(
                    title = "No investments yet",
                    body = "Add a holding to record where it is placed and update its value over time.",
                    iconRes = R.drawable.ic_ph_wallet,
                    actionLabel = "Add investment",
                    onAction = { error = null; editing = null; showDialog = true },
                )
            } else {
                active.forEach { investment ->
                    InvestmentCard(
                        investment = investment,
                        currency = currency,
                        balanceVisible = state.walletBalancesVisible,
                        onClick = { error = null; editing = investment; showDialog = true },
                    )
                }
            }

            if (archived.isNotEmpty()) {
                Text("Archived", style = MaterialTheme.typography.titleSmall)
                archived.forEach { investment ->
                    InvestmentCard(
                        investment = investment,
                        currency = currency,
                        balanceVisible = state.walletBalancesVisible,
                        onClick = { error = null; editing = investment; showDialog = true },
                    )
                }
            }

            Text(
                "Investment records do not change wallet balances or create ledger entries. If money moves from a wallet to an investment account, record that movement separately as a transfer.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InvestmentCard(
    investment: InvestmentHoldingEntity,
    currency: Currency,
    balanceVisible: Boolean,
    onClick: () -> Unit,
) {
    val gainLoss = investment.currentValueMinor - investment.costBasisMinor
    MizaniCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(investment.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${investment.provider} · ${investment.assetType.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Valued ${DateText.medium(LocalDate.ofEpochDay(investment.valuationDateEpochDay))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ProtectedInvestmentValue("Current", investment.currentValueMinor, currency, balanceVisible, Modifier.weight(1f))
            ProtectedInvestmentValue("Gain / loss", gainLoss, currency, balanceVisible, Modifier.weight(1f))
        }
        if (investment.archived) {
            Text("Archived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProtectedInvestmentValue(
    label: String,
    value: Long,
    currency: Currency,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.semantics {
        contentDescription = if (visible) "$label ${MoneyFormat.format(value, currency)}" else "$label hidden"
    }) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (visible) MoneyFormat.format(value, currency) else "••••••",
            style = MaterialTheme.typography.titleSmall,
            color = if (visible && value < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun InvestmentDialog(
    initial: InvestmentHoldingEntity?,
    currency: Currency,
    balanceVisible: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, InvestmentAssetType, Long, Long, LocalDate, String) -> Unit,
    onArchive: (InvestmentHoldingEntity, Boolean) -> Unit,
) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var provider by rememberSaveable(initial?.id) { mutableStateOf(initial?.provider ?: "") }
    var assetType by rememberSaveable(initial?.id) { mutableStateOf(initial?.assetType ?: InvestmentAssetType.OTHER) }
    var costBasisText by rememberSaveable(initial?.id) {
        mutableStateOf(if (initial != null && !balanceVisible) "" else initial?.let { MoneyFormat.toInput(it.costBasisMinor, currency) } ?: "")
    }
    var currentValueText by rememberSaveable(initial?.id) {
        mutableStateOf(if (initial != null && !balanceVisible) "" else initial?.let { MoneyFormat.toInput(it.currentValueMinor, currency) } ?: "")
    }
    var valuationDate by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.valuationDateEpochDay) } ?: LocalDate.now())
    }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    MizaniAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add investment" else "Edit investment") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    "This saves a manual value snapshot. It does not place orders, connect to a provider, or affect your ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Investment name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider or where held") },
                    supportingText = { Text("For example, a bank, fund manager, platform or property") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Asset category", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = assetType.label,
                    options = InvestmentAssetType.entries.map { PickerOption(it.name, it.label) },
                    onSelect = { assetType = InvestmentAssetType.valueOf(it) },
                    fieldLabel = "Asset category",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initial != null && !balanceVisible) {
                    Text(
                        "Values are hidden. Confirm with your PIN before changing cost basis or current value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MizaniAmountField(
                        value = costBasisText,
                        onValueChange = { costBasisText = it },
                        label = "Cost basis",
                        currency = currency,
                    )
                    MizaniAmountField(
                        value = currentValueText,
                        onValueChange = { currentValueText = it },
                        label = "Current value",
                        currency = currency,
                    )
                }
                DateField(
                    label = "Valuation date",
                    date = valuationDate,
                    onDateSelected = { valuationDate = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                val visibleError = error ?: saveError
                if (visibleError != null) {
                    Text(visibleError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (initial != null) {
                    TextButton(
                        onClick = { onArchive(initial, !initial.archived) },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(
                            if (initial.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(Spacing.xs))
                        Text(if (initial.archived) "Restore investment" else "Archive investment")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val costBasis = if (initial != null && !balanceVisible) initial.costBasisMinor else MoneyFormat.parse(costBasisText, currency)
                val currentValue = if (initial != null && !balanceVisible) initial.currentValueMinor else MoneyFormat.parse(currentValueText, currency)
                when {
                    name.isBlank() -> error = "Give this investment a name"
                    provider.isBlank() -> error = "Enter where this investment is held"
                    costBasis == null || costBasis <= 0L -> error = "Enter a cost basis greater than zero"
                    currentValue == null || currentValue < 0L -> error = "Enter a current value"
                    valuationDate.isAfter(LocalDate.now()) -> error = "Valuation date cannot be in the future"
                    else -> onSave(name, provider, assetType, costBasis, currentValue, valuationDate, note)
                }
            }) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun protectedMoney(value: Long, currency: Currency, visible: Boolean): String =
    if (visible) MoneyFormat.format(value, currency) else "••••••"

private fun protectedMoneyDescription(label: String, value: Long, currency: Currency, visible: Boolean): String =
    if (visible) "$label ${MoneyFormat.format(value, currency)}" else "$label hidden"
