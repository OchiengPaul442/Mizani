package com.mizani.money.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.mizani.money.core.model.ColorTheme
import com.mizani.money.core.model.DarkThemeStyle
import com.mizani.money.core.model.ThemeMode

// The default uses the official Mizani brand kit. User-selected palettes change only the primary
// accent and hero gradient; financial colors stay emerald, gold, and muted teal across themes.
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
    inverseSurface = DarkBackground,
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
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = SurfaceDarkHigh,
    surfaceContainerHighest = SurfaceDarkHighest,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = ForegroundDark,
    inverseOnSurface = DarkBackground,
    inversePrimary = Emerald,
)

private val WarmSlateDarkColors = DarkColors.copy(
    background = DarkBackgroundWarm,
    surface = DarkSurfaceWarm,
    surfaceVariant = SurfaceVariantWarmDark,
    onSurfaceVariant = OnSurfaceVariantWarmDark,
    tertiaryContainer = SurfaceVariantWarmDark,
    surfaceContainerLowest = SurfaceDarkWarmLowest,
    surfaceContainerLow = SurfaceDarkWarmLow,
    surfaceContainer = DarkSurfaceWarm,
    surfaceContainerHigh = SurfaceDarkWarmHigh,
    surfaceContainerHighest = SurfaceDarkWarmHighest,
    outline = OutlineWarmDark,
    outlineVariant = OutlineVariantWarmDark,
    inverseOnSurface = DarkBackgroundWarm,
)

private data class AccentTokens(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroAccent: Color,
    val heroAccentOn: Color,
)

private fun accentTokens(theme: ColorTheme): AccentTokens = when (theme) {
    ColorTheme.MIZANI -> AccentTokens(
        lightPrimary = Emerald,
        lightOnPrimary = Color.White,
        lightContainer = EmeraldTint,
        lightOnContainer = EmeraldDeep,
        darkPrimary = Mint,
        darkOnPrimary = EmeraldDeep,
        darkContainer = EmeraldSurface,
        darkOnContainer = EmeraldTint,
        heroStart = DarkBackground,
        heroEnd = Emerald,
        heroAccent = Mint,
        heroAccentOn = EmeraldDeep,
    )
    ColorTheme.OCEAN -> AccentTokens(
        lightPrimary = Color(0xFF215B9E),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFDCE9FF),
        lightOnContainer = Color(0xFF102C5C),
        darkPrimary = Color(0xFFA6C8FF),
        darkOnPrimary = Color(0xFF142C50),
        darkContainer = Color(0xFF294A78),
        darkOnContainer = Color(0xFFDCE9FF),
        heroStart = Color(0xFF142747),
        heroEnd = Color(0xFF215B9E),
        heroAccent = Color(0xFFA6C8FF),
        heroAccentOn = Color(0xFF142C50),
    )
    ColorTheme.VIOLET -> AccentTokens(
        lightPrimary = Color(0xFF6E42A1),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFEEDFFF),
        lightOnContainer = Color(0xFF2B1346),
        darkPrimary = Color(0xFFD0B3F3),
        darkOnPrimary = Color(0xFF2D1948),
        darkContainer = Color(0xFF513276),
        darkOnContainer = Color(0xFFEEDFFF),
        heroStart = Color(0xFF2B1546),
        heroEnd = Color(0xFF6E42A1),
        heroAccent = Color(0xFFD0B3F3),
        heroAccentOn = Color(0xFF2D1948),
    )
    ColorTheme.FOREST -> AccentTokens(
        lightPrimary = Color(0xFF2B7048),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFDDF1E2),
        lightOnContainer = Color(0xFF183B26),
        darkPrimary = Color(0xFF9FD5AF),
        darkOnPrimary = Color(0xFF183B26),
        darkContainer = Color(0xFF235238),
        darkOnContainer = Color(0xFFDDF1E2),
        heroStart = Color(0xFF173724),
        heroEnd = Color(0xFF2B7048),
        heroAccent = Color(0xFF9FD5AF),
        heroAccentOn = Color(0xFF183B26),
    )
    ColorTheme.ROSE -> AccentTokens(
        lightPrimary = Color(0xFFA7374F),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFFADDE3),
        lightOnContainer = Color(0xFF4D1524),
        darkPrimary = Color(0xFFFFB3C2),
        darkOnPrimary = Color(0xFF4D1524),
        darkContainer = Color(0xFF6A2939),
        darkOnContainer = Color(0xFFFADDE3),
        heroStart = Color(0xFF3F1622),
        heroEnd = Color(0xFFA7374F),
        heroAccent = Color(0xFFFFB3C2),
        heroAccentOn = Color(0xFF4D1524),
    )
}

