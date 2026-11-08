# Verification and release notes

## Automated verification

The current debug build, JVM unit tests, Android test compilation, and isolated guest journey passed
on 2026-10-26. The guest journey ran on the API 36.1 `Medium_Phone_API_36.1` emulator and drove
first-run onboarding, expense entry, income entry, and ledger balance assertions.

```powershell
.\gradlew.bat --% :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.GuestJourneyTest --no-daemon
```

Results: 93 JVM tests passed; Android test sources compiled; `GuestJourneyTest` passed (1 test);
`app/build/outputs/apk/debug/app-debug.apk` was produced (30,489,483 bytes). `git diff --check`
also passed; Git reported only the repository's existing LF-to-CRLF working-copy warnings.

Each instrumentation class runs in a separate Gradle invocation because the classes reset local app state.
On 2026-10-28, all 11 classes passed individually on the API 36.1 QA emulator: AppLockTest,
AppearanceAndSecurityEndToEndTest, BackupRestoreTest, DatabaseMigrationTest, DemoDataTest,
GuestJourneyTest, RepositoryIntegrationTest, SettingsAndReminderEndToEndTest, SettingsPersistenceTest,
SplashScreenTest, and WalletBalancePrivacyEndToEndTest.
The high-volume `LargeDatasetPerformanceTest` also passed with 10,000 encrypted transaction rows:
seed 3,774 ms, snapshot 261 ms, export 176 ms, Home ready 3,724 ms, Activity ready 1,103 ms,
search result 744 ms, and Insights ready 1,275 ms on the same emulator. The test leaves generous
failure thresholds for slower devices and logs the timings so future regressions are measurable.
On 2026-10-28, testDebugUnitTest passed all 118 tests across 17 suites. The debug APK and Android test
APK built successfully, and lintDebug completed with no errors (50 findings: 17 unused resources,
8 autoboxing allocations, 7 KTX suggestions, 7 Gradle dependency recommendations, 5 newer-version
notices, and 6 smaller style/resource findings). The migration tests use the checked-in v1-v3 Room
schema snapshots as test assets; a debug-only serialization BOM keeps Room 2.8.5 migration parsing
compatible with its runtime.
On 2026-10-29, the focused `NavigationBackRegressionTest` passed on the API 36.1 emulator. It
exercises More → Wallets → Back and More → Categories → Back, then asserts that the returned More
screen remains interactive. `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, and `lintDebug`
also passed after the transition and Money Outlook copy changes. The two-test
`WalletBalancePrivacyEndToEndTest` class passed after being updated to assert the dot mask and its
non-revealing accessibility label.
The three-test `SettingsAndReminderEndToEndTest` class also passed: it verifies the settings toggle
queues WorkManager work, a delayed daily check still posts after its target date, repeated daily work
does not duplicate a notification, and a commitment reminder posts generic private copy.

## Manual emulator acceptance

On 2026-10-28, the new flows were exercised on a fresh, disposable API 36 x86_64 Google Play AVD
(`Mizani_QA_API_36_1`) created inside the workspace. Each instrumentation class ran in its own
Gradle invocation because tests clear their local app state.

```powershell
.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.GuestJourneyTest --no-daemon
.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.AppearanceAndSecurityEndToEndTest --no-daemon
```

Results: `GuestJourneyTest` passed (1 test), including type-specific wallet suggestions, the final
Storyset illustration's accessible description, and tapping a recent expense to open that
transaction's edit screen from Home. `AppearanceAndSecurityEndToEndTest` passed
(3 tests), covering saved appearance options and persistence, absence of the app and budget sharing
entry points, and fresh PIN confirmation for financial exports and encrypted backup creation.

The APK update issue was checked separately on that disposable AVD: after accepting the current
legal documents with setup unfinished on 1.2.0, a temporary 1.2.1 debug APK was built with a
command-line version override and installed in place. After force-stop and relaunch, setup resumed
without showing the Terms page. The repository version and normal debug artifact were restored to
1.2.0 afterward. `FLAG_SECURE` blanks app-window screenshots, so
UI assertions and the resumed system activity were used instead of screenshot review; pixel-level
visual contrast was not assessed.

### Hands-on exploratory run (2026-10-28)

A first-use and returning-user walkthrough ran on the API 36.1 `Medium_Phone_API_36.1` emulator,
Android user 10, using the read-only AVD image. Sample records and the disposable PIN stayed in that
isolated user.

- Completed consent and setup with Uganda, UGX and a cash wallet. Home exposed Add expense, Add income,
  masked balance, PIN setup and Bills in bottom navigation.
- Recorded UGX 600,000 income and UGX 250,000 expense. Home totals updated and Activity showed exactly
  two rows.
- Opened Bills from bottom navigation, confirmed the one-off bill guidance, exercised empty-name
  validation, opened the category and due-date controls, and saved a UGX 500,000 bill. Recorded and
  linked the UGX 250,000 expense; Bills showed UGX 250,000 paid and the Activity list still contained
  two transactions, so linking did not create a duplicate.
- Measured the commitment and daily-target dialogs at about 94% of the emulator's 1080 px width
  (bounds 32-1047 px). The controls reflowed with the keyboard; secure-screen capture prevented a
  pixel-level visual check of keyboard overlap.
- Set a UGX 35,000 daily target. Insights kept the target visible while masking estimated coverage and
  upcoming-payment amounts with wallet balances hidden. Activity search for Fuel returned one row.
- More exposed Wallets, Categories, Export or back up data, Settings, and About and privacy. Bills
  remained a first-level bottom tab.
- Set up a six-digit PIN from the Home prompt, used Lock now, unlocked, revealed balances after another
  PIN confirmation, then hid them again.

`FLAG_SECURE` blanks app screenshots, so labels and bounds were checked through the UI hierarchy rather
than screenshots. This pass used one emulator size; TalkBack and font scaling were not exercised on a
physical device.

### Home wallet layout audit (2026-10-28)

The Home flow was reviewed on the disposable API 36.1 emulator before and after the wallet redesign.
The previous state used a horizontal wallet carousel (bounds `[42,699][1038,997]`) plus a separate
balance-visibility row (`[42,553][261,679]`). The updated empty-ledger state places the getting-started
hero first (`[95,458][985,1215]`) and keeps wallets below it in one grouped card. With two wallets,
both names and balances were visible in the same vertical card; no horizontal carousel was present.
The privacy action and `See all` remain in the wallet header, and the whole grouped card opens Wallets.

The same flow was checked after temporarily setting the disposable emulator to a compact `720x1600`
display and `1.3` font scale. The hero actions and both wallet rows remained reachable by scrolling;
that pass also identified that the three header actions needed a responsive treatment. The final
header now switches the privacy action to its icon-only accessible variant at compact width or large
font scale, preserving the title and `See all` action without competing for space. The emulator was
restored to `1080x2400` and font scale `1.0` afterward. Pixel screenshots remain unavailable because
the app uses `FLAG_SECURE`; this audit therefore used the user-provided screenshot plus emulator
hierarchy bounds. TalkBack, a physical device, and extreme long-balance values remain follow-up
coverage items.

The reminder instrumentation verifies that the UI enables the setting, WorkManager queues a tagged
request, the worker posts the generic private notification, and a repeated worker run does not post a
duplicate. A real wall-clock attempt did not run within 240 seconds on this emulator, so exact delivery
timing under Android's background scheduler remains unverified.

### Money Outlook and More navigation regression (2026-10-29)

The Money Outlook subtitle now describes the feature in plain language: “See how long your money may
last at your current daily spending.” When wallet balances are hidden, the estimate is rendered as
six dots rather than the confusing phrase “Hidden while balances are hidden”; its accessibility label
still announces that the estimate is hidden without revealing the value. The daily target remains
visible as the user requested.

The reported gray/frozen state after returning from a More-screen detail was traced to fading both
routes during a pop transition, which could expose the Scaffold background during a quick Back press.
Route transitions now retain the restrained slide motion without cross-fading, and the focused
instrumentation test covers the affected detail routes. Pixel-level screenshot validation remains
unavailable because the app intentionally uses `FLAG_SECURE`; hierarchy assertions and emulator
interaction were used instead.

Reminder delivery now includes a user-facing “Send a test notification” action in Settings. Daily
reminders keep their generic privacy-safe copy, and delayed WorkManager execution no longer drops a
check when Android runs it after the originally scheduled date. Exact wall-clock delivery under
Doze, OEM battery restrictions, reboot and timezone changes remains physical-device follow-up coverage.

### Remaining manual acceptance

1. Start a fresh workspace offline and check that core ledger actions remain available.
2. Record foreign-currency transactions and validate conversion, original values, rate date and exports.
3. Check cash-flow trends across multiple months and Money Outlook with more than seven days of history.
4. Validate CSV and Excel file contents, and test backup restore against a real exported file.
5. Use TalkBack, larger text, and small-screen/landscape layouts on a physical device.
6. Share or paste an MTN MoMo or Airtel Money confirmation, review the suggestion, save it, and retry
   the same reference to verify duplicate rejection and no SMS permission request.
7. Link multiple partial bill payments, unlink one while preserving the ledger expense, and confirm
   commitment reminders reveal no name or amount on the lock screen.
8. Verify actual scheduled reminder timing on a physical device across app restart, reboot and a
   timezone change. The worker itself is covered deterministically; these system-delivery conditions
   are not.
9. Repeat the 10,000-row stress scenario at 50,000+ rows on representative low-end physical devices
   before making a hard capacity claim; the current automated gate covers 10,000 rows on the API 36.1
   emulator.
### Investment tracking validation (2026-10-30)

The manual investment snapshot feature was added as Room schema version 5 with a forward-only
4→5 migration. The focused JVM suite and Android test-source compilation passed after the feature,
backup, legal, and migration changes:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon
```

