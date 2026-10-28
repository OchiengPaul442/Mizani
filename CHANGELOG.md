# Changelog

Notable changes to Mizani are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/2.0.0/), and release versions follow the `MAJOR.MINOR.PATCH` format used by Gradle.

The first recorded version is `1.1.0`; earlier history for the `v1.0.0` tag has not been reconstructed.

## [Unreleased]

### Fixed

- Hide Money Outlook coverage estimates while wallet balances are hidden; keep the saved daily target visible.

### Changed

- Move Bills & commitments into the main navigation and clarify the one-off
  payment flow for first-time users.
- Widen the commitment and daily-target dialogs to match the shared mobile layout, and add a
  direct expense-entry action to the bill payment flow.
- Simplify Money Outlook by removing the detailed calculation panel. Keep a concise estimate basis
  and show the saved daily target while wallet balances are hidden.
- Clean up the Home wallet section: separate privacy controls from navigation, avoid
  duplicate wallet type labels, and use a full-width summary when there is one wallet.

## [1.3.0] - 2026-10-28

### Added

- Import reviewed MTN MoMo and Airtel Money confirmation text by paste or Android share, with local reference deduplication and no SMS access.
- Track one-off commitments with partial payments linked to existing expenses and optional private reminders.
- Add Money Outlook with a clear 30-day obligation reserve, recorded-spending runway, and a user-set daily target when history is insufficient.

### Fixed

- Keep app lock, appearance preferences, and saved legal acceptance through compatible app updates; only changed legal documents require renewed acceptance.
- Associate imported amounts with the sent or received action, excluding fee and balance values from transaction suggestions.

### Security

- Require fresh PIN or enabled biometric confirmation before exporting financial records or creating encrypted backups.
- Keep Home and Wallets balances concealed until the user confirms with their app PIN or enabled biometrics.
- Process shared transaction text only in memory, discard the source message after review, and keep commitment notifications generic.

### Changed

- Replace deep-teal and true-black dark surfaces with soft-charcoal and warm-slate choices; keep saved theme mode and accent preferences.
- Add active wallet shortcuts to Home, including for a new ledger with no transactions.
- Replace cramped preset accent chips with a spaced custom color picker; saved accents apply across the app.
- Add subtle accent-tinted edges to shared cards and the bottom bar, with stronger definition in dark mode.
- Replace the post-splash loader copy and progress bar with a centered logo and radiating pulse rings.
- Reuse rendered Storyset illustrations on screen revisits instead of parsing the same SVG again.
- Center onboarding content consistently in its available viewport, with scrolling on shorter screens.
- Inset the bottom navigation in a rounded, lightly bordered surface with consistent spacing.
- Open the selected transaction when a row in Home’s recent activity is tapped.
- Clarify the floating Add sheet with equally prominent, descriptive choices for expenses, income, and wallet transfers.
- Make Money Outlook easier to scan with a clearer coverage estimate, concise setup guidance, and expandable calculation details.

### Removed

- Temporarily remove app and budget sharing entry points.

## [1.2.0] - 2026-10-28

### Added

- Added a Storyset illustration to the final onboarding step.
- Added saved appearance preferences for system/light/dark mode, five accent themes, and a true-black dark surface option.
- Added sharing for the official Mizani project link and a reviewed, plan-only text summary for each saved budget.

### Changed

- Simplified first-wallet setup so name suggestions follow the selected wallet type.
- Applied the selected accent and dark surface style across app surfaces while keeping income, expense, transfer, and positive insight colors consistent.
- Updated the Privacy Notice to explain which budget details are sent to an app the user selects for sharing.

### Fixed

- Resuming unfinished setup no longer asks users to accept current Terms and Privacy Notice again.

## [1.1.0] - 2026-10-27

### Added

- Added in-app screens for dependency licenses and required third-party notices.

### Changed

- Licensed Mizani-authored source and executable forms under Apache-2.0, with the original developer and project reference in `NOTICE`; third-party components, artwork, and branding remain separately governed.
- Moved the required Storyset credit from the main About screen into the dedicated Third-party notices screen.
- Updated the Terms and Privacy notices with provider information; the Terms also clarify the Apache source-code grant and separate third-party terms.
