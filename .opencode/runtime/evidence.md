# Evidence — foldable responsiveness

## Environment (confirmed via test-worker)
- Emulator serial `emulator-5554`, Pixel 10 Pro Fold AVD, API 37 (Android 17),
  `sdk_gphone64_x86_64`, physical 2076x2152 @390dpi when unfolded.
- Posture control: `adb emu posture 1|2|3` = CLOSED | HALF_OPENED | OPENED.
  `adb emu rotate` rotates clockwise. `adb emu posture 4|5` (flipped/tent) rejected by this AVD.
  `cmd device_state` states: 0 CLOSED, 1 HALF_OPENED, 2 OPENED, 3 REAR_DISPLAY_MODE,
  4 CONCURRENT_INNER_DEFAULT.
- Current tree version 1.10.0 / versionCode 1010000. Debug APK built and installed over existing
  data (no clear/uninstall). Home renders; app window in CLOSED is 1080x2364.
- FLAG_SECURE confirmed: guest `screencap` is 100% black (0 non-black pixels).
  Host-side emulator-window capture is possible and shows real app pixels (user screenshot proves).

## Confirmed current behavior
- OPENED flat / CLOSED cover: Home hierarchy has bottom nav at y≈2207 in 2364-high window;
  content and controls are inside display bounds.
