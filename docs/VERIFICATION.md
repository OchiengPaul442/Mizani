# Verification and release notes

## Automated gates

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --stacktrace
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

**Status of the final build:** both gates green — **87 unit tests** in **8 suites** (0 failures,
0 errors, 0 skipped) and a debug APK of **31,141,566 bytes ≈ 29.7 MiB** at
`app/build/outputs/apk/debug/app-debug.apk`.

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun   # final verification run: exit 0, 87 tests
```

Additional gates green (exit 0): `:app:compileDebugKotlin`, `:app:processDebugResources`,
`:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`.

Suite breakdown (`app/build/test-results/testDebugUnitTest/*.xml`): `MoneyTest` 12,
`PinBackoffPolicyTest` 8, `PinHasherTest` 3, `PeriodsTest` 7, **`BackupValidatorTest` 29**,
**`AnalyticsEngineTest` 19**, `ExportManagerTest` 4, `InsightEngineTest` 5 — 87 total.

**Build-run note (environmental):** an earlier `assembleDebug --rerun-tasks` attempt failed with a
JBR/jlink toolchain-transform error in the Gradle environment. That failure is environmental, not an
app defect; the normal `.\gradlew.bat :app:assembleDebug` run succeeded and is the authoritative
build result.

Unit tests cover:

- money parsing/formatting, exact arithmetic, currency mismatch and overflow;
- transfer/refund/adjustment analytics invariants;
- expected-vs-received income separation;
- budget pace, safe-to-spend, rollover and refund handling;
- recurring month-end and leap-year dates;
- **forecast short-history gating** (`forecast is null when the spending history covers less than
  seven days`, plus `forecast needs recent spending and states a basis`);
- **month-to-date comparison ranges** (`month to date compares with the same elapsed days of the
  previous month`, `month to date comparison clamps to the previous month length`, `dashboard
  previous values use the month to date comparison range`);
- deterministic insight rules;
- PBKDF2-HMAC-SHA256 known vector and PIN verification;
- persisted PIN backoff policy (5 free attempts, 5s → 60s cap) — `PinBackoffPolicyTest`;
- encrypted-back repository/restore validation including duplicate IDs — `BackupValidatorTest`;
- CSV export hardening: formula neutralization with numeric vs free-text paths and
  leading-whitespace handling — `ExportManagerTest`;
- refund/fee accounting invariants across ledger, analytics and export.

## Instrumented tests (emulator or device required)

The instrumented suite runs against the real encrypted Room/SQLCipher database and drives the
Compose UI. **5 test classes** live in `app/src/androidTest/java/com/mizani/money/`:
`GuestJourneyTest` (onboarding → ledger → analytics), `RepositoryIntegrationTest` (validated write
boundary), `AppLockTest` (PIN/biometric lock), `BackupRestoreTest` (encrypted `.mizani` export and
restore) and `DemoDataTest` (test-only demo seeding). `TestState.kt` is the shared cleanup helper.
The temporary `.opencode/runtime/drop-androidtest/` staging copy has been removed because the
classes are integrated.

**Current status — all five classes PASSED on API 36.1 (this build):** a headless
`Medium_Phone_API_36.1` emulator (API 36.1, `adb` serial `emulator-5554`, boot completed in ~75s)
ran the full matrix, one class per Gradle invocation in the order below:

| Class | Outcome | Wall time |
|---|---|---|
| `GuestJourneyTest` | 1 test, 0 failures | 84.2s |
| `RepositoryIntegrationTest` | 2 `@Test` methods, exit 0 | 56s |
| `AppLockTest` | 1 test, 0 failures | 88.7s |
| `BackupRestoreTest` | 1 test, 0 failures | 63.8s |
| `DemoDataTest` | 1 test, 0 failures | 17.8s |

**`GuestJourneyTest` ANR retry:** the first attempt hit a transient systemui ANR that stole input
focus; the ANR was dismissed and the test retried, after which it passed (that passing run is the
84.2s above). The emulator worker edited no app files; the release worker changed only the
release optimization in `app/build.gradle.kts` (`optimization { enable = true }`). After the
matrix, cleanup ran `pm clear` + `adb uninstall`, then `adb emu kill` — the
emulator processes exited. The temporary screenshot, the five run logs, both smoke APKs and
idsigs, and `app/build/outputs/androidTest-results` and `app/build/reports/androidTests` were
deleted; **no test artifacts or screenshots are retained in the tree** (only launcher mipmap
PNGs remain). The manual 9-step checklist below was **not** executed — see that section.

```powershell
# One class at a time — see the separation note below.
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.GuestJourneyTest
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.RepositoryIntegrationTest
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.AppLockTest
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.BackupRestoreTest
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.DemoDataTest
```

**Known test-run separation (applies to any future instrumented run):** every instrumented class
resets the test-owned local state
(`TestState.reset()` deletes `mizani.db`, the Keystore-wrapped key preferences and the settings
DataStore) in `@Before`. Running the classes in a single task pass therefore wipes each class's
database mid-run. Run each class as its own Gradle invocation, in the order above, and do not
parallelize them on one device.

**Historical known flake — `AppLockTest` teardown:** in earlier runs a transient `SlotTable`
exception in the Compose test tearDown reproduced in 1 of 3 runs with no application frames in the
stack; the same class passed on re-run and on that run's final matrix. It is a test-harness/Compose-
teardown flake, not an app defect — the lock/unlock assertions themselves passed. In the current
matrix's `AppLockTest` run (1 test, 88.7s) the flake was **not observed**; it was not exercised
repeatedly, so re-run `AppLockTest` alone before treating a future failure as a regression.

## Manual acceptance on emulator

**Status: NOT executed for this build.** The instrumented matrix above ran on the API 36.1
emulator, but the human checklist below was not performed. It remains a required manual gate and
must be run (ideally together with a TalkBack accessibility pass) before any release.

1. Fresh install in airplane mode. Complete onboarding without an account.
2. Create a wallet, record income, expense, transfer and refund. Confirm transfers do not
   change income/expense totals but fees do.
   Also enter an expense in a foreign currency, verify the rate date and converted workspace
   amount, then confirm the original amount and rate remain in the transaction and exports.
3. Create a budget with essential and flexible categories, fund a line, mark a bill paid.
4. Add a goal, contribute, verify progress and that a transfer contribution is not spending.
5. Use search and filters; change reporting period to this week, last month and custom.
6. Enable app lock, close and reopen, unlock with PIN; verify wrong PIN is rejected.
7. Export transactions as CSV and all records as an Excel workbook; confirm both files open and that the workbook separates recorded transactions from recurring forecasts.
8. Export an encrypted backup; restore it and compare totals.
8. Kill the process after saving; reopen and confirm the ledger is intact.
9. Confirm no crash in logcat, no financial values in logcat, and that the only network request
   is an explicitly requested currency-pair rate lookup with no transaction data.

## Known limitations of this build

- No cloud account/sync backend is configured. INTERNET access is limited to public reference-rate lookups.
- SMS import and split transactions are not implemented.
- **Manual on-device acceptance not performed:** the five instrumented classes passed on the
  API 36.1 emulator, but the 9-step manual checklist below and a TalkBack pass were not run.
- Current artifacts:
  - Debug: **31,141,566 bytes (≈ 29.7 MiB)** debug APK at
    `app/build/outputs/apk/debug/app-debug.apk`.
  - Release: **10,663,390 bytes (≈ 10.2 MiB)** unsigned release APK at
    `app/build/outputs/apk/release/app-release-unsigned.apk` — R8 shrinking +
    resource optimization enabled via `optimization { enable = true }` (AGP 9.3+
    DSL). Unsigned (no signing config, no keystore); must be signed before any
    Play distribution. No app-specific ProGuard rules were needed.
- **Analytics chart edge (corrected in code; runtime visuals unverified):** Home and Insights now
  pass only positive net category slices and a positive total to the "Where money went"
  chart/legend, with a neutral empty state when no positive slice remains, so a refund-heavy
  negative-net category no longer yields a negative or confusing chart/share; day-header/net signs
  and the "Net spending" labels were also corrected. The emulator matrix ran and passed, but no
  chart-specific visual assertion or retained screenshot accompanies it, so runtime chart visuals
  remain unverified.
- Localization is English-only (`res/values/` plus `values-night/`; no translated locales) and part
  of the UI copy lives in Kotlin literals rather than string resources.
- **Release build:** `:app:assembleRelease` exits 0 and produces `app-release-unsigned.apk`
  (**10,663,390 bytes ≈ 10.2 MiB**). R8 full-mode shrinking + optimized resource shrinking
  are enabled (`optimization { enable = true }`, default Android keep rules, no custom
  ProGuard rules). The retained artifact is **unsigned** (no signing config, no keystore) and
  cannot be installed as-is. As a verification-only smoke, a **temporary copy** was signed with
  the existing **debug** keystore (`CN=Android Debug` — not production signing),
  `apksigner`-verified, installed with app data cleared, and `MainActivity` launched to resumed
  state with no `FATAL EXCEPTION` and no E-level `mizani` logcat lines; the test-signed copy (and
  its idsig) was then deleted. This smoke does **not** constitute production signing — sign with
  a real release keystore before any Play distribution. Application ID, Room schema, and
  behavior are unchanged.
- Baseline profiles/macrobenchmarks require physical reference devices and are not included.
