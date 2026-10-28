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

A diagnostic run of all instrumented classes in one Gradle invocation was not green; it reported
cross-test state failures. This project requires one class per invocation because each class resets
its test database. Isolated runs passed for `GuestJourneyTest` (1 test), `RepositoryIntegrationTest`
(2 tests), `BackupRestoreTest` (1 test), and `DemoDataTest` (1 test). `AppLockTest` still times out
waiting for `Welcome back` when run alone and remains unresolved.

On 2026-10-28, `:app:testDebugUnitTest` passed all 118 tests across 17 suites. The debug APK and
Android test APK built successfully, and `:app:lintDebug` completed with no errors (41 warnings,
mostly dependency-version, unused-resource, and existing style findings). The new migration, backup,
and repository instrumentation tests compiled, but were not run during this verification because no
ADB-ready disposable emulator was available. The existing emulator remained offline, and the fresh
workspace AVD was unauthorized by ADB despite a restart with the local host key; no app state was
cleared or reinstalled on either device.

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

The broader manual checklist below remains unverified:

1. Set up a fresh local workspace and try the app offline.
2. Record income and expenses in the workspace currency; confirm Home totals, Activity rows and
   category analysis update together.
3. Record foreign-currency income and expenses. Check the converted amount, original amount,
   reference-rate date, and exported values. Rates are daily reference rates and may differ from a
   bank, card or mobile-money provider's settlement rate.
4. Review the six-month cash-flow chart and income trends across multiple reporting periods.
5. Export CSV and Excel, then create and restore an encrypted backup.
6. Check app lock, larger text, TalkBack labels, and navigation transitions on a physical device or
   emulator.
7. Share or paste an MTN MoMo or Airtel Money confirmation, review and edit the suggestion, save it,
   then retry the same reference and confirm it is rejected. Confirm no SMS permission is requested.
8. Create a one-off commitment, link multiple recorded expenses as partial payments, then unlink one
   and confirm the ledger expense remains. Check the due reminder shows no name or amount on the lock screen.
9. Review Money Outlook with less than seven days of activity, with a saved daily target, and with
   enough history. Confirm expected income does not raise the estimate and linked payments are not reserved twice.

## Scope and data compatibility

The active app flow includes Home, Activity, Insights, wallets, categories, data export, user-reviewed
mobile-money text import, and one-off commitments. Repeating schedules and debt remain separate
existing data models; this feature does not create duplicate recurring or debt modules. Budget,
savings-goal, and account/sync screens remain outside the active flow, while their existing database
entities and encrypted backup fields are retained. No production release or external deployment was
performed.
