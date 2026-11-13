package com.mizani.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.Spacing

/**
 * Book-mode navigation pane: a compact Mizani header, the five top-level destinations, and a
 * single privacy footer pinned to the bottom. The pane is navigation-only; recording actions stay
 * on Home and in the global Add chooser. Only the destination list scrolls, and it collapses to
 * icon-only navigation when narrower than 300dp.
 */
@Composable
fun MizaniFoldSidebar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 300.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MizaniMark(size = 36, rounded = true)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }

            // Only the destination list scrolls; the privacy footer below stays pinned.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (compact) {
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = currentRoute == tab.route,
                            onClick = { onNavigate(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                        )
                    }
                } else {
                    tabs.forEach { tab ->
                        NavigationDrawerItem(
                            label = { Text(tab.label, maxLines = 1) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            selected = currentRoute == tab.route,
                            onClick = { onNavigate(tab.route) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Your records stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Tabletop-mode navigation dock: a compact Mizani header and one privacy line above the five
 * top-level destinations, which are pinned to the bottom of the pane. The dock is navigation-only;
 * recording actions stay on Home and in the global Add chooser. The dock's outer pane already
 * applies the bottom inset, so the navigation bar consumes none of its own.
 */
@Composable
fun MizaniTabletopDock(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 320.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MizaniMark(size = if (compact) 28 else 32, rounded = true)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }
            Text(
                "Your records stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(Modifier.weight(1f))
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                // The dock's pane inset already covers the system navigation bar.
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { onNavigate(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}
