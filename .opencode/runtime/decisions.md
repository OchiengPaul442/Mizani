# Decisions

## Direction
- Keep the Mizani brand system (emerald/mint/gold, soft-white, medium radii). Redesign hierarchy, not identity.
- Home becomes a dashboard: header+period, balance hero, one action row, spending, recent, wallets, coming up, momentum, insight.
- One primary action per intent; remove the duplicate Quick Add card and the old 3-column Summary card.
- Use existing analytics (`metrics.upcoming`, `walletTotalMinor`, `categorySlices`) — no new financial calculations, no schema changes.

## Foldables
- Closed (narrow): single column, bottom nav with labels, quick actions one row.
- Half-open (tabletop): keep existing `largestSafePane` policy (render only in a safe pane, never under the hinge); single column is acceptable because the pane height is short; entire Home scrolls.
- Open (wide): rail at >=600dp safe width; two-column dashboard only when the content pane is >=700dp, max 1180dp total; otherwise a centered 680dp single column.
- Bottom nav labels stay visible at >=300dp and fontScale <=1.30. Rail labels stay visible unless fontScale >1.30.
- Global Add FAB is hidden on Home only (the action row owns recording); Activity/Insights/More keep it because instrumentation tests use "Add" there.

## Creative signature (calm, brand-led)
- Hero: gradient + oversized low-opacity balance rings + one gold focal dot; hairline period-progress track with "Day X of Y". Expanded layout promotes the hero to a full-width band (balance left, period flow right).
- Quick actions: one inset command bar with hairlines and tinted icon medallions; short press-scale. One CTA row.
- Coming up: date-badge timeline with category-colored dot/connector and relative due labels; expected income is visually distinct from bills.
- Section titles get an optional small gold accent dot (SectionHeader opt-in, no global behavior change).
- Spending card footer gets a 7-day "pulse" mini bar strip computed from `AnalyticsEngine.expenseMinor(day, day)` and remembered per snapshot/day; includes a text summary for TalkBack.
- Empty state uses concentric balance rings behind the existing Storyset illustration.
- Activity (second pass): day timeline rail + grouped day card, not a card per transaction.

## New functionality surfaced (no model changes)
- "Coming up" card reads `DashboardMetrics.upcoming` and links to Bills; shows first 3 items + 7-day due total. This field was computed but unused.
- Quick actions expose Transfer and Import directly instead of hiding them in the Add sheet.
- Privacy: hero uses `ProtectedBalanceValue`; eye control remains the existing PIN/biometric gate, extended with a tint parameter for the gradient.

## Security / verification constraints
- FLAG_SECURE stays. Screenshots are black by design; visual verification uses window metrics + uiautomator bounds/ASCII layout maps and user inspection.
- No instrumentation tests on the shared emulator (they reset app data). Only build + unit tests + non-destructive UI dumps.
- Test-visible strings and routes are frozen: Daily momentum texts, Recent activity, Add, Bills, Bills & commitments, TransactionRow semantics.

## Currency selection decision
- Offer **all current world currencies** (runtime currencies attached to an ISO country/territory + curated) in the searchable picker.
- Keep discontinued/historical codes available only for formatting/restoring old records; do not offer them for new entries (existing `MoneyTest` invariant: ADP/AFA/ALK excluded).
- Expand `all` (workspace/primary currency) to the same current union so onboarding is not limited to 10 + African codes.
- Replace the dropdown with a Dialog-based full-height picker: LazyColumn, pinned search by code/name/symbol, visible count, selected highlight. Search must be case-insensitive and reset on open.
- If the device runtime reports a surprisingly small current list, verify the count in the picker before changing the catalog logic.

## Open questions
- Exact emulator posture control command pending evidence worker (`cmd device_state`).
- Whether half-open posture exposes a horizontal hinge and which safe pane is selected; verification must confirm no clipped/overlapping bounds.