The checked-in Room schema is `app/schemas/com.mizani.money.data.local.MizaniDatabase/5.json`.
The repository now includes unit coverage for active/archive filtering, zero-value holdings,
checked aggregate arithmetic, a real encrypted repository investment invariant test, backup summary
and restore assertions, and a v4→v5 migration test. The migration, repository, investment UI,
navigation, reminder, backup, privacy, and performance instrumentation tests were run on the
disposable `Medium_Phone_API_36.1` emulator (API 36, x86_64): 30 tests passed with zero failures,
errors, or skips. The 100-scenario matrix is tracked in `docs/INVESTMENT_QA_MATRIX.md`; manual
physical-device and Play Console rows remain release-owner gates.

### 1.9.0 release metadata preparation (2026-11-08)

The release metadata was advanced to `1.9.0` with the derived Android version code `1009000`.
The following repository checks passed against this source tree:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

This is metadata and debug-build preparation, not release sign-off. No signed release artifact,
connected instrumentation run, Play Console submission, or production deployment was performed for
this version in this workspace.

### 1.8.0 release-candidate verification (2026-11-01)

The release metadata was advanced to `1.8.0` with derived Android version code `1008000`. The
following checks passed against that source tree:

```powershell
.\gradlew.bat --% :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon -Pkotlin.incremental=false
.\gradlew.bat --% :app:connectedDebugAndroidTest --no-daemon -Pkotlin.incremental=false
.\gradlew.bat --% :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon -Pkotlin.incremental=false --no-configuration-cache
.\gradlew.bat --% :app:assembleRelease :app:bundleRelease :app:lintRelease :app:signingReport --no-daemon -Pkotlin.incremental=false --no-configuration-cache
```

