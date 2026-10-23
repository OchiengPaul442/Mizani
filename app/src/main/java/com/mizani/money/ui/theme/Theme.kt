package com.mizani.money.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mizani.money.core.model.ThemeMode

// Official Mizani brand kit palette — see docs/brand/BRAND_GUIDE.md.
private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldTint,
    onPrimaryContainer = EmeraldDeep,
    secondary = GoldAction,
    onSecondary = Color.White,
    secondaryContainer = GoldTint,
    onSecondaryContainer = GoldInk,
    tertiary = TransferTeal,
    onTertiary = Color.White,
    tertiaryContainer = TransferTealTint,
    onTertiaryContainer = DeepTeal,
    background = SoftWhite,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceLightLow,
    surfaceContainer = SurfaceLightContainer,
    surfaceContainerHigh = SurfaceLightHigh,
    surfaceContainerHighest = SurfaceLightHighest,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = DeepTeal,
    inverseOnSurface = SoftWhite,
    inversePrimary = Mint,
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = EmeraldDeep,
    primaryContainer = EmeraldSurface,
    onPrimaryContainer = EmeraldTint,
    secondary = Gold,
    onSecondary = GoldOnDark,
    secondaryContainer = GoldSurface,
    onSecondaryContainer = GoldTint,
    tertiary = TransferTealBright,
    onTertiary = DeepTeal,
    tertiaryContainer = SurfaceVariantDark,
    onTertiaryContainer = TransferTealTint,
    background = DarkBackground,
    onBackground = ForegroundDark,
    surface = DarkSurface,
    onSurface = ForegroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceDarkLowest,
    surfaceContainerLow = SurfaceDarkLow,
    surfaceContainer = DeepTeal,
    surfaceContainerHigh = DarkSurface,
    surfaceContainerHighest = SurfaceDarkHighest,
    outline = OutlineDark,
    outlineVariant = SurfaceVariantDark,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = ForegroundDark,
    inverseOnSurface = DarkBackground,
    inversePrimary = Emerald,
)

@Composable
fun MizaniTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MizaniTypography,
        shapes = MizaniShapes,
        content = content,
    )
}
