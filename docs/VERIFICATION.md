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
On 2026-10-28, testDebugUnitTest passed all 118 tests across 17 suites. The debug APK and Android test
APK built successfully, and lintDebug completed with no errors (50 findings: 17 unused resources,
8 autoboxing allocations, 7 KTX suggestions, 7 Gradle dependency recommendations, 5 newer-version
notices, and 6 smaller style/resource findings). The migration tests use the checked-in v1-v3 Room
schema snapshots as test assets; a debug-only serialization BOM keeps Room 2.8.5 migration parsing
compatible with its runtime.

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

The reminder instrumentation verifies that the UI enables the setting, WorkManager queues a tagged
request, the worker posts the generic private notification, and a repeated worker run does not post a
duplicate. A real wall-clock attempt did not run within 240 seconds on this emulator, so exact delivery
timing under Android's background scheduler remains unverified.

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
## Scope and data compatibility

The active app flow includes Home, Activity, Insights, wallets, categories, data export, user-reviewed
mobile-money text import, and one-off commitments. Repeating schedules and debt remain separate
existing data models; this feature does not create duplicate recurring or debt modules. Budget,
savings-goal, and account/sync screens remain outside the active flow, while their existing database
entities and encrypted backup fields are retained. No production release or external deployment was
performed.
