package com.mizani.money.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.AppInfo
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.Spacing

@Composable
fun AboutScreen(state: AppUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "About & privacy", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MizaniMark(size = 64)
                Spacer(Modifier.width(Spacing.lg))
                Column {
                    Text(AppInfo.NAME, style = MaterialTheme.typography.headlineMedium)
                    Text(AppInfo.TAGLINE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Version ${AppInfo.VERSION}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            MizaniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Privacy by architecture", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.sm))
                Bullet("The local database is encrypted with SQLCipher.")
                Bullet("The database passphrase is wrapped by a non-exportable Android Keystore key.")
                Bullet("No account is required, and there is no backend to upload to.")
                Bullet("No advertising SDKs and no financial telemetry.")
                Bullet("Financial analytics are deterministic and computed on this device.")
                Bullet("No generative AI is used for transactions or advice.")
                Bullet("Cloud backup is excluded for the app's private data at the platform level.")
            }

            MizaniCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("What stays on the phone", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Wallets, transactions, budgets, goals, bills, debts, notes and balances are stored only in the " +
                        "encrypted local database. Uninstalling the app removes this local data. Without an exported " +
                        "backup it cannot be recovered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "If someone copies the database file without the device Keystore key, the content is not readable. " +
                        "Android manufacturer device-transfer tools can vary, which is why the platform backup rules " +
                        "explicitly exclude this data and the database remains encrypted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MizaniCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("No financial surveillance", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "This app contains no analytics SDK, no crash reporter that sends financial values, no advertising " +
                        "identifier and no server-side profiling. Financial figures never appear in application logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MizaniCard {
                Text("What Mizani is not", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Mizani is not a bank, payment processor, wallet holding customer funds, lender, credit-scoring " +
                        "service, investment broker or tax platform. It does not provide credit decisions or guarantee " +
                        "that safe-to-spend amounts will be sufficient.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MizaniCard {
                Text("Your controls", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Create a full encrypted backup or a CSV at any time from Settings. Delete all local data from " +
                        "Settings with confirmation. App lock adds a 6-digit PIN and optional biometric unlock. " +
                        "Cloud backup to an online account is not available in Mizani and is never silently enabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.settings.lockEnabled) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text("App lock: on", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                "Built with Kotlin, Jetpack Compose, Room and SQLCipher.\n" +
                    "MPAndroidChart by Philipp Jahoda is used under the Apache License 2.0. Brand assets are original.\n" +
                    "SQLCipher is provided by Zetetic under the BSD-style SQLCipher Community Edition license.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
