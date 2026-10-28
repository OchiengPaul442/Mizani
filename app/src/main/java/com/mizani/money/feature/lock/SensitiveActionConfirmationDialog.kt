package com.mizani.money.feature.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mizani.money.core.security.PinDefaults
import com.mizani.money.ui.PIN_RETRY_MESSAGE
import com.mizani.money.ui.components.Spacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fresh, per-action PIN or app-enabled biometric confirmation before sensitive data leaves Mizani. */
@Composable
fun SensitiveActionConfirmationDialog(
    actionDescription: String,
    allowBiometrics: Boolean,
    verifyPin: suspend (String) -> Boolean,
    pinBackoffRemainingMs: suspend () -> Long,
    onAuthorized: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }
    var verificationPhase by remember { mutableStateOf("Verifying PIN") }
    var blockedForMs by remember { mutableStateOf(0L) }
    var actionCompleted by remember { mutableStateOf(false) }
    var pinAccepted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val pinScope = rememberCoroutineScope()
    val currentOnAuthorized = rememberUpdatedState(onAuthorized)
    val currentOnDismiss = rememberUpdatedState(onDismiss)

    val callback = remember(activity) {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (!actionCompleted) {
                    actionCompleted = true
                    currentOnAuthorized.value()
                }
            }

            override fun onAuthenticationFailed() {
                biometricMessage = "Biometric not recognized. Try again or enter your PIN."
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    biometricMessage = null
                } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    biometricMessage = "Biometric confirmation did not finish. Enter your PIN or try again."
                }
            }
        }
    }
    val prompt = remember(activity, callback) {
        activity?.let { BiometricPrompt(it, ContextCompat.getMainExecutor(it), callback) }
    }

    DisposableEffect(prompt) {
        onDispose { prompt?.cancelAuthentication() }
    }

    LaunchedEffect(Unit) {
        val remaining = runCatching { withContext(Dispatchers.IO) { pinBackoffRemainingMs() } }
            .getOrDefault(0L)
        if (remaining > 0L) {
            blockedForMs = remaining
            message = PIN_RETRY_MESSAGE
        }
    }

    LaunchedEffect(blockedForMs) {
        if (blockedForMs > 0L) {
            delay(blockedForMs)
            blockedForMs = 0L
            message = null
        }
    }

    LaunchedEffect(pinAccepted) {
        if (pinAccepted) currentOnAuthorized.value()
    }

    fun submitPin(candidate: String) {
        if (verifying || blockedForMs > 0L) return
        verifying = true
        verificationPhase = "Verifying PIN"
        biometricMessage = null
        pinScope.launch {
            val result = try {
                Result.success(withContext(Dispatchers.IO) { verifyPin(candidate) })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
            if (result.getOrDefault(false)) {
                actionCompleted = true
                verifying = false
                pinAccepted = true
            } else {
                verificationPhase = "Checking PIN retry status"
                val remaining = try {
                    withContext(Dispatchers.IO) { pinBackoffRemainingMs() }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    0L
                }
                if (remaining > 0L) {
                    blockedForMs = remaining
                    message = PIN_RETRY_MESSAGE
                } else {
                    message = if (result.isFailure) {
                        "Could not verify the PIN. Try again."
                    } else {
                        "Incorrect PIN. Try again."
                    }
                }
                pin = ""
                verifying = false
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!actionCompleted) currentOnDismiss.value()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOn,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Confirm your identity", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    if (allowBiometrics && prompt != null) {
                        "Confirm to $actionDescription with your app PIN or enabled biometrics."
                    } else {
                        "Enter your 6-digit app PIN to $actionDescription."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (allowBiometrics && prompt != null) {
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedButton(
                        onClick = {
                            biometricMessage = null
                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Confirm your identity")
                                .setSubtitle("Confirm to $actionDescription or choose PIN.")
                                .setNegativeButtonText("Use PIN")
                                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                .build()
                            prompt.authenticate(promptInfo)
                        },
                        enabled = !verifying,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Use biometrics")
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                PinDots(entered = pin.length)
                Spacer(Modifier.height(Spacing.sm))
                Column(
                    modifier = Modifier.heightIn(min = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    message?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    biometricMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (verifying) {
                    Spacer(Modifier.height(Spacing.sm))
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = verificationPhase },
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                PinKeypad(
                    onDigit = {
                        if (pin.length < PinDefaults.LENGTH && !verifying && blockedForMs == 0L) {
                            val updatedPin = pin + it
                            pin = updatedPin
                            message = null
                            biometricMessage = null
                            if (updatedPin.length == PinDefaults.LENGTH) submitPin(updatedPin)
                        }
                    },
                    onBackspace = {
                        pin = pin.dropLast(1)
                        message = null
                    },
                    enabled = !verifying && blockedForMs == 0L,
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(
                    onClick = { if (!actionCompleted) currentOnDismiss.value() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Cancel") }
            }
        }
    }
}
