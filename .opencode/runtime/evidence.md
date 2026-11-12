# Evidence

## Build (confirmed)
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL (after structural redesign and after final defect fixes).
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL (repeated after each pass).
- `:app:assembleDebug` — BUILD SUCCESSFUL (repeated after each pass).
- `:app:compileDebugAndroidTestKotlin` — BUILD SUCCESSFUL (after stale Bills assertion correction).
- Final `:app:testDebugUnitTest` after the forward-looking upcoming-window fix — BUILD SUCCESSFUL in 8s, EXIT=0, no failing tests.
- `git status --short` after all changes — exactly the 13 expected paths, no unexpected files.
- No instrumentation suite run (would reset the emulator ledger).

## Device (foldable emulator, confirmed)
- emulator-5554, sdk_gphone64_x86_64, Android 17, 390 dpi.
- Posture command: `adb shell cmd device_state state <int>` (0 closed, 1 half-opened, 2 opened).
- Closed: 1080x2364 px = 443x970 dp, bottom nav (baseline).
- Half-open: 2076x2152 px, app content confined to the left ~970px pane (398dp), bottom nav (baseline).
- Open: 2076x2152 px = 852x883 dp, left rail; content ~724dp; two columns expected.
- FLAG_SECURE: captures are black; never bypass. Bounds evidence only.

## Open posture, populated (confirmed after install)
- Two test transactions added through the UI: expense UGX 25,000 (Housing & rent, Cash) and income UGX 100,000 (Salary, Cash).
- Nav: left rail (Home x=76, Activity x=65, Insights x=62, Bills x=88, More x=82).
- Two columns: `Recent activity` x=309; `Spending` x=1210.
- Full-width hero band: `Across wallets` x=323; `Income` x=1191, `Spending` x=1476, `Net` x=1815; values `UGX 100,000`, `UGX 25,000`, `+UGX 75,000`.
- Period progress: `1–30 November 2026`, `Day 12 of 30`.
- 7-day pulse present: `Last 7 days`, bars for 6–12 Nov, today UGX 25,000.
- Max text right edge 2008 < 2076; no clipped/overlapping bounds.
- Defect found: quick actions were 2x2 (Add expense y=906, Transfer y=1117) because the 354dp column hit the 340dp threshold. Fixed to 300dp threshold; divider flush fix pending build.

## Final posture matrix (all PASS)
- **Open 852dp:** rail labels share x (Home 76, Activity 65, Insights 62, Bills 88, More 82). Actions one row (labels y=1043; Add expense x=313, Add income 522, Transfer 747, Import 961). Left column: Recent activity x=309, Your wallets x=309 (y=1719); right: Spending x=1210, Daily momentum x=1341. Max right 2008 <= 2076. Recent activity / Last 7 days / Daily momentum present.
- **Half-open:** print-state=1; bottom nav shares y=1976; actions one row all y=1186; max right 970 (inside the app pane); no horizontal overflow. The app is confined to the OS-assigned pane; the other pane remains available to the system.
- **Closed 443dp:** bottom nav shares y=2207; actions one row all y=1202; max right 1012 <= 1080; Recent activity + Spending visible; lower sections scroll.
- **Large font 1.3 (Open):** actions 2x2 (Add expense/Income y=1378, Transfer/Import y=1599); rail labels present; hero stats end <=1850; max right 2008 <= 2076. Original font scale 1.0 restored and verified.
- Final state left: posture 2, font 1.0, app foreground, two disposable transactions.

## Final regression on the final APK
- `NavigationBackRegressionTest` after Insights/More/Settings/width fixes: **3 tests, 0 failures, 0 errors, 0 skipped**, exit 0. The test task uninstalled app+test packages afterward; both were reinstalled.
- The Pixel 10 Pro Fold AVD crashed again during the final demo seed; a stable `Medium_Phone_API_36.1` (1080x2400, 420dpi) auto-started. Fold posture verification remains valid from the earlier run; wide-layout verification for the new pages will use a temporary logical `wm size` override (838dp) on the phone, then reset.

## Phone demo state (final device available)
- Medium_Phone_API_36.1, 1080x2400, 420dpi, font 1.0. App 1.9.0 installed; onboarding complete.
- Seeded through UI: expense UGX 25,000 (Housing & rent, MTN MoMo), income UGX 100,000 (Salary), one-off bill "Internet" UGX 75,000 due today, uncategorised.
- `demo-phone-home.txt` (9,299 B, 3 scroll segments) proves Coming up + Internet + Due in the next 7 days + UGX 75,000, Recent activity, Last 7 days, Daily momentum, four actions one row y=1318, max right 1006 <= 1080.
- The Pixel Fold AVD remains unavailable; wide-layout verification for the new pages will use a temporary logical width override (2200x2400, 420dpi ≈ 838dp) and be reset afterward.

