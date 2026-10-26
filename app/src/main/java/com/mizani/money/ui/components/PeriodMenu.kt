package com.mizani.money.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.mizani.money.core.time.DateRange
import com.mizani.money.core.time.DateText
import com.mizani.money.core.time.PeriodPreset

/**
 * Compact period selector: a single anchored surface with a dropdown instead of
 * the horizontally clipped chip row. "Custom period…" delegates to the caller's
 * date range picker.
 */
@Composable
fun PeriodMenu(
    selected: PeriodPreset,
    customRange: DateRange?,
    range: DateRange,
    onSelect: (PeriodPreset) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        PeriodPreset.CUSTOM -> customRange?.let { DateText.rangeLabel(it) } ?: selected.label
        PeriodPreset.THIS_WEEK -> DateText.rangeLabel(range)
        else -> DateText.month(range.start)
    }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    // 48dp minimum touch target; content is vertically centred inside it.
                    .heightIn(min = 48.dp)
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(securePolicy = SecureFlagPolicy.SecureOn),
        ) {
            DropdownMenuItem(
                text = { Text(PeriodPreset.THIS_WEEK.label) },
                onClick = {
                    expanded = false
                    onSelect(PeriodPreset.THIS_WEEK)
                },
            )
            DropdownMenuItem(
                text = { Text(PeriodPreset.THIS_MONTH.label) },
                onClick = {
                    expanded = false
                    onSelect(PeriodPreset.THIS_MONTH)
                },
            )
            DropdownMenuItem(
                text = { Text(PeriodPreset.LAST_MONTH.label) },
                onClick = {
                    expanded = false
                    onSelect(PeriodPreset.LAST_MONTH)
                },
            )
            DropdownMenuItem(
                text = { Text("Custom period…") },
                onClick = {
                    expanded = false
                    onCustom()
                },
            )
        }
    }
}
