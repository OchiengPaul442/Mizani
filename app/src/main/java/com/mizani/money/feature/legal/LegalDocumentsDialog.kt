package com.mizani.money.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.mizani.money.core.legal.LegalDocument
import com.mizani.money.core.legal.LegalDocumentId
import com.mizani.money.core.legal.LegalDocuments

@Composable
fun LegalDocumentsDialog(
    initialDocument: LegalDocumentId,
    onClose: () -> Unit,
) {
    var selectedDocument by rememberSaveable { mutableStateOf(initialDocument) }
    val document = when (selectedDocument) {
        LegalDocumentId.TERMS -> LegalDocuments.terms
        LegalDocumentId.PRIVACY -> LegalDocuments.privacy
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOn,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 640.dp)
                .fillMaxHeight(0.94f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Legal information", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close legal information")
                    }
                }

                PrimaryTabRow(selectedTabIndex = selectedDocument.ordinal) {
                    Tab(
                        selected = selectedDocument == LegalDocumentId.TERMS,
                        onClick = { selectedDocument = LegalDocumentId.TERMS },
                        text = { Text("Terms of Service") },
                    )
                    Tab(
                        selected = selectedDocument == LegalDocumentId.PRIVACY,
                        onClick = { selectedDocument = LegalDocumentId.PRIVACY },
                        text = { Text("Privacy Notice") },
                    )
                }

                DocumentBody(document, Modifier.weight(1f))
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onClose) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun DocumentBody(document: LegalDocument, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        item {
            Text(document.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Effective ${document.effectiveDate}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(document.introduction, style = MaterialTheme.typography.bodyLarge)
        }
        items(document.sections) { section ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(section.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                section.paragraphs.forEach { paragraph ->
                    Text(paragraph, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
