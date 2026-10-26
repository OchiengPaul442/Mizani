# Mizani design system

## Direction

Mizani is a financial tool, not a game. The interface should feel **calm, precise and trustworthy**:
restrained colour, generous whitespace, strong typographic hierarchy, medium roundedness and no decorative noise.
The tagline is **"Your money, in balance."** and it is the test every visual decision must pass.
The visual language follows the **official Mizani brand kit** (archived in `docs/brand/`): balance as the brand
idea, an M-shaped mark with a warm-gold focal dot, deep teal and emerald for trust, mint for progress,
warm gold as the single accent, soft-white/ink neutrals, and red strictly for errors. It also adopts
Material 3 Expressive's emphasis on hierarchy and containment while keeping radii deliberately medium.

References consulted:
- Official Mizani brand kit v1.0 (`docs/brand/BRAND_GUIDE.md`, `docs/brand/brand-tokens.json`) — canonical
- Material 3 Expressive guidance on hierarchy, containment and shape (m3.material.io, 2025–2026)
- Fintech UI/UX guidance on restrained palettes, neutral backgrounds and trust signals (The Skins Factory,
  Eleken, 2026)
- Android Compose typography guidance: tabular figures for money, editorial scale

## Colour

Canonical brand tokens: Deep Teal `#102A32`, Emerald `#087F72`, Mint `#76D7C2`, Warm Gold `#E8BF6A`,
Soft White `#F7FAF8`, Body Text `#17353D`, Muted `#61777A`, Dark Background `#0B2028`,
Dark Surface `#12323A`.

**Light**
| Role | Hex | Use |
|---|---|---|
| Primary | `#087F72` emerald | primary actions, income, selected states |
| On primary | `#FFFFFF` | text/icons on emerald |
| Primary container | `#D8F3EA` | tinted surfaces, selected chips |
| On primary container | `#073E38` | text/icons on the emerald container |
| Secondary | `#865C19` warm gold, darkened for contrast | secondary emphasis, expense series |
| Secondary container | `#FFE8B8` | gold tinted surfaces |
| On secondary container | `#47300B` | text/icons on the gold container |
| Tertiary | `#3F6F76` muted deep teal | transfers |
| Tertiary container | `#D7E6E8` | transfer chips and tiles |
| Background | `#F7FAF8` soft white | page background |
| Surface | `#FFFFFF` | cards and sheets |
| On surface | `#17353D` ink | primary text |
| On surface variant | `#405A5E` | secondary text |
| Muted | `#61777A` | disabled and placeholder text |
| Outline | `#7B8F8A` | field and selector borders |
| Outline variant | `#E1ECE8` | hairlines, subtle separators |
| Error | `#B3261E` | errors only |

**Dark**
Primary `#76D7C2` mint, on-primary `#073E38`, container `#13594F`; secondary `#E8BF6A`,
on-secondary `#46310B`, container `#61471B`; tertiary `#9CC4C8`, container `#29464D`;
background `#0B2028`; surface `#12323A`; on-surface `#EAF4F1`; surface variant `#29464D`,
on-surface variant `#C4D7D4`; containers `#07161C`–`#1A3D45`; outline `#3D5A60`;
error `#FFB4AB`, on-error `#690005`.

Income uses the emerald primary. Ordinary expenses use on-surface ink, **not red**. Transfers use the
muted deep-teal tertiary — never an unrelated blue. Warm gold is reserved for accents and emphasis
(never small text on white). Red appears only for real errors, overspend warnings and
destructive actions.

## Shape

Medium roundedness throughout, from the brand kit (cards 16dp, buttons 12dp):

| Token | Radius | Use |
|---|---:|---|
| extraSmall | 6dp | tiny badges |
| small | 12dp | buttons, text fields, chip containers |
| medium | 16dp | cards, rows |
| large | 20dp | dialogs, bottom sheets |
| extraLarge | 22dp | hero cards, onboarding panel |

Avatars, progress tracks and status dots remain circular. Chips remain pill-shaped.

## Spacing

8-point rhythm, enforced through `Spacing`:

| Token | Value | Use |
|---|---:|---|
| xs | 4dp | icon/label gap |
| sm | 8dp | tight row gaps |
| md | 12dp | compact card padding, list rows |
| lg | 16dp | screen horizontal padding, card padding, section gap |
| xl | 24dp | hero internal spacing |
| xxl | 32dp | onboarding emphasis |
| bottomFab | 96dp | bottom content inset on tab screens |
| bottom | 32dp | bottom inset on sub-screens |

Rules:
- Every screen uses 16dp horizontal padding.
- Every card uses 16dp internal padding; compact metric cards may use 12dp.
- Top-level scroll content ends 96dp above the bottom to clear the FAB.
- Sub-screens end 32dp above the system navigation bar.
- Section titles sit outside cards on Home for clearer hierarchy.
- No content is placed under the status bar or navigation bar; onboarding/lock apply safe-drawing insets.

## Typography

System sans with tabular figures enabled for money. No custom font is bundled (offline, small APK).
- display 30–46sp bold; used for hero amounts and onboarding.
- headline 20–27sp semibold; screen and card titles.
- title 13.5–18sp semibold; rows and chips.
- body 12.5–16sp; explanatory copy.
- label 11–14sp medium/semibold; metadata and actions.
- Negative letter spacing on display/headline, positive tracking on small labels.
- Monetary values use `fontFeatureSettings = "tnum"` so columns align and values do not jitter.

## Components

- **Hero card**: Deep Teal → Emerald → Emerald Bright gradient with warm-gold accents, large watermarked
  mark, currency symbol small, amount large, thin mint progress bar.
- **Safe-to-spend**: white card with warm-gold icon tile; amount and a one-line basis statement;
  expandable calculation.
- **Cash-flow summary**: a single card with three columns separated by 1dp dividers, instead of three
  competing cards.
- **Transaction row**: 42dp rounded-square category tile, title, wallet/date, signed amount;
  transfer icon + destination.
- **Insight card**: tinted semantic surface, title, body, expandable "How this is calculated".
- **Dialogs**: `surfaceContainerHighest` container, `onSurface` title/body, and `onSurfaceVariant` for secondary details.
- **Inputs**: 12dp radius, 56dp height, currency prefix, inline supporting text.
- **Buttons**: 12dp radius, 52dp primary height, filled primary; outlined secondary; text actions otherwise.
- **Charts**: category donut and six-month cash-flow bars; every chart has a text summary
  and a TalkBack description.
- **Navigation**: four-tab Material bar (Home, Activity, Insights, More) and a prominent Add action;
  short, consistent transitions and a translucent-free surface.

## Accessibility

- All tappable controls are at least 48dp.
- Colour is never the only signal: amounts are signed and transfer rows show "Transfer to".
- Donut and bar charts expose content descriptions and accompanying legends.
- Type scale supports large-font rendering; amounts are allowed to wrap to two lines in compact cards.
- Dark mode uses WCAG-AA contrast for text and amounts.
