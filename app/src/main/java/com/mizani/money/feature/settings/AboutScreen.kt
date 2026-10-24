package com.mizani.money.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mizani.money.BuildConfig
import com.mizani.money.core.legal.LegalDocumentId
import com.mizani.money.core.model.AppInfo
import com.mizani.money.data.repo.UpdateCheckResult
import com.mizani.money.data.repo.UpdateRepository
import com.mizani.money.feature.legal.LegalDocumentsDialog
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniInfoRow
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.MizaniSectionCard
import com.mizani.money.ui.components.MizaniSectionDivider
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.Spacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(state: AppUiState, onBack: () -> Unit) {
    var legalDocument by rememberSaveable { mutableStateOf<LegalDocumentId?>(null) }
    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var updateMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var releasePageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateRepository = remember { UpdateRepository() }
    val installedFromPlay = remember(context) { isInstalledFromPlay(context) }

    fun checkForUpdates() {
        if (isCheckingForUpdate) return
        isCheckingForUpdate = true
        updateMessage = null
        releasePageUrl = null
        scope.launch {
            try {
                when (val result = withContext(Dispatchers.IO) {
                    updateRepository.checkForUpdate(BuildConfig.VERSION_CODE)
                }) {
                    UpdateCheckResult.NoReleases -> updateMessage = "No release is available yet."
                    UpdateCheckResult.UpToDate -> updateMessage = "You’re up to date."
                    is UpdateCheckResult.Available -> {
                        updateMessage = "Version ${result.release.versionName} is available."
                        releasePageUrl = result.release.pageUrl
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateMessage = "Couldn’t check for updates. Try again later."
            } finally {
                isCheckingForUpdate = false
            }
        }
    }

    fun openReleasePage() {
        val url = releasePageUrl ?: return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            updateMessage = "No browser is available to open the release."
        }
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "About & privacy", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MizaniMark(size = 56)
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(AppInfo.NAME, style = MaterialTheme.typography.headlineSmall)
                    Text(AppInfo.TAGLINE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Version ${AppInfo.VERSION}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Download,
                    title = if (installedFromPlay) "App updates" else "Check for updates",
                    subtitle = when {
                        installedFromPlay -> "Google Play manages updates for this app."
                        isCheckingForUpdate -> "Checking for a new version…"
                        else -> "Version ${AppInfo.VERSION}"
                    },
                    onClick = if (installedFromPlay || isCheckingForUpdate) null else ::checkForUpdates,
                )
                if (updateMessage != null) {
                    MizaniSectionDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            updateMessage.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (releasePageUrl != null) {
                            TextButton(onClick = ::openReleasePage) { Text("View release") }
                        }
                    }
                }
            }

            MizaniCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Your privacy", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Your records stay on this phone. Rate and update checks never include transaction details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MizaniCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Backups & exports", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "CSV and Excel files are unencrypted. Backups use your passphrase. Uninstalling may erase local data, " +
                        "so create a backup first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.settings.lockEnabled) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text("App lock is on.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            MizaniCard {
                Text("A money-tracking tool", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Mizani is not a bank, lender, investment adviser or tax service.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MizaniSectionCard {
                MizaniInfoRow(
                    icon = Icons.Rounded.Description,
                    title = "Terms of Service",
                    subtitle = "How to use Mizani",
                    onClick = { legalDocument = LegalDocumentId.TERMS },
                )
                MizaniSectionDivider()
                MizaniInfoRow(
                    icon = Icons.Rounded.Shield,
                    title = "Privacy Notice",
                    subtitle = "How your information is handled",
                    onClick = { legalDocument = LegalDocumentId.PRIVACY },
                )
            }

        }
    }
    legalDocument?.let { document ->
        LegalDocumentsDialog(initialDocument = document, onClose = { legalDocument = null })
    }
}

@Suppress("DEPRECATION")
private fun isInstalledFromPlay(context: android.content.Context): Boolean {
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
    } else {
        context.packageManager.getInstallerPackageName(context.packageName)
    }
    return installer == "com.android.vending"
}
