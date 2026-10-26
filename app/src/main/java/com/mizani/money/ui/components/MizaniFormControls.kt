package com.mizani.money.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.money.Currency

/** One option in a consistent picker. */
data class PickerOption(val id: String, val label: String)

/**
 * A single dropdown abstraction used across forms. Keeping this in one place guarantees the same
 * accessibility, overflow behaviour and menu semantics for country, wallet, category and plan forms.
 */
@Composable
fun MizaniPicker(
    selectedText: String,
    options: List<PickerOption>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    noneLabel: String? = null,
    onSelectNone: (() -> Unit)? = null,
    searchable: Boolean = false,
    searchHint: String = "Search",
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filteredOptions = if (searchable && query.isNotBlank()) {
        options.filter { it.label.contains(query.trim(), ignoreCase = true) }
    } else {
        options
    }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = {
                query = ""
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                selectedText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Open options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                query = ""
            },
            modifier = Modifier.heightIn(max = 560.dp),
        ) {
            if (searchable) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(searchHint) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.width(280.dp).padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
            if (noneLabel != null && onSelectNone != null) {
                DropdownMenuItem(
                    text = { Text(noneLabel) },
                    onClick = {
                        onSelectNone()
                        expanded = false
                        query = ""
                    },
                )
            }
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option.id)
                        expanded = false
                        query = ""
                    },
                )
            }
            if (searchable && filteredOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No matching currencies", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}

/**
 * Currency-aware amount input with consistent filtering, prefix and numeric keyboard.
 * Callers own validation; malformed text is never silently converted to a value here.
 */
@Composable
fun MizaniAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currency: Currency,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    placeholder: String = "0",
) {
    val supportingContent: (@Composable () -> Unit)? = if (supportingText.isNullOrBlank()) {
        null
    } else {
        { Text(supportingText) }
    }
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            onValueChange(raw.filter { it.isDigit() || it == '.' || it == ',' })
        },
        label = { Text(label) },
        prefix = {
            Text(
                currency.symbol + " ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        placeholder = { Text(placeholder) },
        supportingText = supportingContent,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = enabled,
        textStyle = textStyle,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Standard inline validation / success banner used by every form and settings section. */
@Composable
fun StatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onDismiss: (() -> Unit)? = null,
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            // Announced when it appears: errors interrupt (assertive), confirmations wait (polite).
            .semantics {
                liveRegion = if (isError) LiveRegionMode.Assertive else LiveRegionMode.Polite
            },
        color = if (isError) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (onDismiss != null) {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) }
            }
        }
    }
}
