package com.mizani.money.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mizani.money.ui.AdaptiveLayout
import com.mizani.money.ui.components.MizaniInfoRow
import com.mizani.money.ui.components.MizaniSectionCard
import com.mizani.money.ui.components.MizaniSectionDivider
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.SectionHeader
import com.mizani.money.ui.components.Spacing

/** Cap for the two-column More layout so wide screens never stretch cards. */
private val MoreContentMaxWidth = 1180.dp

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "More")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottomFab),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumn = maxWidth >= AdaptiveLayout.EXPANDED_CONTENT_WIDTH_DP.dp
                val contentMaxWidth = if (twoColumn) {
                    MoreContentMaxWidth
                } else {
                    AdaptiveLayout.SINGLE_COLUMN_MAX_WIDTH_DP.dp
                }
                Column(
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    if (twoColumn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                        ) {
                            ManageMoneySection(
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                            ) {
                                YourDataSection(onNavigate)
                                AppSection(onNavigate)
                            }
                        }
                    } else {
                        ManageMoneySection(onNavigate)
                        YourDataSection(onNavigate)
                        AppSection(onNavigate)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageMoneySection(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SectionHeader(title = "Manage money", accent = true)
        MizaniSectionCard {
            MizaniInfoRow(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "Wallets",
                subtitle = "Cash, mobile money and bank balances",
                onClick = { onNavigate("wallets") },
            )
            MizaniSectionDivider()
            MizaniInfoRow(
                icon = Icons.Rounded.AccountBalance,
                title = "Debts",
                subtitle = "What you owe and payments made against it",
                onClick = { onNavigate("debts") },
            )
            MizaniSectionDivider()
            MizaniInfoRow(
                icon = Icons.AutoMirrored.Rounded.ShowChart,
                title = "Investments",
                subtitle = "Track holdings, providers and manual values",
                onClick = { onNavigate("investments") },
            )
            MizaniSectionDivider()
            MizaniInfoRow(
                icon = Icons.Rounded.Category,
                title = "Categories",
                subtitle = "Organize income and expenses",
                onClick = { onNavigate("categories") },
            )
        }
    }
}

@Composable
private fun YourDataSection(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SectionHeader(title = "Your data", accent = true)
        MizaniSectionCard {
            MizaniInfoRow(
                icon = Icons.Rounded.Download,
                title = "Export or back up data",
                subtitle = "CSV, Excel, encrypted backup and restore",
                onClick = { onNavigate("export") },
            )
        }
    }
}

@Composable
private fun AppSection(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SectionHeader(title = "App", accent = true)
        MizaniSectionCard {
            MizaniInfoRow(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                subtitle = "Theme, app lock, country and reminders",
                onClick = { onNavigate("settings") },
            )
            MizaniSectionDivider()
            MizaniInfoRow(
                icon = Icons.Rounded.Info,
                title = "About and privacy",
                subtitle = "How Mizani stores and protects your records",
                onClick = { onNavigate("about") },
            )
            MizaniSectionDivider()
            MizaniInfoRow(
                icon = Icons.Rounded.Feedback,
                title = "Send feedback",
                subtitle = "Help improve Mizani",
                onClick = { onNavigate("feedback") },
            )
        }
    }
}