The full connected instrumentation suite passed on the disposable `Pixel_10_Pro_Fold(AVD) - 17`
device: 39 tests, 0 failures, 0 errors, and 0 skips. The final debug quality gate also passed 148
JVM tests, debug lint, and debug assembly. The 10,000-transaction performance case completed
Activity search in 3,559 ms after the local search-index and typing-coalescing fix. The release APK,
AAB, and release lint also passed; `aapt` confirmed `versionName=1.8.0`, `versionCode=1008000`,
`minSdkVersion=24`, and `targetSdkVersion=37`. The release outputs were
`app-release-unsigned.apk` (6,981,758 bytes, SHA-256
`E2E48E75C8066C60F74BF8DBA72B6A3F38AAD71F78B1F7949E55A9538433093F`) and `app-release.aab`
(9,955,045 bytes, SHA-256
`7D81CB5C83F4642B71168D997953B4E0D106ED85FF1DAFFBF42A868226F3FA52`). `jarsigner` confirmed the
release AAB is unsigned in this workspace because release signing credentials are intentionally not
stored here; signing, signed-AAB verification, Play Console review, and physical-device gates remain
owner actions.

The foldable exploratory pass covered open, half-open, and cover states at normal and 130% system
font scale, including navigation, Home/Momentum scrolling, Activity, Settings/reminder controls, and
the add-entry sheet. UI hierarchy bounds remained inside the active display. Because `FLAG_SECURE`
intentionally blanks ordinary screenshots, this is not a pixel-level screenshot sign-off; the pass
used semantics, bounds, and real interactions without weakening the financial-data protection.

### Reminder time validation (2026-10-30)

Reminder times are stored as minute-of-day values and scheduled against the device's current local
`ZonedDateTime`. The Settings picker and summary now use an explicit locale-aware 12-hour display,
including `12:00 AM` and `12:00 PM`, while the worker compares the full scheduled instant so a
worker that runs just before the selected minute does not wait for the retry backoff window. System
time, date, and timezone changes continue to invalidate and rebuild the pending schedule.

