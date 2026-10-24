package com.mizani.money.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizani.money.R
import com.mizani.money.core.model.CountryPacks
import com.mizani.money.core.model.WalletSuggestion
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniIcons
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald
import com.mizani.money.ui.theme.EmeraldDeep
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
                    IconButton(onClick = { step-- }, cd = stringResource(R.string.common_back))
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Spacer(Modifier.weight(1f))
                StepDots(current = step, total = 4)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(Spacing.sm))

            // Horizontal slide between steps: forward slides in from the right, back from the left.
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    val sign = if (forward) 1 else -1
                    (slideInHorizontally(animationSpec = tween(280)) { full -> sign * full })
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(280)) { full -> -sign * full },
                        )
                },
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
                            currencyCode = CountryPacks.byCode(selected).currencyCode
                        },
                        onCurrencyChange = { currencyCode = it },
                        onContinue = { step = 2 },
                        onSkip = { step = 2 },
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
                        onSkip = {
                            if (walletName.isBlank()) {
                                walletName = "Cash"
                                walletType = WalletType.CASH
                            }
                            step = 3
                        },
                    )
                    else -> ReadyStep(
                        currency = currency,
                        walletName = walletName,
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

// Reusable icon button with a stable 48dp touch target.
@Composable
private fun IconButton(onClick: () -> Unit, cd: String) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.ArrowBack, contentDescription = cd)
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
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
                        else MaterialTheme.colorScheme.outline,
                    ),
            )
        }
    }
}

// A decorative illustration block: soft concentric circles with a primary Phosphor icon and
// floating secondary chips that drift gently forever. Decorative (not announced).
@Composable
private fun IllustrationArt(
    modifier: Modifier = Modifier,
    primaryRes: Int,
    secondaryRes: Int,
    tertiaryRes: Int,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val drift by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float",
    )
    val wobbleX = kotlin.math.sin(drift * Math.PI * 2).toFloat() * 10f
    val wobbleY = kotlin.math.cos(drift * Math.PI * 2).toFloat() * 8f

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        )
        Icon(
            painter = painterResource(primaryRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(92.dp),
        )
        FloatingChip(res = secondaryRes, size = 44, offsetX = (-70 + wobbleX), offsetY = (-48 + wobbleY), tint = tint)
        FloatingChip(res = tertiaryRes, size = 40, offsetX = (72 - wobbleX), offsetY = (44 - wobbleY), tint = tint)
    }
}

@Composable
private fun FloatingChip(res: Int, size: Int, offsetX: Float, offsetY: Float, tint: Color) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = with(density) { offsetX.dp.toPx() }
                translationY = with(density) { offsetY.dp.toPx() }
            }
            .size((size + 18).dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(res),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size.dp),
        )
    }
}

@Composable
private fun WelcomeStep(onGuest: () -> Unit, onCreateAccount: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IllustrationArt(
            primaryRes = R.drawable.ic_ph_wallet,
            secondaryRes = R.drawable.ic_ph_chart_donut,
            tertiaryRes = R.drawable.ic_ph_shield_check,
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MizaniMark(size = 36)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "MIZANI",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp, color = MaterialTheme.colorScheme.primary),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            ValueChip(Icons.Rounded.OfflineBolt, stringResource(R.string.onboarding_chip_offline))
            ValueChip(Icons.Rounded.Lock, stringResource(R.string.onboarding_chip_encrypted))
            ValueChip(Icons.Rounded.PersonOff, stringResource(R.string.onboarding_chip_no_signup))
        }
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onGuest,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(R.string.onboarding_continue_without_account), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(R.string.onboarding_about_accounts))
        }
    }
}

@Composable
private fun ValueChip(icon: ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CountryStep(
    countryCode: String,
    currencyCode: String,
    onCountryChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val country = CountryPacks.byCode(countryCode)
    Column(modifier = Modifier.fillMaxWidth()) {
        IllustrationArt(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            primaryRes = R.drawable.ic_ph_globe,
            secondaryRes = R.drawable.ic_ph_bank,
            tertiaryRes = R.drawable.ic_ph_coins,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.onboarding_country_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_country_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.country), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        MizaniPicker(
            selectedText = country.name,
            options = CountryPacks.all.map { PickerOption(it.code, it.name) },
            onSelect = onCountryChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.primary_currency), style = MaterialTheme.typography.labelLarge)
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
                    "Common here: ${country.mobileMoneyProviders.joinToString(", ")}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.onboarding_country_continue))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.onboarding_country_skip))
        }
    }
}

@Composable
private fun WalletStep(
    packSuggestions: List<WalletSuggestion>,
    walletName: String,
    walletType: WalletType,
    error: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (WalletType) -> Unit,
    onPickSuggestion: (WalletSuggestion) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        IllustrationArt(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            primaryRes = R.drawable.ic_ph_wallet,
            secondaryRes = R.drawable.ic_ph_coins,
            tertiaryRes = R.drawable.ic_ph_bank,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.onboarding_wallet_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_wallet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.suggestions), style = MaterialTheme.typography.labelLarge)
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
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.common_ok))
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.onboarding_wallet_name_label), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.OutlinedTextField(
            value = walletName,
            onValueChange = onNameChange,
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(R.string.onboarding_wallet_type_label), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        MizaniPicker(
            selectedText = "Type · ${walletType.label}",
            options = WalletType.entries.map { PickerOption(it.name, it.label) },
            onSelect = { id -> onTypeChange(WalletType.valueOf(id)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.onboarding_wallet_continue))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.onboarding_wallet_skip))
        }
    }
}

@Composable
private fun ReadyStep(
    currency: Currency,
    walletName: String,
    balanceText: String,
    onBalanceChange: (String) -> Unit,
    saving: Boolean,
    error: String?,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        IllustrationArt(
            primaryRes = R.drawable.ic_ph_shield_check,
            secondaryRes = R.drawable.ic_ph_check_circle,
            tertiaryRes = R.drawable.ic_ph_sparkle,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.onboarding_ready_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_ready_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxWidth().padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MizaniIcons.wallet(WalletType.CASH), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.md))
                    Text(walletName.ifBlank { WalletType.CASH.label }, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.md))
                MizaniAmountField(
                    value = balanceText,
                    onValueChange = onBalanceChange,
                    label = stringResource(R.string.onboarding_ready_balance_label),
                    currency = currency,
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.onboarding_ready_encrypted),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onFinish,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(if (saving) stringResource(R.string.onboarding_ready_preparing) else stringResource(R.string.onboarding_ready_start))
        }
    }
}
