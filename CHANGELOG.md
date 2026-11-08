# Changelog

Notable changes to Mizani are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/2.0.0/), and release versions follow the `MAJOR.MINOR.PATCH` format used by Gradle.

The first recorded version is `1.1.0`; earlier history for the `v1.0.0` tag has not been reconstructed.

## [Unreleased]

### Added

- Add a first-class Recurring section inside Bills so users can create, edit, archive, record and
  delete repeating bills and expected income without confusing schedules with actual transactions.

### Changed

- Standardize Bills and recurring primary call-to-action buttons on the medium corner shape.
- Refine the Daily Momentum card with a compact stat layout, clearer progress hierarchy, and a
  consistent medium-rounded Record today CTA.
- Standardize filled, tonal, and outlined action buttons on the medium corner shape across the app;
  text-only links and icon-only controls remain intentionally lighter.
- Group the Home screen’s primary expense and income actions into a labelled quick-add module for
  clearer dashboard hierarchy.
- Update the Google Play policy audit and release checklist for the 29 October 2026 permission,
  trader-status, age-signals, Japan, and technical-quality changes without adding restricted
  permissions to the app.
- Clarify that recurring schedules are forecasts until the user records an occurrence, and make the
  multi-sheet Excel workbook the recommended export for analysis while retaining CSV compatibility.
- Choose an uninterrupted pane around separating foldable hinges and keep the activity explicitly
  resizable for foldable and multi-window layouts.
- Tighten recurring-item validation and preserve stable IDs, revisions and tombstones as the local
  foundation for a future encrypted device-to-desktop sync protocol.

## [1.8.0] - 2026-11-01

### Added

- Add private, on-device Daily Momentum points, streaks and recognition milestones based on
  tracked calendar days; progress never rewards spending, uses a network, or exposes financial
  details in notifications.

### Changed

- Enable the daily local money reminder by default after onboarding, while preserving an explicit
  Settings opt-out and preventing scheduling before a workspace exists.
- Adapt navigation to use a rail on wider windows and icon-only navigation at narrow or large-font
  scales; reflow Home and Activity actions and summaries instead of squeezing text into rows.
- Clarify Daily Momentum around tracked days and streaks, add a direct record-today action, and
  replace unsupported “unlocked” wording with honest recognition milestones.

### Fixed

- Provide an in-app notification-permission action for the default daily reminder on Android 13+
  instead of leaving delivery dependent on a settings-only link.
- Keep Home, Insights, and new transaction dates current across foregrounding, midnight, and local
  time-zone changes; preserve large monetary summaries at larger text sizes.
- Keep Activity search responsive on large local ledgers by indexing searchable transaction text
  per snapshot and coalescing rapid typing before filtering.
- Re-align shared reminder scheduling when habit-only check-ins are enabled and keep the privacy
  notice aligned with the default reminder behavior.

## [1.7.1] - 2026-10-30

### Changed

- Default the app theme to Light instead of following the system appearance; users can still choose
  Light, Dark or Follow system in Settings.

## [1.7.0] - 2026-10-30

### Added

- Add optional on-device money check-ins for weekly spending, expected income, investment valuation,
  debt payments and savings goals. Check-ins use category controls, a single daily notification
  budget and generic private copy so financial details stay out of notification surfaces.
- Add an accessible Send feedback flow from More and About & privacy that prepares a privacy-safe
  message for the official Mizani contact through the user's email app, with clear delivery and
  sensitive-data guidance.

### Changed

- Prevent previous pages from showing through during route changes by giving every navigation
  destination an opaque, clipped surface and adding a top-level tab regression check.
- Add concise, accessible in-app help popups for unfamiliar money concepts and clarify primary-currency,
  conversion-rate, investment, transaction-type, message-import and notification wording.
- Extend the existing WorkManager reminder schedule to evaluate local habit signals without a
  network, analytics SDK or new account/backend dependency; update the Privacy Notice and security
  guidance to document the local processing and descriptive-only behavior.
- Use Google Play's in-app update flow for Play-installed builds: ordinary releases can download
  flexibly in the background and high-priority releases can request an immediate update when the
  app is active. Sideloaded builds retain the manual GitHub release check.
