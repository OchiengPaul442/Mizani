package com.mizani.money.feature.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mizani.money.R
import com.mizani.money.core.security.PinDefaults
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.PIN_RETRY_MESSAGE
import com.mizani.money.ui.PinAttemptResult
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.Spacing
import kotlinx.coroutines.delay

@Composable
fun LockScreen(vm: AppViewModel, biometricEnabled: Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    // While > 0 the keypad stays disabled, so verification cannot be invoked
    // again during the backoff; the message is static (no countdown).
    var blockedForMs by remember { mutableStateOf(0L) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBiometrics = biometricEnabled && activity != null &&
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

    // Resolved in composition so the non-composable callback/effects can use them.
    val pinErrorText = stringResource(R.string.lock_error_pin)
    val biometricUnavailableText = stringResource(R.string.lock_biometric_unavailable)

    val callback = remember {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                vm.onBiometricUnlocked()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                biometricMessage = errString.toString()
            }
        }
    }
    val prompt = remember {
        activity?.let {
            BiometricPrompt(it, ContextCompat.getMainExecutor(it), callback)
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == PinDefaults.LENGTH && !verifying) {
            verifying = true
            when (val result = vm.attemptUnlockWithPin(pin)) {
                is PinAttemptResult.WrongPin -> {
                    error = pinErrorText
                    delay(280)
                    pin = ""
                }
                is PinAttemptResult.Blocked -> {
                    // Static retry message; disabling the keypad below stops any
                    // further verification attempts until the window expires.
                    error = PIN_RETRY_MESSAGE
                    blockedForMs = result.remainingMs
                    pin = ""
                }
                is PinAttemptResult.Success -> Unit
            }
            verifying = false
        }
    }

    // Backoff state persists in DataStore, so re-check it on first composition
    // (e.g. the process was restarted mid-block).
    LaunchedEffect(Unit) {
        val remaining = vm.pinBackoffRemainingMs()
        if (remaining > 0L) {
            blockedForMs = remaining
            error = PIN_RETRY_MESSAGE
        }
    }

    LaunchedEffect(blockedForMs) {
        if (blockedForMs > 0L) {
            delay(blockedForMs)
            blockedForMs = 0L
            error = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primaryContainer),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.xl))
            MizaniMark(size = 72)
            Spacer(Modifier.height(Spacing.lg))
            Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Enter your 6-digit PIN to open Mizani",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            PinDots(entered = pin.length)
            Spacer(Modifier.height(Spacing.md))
            // heightIn (not a fixed height) so large-font users never lose the message;
            // liveRegion announces failures and biometric status without stealing focus.
            Box(Modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
                when {
                    verifying -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    error != null -> Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    biometricMessage != null -> Text(
                        biometricMessage!!,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            PinKeypad(
                onDigit = { digit ->
                    if (pin.length < PinDefaults.LENGTH) {
                        error = null
                        biometricMessage = null
                        pin += digit
                    }
                },
                onBackspace = { pin = pin.dropLast(1) },
                enabled = !verifying && blockedForMs <= 0L,
            )
            Spacer(Modifier.height(Spacing.lg))
            if (canUseBiometrics && prompt != null) {
                OutlinedButton(
                    onClick = {
                        biometricMessage = null
                        try {
                            prompt.authenticate(
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Unlock Mizani")
                                    .setSubtitle("Use your fingerprint or face")
                                    .setNegativeButtonText("Use PIN")
                                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                    .build(),
                            )
                        } catch (error: Exception) {
                            biometricMessage = biometricUnavailableText
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Unlock with biometrics")
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(
                    "If you forget your PIN, local data cannot be recovered without your encrypted backup.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun PinDots(entered: Int, length: Int = PinDefaults.LENGTH) {
    // Dots are read as progress ("3 of 6 digits entered"), never digit by digit.
    val progressLabel = stringResource(R.string.lock_pin_digits, entered, length)
    Row(
        modifier = Modifier.semantics { contentDescription = progressLabel },
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        repeat(length) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < entered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .then(
                        if (index < entered) Modifier
                        else Modifier.background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    ),
            )
        }
    }
}

@Composable
fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean = true,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(68.dp))
                        "⌫" -> Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable(enabled = enabled, onClick = onBackspace),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Backspace,
                                contentDescription = "Delete last digit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(enabled = enabled) { onDigit(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                key,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var stage by remember { mutableStateOf(0) }
    var first by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit(pin: String) {
        if (stage == 0) {
            first = pin
            current = ""
            stage = 1
        } else {
            if (pin == first) {
                onConfirm(pin)
            } else {
                error = "PINs do not match. Start again."
                first = ""
                current = ""
                stage = 0
            }
        }
    }

    LaunchedEffect(current) {
        if (current.length == PinDefaults.LENGTH) {
            delay(80)
            submit(current)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (stage == 0) "Create a 6-digit PIN" else "Confirm your PIN",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Mizani will ask for this when the app opens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.lg))
                PinDots(entered = current.length)
                Spacer(Modifier.height(Spacing.md))
                Box(Modifier.heightIn(min = 20.dp), contentAlignment = Alignment.Center) {
                    if (error != null) {
                        Text(
                            error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                PinKeypad(
                    onDigit = { if (current.length < PinDefaults.LENGTH) current += it },
                    onBackspace = { current = current.dropLast(1) },
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun PinVerifyDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pin) {
        if (pin.length == PinDefaults.LENGTH) {
            delay(80)
            if (pin.length == PinDefaults.LENGTH) onSubmit(pin)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.lg))
                PinDots(entered = pin.length)
                Spacer(Modifier.height(Spacing.md))
                Box(Modifier.heightIn(min = 20.dp), contentAlignment = Alignment.Center) {
                    if (error != null) {
                        Text(
                            error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                PinKeypad(
                    onDigit = { if (pin.length < PinDefaults.LENGTH) pin += it },
                    onBackspace = { pin = pin.dropLast(1) },
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
