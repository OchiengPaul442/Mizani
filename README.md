# Mizani — offline-first personal money management

Mizani is a native Android budgeting app built with Kotlin and Jetpack Compose. It is
guest-first: the local encrypted database is the source of truth, and core
money-management works without an account. Entering an amount in another currency requires an
internet connection to fetch its daily reference rate.

**Tagline:** Your money, in balance.

## What is implemented

- Guest onboarding with country pack, primary currency, first wallet and optional opening balance.
- Wallets: cash, mobile money, bank, card, savings, debt and other; create, edit, archive.
- Categories: seeded defaults plus custom categories, icons, colours, rename, archive/restore.
- Transactions: expense, income, transfer, refund and balance adjustment, with optional fees,
  payee, note and date. Foreign-currency expenses and income use a dated public reference rate;
  the original amount and rate are retained alongside the workspace-currency ledger amount.
  Editing and soft deletion retain tombstones.
- Budgets: weekly, monthly, four-week and custom periods; planned vs funded amounts,
  essential flags, priority, rollover rules and category-level funding.
- Expected income via recurring income rules; received income never includes expectations.
- Recurring bills and commitments with local “mark paid” and next-date advancement.
- Savings goals with contributions, optional linked savings wallet and target dates.
- Debt tracking with balances, periodic payments, due dates and payment recording.
- Deterministic analytics: cash flow, category shares, budget pace, safe-to-spend,
  six-month history, income variability, wallet composition, fee trend, forecast,
  savings rate and essential coverage — each with a “How this is calculated” explanation.
  Charts (donut + grouped cash-flow bars) are rendered with **MPAndroidChart v3.1.0** and always
  keep a textual equivalent.
- Local search and filters: text, date range, type, wallet, category and amount range.
- Optional 6-digit PIN app lock (PBKDF2-HMAC-SHA256) and AndroidX biometric unlock.
- Multi-sheet Excel and transaction CSV exports (with an unencrypted-file warning), plus a versioned, passphrase-encrypted `.mizani` backup with
  validated, all-or-nothing restore.
- SQLCipher database encrypted with a device-bound Android Keystore-wrapped key.
- Platform backup explicitly disabled; no ads or telemetry SDKs. Internet access is used only
  when a user requests foreign-currency conversion.

## Technology

- Kotlin 2.2.10 with Jetpack Compose (Compose BOM 2026.02.01), AGP 9.4.1, Gradle 9.6 wrapper,
  single `:app` module with manual DI.
- Room 2.8.5 (KSP) on SQLCipher 4.19.1 with an Android Keystore-wrapped key; DataStore 1.3.0 for
  preferences; AndroidX Biometric 1.1.0; kotlinx-coroutines 1.10.2.
- **MPAndroidChart v3.1.0 (Apache License 2.0)** — Philipp Jahoda's charting library, used only for
  the donut and grouped cash-flow bars, wrapped by `ui/components/MizaniDonutChart.kt` and
  `ui/components/MizaniCashFlowBars.kt`. All chart text, legends and accessibility labels are
  rendered in Compose, so every chart keeps a textual equivalent.
- No app backend, advertising or analytics SDK, or AI model/inference runtime. A small HTTPS
  client fetches public currency-pair reference rates from Frankfurter when conversion is used.

## Brand

The interface follows the official Mizani brand kit (archived in `docs/brand/`): the balance
mark with a warm-gold focal dot, the tagline *Your money, in balance.*, and the canonical
palette — Deep Teal `#102A32`, Emerald `#087F72`, Mint `#76D7C2`, Warm Gold `#E8BF6A`,
Soft White `#F7FAF8` and Ink `#17353D`. Emerald drives primary actions, gold is reserved for
accents, muted deep teal marks transfers, and red is used only for errors. Shapes are cards
16dp, buttons and fields 12dp, dialogs 20dp. See `docs/DESIGN_SYSTEM.md` for the full token
table and `docs/brand/BRAND_GUIDE.md` for the source guidelines.

## Documentation

- `docs/DESIGN_SYSTEM.md` — official palette, shape, spacing, typography and component rules.
- `docs/PROJECT_STRUCTURE.md` — single-module package layout, dependency direction and where changes belong.
- `docs/ARCHITECTURE_CONTRACT.md` — data, ledger, analytics, security and AI boundaries.
- `docs/VERIFICATION.md` — build, unit, instrumented and manual verification gates.
- `docs/brand/` — official brand kit (brand guide, colour/shape tokens and logo SVGs).
- `MIZANI_PRODUCTION_QA_REVIEW.md` (repository root) — production QA review: findings, status
  labels, copy changes, exact build/test results and the remaining production checklist.

Documentation cleanup: obsolete screenshot sets, AI-strategy/AI-redline notes, duplicate setup
instructions and brand marketing packages were deleted. `docs/` now contains only `brand/`,
`DESIGN_SYSTEM.md`, `PROJECT_STRUCTURE.md`, `ARCHITECTURE_CONTRACT.md` and `VERIFICATION.md`;
no doc references the removed files.

## Build

Prerequisites: JDK 21+ (the project daemon uses the Android Studio JBR), Android SDK 37,
and Gradle 9.6 via the wrapper.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Release versions and app updates

Set `mizaniVersionName` in `gradle.properties` for each release (for example, `1.0.1`). Gradle
derives Android's increasing `versionCode` from the numeric `MAJOR.MINOR.PATCH` parts, and the
About and More screens read the generated version name. Keep release versions moving forward; do
not reuse a version or lower any part after publishing.

