package com.mizani.money.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizani.money.R
import com.mizani.money.core.model.CountryPacks
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.MizaniIcons
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald
import com.mizani.money.ui.theme.Mint
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var countryCode by rememberSaveable { mutableStateOf("UG") }
    var currencyCode by rememberSaveable { mutableStateOf("UGX") }
    var walletName by rememberSaveable { mutableStateOf("Cash") }
    var walletType by rememberSaveable { mutableStateOf(WalletType.CASH) }
    var balanceText by rememberSaveable { mutableStateOf("") }
    var showAccountDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pack = CountryPacks.byCode(countryCode)
    val currency = Currencies.byCode(currencyCode)
    // Resolved here (composable context) so the plain local `finish` closure can use them.
    val balanceErrorText = stringResource(R.string.onboarding_error_balance)
    val walletNameErrorText = stringResource(R.string.onboarding_error_wallet_name)
    val workspaceErrorText = stringResource(R.string.onboarding_error_workspace)

    fun finish() {
        val opening = if (balanceText.isBlank()) 0L else {
            MoneyFormat.parse(balanceText, currency) ?: run {
                error = balanceErrorText
                return
            }
        }
        saving = true
        error = null
        scope.launch {
            val result = vm.completeOnboarding(
                countryCode = countryCode,
                currencyCode = currencyCode,
                walletName = walletName,
                walletType = walletType,
                openingBalanceMinor = opening,
            )
            saving = false
            result.exceptionOrNull()?.let { error = it.message ?: workspaceErrorText }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainerLow),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Spacer(Modifier.weight(1f))
                StepDots(current = step, total = 4)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(Spacing.lg))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding",
            ) { current ->
                when (current) {
                    0 -> WelcomeStep(
                        onGuest = { step = 1 },
                        onCreateAccount = { showAccountDialog = true },
                    )
                    1 -> CountryStep(
                        countryCode = countryCode,
                        currencyCode = currencyCode,
                        onCountryChange = { selected ->
                            countryCode = selected
                            val nextCurrency = CountryPacks.byCode(selected).currencyCode
                            currencyCode = nextCurrency
                        },
                        onCurrencyChange = { currencyCode = it },
                        onContinue = { step = 2 },
                    )
                    2 -> WalletStep(
                        packSuggestions = pack.walletSuggestions,
                        walletName = walletName,
                        walletType = walletType,
                        error = error,
                        onNameChange = { walletName = it.take(40) },
                        onTypeChange = { walletType = it },
                        onPickSuggestion = { suggestion ->
                            walletName = suggestion.name
                            walletType = suggestion.type
                        },
                        onContinue = {
                            if (walletName.isBlank()) {
                                error = walletNameErrorText
                            } else {
                                error = null
                                step = 3
                            }
                        },
                    )
                    else -> ReadyStep(
                        countryName = pack.name,
                        currency = currency,
                        walletName = walletName,
                        walletType = walletType,
                        balanceText = balanceText,
                        onBalanceChange = { input -> balanceText = input },
                        saving = saving,
                        error = error,
                        onFinish = ::finish,
                    )
                }
            }
        }
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.onboarding_accounts_title)) },
            text = { Text(stringResource(R.string.onboarding_accounts_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showAccountDialog = false
                    step = 1
                }) { Text(stringResource(R.string.onboarding_continue_without_account)) }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) { Text(stringResource(R.string.onboarding_not_now)) }
            },
        )
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    // Decorative progress indicator: announced as one description, not as N loose boxes.
    val stepDescription = stringResource(R.string.onboarding_step_of, current + 1, total)
    Row(
        modifier = Modifier.semantics { contentDescription = stepDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (index == current) 22.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun WelcomeStep(onGuest: () -> Unit, onCreateAccount: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(DeepTeal, Emerald, Color(0xFF0F5A57))))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MizaniMark(size = 56)
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    "MIZANI",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                    color = Mint,
                )
                Text(
                    "Personal money, offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Your money, in balance.",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            maxLines = 2,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Understand your money, even offline. Cash, mobile money and bank accounts in one calm place — " +
                "built for income that does not always arrive on the same day.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(20.dp))
        FeatureLine(Icons.Rounded.OfflineBolt, "Works without internet, from the first screen")
        Spacer(Modifier.height(Spacing.sm))
        FeatureLine(Icons.Rounded.PersonOff, "No account needed. Your data stays on this phone")
        Spacer(Modifier.height(Spacing.sm))
        FeatureLine(Icons.Rounded.Lock, "Encrypted local database and optional app lock")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGuest,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepTeal),
        ) {
            Text(stringResource(R.string.onboarding_continue_without_account), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
        ) {
            // Opens an explanation, not a sign-up flow: the label must not promise one.
            Text(stringResource(R.string.onboarding_about_accounts))
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Guest mode is complete. You never need an account to use Mizani.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FeatureLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun CountryStep(
    countryCode: String,
    currencyCode: String,
    onCountryChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val country = CountryPacks.byCode(countryCode)
    Column {
        Text("Where do you keep your money?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Sets your wallet suggestions and primary currency. You can change these later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Text("Country", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        MizaniPicker(
            selectedText = country.name,
            options = CountryPacks.all.map { PickerOption(it.code, it.name) },
            onSelect = onCountryChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text("Primary currency", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        MizaniPicker(
            selectedText = "${Currencies.byCode(currencyCode).code} · ${Currencies.byCode(currencyCode).name}",
            options = Currencies.all.map { PickerOption(it.code, "${it.code} · ${it.name}") },
            onSelect = onCurrencyChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Icon(MizaniIcons.wallet(WalletType.MOBILE_MONEY), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    "Common here: ${country.mobileMoneyProviders.joinToString(", ")}. You can add any wallet yourself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.xxl))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.small) {
            Text("Continue")
        }
    }
}

@Composable
private fun WalletStep(
    packSuggestions: List<com.mizani.money.core.model.WalletSuggestion>,
    walletName: String,
    walletType: WalletType,
    error: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (WalletType) -> Unit,
    onPickSuggestion: (com.mizani.money.core.model.WalletSuggestion) -> Unit,
    onContinue: () -> Unit,
) {
    Column {
        Text("Add your first wallet", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Any place your money lives — cash, mobile money or a bank account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Text("Suggestions", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(Spacing.sm))
        packSuggestions.forEach { suggestion ->
            val selected = walletName == suggestion.name && walletType == suggestion.type
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onPickSuggestion(suggestion) },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        MizaniIcons.wallet(suggestion.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(suggestion.name, style = MaterialTheme.typography.titleSmall)
                        Text(suggestion.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected")
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        OutlinedTextField(
            value = walletName,
            onValueChange = onNameChange,
            label = { Text("Wallet name") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        MizaniPicker(
            selectedText = "Type · ${walletType.label}",
            options = WalletType.entries.map { PickerOption(it.name, it.label) },
            onSelect = { id -> onTypeChange(WalletType.valueOf(id)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(Spacing.md))
            // Announced when it appears so validation is not visual-only.
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.small) {
            Text("Continue")
        }
    }
}

@Composable
private fun ReadyStep(
    countryName: String,
    currency: Currency,
    walletName: String,
    walletType: WalletType,
    balanceText: String,
    onBalanceChange: (String) -> Unit,
    saving: Boolean,
    error: String?,
    onFinish: () -> Unit,
) {
    Column {
        Text("Start from where you are", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Enter the current balance of this wallet if you know it. Skipping is fine — you can add it later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxWidth().padding(Spacing.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MizaniIcons.wallet(walletType), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(walletName.ifBlank { walletType.label }, style = MaterialTheme.typography.titleMedium)
                        Text("${walletType.label} · ${currency.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                MizaniAmountField(
                    value = balanceText,
                    onValueChange = onBalanceChange,
                    label = "Starting balance (optional)",
                    currency = currency,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Country: $countryName · Currency: ${currency.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(Spacing.md))
            // Announced when it appears so validation is not visual-only.
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Your information is stored in an encrypted database on this device. Uninstalling the app without an " +
                "export removes guest data permanently.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onFinish,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(if (saving) "Preparing your workspace…" else "Start using Mizani")
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "You can leave the balance blank and add it later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
