package com.mizani.money.ui.theme

import androidx.compose.ui.graphics.Color

// Official Mizani brand palette (canonical source: docs/brand/BRAND_GUIDE.md, tokens in
// docs/brand/brand-tokens.json). Brand idea: balance — an M-shaped mark with a warm-gold focal dot.
// Emerald carries primary actions, gold is reserved for accents and emphasis, red only for errors.
// App dark surfaces use neutral charcoal tones.

// Core brand colours
val DeepTeal = Color(0xFF102A32)
val Emerald = Color(0xFF087F72)
val EmeraldBright = Color(0xFF0A9C8B)
val Mint = Color(0xFF76D7C2)
val Gold = Color(0xFFE8BF6A)
val GoldDeep = Color(0xFFB78331)
val SoftWhite = Color(0xFFF7FAF8)
val Ink = Color(0xFF17353D)
val Muted = Color(0xFF61777A)
// Dark surfaces use layered charcoal tones. True black and deep-teal page surfaces were
// difficult to scan for some users; emerald remains the action accent.
val DarkBackground = Color(0xFF171A1C)
val DarkSurface = Color(0xFF222629)
val DarkBackgroundWarm = Color(0xFF211F1D)
val DarkSurfaceWarm = Color(0xFF2A2825)
val SurfaceVariantWarmDark = Color(0xFF393531)
val OnSurfaceVariantWarmDark = Color(0xFFD0C9C1)
val SurfaceDarkWarmLowest = Color(0xFF191715)
val SurfaceDarkWarmLow = Color(0xFF24211F)
val SurfaceDarkWarmHigh = Color(0xFF312E2B)
val SurfaceDarkWarmHighest = Color(0xFF3A3632)
val OutlineWarmDark = Color(0xFF827A72)
val OutlineVariantWarmDark = Color(0xFF5B554F)

// Tertiary support colour for transfers: a muted deep teal, never an unrelated blue.
val TransferTeal = Color(0xFF3F6F76)
val TransferTealTint = Color(0xFFD7E6E8)
val TransferTealBright = Color(0xFF9CC4C8)

// Emerald-derived containers
val EmeraldDeep = Color(0xFF073E38) // on-primary-container (light), on-primary (dark)
val EmeraldTint = Color(0xFFD8F3EA) // primary-container (light), on-primary-container (dark)
val EmeraldSurface = Color(0xFF13594F) // primary-container (dark)

// Gold-derived containers
val GoldAction = Color(0xFF865C19) // secondary (light)
val GoldTint = Color(0xFFFFE8B8) // secondary-container (light), on-secondary-container (dark)
val GoldInk = Color(0xFF47300B) // on-secondary-container (light)
val GoldOnDark = Color(0xFF46310B) // on-secondary (dark)
val GoldSurface = Color(0xFF61471B) // secondary-container (dark)

// Muted surface containers
val SurfaceVariantLight = Color(0xFFE6F0EC)
val OnSurfaceVariantLight = Color(0xFF405A5E)
val SurfaceVariantDark = Color(0xFF32383B) // also dark tertiary/outline variant
val OnSurfaceVariantDark = Color(0xFFC1C8CB)

// Light surface containers
val SurfaceLightLow = Color(0xFFFBFDFC)
val SurfaceLightContainer = Color(0xFFF1F7F4)
val SurfaceLightHigh = Color(0xFFE9F2EE)
val SurfaceLightHighest = Color(0xFFE0ECE7)
// Form and selector boundaries need enough contrast to remain visible on soft-white surfaces.
val OutlineLight = Color(0xFF7B8F8A)
val OutlineVariantLight = Color(0xFFE1ECE8)

// Dark surface containers
val SurfaceDarkLowest = Color(0xFF111416)
val SurfaceDarkLow = Color(0xFF1C2022)
val SurfaceDarkHigh = Color(0xFF2C3134)
val SurfaceDarkHighest = Color(0xFF383E41)
val OutlineDark = Color(0xFF788388)
val OutlineVariantDark = Color(0xFF51595D)
val ForegroundDark = Color(0xFFF0F3F3)

// Error roles
val ErrorRed = Color(0xFFB3261E)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
val ErrorRedDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
