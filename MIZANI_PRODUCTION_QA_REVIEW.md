# Mizani — production QA review

Date: 2026-10-23 · Tree: `D:\projects\budgeting-application` · Reviewer verdict on record: **PASS
WITH RISKS** (independent reviewer, after corrections)

## Executive summary

**Mizani is not production-ready.**

The audit's confirmed fixes are in the tree, the JVM gates are green (`:app:testDebugUnitTest
--rerun` exits 0 with **87 tests, 0 failures/errors/skips across 8 suites**; `:app:assembleDebug`
produces a **31,141,566-byte (≈ 29.7 MiB)** debug APK), and instrumented on-device verification
is now recorded: **all five instrumented classes passed** on a headless `Medium_Phone_API_36.1`
emulator (API 36.1, serial `emulator-5554`), one class per Gradle invocation —
`GuestJourneyTest` (1 test, 0 failures, 84.2s; first attempt hit a transient systemui ANR that
stole input focus, and it passed after the ANR was dismissed and the test retried),
`RepositoryIntegrationTest` (2 tests, exit 0, 56s), `AppLockTest` (1 test, 88.7s),
`BackupRestoreTest` (1 test, 63.8s), `DemoDataTest` (1 test, 17.8s). Refund/fee accounting,
backup/restore validation (including duplicate IDs), CSV formula neutralization, persisted PIN
backoff, app-lock Result handling, the refund-heavy chart/display edge (positive-slice filtering,
neutral empty state, corrected day/net signs) and the copy/accessibility corrections are
implemented and unit-covered.

Three residual blockers keep the build out of production:

1. **No production-signed release artifact** — `:app:assembleRelease` produces a
   **10,663,390-byte (≈ 10.2 MiB)** APK with R8 shrinking + resource optimization enabled
   (`optimization { enable = true }`), but the artifact is **unsigned** (no signing config, no
   keystore) and cannot be installed as-is. A temporary copy signed with the existing **debug**
   keystore (`CN=Android Debug` — not production signing) passed a launch smoke
   (`apksigner` verify OK, `MainActivity` resumed, app data cleared, no `FATAL EXCEPTION` and no
   E-level `mizani` logcat lines); that test-signed copy was deleted and the unsigned artifact
   retained. Sign with a real release keystore before any Play distribution.
2. **Localization** — English-only (`res/values/`, `values-night/`; no translated locales) with part
   of the copy hardcoded in Kotlin instead of string resources.
3. **Manual acceptance residual** — the emulator matrix is instrumented evidence only; the 9-step
   manual checklist and a TalkBack pass were not performed.

Also unresolved: the reviewer's residual hypotheses on PIN backoff (wall-clock bypass,
non-atomic failure counter) and unverified real-spreadsheet rendering of exported CSV. The
emulator run used a temporary screenshot and run logs which were deleted during cleanup — this
report retains no screenshots.

## Scope

- Audited and documented: app behavior findings from the completed audit, copy/UX corrections,
  build and test results, documentation cleanup state.
- Files owned and changed by this report: `README.md`, `docs/VERIFICATION.md`, and this root
  `MIZANI_PRODUCTION_QA_REVIEW.md`. For this report, no app source, resources, tests or Room
  schemas were edited; no dependencies installed; no secrets. The release worker changed
  `app/build.gradle.kts` only to enable release `optimization { enable = true }` (R8 + resource
  shrinking); the emulator worker edited no app source, resources, tests, Gradle files or Room
  schemas. The emulator/release workers' only destructive actions were scoped cleanup:
  uninstalling the test app, killing the emulator, and deleting temp screenshots, run logs,
  smoke APKs and transient `androidTest` build outputs.
- Documentation inventory verified: `docs/` now contains only `brand/`, `DESIGN_SYSTEM.md`,
  `PROJECT_STRUCTURE.md`, `ARCHITECTURE_CONTRACT.md`, `VERIFICATION.md`.

## Findings

Status labels: **Fixed** · **Partially fixed** · **Not fixed** · **Not verified**.

Concise mapping (severity → # → status):

| Severity | #s | Status mix |
|---|---|---|
| Critical | C1, C2, C3 | Fixed, Fixed, Partially fixed |
| High | H1, H2, H3, H4 | Fixed, Fixed, Fixed (verified on emulator), Fixed (code-level; runtime visual not verified) |
| Medium | M1, M2, M3, M4, M5 | Not verified, Not verified, Partially fixed, Partially fixed (unsigned release build + debug-signed smoke), Partially fixed |
| Low | L1, L2, L3 | Fixed, Fixed, Not verified |

### Critical

| # | Finding | Status | Affected screens / files |
|---|---------|--------|--------------------------|
| C1 | Encrypted backup restore accepted insufficiently validated payloads, including duplicate transaction IDs (all-or-nothing restore guarantee at risk). Repository now validates before writing and `BackupValidator` checks duplicates; covered by 29 unit tests. | Fixed | `data/repo/FinanceRepository.kt`, `data/repo/BackupValidator.kt`, `BackupRestoreTest` (androidTest, passed on API 36.1 emulator — 1 test, 63.8s), `BackupValidatorTest` |
| C2 | CSV export allowed spreadsheet formula injection from free-text fields; negative numbers and leading-whitespace values also broke out of the free-text path. Formula neutralization now splits numeric vs free-text paths and handles leading whitespace. | Fixed (code+unit tests); real spreadsheet rendering **Not verified** | `domain/export/ExportManager.kt`, `ExportManagerTest` (4), export UI copy in CSV export sheet |
| C3 | No brute-force protection on the app-lock PIN. Persisted backoff implemented: 5 free attempts, then 5s → 60s cap, persisted across process death. Residual hypotheses: wall-clock (System.currentTimeMillis) bypass and a non-atomic failure counter. | Partially fixed (backoff implemented); bypass/atomicity hypotheses **Not verified** | `core/security/PinDefaults.kt`, `data/repo/SettingsRepository.kt`, `ui/AppViewModel.kt`, `feature/lock/LockScreen.kt`, `feature/settings/SettingsScreen.kt`, `PinBackoffPolicyTest` (8) |

### High

| # | Finding | Status | Affected screens / files |
|---|---------|--------|--------------------------|
| H1 | Refund and fee accounting was wrong across the ledger, analytics, transactions screen and CSV export (refunds not reducing category totals, fees mis-attributed). | Fixed | `data/repo/FinanceModels.kt`, `domain/analytics/Analytics.kt`, `feature/transactions/TransactionsScreen.kt`, `domain/export/ExportManager.kt` |
| H2 | App-lock flow mishandled nested `Result` values, masking success/failure from the user. | Fixed | App-lock enable flow: `ui/AppViewModel.kt`, `feature/settings/SettingsScreen.kt` (`SettingsScreen` handling `AppViewModel.enableLock`) |
| H3 | No fresh device verification existed for the current tree. **Resolved for instrumented scope:** all five androidTest classes ran green on a headless `Medium_Phone_API_36.1` emulator (serial `emulator-5554`, boot ~75s), one class per Gradle invocation: `GuestJourneyTest` 1 test / 0 failures / 84.2s (first attempt hit a transient systemui ANR that stole input focus; passed after dismissing the ANR and retrying), `RepositoryIntegrationTest` 2 tests / exit 0 / 56s, `AppLockTest` 1 test / 88.7s, `BackupRestoreTest` 1 test / 63.8s, `DemoDataTest` 1 test / 17.8s. The 9-step manual checklist and TalkBack remain unperformed (see M3/residuals). | **Fixed / Verified (instrumented on emulator); manual acceptance still open** | `app/src/androidTest/**` (`GuestJourneyTest`, `RepositoryIntegrationTest`, `AppLockTest`, `BackupRestoreTest`, `DemoDataTest`), all Compose screens reached by those tests |
| H4 | Home/Insights "Where money went" chart/legend received all category slices and `expenseMinor`; a refund-heavy negative-net category could render a confusing chart or share. Corrected: both screens now chart only positive net category slices against a positive total, with a neutral empty state when no positive slice remains; day-header/net signs and the "Net spending" labels were also corrected. | **Fixed (code-level; runtime visual not verified)** | `feature/home/HomeScreen.kt`, `feature/home/HomeComponents.kt`, `feature/insights/InsightsScreen.kt`, `feature/transactions/TransactionsScreen.kt` (day-header/net signs) |

### Medium

| # | Finding | Status | Affected screens / files |
|---|---------|--------|--------------------------|
| M1 | PIN backoff trust assumptions: elapsed time read from wall clock (resettable by changing device time) and the failure counter increment is not atomic. | Not verified (reviewer residual hypotheses) | `core/security/PinDefaults.kt`, `data/repo/SettingsRepository.kt`, `ui/AppViewModel.kt` |
| M2 | CSV output not opened in a real spreadsheet application; neutralization proven only by unit tests. | Not verified | `domain/export/ExportManager.kt`, export flow |
| M3 | Copy and accessibility defects: misleading primary/secondary button copy, string resources not extracted, missing error states, incorrect live-region semantics, picker touch targets under 48dp, missing destructive-action confirmation before archive/delete. | Partially fixed (source-level copy/a11y improvements; hardcoded strings and TalkBack remain unverified) | `res/values/strings.xml`, `feature/account/AccountScreen.kt`, `feature/settings/SettingsScreen.kt`, `feature/home/HomeScreen.kt`, `feature/insights/InsightsScreen.kt`, `feature/categories/CategoriesScreen.kt`, `feature/onboarding/OnboardingScreen.kt`; TalkBack behavior **Not verified** on device |
| M4 | Release build path: `:app:assembleRelease` now exits 0 with R8 full-mode shrinking + optimized resource shrinking enabled (`optimization { enable = true }`, default Android keep rules, no custom ProGuard rules), producing a 10,663,390-byte (≈ 10.2 MiB) unsigned `app-release-unsigned.apk`. A temporary copy signed with the existing debug keystore (`CN=Android Debug` — not production signing) passed a launch smoke (`apksigner` verify OK, `MainActivity` resumed, no `FATAL EXCEPTION` / no E-level `mizani` logcat) and was then deleted. Still missing: production signing, mapping-file handling, baseline profiles/macrobenchmarks. | Partially fixed (unsigned release build + debug-signed smoke only) | `app/build.gradle.kts` (unchanged by this report), release pipeline |
| M5 | Localization: English-only with no translated locales; part of the UI copy lives in Kotlin literals rather than string resources (some copy extracted during the UX pass, but many single-use literals remain). | Partially fixed | `res/values/strings.xml`, `res/values-night/`, multiple `feature/*` screens |

### Low

| # | Finding | Status | Affected screens / files |
|---|---------|--------|--------------------------|
| L1 | Documentation drift: obsolete screenshots, AI-strategy/redline notes, duplicate setup instructions and brand marketing packages; stale verified-gate numbers (42 tests / 31,149,028-byte APK / instrumented claim). | Fixed | `README.md`, `docs/VERIFICATION.md`; deleted: `docs/screenshots/` (49 PNGs), `docs/SCREENSHOTS.md`, `docs/ANDROID_ON_DEVICE_AI_STRATEGY.md`, root `AI-RED.md`, `docs/brand/README.md`, `ANDROID_SETUP.md`, `INSTRUCTIONS_FOR_AI_AGENT.md`, `manifest.json`, `docs/brand/marketing/` (6 PNGs) |
| L2 | Docs claimed "Category reordering is not exposed"; the category editor exposes Move up / Move down. Claim removed from `docs/VERIFICATION.md`. | Fixed (docs); on-device behavior **Not verified** | `docs/VERIFICATION.md`, `feature/categories/CategoriesScreen.kt` |
| L3 | An earlier `assembleDebug --rerun-tasks` failed with an environmental JBR/jlink toolchain-transform error. Normal `assembleDebug` succeeded and is the authoritative result; the app build itself is not implicated. | Not verified (environmental; non-blocking) | Gradle/JBR environment, `build/` outputs |

## Original wording → replacement

| Original wording | Replacement | Where (source-verified) |
|---|---|---|
| "Create account" | "About accounts" | `res/values/strings.xml` (`onboarding_about_accounts`) |
| "Record your first money" | "Add your first transaction" | `res/values/strings.xml` (`home_get_started_title`) |
| "The three states" | "How your data works" | `feature/account/AccountScreen.kt` |
| "Money lost to moving money" | "Fees on transactions and transfers" | `feature/insights/InsightsScreen.kt` |
| "You can plan a budget after your first transaction" | "You can add a transaction and create a budget from Plan whenever you choose" | `feature/home/HomeScreen.kt` |
| "PIN is encrypted…" | "Your PIN is stored only as a salted hash and is never saved in plain text…" | `feature/settings/SettingsScreen.kt` |
| "Danger zone" | "Delete data" | `feature/settings/SettingsScreen.kt` |

## Bugs fixed and UX improvements

- Refund-fee accounting corrected end to end (model, analytics, transactions UI, CSV export).
- Refund-heavy chart/display edge corrected: Home and Insights "Where money went" chart/legend now
  use only positive net category slices against a positive total, with a neutral empty state when
  no positive slice remains; day-header/net signs and the "Net spending" labels were also
  corrected. Code-level fix; runtime visuals not device-verified.
- Repository/restore validation with duplicate-ID rejection and all-or-nothing restore.
- CSV formula neutralization with numeric vs free-text paths and leading-whitespace handling.
- Persisted PIN backoff (5 free attempts, 5s → 60s cap) with unit-tested policy; app-lock nested
  `Result` handling corrected.
- Copy pass: clearer primary/secondary actions, string-resource extraction, explicit error states,
  corrected accessibility live-region semantics, 48dp picker targets, destructive archive
  confirmation dialog.
- Documentation cleanup: dead screenshots/marketing/AI-strategy files removed; README and
  VERIFICATION gates re-stated with current numbers, the API 36.1 emulator matrix, and the
  unsigned-release / debug-signed-smoke distinction.

## Features removed, hidden or unchanged

- **Removed from documentation only:** 49 screenshot PNGs + `docs/SCREENSHOTS.md`,
  `docs/ANDROID_ON_DEVICE_AI_STRATEGY.md`, `AI-RED.md`, `docs/brand/README.md`, `ANDROID_SETUP.md`,
  `INSTRUCTIONS_FOR_AI_AGENT.md`, `manifest.json`, `docs/brand/marketing/` (6 PNGs).
- **Absent by design (unchanged):** no AI model or inference runtime (insights are deterministic
  local analytics only), no network stack, no ads/analytics SDKs, no telemetry, no INTERNET
  permission, and platform auto-backup explicitly disabled. None of these were ever present, so
  none were removed.
- **Out-of-scope V1 features (unchanged):** cloud sync / account backend (no backend, no
  credentials), SMS mobile-money import, split transactions, multi-currency conversion. These
  remain unimplemented by scope decision, not removed.
- **Unchanged and retained:** guest onboarding, wallets, categories, transactions (expense, income,
  transfer, refund, adjustment), budgets/periods, expected income, recurring bills, savings goals,
  debts, search/filters, deterministic analytics with explanations, CSV export, encrypted `.mizani`
  backup/restore, SQLCipher + Keystore, PIN + biometric lock, brand kit in `docs/brand/`.
- **Hidden/altered in the UI by the copy pass:** onboarding "Create account" entry reworded,
  Settings "Danger zone" reworded, dashboard empty-state and insights fee wording reworded (table
  above).

## Exact build and test results

- `.\gradlew.bat :app:testDebugUnitTest --rerun` — **exit 0**; **87 tests, 0 failures, 0 errors,
  0 skipped**, 8 suites: `MoneyTest` 12, `PinBackoffPolicyTest` 8, `PinHasherTest` 3, `PeriodsTest`
  7, `BackupValidatorTest` 29, `AnalyticsEngineTest` 19, `ExportManagerTest` 4, `InsightEngineTest` 5.
- `:app:compileDebugKotlin` — exit 0. `:app:processDebugResources` — exit 0.
  `:app:assembleDebug` — exit 0. `:app:compileDebugAndroidTestKotlin` — exit 0.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, **31,141,566 bytes ≈ 29.7 MiB**.
- **Emulator E2E (headless `Medium_Phone_API_36.1`, API 36.1, serial `emulator-5554`, boot ~75s) —
  all five instrumented classes passed**, one class per Gradle invocation:
  `GuestJourneyTest` 1 test / 0 failures / **84.2s** (first attempt hit a transient systemui ANR
  that stole input focus; passed after dismissing the ANR and retrying),
  `RepositoryIntegrationTest` 2 `@Test` methods / exit 0 / **56s**, `AppLockTest` 1 test /
  **88.7s**, `BackupRestoreTest` 1 test / **63.8s**, `DemoDataTest` 1 test / **17.8s**. No app
  source, resources, tests or Room schema files were edited by the emulator/release workers;
  `app/build.gradle.kts` was changed only for release optimization
  (`optimization { enable = true }`).
- **Release build:** `:app:assembleRelease` — exit 0;
  `app/build/outputs/apk/release/app-release-unsigned.apk`, **10,663,390 bytes ≈ 10.2 MiB**, R8
  full-mode + optimized resource shrinking (`optimization { enable = true }`, default keep rules,
  no custom ProGuard rules). **Unsigned** — no signing config, no keystore.
- **Release launch smoke (verification only):** a **temporary copy** of the unsigned release APK
  was signed with the existing **debug** keystore (`CN=Android Debug`, not production signing),
  `apksigner` verified OK, installed with app data cleared, `MainActivity` launched to resumed
  state with no `FATAL EXCEPTION` and no E-level `mizani` logcat lines. The test-signed APK +
  idsig were deleted; `app-release-unsigned.apk` was retained. This is not a production-signed
  artifact.
- **Cleanup:** `pm clear` + `adb uninstall`, `adb emu kill` (emulator processes exited); the
  temporary screenshot, the five run logs, both smoke APKs + idsigs,
  `app/build/outputs/androidTest-results` and `app/build/reports/androidTests` were deleted. No
  screenshots remain in the project (only launcher mipmap PNGs).
- **Environmental limitation:** an earlier `assembleDebug --rerun-tasks` hit a JBR/jlink
  toolchain-transform failure; normal `assembleDebug` succeeded and is authoritative.

## Outstanding issues

1. PIN backoff wall-clock bypass and non-atomic failure counter — **Not verified**.
2. CSV neutralization in real spreadsheet applications — **Not verified**.
3. No production-signed release artifact — the release APK is **unsigned**; the only signed
   smoke used the debug keystore. **Not fixed**.
4. Localization / hardcoded Kotlin copy — **Not fixed**.
5. Manual acceptance: the 9-step checklist and a TalkBack pass — **Not verified** (instrumented
   matrix passed; human checklist not run).

## Areas not inspected or not tested

- Manual on-device behavior: the 9-step manual acceptance checklist, a full logcat
  network-traffic audit, TalkBack traversal, biometric prompt on real hardware. (The 5
  instrumented classes are no longer in this list — they passed on the API 36.1 emulator.)
- Refund-heavy chart visuals: the corrected "Where money went" positive-slice filtering, positive
  total, neutral empty state, day-header/net signs and "Net spending" labels were **not
  device-verified**; the fix is code-level only for this build.
- Real spreadsheet rendering (Excel/Google Sheets/Numbers) of exported CSV.
- Release-build tooling beyond the smoke: mapping-file handling, baseline profiles,
  macrobenchmarks, performance, battery and memory profiling (R8 shrinking is enabled and the
  launch smoke passed; production signing is absent).
- Localization: translations, RTL layouts, locale-specific date/currency formatting (none exist).
- DB migration behavior across historical schema versions on real devices (schemas present in
  `app/schemas/`, not exercised here).
- Non-debug build flavors, CI pipelines, Play Store listing/compliance.
- The residual security hypotheses (clock rollback, counter atomicity) were reasoned about, not
  exploited or tested.

## Remaining production checklist (prioritized)

1. **Run the manual acceptance gate:** perform all 9 manual acceptance steps in
   `docs/VERIFICATION.md` on a device/emulator (the five instrumented classes already passed on
   the API 36.1 emulator), including a TalkBack accessibility pass.
2. **Harden PIN backoff**: use elapsed-realtime (or a monotonic source) for delay scheduling and
   make the failure counter increment atomic; add tests for clock rollback.
3. **Verify CSV in real spreadsheets** (leading whitespace, negative numbers, formula-looking
   free text) and keep the unit tests as the regression gate.
4. **Production-sign the release build**: configure a release keystore (credentials kept out of
   VCS), sign the R8-optimized release APK, `apksigner verify`, install it, re-run the launch
   smoke and the five instrumented classes against the signed build. The debug-keystore-signed
   smoke is verification evidence only, not production signing.
5. **Localization pass**: move remaining Kotlin-literal copy into string resources, decide target
   locales, verify RTL and locale formatting.
6. **On-device accessibility pass**: TalkBack traversal, live regions, 48dp targets, destructive
   confirmation dialogs.
7. **Keep documentation in sync**: `README.md` and `docs/VERIFICATION.md` now record the API 36.1
   emulator matrix and the unsigned-release / debug-signed-smoke distinction; update them again
   when a production-signed artifact exists.

## Verdict

**Mizani is not production-ready.** The unit-tested core, the emulator-verified instrumented
suite and the documentation are in good shape — including the refund-heavy chart/display edge,
now corrected in code — but release is blocked by (a) the absence of a **production-signed
release artifact** — the R8-optimized release APK is unsigned and the only signed build was a
debug-keystore smoke copy; and (b) **localization** debt — English-only with hardcoded copy —
plus unverified PIN-backoff and CSV-in-spreadsheet assumptions, an unrun manual acceptance /
TalkBack pass, and unverified runtime visuals for the corrected chart edge. Clear items 1–7
above before reconsidering this verdict.
