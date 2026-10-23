package com.mizani.money.ui.theme

import androidx.compose.ui.graphics.Color

// Official Mizani brand palette (canonical source: docs/brand/BRAND_GUIDE.md, tokens in
// docs/brand/brand-tokens.json). Brand idea: balance — an M-shaped mark with a warm-gold focal dot.
// Emerald carries primary actions, gold is reserved for accents and emphasis, red only for errors.
// All hex values below come from the brand kit; nothing is invented here.

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
val DarkBackground = Color(0xFF0B2028)
val DarkSurface = Color(0xFF12323A)

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
val SurfaceVariantDark = Color(0xFF29464D) // also dark tertiary/outline variant
val OnSurfaceVariantDark = Color(0xFFC4D7D4)

// Light surface containers
val SurfaceLightLow = Color(0xFFFBFDFC)
val SurfaceLightContainer = Color(0xFFF1F7F4)
val SurfaceLightHigh = Color(0xFFE9F2EE)
val SurfaceLightHighest = Color(0xFFE0ECE7)
val OutlineLight = Color(0xFFC9D9D4)
val OutlineVariantLight = Color(0xFFE1ECE8)

// Dark surface containers
val SurfaceDarkLowest = Color(0xFF07161C)
val SurfaceDarkLow = Color(0xFF0D252C) // compact card tone raised above the #0B2028 page
val SurfaceDarkHighest = Color(0xFF1A3D45)
val OutlineDark = Color(0xFF3D5A60)
val ForegroundDark = Color(0xFFEAF4F1)

// Error roles
val ErrorRed = Color(0xFFB3261E)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
val ErrorRedDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