For direct APK installs, About offers a user-initiated check against the public GitHub Releases
page for this repository. It opens a verified release page in the browser; the user downloads the
APK and confirms installation in Android. It does not download or install in the background.
Publish a stable `vMAJOR.MINOR.PATCH` release with an APK signed using the same production key as
previous releases for this check to find it. Do not publish debug or unsigned APKs.

Google Play will manage updates for Play-installed copies once Mizani is published there. This
repository has no production signing configuration or Play publishing workflow yet; keep the
production signing key secure and configure it before distributing release APKs.

### Verified gates (this build)

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest --rerun
```

- **87 unit tests** green across **8 suites** with 0 failures, 0 errors and 0 skipped:
  `MoneyTest` 12, `PinBackoffPolicyTest` 8, `PinHasherTest` 3, `PeriodsTest` 7,
  `BackupValidatorTest` 29, `AnalyticsEngineTest` 19, `ExportManagerTest` 4, `InsightEngineTest` 5
  (`AnalyticsEngineTest` alone accounts for 19).
- `:app:compileDebugKotlin`, `:app:processDebugResources`, `:app:assembleDebug` and
  `:app:compileDebugAndroidTestKotlin` all exit 0.
- **All 5 instrumented classes passed on an emulator** — headless `Medium_Phone_API_36.1`
  (API 36.1, `adb` serial `emulator-5554`, boot completed in ~75s), one class per Gradle
  invocation: `GuestJourneyTest` (1 test, 0 failures, 84.2s — first attempt hit a transient
  systemui ANR that stole input focus; it passed after the ANR was dismissed and the test
  retried), `RepositoryIntegrationTest` (2 `@Test` methods, exit 0, 56s), `AppLockTest` (1 test,
  88.7s), `BackupRestoreTest` (1 test, 63.8s), `DemoDataTest` (1 test, 17.8s). See
  `docs/VERIFICATION.md` for the per-class table and run discipline. The 9-step manual acceptance
  checklist and a TalkBack pass were not performed — instrumented execution is verified, manual
  on-device acceptance is not.
- **Final debug APK: 31,141,566 bytes ≈ 29.7 MiB** at
  `app/build/outputs/apk/debug/app-debug.apk` (includes **MPAndroidChart v3.1.0**, used only for the
  donut and grouped cash-flow bars — see "Technology").
- **Release APK: 10,663,390 bytes ≈ 10.2 MiB** unsigned at
  `app/build/outputs/apk/release/app-release-unsigned.apk`. R8 full-mode code shrinking plus
  optimized resource shrinking enabled via `optimization { enable = true }` (AGP 9.3+ DSL); default
  Android keep rules were sufficient (no custom ProGuard rules added). The retained artifact is
  **unsigned** (no signing config, no keystore), so it cannot be installed as-is and no
  production-signed release artifact exists. As a verification-only smoke test, a **temporary
  copy** was signed with the existing **debug** keystore (`CN=Android Debug` — not a production
  key), `apksigner`-verified, installed with app data cleared, and `MainActivity` launched to
  resumed state with no `FATAL EXCEPTION` and no E-level `mizani` logcat lines; that test-signed
  copy was deleted and the unsigned artifact retained. Sign with a real release keystore before
  any Play distribution. Behavior, application ID and Room schema unchanged.

Install the debug APK on a running emulator/device:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

Single `:app` module with enforced package boundaries (see `docs/ARCHITECTURE_CONTRACT.md`):

```
core/money, core/model, core/time, core/security
data/local (Room + SQLCipher + Keystore)
data/repo   (single validated write boundary)
domain/analytics, domain/export
feature/*   (Compose screens)
ui/theme, ui/components, ui/navigation
```

Money is stored as integer minor units plus a currency code, never as floating point.
Transfers never count as income or spending; their fees do. Refunds reduce the original
category. Expected income never counts as received. All analytics are computed on-device
by a pure Kotlin layer with unit tests.

## Deliberate V1 deviations

- **Cloud sync / accounts:** no backend is provisioned in this build and no credentials may
  ship in the APK. The Account screen is explicit that sync is not configured. Encrypted
  backup is the available portability path.
- **SMS mobile-money import:** policy-gated in the PRD; not included here. The importer
  boundary is documented but no SMS permission is declared.
- **Split transactions:** P1; not in this build.
- **Currency conversion:** expenses and income may be entered in another supported currency.
  Frankfurter supplies the latest available daily reference rate; the app records the converted
  workspace-currency amount plus the original amount, rate and rate date. Reports and balances
  remain in one workspace currency. This is not a live bank/card settlement rate.
- **Manual DI** rather than Hilt, and a single module rather than a large module graph.
  These are documented architecture decisions for V1.

## Artificial intelligence

AI is an **optional future layer**, not part of this build. **No AI model, inference runtime or
model-download path ships in the APK** — there is no model file, no runtime dependency or
AI network access. Every amount, percentage and comparison shown to the user is
produced by the deterministic on-device analytics layer, and those deterministic insights are what
the app ships with; adding AI later would not replace or override them. If AI is ever added it must
be local, opt-in, fact-constrained and verifier-gated — it may explain numbers, never produce them,
and no financial payload is ever sent in a prompt. See `docs/ARCHITECTURE_CONTRACT.md` for the
binding boundary.

## Privacy summary

Guest financial data never leaves the device. For an optional currency conversion, the app sends
only the source and workspace currency codes over HTTPS to Frankfurter; it does not send the amount,
payee, note or other transaction details. The app contains no advertising or analytics SDK and does
not log financial values.
Android Auto Backup is disabled and the app's data domains are excluded from backup and
device-transfer rules. The SQLCipher passphrase is random and wrapped by a non-exportable
Keystore key; uninstalling without an exported backup loses the local data permanently.
