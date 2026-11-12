# Mizani UI redesign — plan

## Objective
Redesign Mizani's interface for clarity, calmer density, discoverability and responsive/foldable use, centered on Home, without changing financial data semantics, persistence, backup compatibility or legal copy.

## Product direction
- Calm, precise, trustworthy; brand unchanged (emerald/mint/gold, deep teal, soft-white; medium radii).
- Home becomes a **dashboard with one clear hierarchy**: greeting + period control → balance/period hero → one row of quick actions → spending → recent activity → wallets → coming up → momentum → insight.
- Remove duplicate calls to action and repeated summary blocks. Every destination reachable in one tap from the Home action row or section header.
- Less card-per-row noise in Activity; grouped day cards with hairline dividers.
- Responsive: compact (<600dp) single column; expanded content (>=700dp after rail) two-column dashboard; max content width on tablets; bottom labels kept on mini devices; quick actions stay in one row at normal text scale and wrap 2x2 only for large font/smallest width.
- **Fold postures are a requirement** (foldable emulator with Closed / Half-Open / Open):
  - Closed (cover display, narrow): single column, bottom navigation with labels, quick actions one row, hero and actions above the fold.
  - Half-open (horizontal hinge / tabletop): render inside the chosen safe pane only; content must be usable in a half-height window; no controls under the hinge; single column; sections scroll; bottom nav stays reachable. Hinge bounds continue to come from `WindowInfoTracker`/`FoldingFeature` via `AdaptiveLayout.largestSafePane`.
  - Open (inner display): rail navigation + two-column dashboard when the content pane is >=700dp; otherwise single column capped at 680dp and centered.
  - Verify all three postures by window metrics + uiautomator bounds (FLAG_SECURE prevents readable screenshots).
- Accessibility preserved: >=48dp targets, content descriptions, tabular money, no color-only meaning, chart text alternatives.

## Known facts (evidence)
- Home: `feature/home/HomeScreen.kt` stacks 9+ blocks: greeting, PeriodMenu, wallet list, momentum, 3-column summary, duplicate quick-add card, recent, donut, insight. `QuickAddSection` and FAB both duplicate actions.
- `QuickAddActions` only exposes expense/income. Transfer and import are hidden in the FAB sheet.
- `DashboardMetrics.upcoming` is computed but unused in any screen; `budget`/`safeToSpend` also unused outside analytics.
- Activity: `TransactionsScreen.kt` wraps every transaction row in its own `MizaniCard` (line ~297).
- Navigation: 5 tabs + extended FAB; labels hidden below 360dp or fontScale>1.15; FAB hidden on Home/Activity when ledger empty; content insets inconsistent (`Spacing.bottom` vs `bottomFab`).
- Tests depend on: "Activity", "Daily momentum", "Track days, not amounts · 10 points per day.", "0 points", "Record today", "Milestone: Starting out", "Next milestone: First steps · 3 days to go", "Recent activity", FAB text "Add", Bills tab, "Bills & commitments".
- Design system doc: `docs/DESIGN_SYSTEM.md` (calm, restrained, 16dp rhythm, medium radii).
- Build/verify: `.\gradlew.bat :app:testDebugUnitTest` and `.\gradlew.bat :app:assembleDebug` (README, CI).

