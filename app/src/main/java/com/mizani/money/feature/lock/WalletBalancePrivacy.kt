package com.mizani.money.feature.lock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel

/** Revealing wallet amounts always requires fresh PIN or enabled biometric confirmation. */
@Composable
fun WalletBalanceVisibilityControl(
    vm: AppViewModel,
    state: AppUiState,
    onSetupPin: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    val visible = state.walletBalancesVisible
    val hasPin = state.settings.hasPin
    val description = when {
        visible -> "Hide wallet balances"
        hasPin -> "Show wallet balances; confirm with your app PIN or enabled biometrics"
        else -> "Set up an app PIN to view wallet balances"
    }

    if (confirming && hasPin) {
        SensitiveActionConfirmationDialog(
            actionDescription = "view wallet balances",
            allowBiometrics = state.settings.biometricEnabled,
            verifyPin = vm.settingsRepository::verifyPin,
            pinBackoffRemainingMs = vm.settingsRepository::pinBackoffRemainingMs,
            onAuthorized = {
                vm.revealWalletBalances()
                confirming = false
            },
            onDismiss = { confirming = false },
        )
    }

    IconButton(
        onClick = {
            when {
                visible -> vm.hideWalletBalances()
                hasPin -> confirming = true
                else -> onSetupPin()
            }
        },
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Icon(
            imageVector = when {
                visible -> Icons.Rounded.VisibilityOff
                hasPin -> Icons.Rounded.Visibility
                else -> Icons.Rounded.Lock
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Keeps hidden amounts out of both visible text and accessibility semantics. */
@Composable
fun ProtectedBalanceValue(
    amountMinor: Long,
    currency: Currency,
    visible: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
) {
    val formatted = if (visible) MoneyFormat.format(amountMinor, currency) else null
    Text(
        text = formatted ?: "••••••",
        modifier = modifier.semantics {
            contentDescription = if (formatted == null) "Balance hidden" else "Balance $formatted"
        },
        style = style,
        color = color,
        maxLines = maxLines,
    )
}
