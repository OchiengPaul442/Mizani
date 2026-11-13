package com.mizani.money.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.mizani.money.ui.FoldAwareDialogHost
import com.mizani.money.ui.LocalFoldWindowLayout

/** Shared alert-dialog sizing: 94% of a phone width, capped on large displays. */
@Composable
fun MizaniAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp),
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        securePolicy = SecureFlagPolicy.SecureOn,
    ),
) {
    val foldLayout = LocalFoldWindowLayout.current
    if (foldLayout != null && foldLayout.isHalfOpen) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            FoldAwareDialogHost {
                Surface(
                    modifier = modifier,
                    shape = shape,
                    color = containerColor,
                    tonalElevation = tonalElevation,
                ) {
                    FoldAwareAlertContent(
                        confirmButton = confirmButton,
                        dismissButton = dismissButton,
                        icon = icon,
                        title = title,
                        text = text,
                        iconContentColor = iconContentColor,
                        titleContentColor = titleContentColor,
                        textContentColor = textContentColor,
                    )
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}

/**
 * Material3 alert layout for the half-open fold. The public Material3 `AlertDialogContent`
 * is internal to the library in 1.4.0, so this recreates its spacing, styles and button flow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoldAwareAlertContent(
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
) {
    Column(modifier = Modifier.padding(24.dp)) {
        icon?.let {
            CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally),
                ) {
                    icon()
                }
            }
        }
        title?.let {
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(
                                if (icon == null) Alignment.Start else Alignment.CenterHorizontally,
                            ),
                    ) {
                        title()
                    }
                }
            }
        }
        text?.let {
            CompositionLocalProvider(LocalContentColor provides textContentColor) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Box(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(bottom = 24.dp)
                            .align(Alignment.Start)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        text()
                    }
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.End)) {
            FlowRow(
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