private fun customAccentTokens(hex: String): AccentTokens {
    val seed = Color(android.graphics.Color.parseColor(hex))
    val lightPrimary = withHslLightness(seed, 0.40f)
    val darkPrimary = withHslLightness(seed, 0.78f)
    val lightContainer = withHslLightness(seed, 0.94f)
    val darkContainer = withHslLightness(seed, 0.28f)
    val lightOnContainer = withHslLightness(seed, 0.20f)
    val darkOnContainer = withHslLightness(seed, 0.92f)
    return AccentTokens(
        lightPrimary = lightPrimary,
        lightOnPrimary = contrastingForeground(lightPrimary),
        lightContainer = lightContainer,
        lightOnContainer = contrastingForeground(lightContainer),
        darkPrimary = darkPrimary,
        darkOnPrimary = contrastingForeground(darkPrimary),
        darkContainer = darkContainer,
        darkOnContainer = contrastingForeground(darkContainer),
        heroStart = withHslLightness(seed, 0.18f),
        heroEnd = lightPrimary,
        heroAccent = darkPrimary,
        heroAccentOn = contrastingForeground(darkPrimary),
    )
}

private fun withHslLightness(color: Color, lightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun contrastingForeground(background: Color): Color {
    val backgroundArgb = background.toArgb()
    val blackContrast = ColorUtils.calculateContrast(Color.Black.toArgb(), backgroundArgb)
    val whiteContrast = ColorUtils.calculateContrast(Color.White.toArgb(), backgroundArgb)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

fun colorThemePreviewColor(theme: ColorTheme): Color = accentTokens(theme).lightPrimary

data class MizaniThemeTokens(
    val heroStart: Color,
    val heroEnd: Color,
    val heroAccent: Color,
    val heroAccentForeground: Color,
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val positiveContainer: Color,
    val onPositiveContainer: Color,
)

val LocalMizaniThemeTokens = staticCompositionLocalOf {
    MizaniThemeTokens(
        heroStart = DarkBackground,
        heroEnd = Emerald,
        heroAccent = Mint,
        heroAccentForeground = EmeraldDeep,
        income = Emerald,
        expense = GoldAction,
        transfer = TransferTeal,
        positiveContainer = EmeraldTint,
        onPositiveContainer = EmeraldDeep,
    )
}

@Composable
fun MizaniTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: ColorTheme = ColorTheme.MIZANI,
    customAccentColorHex: String? = null,
    darkThemeStyle: DarkThemeStyle = DarkThemeStyle.DEFAULT,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = customAccentColorHex?.let(::customAccentTokens) ?: accentTokens(colorTheme)
    val baseColors = when {
        dark && darkThemeStyle == DarkThemeStyle.WARM_SLATE -> WarmSlateDarkColors
        dark -> DarkColors
        else -> LightColors
    }
    val colors = if (dark) {
        baseColors.copy(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = accent.darkOnContainer,
            inversePrimary = accent.lightPrimary,
        )
    } else {
        baseColors.copy(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = accent.lightOnContainer,
            inversePrimary = accent.darkPrimary,
        )
    }
    val themeTokens = MizaniThemeTokens(
        heroStart = accent.heroStart,
        heroEnd = accent.heroEnd,
        heroAccent = accent.heroAccent,
        heroAccentForeground = accent.heroAccentOn,
        income = if (dark) Mint else Emerald,
        expense = if (dark) Gold else GoldAction,
        transfer = if (dark) TransferTealBright else TransferTeal,
        positiveContainer = if (dark) EmeraldSurface else EmeraldTint,
        onPositiveContainer = if (dark) EmeraldTint else EmeraldDeep,
    )

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
    CompositionLocalProvider(LocalMizaniThemeTokens provides themeTokens) {
        MaterialTheme(
            colorScheme = colors,
            typography = MizaniTypography,
            shapes = MizaniShapes,
            content = content,
        )
    }
}
