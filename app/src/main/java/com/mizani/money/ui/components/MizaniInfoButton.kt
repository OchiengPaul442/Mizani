package com.mizani.money.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mizani.money.R

/**
 * Small, discoverable help action for concepts that cannot be explained inline without
 * overwhelming the screen. IconButton preserves the platform's accessible touch target.
 */
@Composable
fun MizaniInfoButton(
    infoTitle: String,
    infoBody: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "About $infoTitle",
) {
    var visible by rememberSaveable(infoTitle, infoBody) { mutableStateOf(false) }

    IconButton(
        onClick = { visible = true },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }

    if (visible) {
        MizaniAlertDialog(
            onDismissRequest = { visible = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                )
            },
            title = { Text(infoTitle) },
            text = { Text(infoBody) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { visible = false }) {
                    Text(stringResource(R.string.info_dialog_got_it))
                }
            },
        )
    }
}
