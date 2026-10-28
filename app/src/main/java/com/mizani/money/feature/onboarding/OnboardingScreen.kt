package com.mizani.money.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.model.CountryPacks
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
import com.mizani.money.ui.components.StorysetIllustration
import com.mizani.money.feature.legal.LegalDocumentsDialog
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    vm: AppViewModel,
    hasAcceptedCurrentLegalDocuments: Boolean,
) {
    var step by rememberSaveable(hasAcceptedCurrentLegalDocuments) {
        mutableIntStateOf(if (hasAcceptedCurrentLegalDocuments) 2 else 0)
    }
    var acceptedCurrentDocumentsThisSession by remember { mutableStateOf(false) }
    val deviceCountry = LocalConfiguration.current.locales[0]?.country.orEmpty()
    val suggestedCountry = CountryPacks.forDeviceRegion(deviceCountry)
    var countryCode by rememberSaveable { mutableStateOf(suggestedCountry.code) }
    var currencyCode by rememberSaveable { mutableStateOf(suggestedCountry.currencyCode) }
    var walletName by rememberSaveable { mutableStateOf("Cash") }
    var walletType by rememberSaveable { mutableStateOf(WalletType.CASH) }
    var balanceText by rememberSaveable { mutableStateOf("") }
    var legalDocument by rememberSaveable { mutableStateOf<LegalDocumentId?>(null) }
    var acceptingLegal by rememberSaveable { mutableStateOf(false) }
    var legalError by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pack = CountryPacks.byCode(countryCode)
    val currency = Currencies.byCode(currencyCode)
    val currentDocumentsAccepted = hasAcceptedCurrentLegalDocuments || acceptedCurrentDocumentsThisSession
    val activeStep = if (currentDocumentsAccepted) step.coerceAtLeast(2) else step.coerceIn(0, 1)
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
                ?: run {
                    acceptedCurrentDocumentsThisSession = true
                    step = 2
                }
        }
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
                val showBack = activeStep > 0 && (!currentDocumentsAccepted || activeStep > 2)
                if (showBack) {
                    IconButton(onClick = { step = activeStep - 1 }, cd = stringResource(R.string.common_back))
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Spacer(Modifier.weight(1f))
                StepDots(
                    current = if (currentDocumentsAccepted) activeStep - 2 else activeStep,
                    total = if (currentDocumentsAccepted) 2 else 4,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(Spacing.sm))

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).weight(1f),
            ) {
                // Every page moves inside the same viewport. Equal-duration fades and slides,
                // plus fixed-height page containers, keep form content from jumping between steps.
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = activeStep,
                    contentAlignment = Alignment.TopCenter,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (
                            fadeIn(tween(180)) + slideInHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                            ) { width -> direction * width / 5 }
                        ).togetherWith(
                            fadeOut(tween(180)) + slideOutHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                            ) { width -> -direction * width / 5 },
                        ).using(SizeTransform(clip = true) { _, _ -> tween(0) })
                    },
                    label = "onboarding",
                ) { current ->
                    when (current) {
                        0, 1 -> WelcomeStep(
                            page = current,
                            onNext = { step = 1 },
                            onAccept = ::acceptLegalAndContinue,
                            accepting = acceptingLegal,
                            legalError = legalError,
                            onOpenLegal = { legalDocument = it },
                        )
                        2 -> OnboardingStepViewport(height = maxHeight) {
                            MoneySetupStep(
                                pack = pack,
                                countryCode = countryCode,
                                currencyCode = currencyCode,
                                walletName = walletName,
                                walletType = walletType,
                                error = error,
                                onCountryChange = { selected ->
                                    countryCode = selected
                                    currencyCode = CountryPacks.byCode(selected).currencyCode
                                },
                                onCurrencyChange = { currencyCode = it },
                                onNameChange = { walletName = it.take(40) },
                                onTypeChange = { walletType = it },
                                onPickSuggestion = { name -> walletName = name },
                                onContinue = {
                                    if (walletName.isBlank()) {
                                        error = walletNameErrorText
                                    } else {
                                        error = null
                                        step = 3
                                    }
                                },
                            )
                        }
                        else -> OnboardingStepViewport(height = maxHeight) {
                            ReadyStep(
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
        }
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

@Composable
private fun OnboardingStepViewport(
    height: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = height),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun WelcomeStep(
    page: Int,
    onNext: () -> Unit,
    onAccept: () -> Unit,
    accepting: Boolean,
    legalError: String?,
    onOpenLegal: (LegalDocumentId) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val viewportHeight = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(224.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(212.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)),
                        )
                        Box(
                            Modifier.size(164.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)),
                        )
                        StorysetIllustration(
                            assetName = if (page == 0) "finance_cuate.svg" else "financial_data_cuate.svg",
                            modifier = Modifier.fillMaxSize().padding(Spacing.xs),
                        )
                    }
                    Spacer(Modifier.height(Spacing.md))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(
                                if (page == 0) R.string.onboarding_welcome_title
                                else R.string.onboarding_analysis_title,
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        stringResource(
                            if (page == 0) R.string.onboarding_welcome_subtitle
                            else R.string.onboarding_analysis_subtitle,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(
                    if (page == 0) R.string.onboarding_review_legal
                    else R.string.onboarding_agree_legal,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onOpenLegal(LegalDocumentId.TERMS) }) { Text("Terms of Service") }
            TextButton(onClick = { onOpenLegal(LegalDocumentId.PRIVACY) }) { Text("Privacy Notice") }
        }
        Button(
            onClick = if (page == 0) onNext else onAccept,
            enabled = page == 0 || !accepting,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            if (page == 1 && accepting) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                stringResource(
                    if (page == 0) R.string.onboarding_next
                    else R.string.onboarding_agree_setup,
                ),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            legalError?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun MoneySetupStep(
    pack: com.mizani.money.core.model.CountryPack,
    countryCode: String,
    currencyCode: String,
    walletName: String,
    walletType: WalletType,
    error: String?,
    onCountryChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (WalletType) -> Unit,
    onPickSuggestion: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val country = CountryPacks.byCode(countryCode)
    val currency = Currencies.byCode(currencyCode)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.onboarding_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_setup_subtitle),
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
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(R.string.primary_currency), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        MizaniPicker(
            selectedText = "${currency.code} | ${currency.name}",
            options = Currencies.all.map { PickerOption(it.code, "${it.code} | ${it.name}") },
            onSelect = onCurrencyChange,
            searchable = true,
            searchHint = "Search currencies",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.onboarding_currency_explainer, currency.code),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            stringResource(R.string.onboarding_wallet_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.onboarding_wallet_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(R.string.onboarding_wallet_type_label), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        MizaniPicker(
            selectedText = walletType.label,
            options = WalletType.entries.map { PickerOption(it.name, it.label) },
            onSelect = { id -> onTypeChange(WalletType.valueOf(id)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        androidx.compose.material3.OutlinedTextField(
            value = walletName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.onboarding_wallet_name_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        val nameIdeas = pack.walletSuggestions.filter { it.type == walletType }
        if (nameIdeas.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(stringResource(R.string.onboarding_wallet_name_ideas), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(Spacing.xs))
            Column(Modifier.selectableGroup()) {
                nameIdeas.chunked(2).forEach { suggestionsRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        suggestionsRow.forEach { suggestion ->
                            val selected = walletName == suggestion.name
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { onPickSuggestion(suggestion.name) },
                                    ),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        suggestion.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 2,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (selected) Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        if (suggestionsRow.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(Spacing.xs))
                }
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
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.onboarding_country_continue))
        }
    }
}

@Composable
private fun ReadyStep(
    currency: Currency,
    walletName: String,
    walletType: WalletType,
    balanceText: String,
    onBalanceChange: (String) -> Unit,
    saving: Boolean,
    error: String?,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(224.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(212.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)),
            )
            Box(
                Modifier.size(164.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)),
            )
            StorysetIllustration(
                assetName = "saving_money_cuate.svg",
                modifier = Modifier.fillMaxSize().padding(Spacing.xs),
                contentDescription = stringResource(R.string.onboarding_ready_illustration_description),
            )
        }
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
                    Icon(MizaniIcons.wallet(walletType), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
