package com.mizani.money.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.core.importing.TransactionMessageParser
import com.mizani.money.R
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.MizaniInfoButton
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MessageImportScreen(
    vm: AppViewModel,
    initialNotice: String?,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    var message by remember { mutableStateOf("") }
    var providerHint by remember { mutableStateOf<String?>(null) }
    var error by remember(initialNotice) { mutableStateOf(initialNotice) }
    var reading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "Import transaction", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Paste or share a mobile-money confirmation. Mizani reads it on this device; the original message is not saved.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MizaniInfoButton(
                    infoTitle = stringResource(R.string.info_import_privacy_title),
                    infoBody = stringResource(R.string.info_import_privacy_body),
                    contentDescription = stringResource(R.string.info_import_privacy_title),
                )
            }
            Text(
                "If the message does not name its provider, choose it here. Supported networks include MTN MoMo, Airtel Money, M-Pesa, Orange Money, Moov, Wave, Telecel, Tigo, EcoCash, OPay and more.",
                style = MaterialTheme.typography.labelLarge,
            )
            MizaniPicker(
                selectedText = providerHint?.let { id ->
                    TransactionMessageParser.providerOptions.firstOrNull { it.id == id }?.label
                } ?: "Provider detected automatically",
                options = TransactionMessageParser.providerOptions.map { PickerOption(it.id, it.label) },
                onSelect = { providerHint = it },
                noneLabel = "Detect automatically",
                onSelectNone = { providerHint = null },
                searchable = true,
                searchHint = "Search mobile-money networks",
                fieldLabel = "Provider hint (optional)",
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = message,
                onValueChange = { value ->
                    if (value.length <= TransactionMessageParser.MAX_MESSAGE_LENGTH) message = value
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 10,
                label = { Text("Transaction message") },
                placeholder = { Text("Paste the confirmation text") },
                supportingText = { Text("${message.length}/${TransactionMessageParser.MAX_MESSAGE_LENGTH}") },
                leadingIcon = { Icon(Icons.Rounded.ContentPaste, contentDescription = null) },
                isError = error != null,
            )
            if (error != null) {
                Text(
                    error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = {
                    if (reading) return@Button
                    reading = true
                    error = null
                    scope.launch {
                        val result = vm.prepareTransactionImport(message, providerHint)
                        // Room may resume the suspend call on its transaction executor. Return to
                        // the UI dispatcher before mutating Compose state or navigating.
                        withContext(Dispatchers.Main.immediate) {
                            result
                                .onSuccess {
                                    message = ""
                                    onReview()
                                }
                                .onFailure { error = it.message ?: "Could not read this transaction message." }
                            reading = false
                        }
                    }
                },
                enabled = message.isNotBlank() && !reading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (reading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = Spacing.sm),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Review transaction")
            }
        }
    }
}
