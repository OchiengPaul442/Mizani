# Mizani — Brand Guide v1.0

## Brand idea
**Mizani** is an Android-first personal budgeting app. The visual concept is
balance: a custom, symmetrical M symbol with a warm gold center point.
The brand should communicate clarity, trust, calm and progress, not wealth promises.

## Tagline
**Your money, in balance.**

## Color palette

| Name | Hex | Intended role |
|---|---|---|
| Deep Teal | #102A32 | Logo, hero backgrounds, primary dark surfaces |
| Emerald | #087F72 | Buttons, highlights, selected controls |
| Mint | #76D7C2 | Dark-mode primary, soft positive accents |
| Warm Gold | #E8BF6A | Small highlights and logo focal point |
| Soft White | #F7FAF8 | Light-mode background |
| Body Text | #17353D | Light-mode text |
| Dark Background | #0B2028 | Dark-mode base |
| Dark Surface | #12323A | Dark-mode cards |

Use semantic Material 3 colors, not raw hex values scattered throughout the UI.
Reserve gold for accents; avoid gold as small text on white.
Do not communicate income/expense state through color alone. Include labels and icons.

## Logo
- The mark is an M-shaped balance symbol.
- Use `logo-lockup-on-light.svg` on pale surfaces.
- Use `logo-lockup-on-dark.svg` on dark surfaces.
- Use `logo-mark-dark.svg` or `logo-mark-light.svg` where space is limited.
- Never stretch, rotate, recolor randomly, or add shadows to the mark.
- Keep clear space around the logo; use at least 15% of its height.
- Use the icon tile for store listings and launcher icons.

## Typography
Use Android's system sans-serif by default to avoid external downloads,
font licensing work and unnecessary APK weight. Inter or Manrope are optional
future typography choices; neither font is included in this pack.

## Accessibility
- Check text contrast for all real backgrounds and component states.
- Use text labels with charts, not color-only legends.
- Respect Android font scaling and reduced-motion preferences.
- Test icons on light and dark launchers and circular masks.

## Localization
Keep currency, transaction labels and localized numbers separate from brand art.
Do not embed UGX, USD or other currency symbols in the core logo.
The tagline is optional and should be localized independently.

## Usage / provenance
Artwork and brand assets in this package were created for the Mizani project.
No external stock art or font binaries are included.
This package does not establish trademark or name availability; verify before
public launch or registration.
