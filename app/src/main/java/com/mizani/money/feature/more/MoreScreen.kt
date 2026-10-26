package com.mizani.money.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mizani.money.ui.components.MizaniInfoRow
import com.mizani.money.ui.components.MizaniSectionCard
import com.mizani.money.ui.components.MizaniSectionDivider
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.Spacing

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
) {
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
            Text("Manage money", style = MaterialTheme.typography.titleMedium)
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Wallets",
                    subtitle = "Cash, mobile money and bank balances",
                    onClick = { onNavigate("wallets") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Category,
                    title = "Categories",
                    subtitle = "Organize income and expenses",
                    onClick = { onNavigate("categories") },
                )
            }

            Text("Your data", style = MaterialTheme.typography.titleMedium)
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Download,
                    title = "Export or back up data",
                    subtitle = "Download CSV or Excel, or move to another phone",
                    onClick = { onNavigate("export") },
                )
            }

            Text("App", style = MaterialTheme.typography.titleMedium)
            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Settings,
                    title = "Settings",
                    subtitle = "Appearance, app lock and country",
                    onClick = { onNavigate("settings") },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Info,
                    title = "About and privacy",
                    subtitle = "How Mizani stores and protects your records",
                    onClick = { onNavigate("about") },
                )
            }
        }
    }
}
