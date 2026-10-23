package com.mizani.money.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import com.mizani.money.core.time.DateRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    initial: DateRange?,
    onDismiss: () -> Unit,
    onConfirm: (DateRange) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.start?.toUtcMillis(),
        initialSelectedEndDateMillis = initial?.end?.toUtcMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        val a = start.toLocalDate()
                        val b = end.toLocalDate()
                        onConfirm(DateRange(minOf(a, b), maxOf(a, b)))
                    }
                },
            ) { Text("Use period") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DateRangePicker(
            state = state,
            title = { Text("Choose a period", modifier = androidx.compose.ui.Modifier.padding(Spacing.lg)) },
            showModeToggle = false,
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
