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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.PersonOff
import com.mizani.money.ui.components.MizaniAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mizani.money.R
import com.mizani.money.core.model.CountryPacks
import com.mizani.money.core.model.WalletSuggestion
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.legal.LegalDocumentId
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniIcons
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import com.mizani.money.feature.legal.LegalDocumentsDialog
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
    var legalDocument by rememberSaveable { mutableStateOf<LegalDocumentId?>(null) }
    var acceptingLegal by rememberSaveable { mutableStateOf(false) }
    var legalError by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pack = CountryPacks.byCode(countryCode)
    val currency = Currencies.byCode(currencyCode)
    val onboardingScrollState = rememberScrollState()
    val balanceErrorText = stringResource(R.string.onboarding_error_balance)
    val walletNameErrorText = stringResource(R.string.onboarding_error_wallet_name)
    val workspaceErrorText = stringResource(R.string.onboarding_error_workspace)

    fun acceptLegalAndContinue() {
        if (acceptingLegal) return
        acceptingLegal = true
        legalError = null
        scope.launch {
            val result = vm.acceptCurrentLegalDocuments()
            acceptingLegal = false
            result.exceptionOrNull()?.let { legalError = "Could not save your agreement. Please try again." }
                ?: run { step = 1 }
        }
    }

    LaunchedEffect(step) {
        onboardingScrollState.scrollTo(0)
    }

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
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
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

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).weight(1f),
            ) {
                // Short steps sit in the visual center. Longer steps remain scrollable on
                // compact screens and while the keyboard is open.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .verticalScroll(onboardingScrollState),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Horizontal slide between steps: forward slides in from the right, back from the left.
                    AnimatedContent(
                        modifier = Modifier.fillMaxWidth(),
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
                                onAccept = ::acceptLegalAndContinue,
                                accepting = acceptingLegal,
                                legalError = legalError,
                                onOpenLegal = { legalDocument = it },
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
        }
    }

    if (showAccountDialog) {
        MizaniAlertDialog(
            onDismissRequest = { showAccountDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.onboarding_accounts_title)) },
            text = { Text(stringResource(R.string.onboarding_accounts_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showAccountDialog = false
                    acceptLegalAndContinue()
                }) { Text("Agree and continue") }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) { Text(stringResource(R.string.onboarding_not_now)) }
            },
        )
    }
    legalDocument?.let { document ->
        LegalDocumentsDialog(initialDocument = document, onClose = { legalDocument = null })
    }
}

// Reusable icon button with a stable 48dp touch target.
@Composable
private fun IconButton(onClick: () -> Unit, cd: String) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = cd)
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
    size: androidx.compose.ui.unit.Dp = 200.dp,
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
    val scale = size.value / 200f
    val wobbleX = kotlin.math.sin(drift * Math.PI * 2).toFloat() * 10f * scale
    val wobbleY = kotlin.math.cos(drift * Math.PI * 2).toFloat() * 8f * scale

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .size(size * 0.75f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        )
        Icon(
            painter = painterResource(primaryRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.46f),
        )
        FloatingChip(res = secondaryRes, size = (44 * scale).toInt(), offsetX = (-70 * scale + wobbleX), offsetY = (-48 * scale + wobbleY), tint = tint)
        FloatingChip(res = tertiaryRes, size = (40 * scale).toInt(), offsetX = (72 * scale - wobbleX), offsetY = (44 * scale - wobbleY), tint = tint)
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
private fun WelcomeStep(
    onAccept: () -> Unit,
    accepting: Boolean,
    legalError: String?,
    onOpenLegal: (LegalDocumentId) -> Unit,
    onCreateAccount: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IllustrationArt(
            size = 180.dp,
            primaryRes = R.drawable.ic_ph_wallet,
            secondaryRes = R.drawable.ic_ph_chart_donut,
            tertiaryRes = R.drawable.ic_ph_shield_check,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
            onClick = onAccept,
            enabled = !accepting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            if (accepting) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text("Agree and continue", style = MaterialTheme.typography.titleSmall)
        }
        legalError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "By continuing, you agree to the Terms of Service and acknowledge the Privacy Notice.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onOpenLegal(LegalDocumentId.TERMS) }) { Text("Terms of Service") }
            TextButton(onClick = { onOpenLegal(LegalDocumentId.PRIVACY) }) { Text("Privacy Notice") }
        }
        Spacer(Modifier.height(4.dp))
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
            size = 156.dp,
            primaryRes = R.drawable.ic_ph_globe,
            secondaryRes = R.drawable.ic_ph_bank,
            tertiaryRes = R.drawable.ic_ph_coins,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.onboarding_country_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_country_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            size = 148.dp,
            primaryRes = R.drawable.ic_ph_wallet,
            secondaryRes = R.drawable.ic_ph_coins,
            tertiaryRes = R.drawable.ic_ph_bank,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.onboarding_wallet_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_wallet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.suggestions), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(Spacing.sm))
        packSuggestions.chunked(2).forEach { suggestionsRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                suggestionsRow.forEach { suggestion ->
                    val selected = walletName == suggestion.name && walletType == suggestion.type
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onPickSuggestion(suggestion) },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Icon(
                                MizaniIcons.wallet(suggestion.type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                suggestion.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) Icon(
                                Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.common_ok),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                if (suggestionsRow.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.xs))
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
            size = 176.dp,
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
