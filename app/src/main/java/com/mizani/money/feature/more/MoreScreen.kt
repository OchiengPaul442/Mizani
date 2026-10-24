package com.mizani.money.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.components.MizaniInfoRow
import com.mizani.money.ui.components.MizaniSectionCard
import com.mizani.money.ui.components.MizaniSectionDivider
import com.mizani.money.ui.components.MizaniStatCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.Spacing

@Composable
fun MoreScreen(
    state: AppUiState,
    onNavigate: (String) -> Unit,
) {
    val snapshot = state.snapshot
    val activeWallets = snapshot.wallets.count { !it.archived && it.deletedAt == null }
    val categories = snapshot.categories.count { it.deletedAt == null }
    val debts = snapshot.debts.count { !it.archived && it.deletedAt == null }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "More")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text("Money", style = MaterialTheme.typography.titleMedium)
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Wallets",
                    subtitle = "$activeWallets active",
                    onClick = { onNavigate("wallets") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Category,
                    title = "Categories",
                    subtitle = "$categories categories",
                    onClick = { onNavigate("categories") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.AccountBalance,
                    title = "Debts",
                    subtitle = if (debts == 0) "No debts tracked" else "$debts tracked",
                    onClick = { onNavigate("debts") },
                )
            }

            Text("App", style = MaterialTheme.typography.titleMedium)
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Settings,
                    title = "Settings",
                    subtitle = "Appearance, app lock, country, export",
                    onClick = { onNavigate("settings") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Lock,
                    title = "App lock",
                    subtitle = if (state.settings.lockEnabled) "On" else "Off",
                    onClick = { onNavigate("settings") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "Move data",
                    subtitle = "Use a backup on another phone",
                    onClick = { onNavigate("account") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Description,
                    title = "Export & backup",
                    subtitle = "Excel, CSV and encrypted backup",
                    onClick = { onNavigate("settings") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Info,
                    title = "About & privacy",
                    subtitle = "How Mizani protects your data",
                    onClick = { onNavigate("about") },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MizaniStatCard(
                    icon = Icons.Rounded.Payments,
                    label = "Days with activity",
                    value = if (state.snapshot.transactions.isEmpty()) "—" else "${state.snapshot.transactions.distinctBy { it.localDateEpochDay }.size}",
                    modifier = Modifier.weight(1f),
                )
                MizaniStatCard(
                    icon = Icons.Rounded.DarkMode,
                    label = "Theme",
                    value = state.settings.themeMode.label,
                    modifier = Modifier.weight(1f),
                )
            }

        }
    }
}

