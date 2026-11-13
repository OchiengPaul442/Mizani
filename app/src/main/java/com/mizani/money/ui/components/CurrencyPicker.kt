package com.mizani.money.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.Currency
import com.mizani.money.ui.FoldAwareDialogHost

/**
 * Full-height, searchable currency picker. The complete current-world catalog is a LazyColumn
 * so every selectable currency stays reachable, and search matches code, name and symbol.
 * Historical codes resolve for old records but are not offered here for new entry.
 */
@Composable
fun MizaniCurrencyPicker(
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    currencies: List<Currency> = Currencies.transactionCurrencies,
    fieldLabel: String = "Currency",
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val selected = remember(currencies, selectedCode) {
        currencies.firstOrNull { it.code.equals(selectedCode, ignoreCase = true) }
            ?: Currencies.byCode(selectedCode)
    }

    OutlinedButton(
        onClick = {
            query = ""
            open = true
        },
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.fieldHeight)
            .semantics { contentDescription = "$fieldLabel: $selectedCode" },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            "${selected.code} · ${selected.name}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Open options")
    }

    if (open) {
        Dialog(
            onDismissRequest = {
                open = false
                query = ""
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                securePolicy = SecureFlagPolicy.SecureOn,
            ),
        ) {
            val sorted = remember(currencies) { currencies.sortedBy { it.code } }
            val trimmedQuery = query.trim()
            val filtered = if (trimmedQuery.isEmpty()) {
                sorted
            } else {
                sorted.filter { currency ->
                    currency.code.contains(trimmedQuery, ignoreCase = true) ||
                        currency.name.contains(trimmedQuery, ignoreCase = true) ||
                        currency.symbol.contains(trimmedQuery, ignoreCase = true)
                }
            }
            val selectedIndex = filtered.indexOfFirst {
                it.code.equals(selectedCode, ignoreCase = true)
            }
            val listState = rememberLazyListState()

            // Bring the current selection into view once when the list is first shown.
            LaunchedEffect(selectedIndex) {
                if (selectedIndex >= 0 && query.isEmpty()) {
                    listState.scrollToItem(selectedIndex)
                }
            }

            FoldAwareDialogHost {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.88f)
                        .imePadding(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Choose currency",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${filtered.size} of ${currencies.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = {
                                query = ""
                                open = false
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close currency picker")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search by code, name or symbol") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (filtered.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text("No currencies match", style = MaterialTheme.typography.titleSmall)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Try a different code, name or symbol.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else {
                                items(filtered, key = { it.code }) { currency ->
                                    val isSelected = currency.code.equals(selectedCode, ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 52.dp)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .clickable(role = Role.Button) {
                                                onSelect(currency.code)
                                                open = false
                                                query = ""
                                            }
                                            .semantics { contentDescription = "${currency.code} ${currency.name}" }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            currency.code,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            currency.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            currency.symbol,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (isSelected) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