## Signature creative moves (calm, not decorative noise)
1. **Balance hero**: gradient field with two oversized, low-opacity brand rings and a single warm-gold focal dot (the brand's "balance" idea); a hairline month-progress track with a gold marker ("Day 12 of 30"). On open/expanded, the hero becomes a full-width band: balance on the left, period flow on the right.
2. **Command bar actions**: the four actions share one inset surface with vertical hairlines, tinted icon medallions and a short press-scale + haptic response — a single CTA row, not four competing buttons.
3. **Coming-up timeline**: date badges with a category-colored connector/dot, relative due labels ("in 3 days"), income shown as expected money rather than a bill.
4. **Section rhythm**: a small gold accent dot before Home section titles; a 7-day "pulse" bar strip in the Spending card footer so the screen has a visual heartbeat without another section.
5. **Activity as a day timeline** (second pass): date rail + one grouped card per day with hairline dividers instead of card-per-row.
6. **Empty state**: the Storyset illustration sits inside soft concentric balance rings, matching the hero language.

## Workstreams
1. **Design foundation** — semantic accent/positive tokens, compact component primitives (quick action tile, grouped list card, hero shell, section header with supporting copy), responsive breakpoint tokens.
2. **Home** — rewrite structure and hierarchy; new balance hero (masked total + period income/spend/net); 4 quick actions in one row; spending snapshot; recent activity; wallet strip; coming-up bills (uses `metrics.upcoming`); compact momentum at bottom; insight.
3. **Navigation shell** — keep 5 destinations; improve bottom bar labels/insets, FAB policy, content max width; expanded dashboard layout.
4. **Activity** — grouped day cards, denser rows, clearer summary/filters.
5. **Polish** — More/Insights spacing and section headers; strings/resources; design doc update.
6. **Verification** — unit tests + assembleDebug + emulator screenshots (compact, expanded, large font) + test-string contract check.

## Acceptance criteria
- Home shows at most one primary CTA per action; expense/income/transfer/import reachable from one row.
- Home top-of-screen (before scroll, 360x800dp) shows greeting, period control, total balance + period summary, and the quick-action row.
- No duplicate "Quick Add" card; wallet visibility control present in hero.
- Layout at 320dp, 360dp, 411dp and >=840dp has no clipped/overlapping controls; 4 actions stay one row at default font scale; labels visible in bottom nav at 360dp.
- Activity groups a day's transactions into one card with dividers.
- All existing instrumentation text contracts still present or tests updated intentionally.
- `:app:testDebugUnitTest` passes; `:app:assembleDebug` succeeds; screenshots captured for phone and expanded width.

## Second redesign pass (after emulator restart)
- DONE Insights: full-width cash-flow hero, responsive two columns (Outlook/Spending/Bars left; What changed/Income trends right), export moved to the top bar, accent section headers; advanced detail full width. Build green.
- DONE More: centered 680dp / two-column hub at >=700dp, accent section headers, all 9 labels/routes/dividers, FAB inset preserved. Build green.
- PENDING Settings: centered 680dp for main and data-tools branches, accent section headers, Appearance card dividers; no behavior/label changes.
- DONE Performance: `AnalyticsEngine.dailySpending(days, today)` single ledger pass replaces Home's 7x `expenseMinor`; unit-tested equivalence. Activity already groups rows into day cards (fewer Lazy items).
- Verification planned: unit tests, assembleDebug, uiautomator bounds on Insights/More/Settings in Open/Closed, no demo-ledger reset.

## Status (final)
- DONE: Home dashboard redesign; full-width balance hero with rings/progress on open; one-row command bar; spending pulse; recent activity; wallets; forward-looking coming up; momentum; insight.
- DONE: Navigation fold policy, labeled bottom nav, Home FAB removal, new Home callbacks.
- DONE: Activity grouped by day; duplicate shown-count removed.
- DONE: Strings/docs/changelog; stale Bills test assertion corrected.
- DONE: Unit tests, compileDebugKotlin, assembleDebug, compileDebugAndroidTestKotlin.
- DONE: Closed / Half-Open / Open / 1.3x bounds verification; all PASS (see evidence.md).
- DONE (1.10.0): Bills tab spacing/line-height; current-only 153-currency catalog + searchable full-height picker (jpy verified); lock redirect focuses Security with banner + toast; About build identity/install source/Play link; release-version task and Play update troubleshooting; version 1.10.0 / code 1010000, changelog dated 2026-11-13.
- LIMITATION: no Play Console access; signed AAB not produced here (owner signing needed); new Insights/More/Settings wide states compile but were not re-captured after the fold AVD was replaced by the phone; no readable screenshots (FLAG_SECURE).

## Risks / limits
- Architect session cannot run shell; delegate build/adb to workers.
- Instrumentation suites reset app state; only run on disposable emulator, one class at a time.
- No new DB fields; no financial logic changes; `metrics.upcoming` is read-only.
- Do not change backup JSON, Room schema, DataStore keys, enum values, navigation route names used by tests.
