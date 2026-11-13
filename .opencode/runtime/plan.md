# Plan — Foldable responsiveness (Pixel 10 Pro Fold)

## Objective
Make Mizani respond correctly and intentionally to foldable postures and window changes:
OPENED, HALF_OPENED (book/tabletop, multiple hinge angles), CLOSED/cover, REAR_DISPLAY_MODE,
CONCURRENT_INNER_DEFAULT, portrait/landscape rotation, and split/resized windows. Verify on the
running API 37 Pixel 10 Pro Fold emulator with installed debug build.

## Acceptance criteria
1. Current debug APK builds and installs over the existing 1.10.0 data without data loss.
2. Every app surface (splash, onboarding, legal, lock, Home, Activity, Insights, Bills, More,
   detail/form screens, bottom sheets/dialogs) stays inside the active display and avoids the
   separating hinge; no clipped, overlapping, or unreachable primary controls.
3. OPENED flat: full inner display used; navigation rail on wide windows; content capped/centered.
4. HALF_OPENED book (vertical hinge): both panes used intentionally (nav rail/panel in one pane,
   content in the other) OR app occupies a clearly intentional pane; nothing crosses the hinge.
5. HALF_OPENED tabletop (horizontal hinge): content and primary controls are reachable in the
   correct pane; navigation/actions are not stranded above/below the hinge.
6. CLOSED/cover: compact phone layout, full cover display, all controls reachable.
7. Rotation/angle changes preserve navigation and in-progress form/sheet state where feasible;
   no crash, black screen, or stale one-frame layout.
8. REAR_DISPLAY_MODE / CONCURRENT_INNER_DEFAULT: app remains launchable/visible or a documented
   platform limitation; no crash.
9. Font scale 1.0 and >=1.3, compact/medium/expanded widths remain usable.
9a. Slight fold: FLAT/no-intersection stays full-window; HALF_OPENED splits only when both panes
    are usable (book >=300dp wide each, tabletop >=260dp tall each); otherwise compact/single
    larger pane + intentional backdrop. No blank half; no state loss at the transition.
9b. Android split-screen: app window resize reflows correctly; a hinge outside the app window does
    not force a split; a hinge inside it follows 9a; dialogs stay inside the app content pane.
10. Executable evidence: posture-by-posture hierarchy/bounds + screenshots where not FLAG_SECURE
    blocked; unit tests for fold/layout policy; focused JVM/compile checks; changelog/docs updated.

## Task graph
1. DONE: Confirm adb/gradle access through test-workers; identify AVD/API/current posture.
2. DONE: Install current 1.10.0 debug APK; confirm Home renders in CLOSED.
3. DONE: Discover posture/rotation/hinge-angle controls; baseline matrix: book blank right half,
   tabletop +24px hinge crossing, open/closed fine.
4. DONE: Audit adaptive code and design fold policy (two-pane sidebar/dock, thresholds, insets).
5. DONE: Core two-pane build installed and visually confirmed in OPENED and BOOK; 12 unit tests pass.
6. DONE: Dock/sidebar polish builds and tests pass (header, bottom nav, pinned footer).
7. DONE: Lock-screen scroll + PIN-dialog host; build passed. Dialog alert/date rewrites exist but
   were not compiled; add-chooser/custom wraps remain.
8. DONE: On-device closed/book/tabletop navigation, hinge-45, logcat checks all passed.
9. ACTIVE: Finish dialogs + navigation-only fold panels (remove duplicate quick actions, restore
   fold-mode Add FAB), build/test; then install and visually verify.
10. PENDING: Final matrix incl. open/book/tabletop/closed rotations, hinge 15/45/90/145/155,
    split-screen, font scale 1.0/1.3, form rotation, lock, dialogs/sheets; screenshots + hierarchy.
11. PENDING: Run testDebugUnitTest, compileDebugAndroidTestKotlin, lintDebug, assembleDebug.
12. PENDING: Reviewer pass (auth/lock + privacy dialog changes); update CHANGELOG/docs
    (VERIFICATION, DESIGN_SYSTEM, PROJECT_STRUCTURE); final honest handoff.

## Risks
- FLAG_SECURE blocks pixel screenshots; use UI hierarchy, app bounds, semantics, and screenshots of
  non-secure surfaces. Do not weaken FLAG_SECURE in shipped code.
- Emulator posture switching may recreate the activity and lose transient form state.
- Rear/concurrent display modes may show launcher by platform design.
- Changing whole-app pane strategy can regress phone/normal tablet layout.
