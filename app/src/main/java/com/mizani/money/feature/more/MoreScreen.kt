package com.mizani.money.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.AppInfo
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.MizaniInfoRow
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.MizaniSectionCard
import com.mizani.money.ui.components.MizaniSectionDivider
import com.mizani.money.ui.components.MizaniStatCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald
import com.mizani.money.ui.theme.Mint

@Composable
fun MoreScreen(
    vm: AppViewModel,
    state: AppUiState,
    onNavigate: (String) -> Unit,
) {
    val snapshot = state.snapshot
    val activeWallets = snapshot.wallets.count { !it.archived && it.deletedAt == null }
    val categories = snapshot.categories.count { it.deletedAt == null }
    val debts = snapshot.debts.count { !it.archived && it.deletedAt == null }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "More", subtitle = "Stored on this phone")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // Brand gradient banner: DeepTeal -> Emerald, 22dp radius, 20dp padding, white copy.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(listOf(DeepTeal, Emerald)))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MizaniMark(size = 48)
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text("Local-only mode", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            "No account, no cloud. Your data stays encrypted on this phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                TextButton(
                    onClick = { onNavigate("account") },
                    colors = ButtonDefaults.textButtonColors(contentColor = Mint),
                ) {
                    Text("How accounts and sync work")
                }
            }

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
                    icon = Icons.Rounded.Description,
                    title = "Export & backup",
                    subtitle = "CSV and encrypted backup",
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

            Text(
                "${AppInfo.NAME} ${AppInfo.VERSION} · Personal money management that works offline\n" +
                    "No ads. No tracking. No generative AI.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

