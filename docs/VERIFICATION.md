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

## Manual emulator acceptance

Only the guest onboarding and income/expense journey was verified through Compose instrumentation.
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

## Scope and data compatibility

The active app flow focuses on Home, Activity, Insights, wallets, categories, and data export. The
budget, recurring-payment, savings-goal, debt-management, and account/sync screens were removed.
Existing database entities and encrypted backup fields for those older records remain so that
restoring or exporting an older workspace does not discard stored data. No production release or
external deployment was performed.