- HALF_OPENED book (user-provided screenshot + current policy): `MizaniNavHost` computes
  `AdaptiveLayout.largestSafePane`, then renders the entire app in only the larger pane with
  `Modifier.width/height/offset`. The other pane is an empty `Surface` background (white in the
  user's screenshot). This is the reported defect.
- HALF_OPENED tabletop/horizontal hinge is not intentionally designed; the app is squeezed into
  the top or bottom pane and still shows the wide-window navigation rail, leaving the rest blank.
- Dialog/bottom-sheet windows are separate full-window windows; `MizaniAlertDialog` and
  `ModalBottomSheet` are centered on the whole display, not the active pane, so they can cross a
  separating hinge. Needs runtime verification.
- Onboarding (`widthIn 560`), LegalConsent (`widthIn 520`) and LockScreen (`safeDrawingPadding`,
  full width) do not consume `FoldingFeature` at all; `MainActivity` passes fold info only to
  `MizaniNavHost`. They can therefore straddle a hinge while locked/onboarding.
- `showAddSheet` uses `remember`, so activity recreation on rotation/posture change dismisses it.
  Critical form fields in `AddTransactionScreen` mostly use `rememberSaveable`; nav controller
  saves its back stack.
- `MainActivity` collects `WindowInfoTracker` via `produceState` with initial `null`, causing a
  first-frame full-window flash before the pane offset is applied.

## New two-pane build (installed 2026-11-13)
- Build/install of 1.10.0 debug succeeded; Home opens in OPENED.
- OPENED rot0: full display, left rail, content fills; no empty half.
- HALF_OPENED book rot0: left pane `[0,136][1038,2074]` = sidebar (My money/Private by design,
  four quick actions, Home/Activity/Insights/Bills/More, privacy footer); right pane
  `[1038,136][2076,2074]` = Home (hero/wallets/momentum); no bottom bar/rail. Both panes filled;
  hinge gap intentional. Screenshot `fold_book.png` visually confirms.
- HALF_OPENED tabletop: top content `[0,136][2152,1038]`; bottom dock quick actions at y≈1059-1176
  and five nav items at y≈1195-1390. The dock leaves ~686px empty below the nav; fix in progress.
  A 90° screenshot was 180° rotated because the emulator panel and framework rotation were not
  synchronized; the 270° capture (`fold_host_C.png`) was upright. Treat as capture artifact.
- Slight-fold hinge control works: `adb emu sensor set hinge-angle0 45` (state remains
  HALF_OPENED); verify layout stability at 45/90/145/155 and restore 90.
- Device left by a worker in HALF_OPENED/rot1 with `wm user-rotation lock 1`; next worker restores.

## Follow-up build / status
- Dock/sidebar polish build: BUILD SUCCESSFUL, 26 test classes, FoldWindowTest 12/12. Dock now has
  header + quick actions + weighted spacer + bottom NavigationBar with zero own insets; sidebar
  footer pinned. Not yet installed on the emulator (last installed build is the core two-pane).
- Lock-screen dedicated split worker returned without edits. Retrying with a minimal, safer fix:
  scrollable lock content + `FoldAwareDialogHost` around PinSetup/PinVerify; the lock still uses
  the `FoldAwareScreen` content-pane wrapper.
- Device was left CLOSED with user-rotation locked; a restore/check worker is running.
- Dialog/add-chooser pane containment and custom dialog wrapping are in progress.

## On-device core layout evidence (installed build)
- Restored OPENED/rot0; screenshot 2076x2152 upright.
- CLOSED outer display 1080x2364: all 104 nodes inside; bottom nav Home [90,2207], Activity
  [284,2207], Insights [485,2207], Bills [715,2207], More [913,2207]; no blank pane.
- Book sidebar Activity row [39,714][999,851] tapped at (519,782): right pane showed Activity,
  "0 transactions", filters, empty state; sidebar remained. Home tap returned Home.
- Tabletop dock Insights item [870,1195][1284,1390] tapped at (1077,1292): top pane showed
  Insights/Money Outlook; dock remained. Home tap returned Home.
- Hinge 45°: print-state 1, mRotation 0, two-pane layout intact; restored 90.
- Logcat: no FATAL EXCEPTION / AndroidRuntime crash for com.mizani.money.
- Final: OPENED, mRotation 0, user-rotation free, Home focused.

## Final dedup build / visual matrix (2026-11-13)
- Build/install/lint: BUILD SUCCESSFUL; lint 0 errors, 49 warnings, 8 hints.
- OPENED rot0: full display, rail, clip0.
- BOOK rot0: PASS. Left panel exactly `Mizani` header + five nav destinations + one privacy line;
  quick-action labels in left panel = 0; right Home pane populated; hinge boundary x=1038 clean.
  Live taps: Activity (519,488) and Home (519,331) work.
- TABLETOP rot3: PASS. Top content; dock `Mizani` header + five nav at y1862-1901 + privacy;
  quick labels in dock = 0; dock bg fills to bottom; pixels below hinge remain dock bg.
  Raw PNG 2076x2152 rotated 90° CCW; host-rotated upright 2152x2076 matches UI.
- CLOSED: bottom nav Home/Activity/Insights/Bills/More inside 1080x2364; screenshot geometry
  caveat: host screenshot reports 2076x2152 while content is the live outer display.
- Reviewer fixes: single-date picker now has a half-open `Dialog`+`FoldAwareDialogHost` branch;
  dialog host max sizes clamped; `showAddSheet` saveable restore confirmed against Material3
  1.4.0 source (`ModalBottomSheet` auto-shows after layout) and documented.
- `:app:testDebugUnitTest :app:assembleDebug` BUILD SUCCESSFUL, 173 tests, 0 failures.

## Promotional capture (in progress)
- Use `DemoDataTest` (three wallets, salary, expenses, transfer, refund, bills, debt, goal, budget)
  for serious chart/prediction content; accept legal; set demo PIN 123456 and reveal balances so
  Insights/Money Outlook are not masked.
- CLOSED cover captures to `docs/brand/promo/screenshots/phone-raw/` (and 9:16 if the emulator
  supports a display-specific capture), plus `foldable/` open/book/tabletop showcase.
- Android-only: no iOS target, so no Apple App Store screenshots.
- Closed capture limitation confirmed: `adb emu screenrecord screenshot --display 0 <path>` gives a
  live non-black 2076x2152 image (inner-panel framebuffer geometry), while the true 1080x2364 outer
  display can only be captured by `screencap -d`, which FLAG_SECURE blanks. Do not weaken the app.
  Promo README must document the geometry; the owner can frame/crop for Play Store.
- Promo capture worker is seeding DemoDataTest, revealing balances with demo PIN 123456, capturing
  CLOSED phone screens + foldable showcase into `docs/brand/promo/screenshots/`.

## Pending evidence
- Promo capture set + README.
- Font scale 1.3; split-screen/multi-window; hinge 15/145/155; logcat after final build.
- Final docs/VERIFICATION update and reviewer confirmation.
