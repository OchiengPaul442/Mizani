package com.mizani.money

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mizani.money.feature.lock.LockScreen
import com.mizani.money.feature.legal.LegalConsentScreen
import com.mizani.money.feature.onboarding.OnboardingScreen
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.MizaniNavHost
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald
import com.mizani.money.ui.theme.MizaniTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels {
        AppViewModel.factory((application as MizaniApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Financial records must not appear in screenshots, screen recordings, or recents previews.
        setSecureWindow()
        enableEdgeToEdge()
        setContent {
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            MizaniTheme(themeMode = state.settings.themeMode) {
                MizaniRoot(appViewModel, state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reassert after returning from external activities or system UI.
        setSecureWindow()
    }

    private fun setSecureWindow() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }

    override fun onStart() {
        super.onStart()
        appViewModel.onForeground()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            appViewModel.onBackgrounded()
        }
    }
}

@Composable
fun MizaniRoot(vm: AppViewModel, state: AppUiState) {
    var acceptingLegal by remember { mutableStateOf(false) }
    var legalError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // Wrap in a Surface so non-Scaffold screens (splash, onboarding, lock) inherit the
    // theme's contentColor. Without this, LocalContentColor defaults to black in dark mode,
    // which fails contrast on the dark page (1.25:1 on #0B2028).
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        when {
            state.loading -> SplashScreen()
            state.startupError -> StartupErrorScreen(onRetry = vm::retryStartup)
            !state.settings.onboardingComplete || state.snapshot.workspace == null -> {
                OnboardingScreen(vm)
            }
            state.locked -> LockScreen(vm, state.settings.biometricEnabled)
            !state.settings.hasAcceptedCurrentLegalDocuments -> {
                LegalConsentScreen(
                    accepting = acceptingLegal,
                    error = legalError,
                    onAgree = {
                        if (!acceptingLegal) {
                            acceptingLegal = true
                            legalError = null
                            scope.launch {
                                val result = vm.acceptCurrentLegalDocuments()
                                acceptingLegal = false
                                if (result.isFailure) {
                                    legalError = "Could not save your agreement. Please try again."
                                }
                            }
                        }
                    },
                )
            }
            else -> MizaniNavHost(vm, state)
        }
    }
}

@Composable
private fun StartupErrorScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Couldn't open your saved data",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Your records are still on this device. Try again to reopen Mizani.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun SplashScreen() {
    // A brand gradient backdrop with a breathing mark, name, tagline and a thin progress bar.
    // No artificial delay: the splash is dismissed the moment the ViewModel's state is ready.
    val transition = rememberInfiniteTransition(label = "splash-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash-pulse",
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "splash-shimmer",
    )
    val loadingLabel = stringResource(R.string.splash_loading)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepTeal, Emerald))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MizaniMark(size = 96).let { mark ->
                // Overlay a subtle pulse via a wrapper that scales/alpha-animates the mark.
                Box(
                    modifier = Modifier
                        .scale(pulse)
                        .alpha(0.6f + 0.4f * pulse),
                ) { mark }
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xxl))
            // Thin brand progress bar with a travelling highlight; reads as loading to screen readers.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(3.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White.copy(alpha = 0.25f))
                    .semantics { contentDescription = loadingLabel },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shimmer)
                        .height(3.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.White),
                )
            }
        }
    }
}