## Release cut
- Version advanced explicitly by the user to **1.10.0**, derived Android versionCode **1010000** (strictly greater than 1.9.0/1009000).
- `CHANGELOG.md` has a dated `[1.10.0] - 2026-11-13` section with all redesign/currency/Bills/lock/About entries and a fresh empty `[Unreleased]`.
- `docs/PLAY_STORE_RELEASE_CHECKLIST.md` and `docs/VERIFICATION.md` updated for 1.10.0/1010000.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:printReleaseVersion` all green; print output 1.10.0/1010000.

## Final 1.10.0 device state
- Version 1.10.0 / versionCode 1010000 built and installed over demo data on Medium_Phone_API_36.1.
- About: `Version 1.10.0 (build 1010000)` and `Sideloaded build 1010000`; Home retains MTN MoMo, +UGX 100,000, −UGX 25,000, Coming up Internet −UGX 75,000.
- Lock redirect: `settings/app-lock` opens Security, shows the inline banner and a toast; auto-scroll is conditional (no movement when Security is already on-screen). Manual swipe proves the content remains scrollable.
- Bills spacing: tab container bottom → intro top = 42px (16dp); intro node 165px, three lines; bottom padding/line-height change applied.
- Currency picker: device count 153 of 153 (Android runtime differs from JVM 155); search `jpy` → 1 result; JPY row selected; field shows `JPY · Japanese Yen`; cancel leaves the bill at UGX 75,000.
- Bills spacing/currency/about/lock dumps saved under `C:\Users\USER\AppData\Local\Temp\opencode\mizani-shots\`.

## Limitations / notes
- Visual pixels cannot be captured: FLAG_SECURE intentionally renders screencaptures black. Evidence is layout bounds + text semantics, not color/spacing screenshots.
- No instrumentation suite run (it resets the emulator ledger). `compileDebugAndroidTestKotlin` passes; the stale "Bills & commitments" assertion was corrected to "Bills & recurring".
- "Coming up" was not visually exercised because the emulator has no bills/recurring items; its data path is the existing `AnalyticsEngine.upcoming` over a 45-day forward range.
- Two disposable transactions remain on the emulator for inspection (UGX 25,000 expense, UGX 100,000 income).
- Final APK smoke: `adb install -r` Success; versionName=1.9.0; Open posture; actions one row y=1043; Recent activity / Last 7 days / Daily momentum present; max right 2008 <= 2076; rail present; `git diff --check` clean; exactly the 13 expected modified files, no untracked artifacts.
- Focused instrumentation `NavigationBackRegressionTest` (final APK, open fold): **3 tests, 0 failures, 0 errors, 0 skipped**, 17.9s, exit code 0. This directly exercised the empty-Home `Add expense` tile, top-level tab switching, back from Wallets/Categories, and the corrected `Bills & recurring` title. TestState.reset cleared the emulator ledger (expected/disclosed).
- `git diff --check` exit 0; 13 modified files, no unexpected artifacts.
- Guest journey `GuestJourneyTest` (final APK, open fold): **1 test, 0 failures, 0 errors, 0 skipped**, 15.1s, exit code 0. Covers onboarding → empty Home (Daily momentum strings) → Activity add expense/income → Recent activity → edit transaction.
- Domain fix after device review: `AnalyticsEngine.upcoming()` now includes one-off commitments (remaining workspace amount, due today..range end, non-archived) in addition to recurring items and debts. New unit test added; `:app:testDebugUnitTest` **20 tests, 0 failures**, compile green. This makes the new Home "Coming up" card correct for one-off bills.
- After the user restarted Android Studio/emulator: device online. App package had been removed by the crash; final APK reinstalled successfully. Onboarding was completed through the UI; demo data seeded: MTN MoMo wallet, expense UGX 25,000 (Housing & rent), income UGX 100,000 (Salary), and a one-off bill "Internet" UGX 75,000 due today.
- Final "Coming up" proof (`demo-home-comingup.txt`): title x=1210 y=1682; Internet row [1341,1872][1729,1911]; amount [1811,1890][1998,1929]; "Due in the next 7 days" + UGX 75,000; four actions share y=1043; max text right 2008 <= 2076; 61 text nodes, 0 overlapping pairs. This proves the one-off-bill domain fix reaches the Home card.
- Additional redesign pass: Insights rebuilt with full-width cash-flow hero, responsive two columns, export in the app bar, accent section headers; More rebuilt with centered 680dp/two-column hub; Settings centered 680dp with accent section headers and divided Appearance card; Home/Insights/More/Settings max-width modifier order corrected (`widthIn(...).fillMaxWidth()` / `widthIn(...).fillMaxHeight()`). Insights/More/Settings compile+assemble green.
- Performance: `AnalyticsEngine.dailySpending(days, today)` single-pass replaces seven `expenseMinor` scans; unit-tested equivalence. Unit suite 20+ green.
- Final working tree: 19 expected modified tracked files (Home, Activity, Navigation, AdaptiveLayout, Components, WalletBalancePrivacy, strings, CHANGELOG, docs, NavigationBackRegressionTest, Analytics, Home, Insights, More, Settings, AnalyticsEngineTest, Fixtures + runtime 3). No untracked artifacts.
- App left on Home, Open posture, font scale 1.0, with demo data. Balance hero/account balances remain masked because no app PIN/reveal was set.
