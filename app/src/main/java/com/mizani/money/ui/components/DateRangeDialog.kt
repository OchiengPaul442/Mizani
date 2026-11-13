package com.mizani.money.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.mizani.money.core.time.DateRange
import com.mizani.money.ui.FoldAwareDialogHost
import com.mizani.money.ui.LocalFoldWindowLayout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val foldLayout = LocalFoldWindowLayout.current
    if (foldLayout != null && foldLayout.isHalfOpen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                securePolicy = SecureFlagPolicy.SecureOn,
            ),
        ) {
            FoldAwareDialogHost {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .heightIn(max = 568.dp),
                    shape = DatePickerDefaults.shape,
                    color = DatePickerDefaults.colors().containerColor,
                    tonalElevation = DatePickerDefaults.TonalElevation,
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            modifier = Modifier
                                .weight(weight = 1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            DateRangePicker(
                                state = state,
                                title = {
                                    Text(
                                        "Choose a period",
                                        modifier = Modifier.padding(Spacing.lg),
                                    )
                                },
                                showModeToggle = false,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(PaddingValues(bottom = 8.dp, end = 6.dp)),
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TextButton(onClick = onDismiss) { Text("Cancel") }
                                TextButton(
                                    enabled = state.selectedStartDateMillis != null &&
                                        state.selectedEndDateMillis != null,
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
                            }
                        }
                    }
                }
            }
        }
        return
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOn,
        ),
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
            title = { Text("Choose a period", modifier = Modifier.padding(Spacing.lg)) },
            showModeToggle = false,
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