- Update the Privacy Notice to explain the optional feedback email handoff and email-provider
  processing.

### Fixed

- Ensure feedback emails open with the official recipient, subject and user-entered message
  populated in compatible email clients.
- Prevent the bottom navigation and Add control from flashing before the first route is attached.

## [1.6.0] - 2026-10-30

### Added

- Add offline investment snapshots so users can record a holding's name, provider or location,
  asset category, cost basis, current manual value, valuation date and note without creating a
  duplicate ledger transaction.
- Add protected Investments navigation, archive handling, checked gain/loss summaries, Room
  migration coverage, encrypted-backup portability and an Investments Excel sheet.

### Changed

- Update the in-app Terms and Privacy Notice to describe local investment records and keep the
  feature explicitly separate from brokerage connectivity, trading and financial advice.
- Show reminder times in an explicit locale-aware 12-hour AM/PM format and compare scheduled
  delivery against the full local wall-clock instant so boundary timing is not delayed by a retry window.

## [1.5.1] - 2026-10-30

### Changed

- Update the in-app Terms and Privacy Notice provider details to Paul Ochieng, the supplied
  `paul.ochieng.dev@gmail.com` contact, and Kampala, Uganda; the legal documents now require
  renewed acceptance at their 30 October 2026 revisions.

### Fixed

- Make all application route changes immediate so the previous page is not composited behind the
  destination during Back, CTA, or detail navigation; onboarding keeps its local page animation.

## [1.5.0] - 2026-10-29

### Fixed

- Expand offline mobile-money import detection beyond MTN MoMo and Airtel Money to include
  M-Pesa, Orange Money, Moov, Wave, Telecel, Tigo, EcoCash, OPay, PalmPay, Moniepoint and
  other supported African networks; accept provider `ID:` references, ISO dates and exact-message
  hashes when a provider omits a reference.
- Keep incoming import reviews honest: received money no longer inherits an arbitrary recent
  income category. The review asks the user to choose what the money represents before saving.
- Preserve daily reminder delivery when Android runs a delayed one-shot after its target date;
  the worker now performs the current-day check instead of silently discarding the reminder.
- Make bill payment linking predictable on mobile by showing only eligible expenses (after refunds,
  in the workspace currency, and within the remaining target) and showing link/unlink failures
  beside the bill instead of away from the action.
- Support bills and commitments in a currency different from the workspace, saving the daily
  reference rate and converted workspace target so progress, Money Outlook, exports, and backups
  remain consistent.
- Stabilize the reviewed-import handoff by returning background parsing results to the main UI
  thread before updating Compose state or navigating.

### Changed

- Rework the Home wallet area into a compact grouped list with inline balance privacy and a clear
  `See all` path; show the getting-started action before wallets for empty ledgers and limit quick
  wallet rows to three to keep the screen scannable.
- Clarify Money Outlook copy and mask protected coverage estimates with dots instead of repeating
  the privacy state in a long sentence; the accessible label still explains that the estimate is hidden.
- Keep More-screen detail routes opaque during Back transitions to prevent the reported gray/frozen
  appearance, with regression coverage for Wallets and Categories navigation.
- Improve reminder wording, explain Android background-delivery limits, and add a test-notification
  action so users can verify permission and channel delivery immediately.
- Improve commitment-card spacing so payment summaries, progress details, and payment guidance are
  easier to scan without lines appearing crowded or touching.

## [1.4.0] - 2026-10-28

### Fixed

- Keep Money Outlook's derived coverage and upcoming-payment amounts masked with wallet balances,
  while keeping the saved daily target visible.

### Changed

- Put Bills & commitments in the primary bottom navigation and make the one-off payment flow clear
  to new users.
- Widen the commitment and daily-target dialogs, improve field-level validation, and add a direct
  route to record or link bill payments without duplicating ledger transactions.
- Remove the detailed calculation panel from Money Outlook while retaining a short estimate basis.
- Refine the Home wallet summary so privacy controls are distinct, labels are not duplicated, and a
  single wallet uses the available width.

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
