# Decisions — foldable responsiveness

## D1. Treat a separating fold as two panes, not a reason to shrink the whole app
Current `largestSafePane` + offset is the root cause of the empty white half. Replace with a
`FoldWindowLayout` model:
- no separating feature / FLAT / CLOSED -> `Flat`, full window.
- separating vertical feature -> `Book`: leading pane + hinge gap + trailing pane.
- separating horizontal feature -> `Tabletop`: top pane + hinge gap + bottom pane.
- degenerate pane -> fall back to `Flat`.
The model is px-based, computed from `WindowLayoutInfo` + `LocalWindowInfo.containerSize`, and
exposed through `LocalFoldWindowLayout`. It is unit-testable independently of Compose.

## D2. Use both panes for top-level navigation
- **Book**: leading pane = persistent `MizaniFoldSidebar` (brand/workspace header, four quick
  actions, five destinations, privacy footer); trailing pane = current screen. Hide the bottom
  bar and navigation rail. Selected nav item follows the current top-level route.
- **Tabletop**: top pane = current screen; bottom pane = `MizaniTabletopDock` with the five
  destinations and four quick actions. Hide the bottom bar/rail. The dock scrolls or compacts
  when the trailing pane is short.
- **Flat/CLOSED**: keep the existing bottom bar / navigation rail behavior; no regression to
  phone/tablet layouts.
This intentionally fills the pane that is currently white.

## D3. Pre-main surfaces also use the active pane
Wrap splash/startup-error/onboarding/legal in `FoldAwareScreen`: content in the content pane,
branded `FoldBackdrop` in the other pane. Lock uses a split: branding pane + PIN pane, with an
adaptive/compact keypad and scroll fallback. This prevents centered content from being bisected by
the hinge.

## D4. Dialogs and sheets must not cross the hinge
- Expose `LocalFoldWindowLayout` to dialog content.
- `MizaniAlertDialog` and `DatePickerDialog`: constrain width/height and offset to the content
  pane center; make long text/calendars scrollable inside the pane.
- Custom `Dialog` surfaces (PIN setup/verify, sensitive-action confirmation, currency picker,
  legal/third-party documents): wrap in `FoldAwareDialogHost`, which fills the dialog window but
  positions the surface in the content pane.
- Add chooser: keep `ModalBottomSheet` for flat/closed; in a half-open posture use a fold-aware
  dialog hosting the same chooser content. Make `showAddSheet` `rememberSaveable`.

## D5. Insets and hinge gap
Do not apply global `safeDrawingPadding` to the full fold container (it would shrink pane math).
Apply side-specific `WindowInsets.safeDrawing` padding inside each pane:
- book leading: top+bottom+start; book trailing: top+bottom+end.
- tabletop top: top+start+end; tabletop bottom: bottom+start+end.
Set the fold content `Scaffold(contentWindowInsets = WindowInsets(0))` to avoid double insets.
Color the root/hinge background with `surfaceContainer` so the gap is intentional, not white.

## D6. Preserve state and avoid flashes
- `showAddSheet` -> `rememberSaveable`.
- Keep `rememberSaveable` in forms (already present in AddTransaction/Onboarding).
- Seed `produceState` with the last/empty layout and gate the first fold render if needed to avoid
  a full-window flash. Do not regress startup latency.
- Verify rotation and fold recreate the activity without losing route, form fields, or sheet.

## D7. Verification
- Extend `AdaptiveLayoutTest` (or new `FoldWindowLayoutTest`) for vertical/horizontal/degenerate
  features, pane sizes, content/nav pane selection, and rotation swaps.
- Emulator matrix: OPENED portrait/landscape, HALF_OPENED book + tabletop at rotation 0/90/180/270,
  CLOSED portrait/landscape, font scale 1.0/1.3, dialogs/sheets, rotation while form open.
- Host-side emulator-window screenshots are allowed and show real pixels despite FLAG_SECURE;
  keep them in temp, never commit.
- Run testDebugUnitTest, compileDebugAndroidTestKotlin, lintDebug, assembleDebug. Report honestly.

## D8. Scope guard
No database, money, backup, permission, network, legal-copy, or dependency changes. UI-only plus
tests/docs/changelog. Keep `FLAG_SECURE` intact. Flipped/tent states unsupported by this AVD are
documented, not faked.

## D9. Slight fold / threshold behavior
`isSeparating` and `FoldingFeature.State` are the platform signals; there is no reliable public
hinge-angle value in `FoldingFeature`. Policy:
- `state == FLAT`, no feature, or no intersection with the app window -> Flat full-window layout.
- `state == HALF_OPENED` + separating feature -> compute panes, then require usable split:
  book both widths >= 300dp; tabletop both heights >= 260dp; window >= 600dp wide (book) or
  >= 480dp tall (tabletop). If usable -> two-pane sidebar/dock.
- If not usable but the smaller pane >= 180dp -> compact split: larger pane content + compact
  icon-only nav/backdrop in the smaller pane. If smaller pane < 180dp or hinge at an edge ->
  single larger pane + branded backdrop strip; never a blank white pane.
- Clamp/intersect feature bounds to the app window so split-screen windows where the hinge is
  outside the app do not force a false split.
- Keep the content subtree stable across Flat/Book/Tabletop transitions (custom Layout or stable
  content call site) so folding does not dispose the NavHost/form state.

## D11. Fold navigation panels are navigation-only (deduplication)
The fold sidebar/dock must not repeat Home's action card or the flat-mode bottom navigation:
- Book sidebar and tabletop dock contain only: a compact `Mizani` brand header, the five top-level
  destinations (Home/Activity/Insights/Bills/More), and one privacy footer. Remove the four
  expense/income/transfer/import quick-action buttons from both fold panels.
- Home keeps its existing "Record activity" card and owns its quick actions. Non-Home screens keep
  the existing global Add FAB in fold modes; it opens the fold-aware add chooser. Do not duplicate
  the four actions in the panel as well.
- Use the app name `Mizani` in the panel header instead of the workspace name already shown in the
  Home header. Keep only one privacy line in the panel (the pinned footer), not both a
  "Private by design" subtitle and a footer.
- Book: vertical nav with generous spacing and the footer pinned to the bottom. Tabletop: horizontal
  dock with the five destinations at the bottom, brand header at the top, and the privacy line above
  the nav. Both fill their pane without a second action grid.

## D10. Android split-screen / multi-window
The same model must behave in OS split-screen: `LocalWindowInfo.containerSize` is the app window,
not the display. No feature -> Flat and existing compact/medium/expanded nav rules. Feature
crossing the app window -> split only when both panes meet D9; otherwise single larger pane +
backdrop. Dialogs remain inside the app window content pane. Verify with
`am start --windowingMode 3` (or `cmd activity`) plus a second app; if the AVD cannot enter
split-screen reliably, document the exact limitation and still unit-test window-size behavior.
