# Changelog

Notable changes to Mizani are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/2.0.0/), and release versions follow the `MAJOR.MINOR.PATCH` format used by Gradle.

The first recorded version is `1.1.0`; earlier history for the `v1.0.0` tag has not been reconstructed.

## [Unreleased]

### Security

- Require fresh PIN or enabled biometric confirmation before exporting financial records or creating encrypted backups.
- Keep Home and Wallets balances concealed until the user confirms with their app PIN or enabled biometrics.

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
