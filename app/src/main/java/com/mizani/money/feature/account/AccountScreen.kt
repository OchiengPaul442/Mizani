package com.mizani.money.feature.account

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
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.Spacing

@Composable
fun AccountScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "Accounts & sync", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MizaniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Works offline", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Everything in Mizani — wallets, transactions, plans and insights — runs on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text("How your data works", style = MaterialTheme.typography.titleMedium)
            ModeCard(
                icon = Icons.Rounded.PersonOff,
                title = "On this phone",
                body = "Everything lives in the encrypted database on this device, with no background " +
                    "network traffic.",
                active = true,
            )
            ModeCard(
                icon = Icons.Rounded.FileUpload,
                title = "Encrypted backup",
                body = "Save a passphrase-protected .mizani file and restore it on this or another phone. " +
                    "Anyone with the file and passphrase can read it, so keep both safe.",
                active = false,
            )
            ModeCard(
                icon = Icons.Rounded.CloudOff,
                title = "No cloud sync",
                body = "Mizani has no servers or online accounts, so nothing is ever uploaded. An encrypted " +
                    "backup file is the only way to move your data between devices.",
                active = false,
            )

            MizaniCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Our commitments", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "No advertising and no sale of transaction data. Planning needs no account, and " +
                        "local export is always available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                "Mizani has no internet permission, so your records cannot leave this phone unless you " +
                    "export them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    body: String,
    active: Boolean,
) {
    MizaniCard(
        containerColor = if (active) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (active) {
                Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