Verified with:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleDebug :app:lintRelease :app:bundleRelease --no-daemon
```

The release lint/build completed successfully. Android background scheduling remains best-effort:
Doze, OEM restrictions, and battery saving can delay a notification. The app intentionally does
not request exact-alarm access for a non-critical budgeting reminder.

### Local habit check-ins (2026-10-30)

The optional local money check-in feature now evaluates spending, expected income, manual
investment valuations, debt payment dates and savings goals from the encrypted Room snapshot. The
master switch and every category are opt-in, notifications use neutral private copy, and delete or
restore paths cancel posted notifications and queued reminder work.

Verified with:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon
.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.SettingsAndReminderEndToEndTest --no-daemon
```

Results: 143 JVM tests passed; Android test sources compiled; and all 5 tests in
`SettingsAndReminderEndToEndTest` passed on the disposable API 36.1 `Medium_Phone_API_36.1`
emulator. The emulator tests covered Settings placement, explicit debt-category opt-in, a real
generic private debt check-in, no debt name or amount in notification extras, repeated-worker
suppression, delayed daily delivery, and existing commitment reminder behavior. Exact delivery
under Doze, OEM battery restrictions, reboot and timezone changes remains follow-up coverage.

### Google Play update flow (2026-10-30)

Play-installed builds now use Google Play's in-app update service when the app is active. Normal
releases use the flexible flow so Play can download in the background; releases with Play priority
4 or higher use the immediate flow. A downloaded flexible update is completed through Play's own
confirmation UI. Sideloaded builds retain the manual GitHub release check in About & privacy.

Verified with:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon
.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.FeedbackEndToEndTest --no-daemon
.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon
```

The unit tests, Android test compilation, focused feedback emulator suite, debug assembly and lint
passed. The emulator was a sideloaded debug build, so Google Play's real availability response and
download UI require a Play internal-test track with a signed published artifact; that release-owner
gate remains unverified here.

### Default reminders, adaptive layout and Daily Momentum (2026-11-01)

The daily local reminder now defaults to enabled for a completed workspace, remains opt-out, and
does not schedule or post before onboarding creates the workspace. The Home screen derives private
daily points, streaks and milestone badges from the encrypted transaction snapshot; it adds no
database fields, network calls, account system, or spending rewards. Navigation and crowded Home and
Activity summaries reflow from the available window width and font scale: compact windows use
accessible icon-only navigation when labels would wrap, while wider windows use a navigation rail.

Verified on the disposable API 36.1 `Medium_Phone_API_36.1` emulator with:

```powershell
.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.GuestJourneyTest --no-daemon -Pkotlin.incremental=false
.\gradlew.bat --% :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mizani.money.SettingsAndReminderEndToEndTest --no-daemon -Pkotlin.incremental=false
.\gradlew.bat --% :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon -Pkotlin.incremental=false
```

All three commands passed. Manual UI-hierarchy checks also passed at font scale 1.3 on a 720 px
compact window and at an 800 dp-equivalent wide window: navigation labels remained available to
accessibility services, action buttons retained usable bounds, and the rail avoided bottom-nav
compression. Screenshot review remains unavailable for the secure app window; physical foldable,
TalkBack and OEM notification-delivery checks remain release-owner follow-up work.

The local SDK did not provide a foldable AVD: `adb shell cmd device_state print-states` exposed only
the `DEFAULT` state. The additional compact and wide checks therefore simulated cover-like and
unfolded window sizes with `adb shell wm size` and density overrides; hinge posture, cutout, and
spanning behavior remain unverified until a real foldable profile is available.

The Pixel 10 Pro Fold API 17 AVD was subsequently exercised in `OPENED`, `HALF_OPENED`, and
`CLOSED` states. Home and Activity stayed within the active display bounds at normal and 1.3x font
scale; the cover navigation retained accessible labels when its visible labels were hidden; and the
Daily Momentum card exposed its title, tracked-day stats, action, milestone, and next-milestone copy
after scrolling. The `CLOSED` state initially showed the emulator keyguard and was dismissed before
app assertions. The app window is secure, so screenshots of its content render black; these checks
use UI hierarchy, bounds, semantics labels, and interaction evidence instead of claiming visual
pixel review.
The additional `REAR_DISPLAY_MODE` and `CONCURRENT_INNER_DEFAULT` states exposed the launcher on
this AVD rather than an app window, so they were recorded as state-transition checks only.

## Scope and data compatibility

The active app flow includes Home, Activity, Insights, wallets, categories, data export, user-reviewed
mobile-money text import, and one-off commitments. Repeating schedules and debt remain separate
existing data models; this feature does not create duplicate recurring or debt modules. Budget,
savings-goal, and account/sync screens remain outside the active flow, while their existing database
entities and encrypted backup fields are retained. No production release or external deployment was
performed.
