package com.mizani.money.feature.wallets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Unarchive
import com.mizani.money.ui.components.MizaniAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.feature.lock.ProtectedBalanceValue
import com.mizani.money.feature.lock.WalletBalanceVisibilityControl
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniStatCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner
import com.mizani.money.ui.components.WalletAvatar
import com.mizani.money.ui.components.parseHexColor
import com.mizani.money.ui.theme.BrandPalette
import kotlinx.coroutines.launch

@Composable
fun WalletsScreen(
    vm: AppViewModel,
    state: AppUiState,
    onBack: () -> Unit,
    onSetupPin: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val balances = remember(snapshot) { snapshot.walletBalances() }
    val active = snapshot.wallets.filter { !it.archived && it.deletedAt == null }
    val archived = snapshot.wallets.filter { it.archived && it.deletedAt == null }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<WalletEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val total = active.sumOf { balances[it.id] ?: 0L }
    val liquid = active.filter { it.type != WalletType.DEBT }.sumOf { balances[it.id] ?: 0L }

    if (showDialog) {
        WalletDialog(
            initial = editing,
            currency = currency,
            balanceVisible = state.walletBalancesVisible,
            canArchive = active.size > 1,
            onDismiss = { showDialog = false; editing = null },
            onSave = { name, type, opening, color, icon ->
                val walletToUpdate = editing
                scope.launch {
                    runCatching {
                        if (walletToUpdate == null) {
                            vm.finance.addWallet(name, type, opening, color, icon)
                        } else {
                            vm.finance.updateWallet(walletToUpdate.id, name, type, opening, color, icon)
                        }
                    }.onSuccess {
                        showDialog = false
                        editing = null
                        error = null
                    }.onFailure { error = it.message ?: "Could not save this wallet. Try again." }
                }
            },
            onArchive = { wallet, archived ->
                scope.launch {
                    runCatching { vm.finance.setWalletArchived(wallet.id, archived) }
                        .onSuccess { showDialog = false; editing = null }
                        .onFailure { error = it.message ?: "Could not update this wallet. Try again." }
                }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = "Wallets",
            subtitle = "${active.size} active",
            onBack = onBack,
            actions = {
                WalletBalanceVisibilityControl(vm, state, onSetupPin)
                IconButton(onClick = { editing = null; showDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add wallet")
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
            if (error != null) {
                StatusBanner(
                    text = error!!,
                    isError = true,
                    onDismiss = { error = null },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MizaniStatCard(
                    label = "Across wallets",
                    value = if (state.walletBalancesVisible) MoneyFormat.format(total, currency) else "••••••",
                    modifier = Modifier.weight(1f),
                    valueContentDescription = if (state.walletBalancesVisible) {
                        "Across wallets ${MoneyFormat.format(total, currency)}"
                    } else {
                        "Across wallet balances hidden"
                    },
                )
                MizaniStatCard(
                    label = "Liquid",
                    value = if (state.walletBalancesVisible) MoneyFormat.format(liquid, currency) else "••••••",
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.primary,
                    infoTitle = stringResource(R.string.info_liquid_title),
                    infoBody = stringResource(R.string.info_liquid_body),
                    valueContentDescription = if (state.walletBalancesVisible) {
                        "Liquid balance ${MoneyFormat.format(liquid, currency)}"
                    } else {
                        "Liquid balance hidden"
                    },
                )
            }

            Text(
                "Balances reflect the activity you record. Transfers move money between your wallets without changing the overall total.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (active.isEmpty()) {
                EmptyState(
                    title = "No active wallets",
                    body = "Create a cash, mobile-money, bank, card or savings wallet to start.",
                    iconRes = R.drawable.ic_ph_wallet,
                    actionLabel = "Add wallet",
                    onAction = { editing = null; showDialog = true },
                )
            } else {
                active.forEach { wallet ->
                    WalletCard(
                        wallet = wallet,
                        balance = balances[wallet.id] ?: 0L,
                        currency = currency,
                        balanceVisible = state.walletBalancesVisible,
                        onClick = { editing = wallet; showDialog = true },
                    )
                }
            }

            if (archived.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text("Archived", style = MaterialTheme.typography.titleSmall)
                archived.forEach { wallet ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { editing = wallet; showDialog = true },
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WalletAvatar(wallet.type, wallet.colorHex, size = 34)
                            Spacer(Modifier.width(Spacing.md))
                            Text(wallet.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (state.walletBalancesVisible) {
                                    MoneyFormat.format(balances[wallet.id] ?: 0, currency)
                                } else {
                                    "••••••"
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = if (state.walletBalancesVisible) {
                                        "Balance ${MoneyFormat.format(balances[wallet.id] ?: 0, currency)}"
                                    } else {
                                        "Balance hidden"
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Text(
                "A wallet is a place money lives, not an online account. Mizani does not connect to banks or " +
                    "mobile-money APIs; you record transactions yourself.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WalletCard(
    wallet: WalletEntity,
    balance: Long,
    currency: Currency,
    balanceVisible: Boolean,
    onClick: () -> Unit,
) {
    val color = parseHexColor(wallet.colorHex)
    MizaniCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WalletAvatar(wallet.type, wallet.colorHex, size = 44)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(wallet.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (wallet.name != wallet.type.label) {
                    Text(wallet.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                ProtectedBalanceValue(
                    amountMinor = balance,
                    currency = currency,
                    visible = balanceVisible,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text("balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletDialog(
    initial: WalletEntity?,
    currency: Currency,
    balanceVisible: Boolean,
    canArchive: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, WalletType, Long, String, String) -> Unit,
    onArchive: (WalletEntity, Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: WalletType.CASH) }
    var openingText by rememberSaveable {
        mutableStateOf(initial?.let { MoneyFormat.toInput(kotlin.math.abs(it.openingBalanceMinor), currency) } ?: "")
    }
    var colorHex by rememberSaveable { mutableStateOf(initial?.colorHex ?: "#087F72") }
    var iconKey by rememberSaveable { mutableStateOf(initial?.icon ?: "wallet") }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = remember(initial?.colorHex) {
        (BrandPalette.swatches + listOfNotNull(initial?.colorHex)).distinct()
    }
    val typeOptions = WalletType.entries.filter {
        it != WalletType.DEBT || initial?.type == WalletType.DEBT
    }
    val opening = if (initial != null && !balanceVisible) {
        kotlin.math.abs(initial.openingBalanceMinor)
    } else {
        MoneyFormat.parse(openingText, currency) ?: 0L
    }

    MizaniAlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(if (initial == null) "Add wallet" else "Edit wallet") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Wallet name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Type", style = MaterialTheme.typography.labelMedium)
                MizaniPicker(
                    selectedText = type.label,
                    options = typeOptions.map { PickerOption(it.name, it.label) },
                    onSelect = { id -> type = WalletType.valueOf(id) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initial != null && !balanceVisible) {
                    Text(
                        "Opening balance hidden. Confirm with your PIN before viewing or changing it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MizaniAmountField(
                        value = openingText,
                        onValueChange = { openingText = it },
                        label = if (type == WalletType.DEBT) "Amount owed (opening)" else "Opening balance",
                        currency = currency,
                    )
                }
                Text(
                    if (type == WalletType.DEBT) {
                        "A debt wallet is stored as a negative balance and excluded from liquid money."
                    } else {
                        "Opening balance is the amount already in this wallet when you start tracking here."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Colour", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    colors.forEach { hex ->
                        ColorDot(
                            hex = hex,
                            selected = colorHex == hex,
                            index = colors.indexOf(hex),
                            onClick = { colorHex = hex },
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (initial != null && initial.archived) {
                    TextButton(
                        onClick = { onArchive(initial, false) },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(Icons.Rounded.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore wallet")
                    }
                } else if (initial != null && canArchive) {
                    TextButton(
                        onClick = { onArchive(initial, true) },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Archive wallet")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = "Give this wallet a name"
                } else {
                    val signedOpening = if (type == WalletType.DEBT) -opening else opening
                    onSave(name, type, signedOpening, colorHex, iconKey)
                }
            }) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, index: Int, onClick: () -> Unit) {
    Box(
        // 48dp touch target; the visible swatch keeps its original size.
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Button,
            )
            // TalkBack needs a name and selection state; colour alone is not accessible.
            .semantics { contentDescription = "Colour option ${index + 1}" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(parseHexColor(hex)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}
