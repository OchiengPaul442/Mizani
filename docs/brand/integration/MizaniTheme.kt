// MIZANI BRAND THEME
// AI AGENT: Change this package to match the actual app namespace.
// Merge with an existing Theme.kt rather than creating conflicting themes.
package com.example.mizani.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MizaniColors {
    val DeepTeal = Color(0xFF102A32)
    val Emerald = Color(0xFF087F72)
    val Mint = Color(0xFF76D7C2)
    val Gold = Color(0xFFE8BF6A)
    val Background = Color(0xFFF7FAF8)
    val Text = Color(0xFF17353D)
    val DarkBackground = Color(0xFF0B2028)
    val DarkSurface = Color(0xFF12323A)
}

private val MizaniLightScheme = lightColorScheme(
    primary = Color(0xFF087F72),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3EA),
    onPrimaryContainer = Color(0xFF073E38),
    secondary = Color(0xFF865C19),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8B8),
    onSecondaryContainer = Color(0xFF47300B),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF17353D),
    surface = Color.White,
    onSurface = Color(0xFF17353D),
    surfaceVariant = Color(0xFFE6F0EC),
    onSurfaceVariant = Color(0xFF405A5E),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val MizaniDarkScheme = darkColorScheme(
    primary = Color(0xFF76D7C2),
    onPrimary = Color(0xFF073E38),
    primaryContainer = Color(0xFF13594F),
    onPrimaryContainer = Color(0xFFD8F3EA),
    secondary = Color(0xFFE8BF6A),
    onSecondary = Color(0xFF46310B),
    secondaryContainer = Color(0xFF61471B),
    onSecondaryContainer = Color(0xFFFFE8B8),
    background = Color(0xFF0B2028),
    onBackground = Color(0xFFEAF4F1),
    surface = Color(0xFF12323A),
    onSurface = Color(0xFFEAF4F1),
    surfaceVariant = Color(0xFF29464D),
    onSurfaceVariant = Color(0xFFC4D7D4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MizaniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MizaniDarkScheme else MizaniLightScheme,
        content = content
    )
}
