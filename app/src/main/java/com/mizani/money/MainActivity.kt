package com.mizani.money

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.mizani.money.feature.lock.LockScreen
import com.mizani.money.feature.onboarding.OnboardingScreen
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.MizaniNavHost
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.theme.MizaniTheme

class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels {
        AppViewModel.factory((application as MizaniApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            MizaniTheme(themeMode = state.settings.themeMode) {
                MizaniRoot(appViewModel, state)
            }
        }
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
    when {
        state.loading -> SplashScreen()
        !state.settings.onboardingComplete || state.snapshot.workspace == null -> {
            OnboardingScreen(vm)
        }
        state.locked -> LockScreen(vm, state.settings.biometricEnabled)
        else -> MizaniNavHost(vm, state)
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MizaniMark(size = 84)
            Spacer(Modifier.height(Spacing.lg))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))
            // Announced to screen readers so the blank wait reads as loading, not a dead end.
            val loadingLabel = stringResource(R.string.splash_loading)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = loadingLabel },
                strokeWidth = 2.dp,
            )
        }
    }
}
