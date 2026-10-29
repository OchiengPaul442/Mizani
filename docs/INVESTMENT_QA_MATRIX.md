# Mizani 1.6.0 QA matrix

This is the release acceptance matrix for the investment enhancement and the surrounding existing
flows. It intentionally separates test design from test evidence. `Automated` means a deterministic
test exists in the repository; `Build-only` means the source compiled but the behavior needs a
device/emulator; `Manual pending` requires a disposable Android profile, a physical device, or
Play Console. The connected suite has now run on the disposable `Medium_Phone_API_36.1` emulator;
only scenarios directly covered by a named test are marked with device evidence. No row is considered
passed without execution evidence.

## 100 scenarios

| ID | Scenario | Expected result | Priority | Evidence/status |
| --- | --- | --- | --- | --- |
| 001 | Fresh install starts onboarding | App opens without a server/account | P0 | Manual pending |
| 002 | Accept current Terms and Privacy | Consent is stored and setup continues | P0 | Build-only |
| 003 | Resume incomplete setup | Setup resumes without losing selections | P1 | Manual pending |
| 004 | Complete Uganda/UGX setup | Workspace and first wallet are created | P0 | Existing device coverage |
| 005 | Start offline | Core setup does not require network | P0 | Manual pending |
| 006 | Restart after setup | Existing local workspace loads | P0 | Manual pending |
| 007 | More tab opens | More screen is interactive | P0 | Existing device coverage |
| 008 | More → Investments | Investments screen opens immediately | P0 | Device: `InvestmentEndToEndTest` |
| 009 | Investments → Back | More screen returns and remains interactive | P0 | Build-only |
| 010 | Investments route repeated quickly | No previous-page overlap or frozen Scaffold | P0 | Build-only |
| 011 | Add expense CTA | Expense form opens immediately | P0 | Existing device coverage |
| 012 | Add income CTA | Income form opens immediately | P0 | Existing device coverage |
| 013 | Add sheet → expense | Sheet closes before form is shown | P0 | Existing device coverage |
| 014 | Add sheet → income | Sheet closes before form is shown | P0 | Existing device coverage |
| 015 | Add sheet → transfer | Sheet closes before form is shown | P0 | Existing device coverage |
| 016 | Save valid expense | One expense is persisted | P0 | Existing device coverage |
| 017 | Save valid income | One income is persisted | P0 | Existing device coverage |
| 018 | Save valid transfer | Wallet balances move without cash-flow duplication | P0 | Existing device coverage |
| 019 | Edit existing transaction | Original row is updated, not duplicated | P0 | Existing device coverage |
| 020 | Delete then restore transaction | Tombstone and restore preserve history | P0 | Existing device coverage |
| 021 | Empty amount validation | Save is blocked with an actionable message | P0 | Existing unit/device coverage |
| 022 | Negative fee validation | Save is blocked | P0 | Existing unit coverage |
| 023 | Wrong category type | Save is blocked at repository boundary | P0 | Existing unit/device coverage |
| 024 | Archived wallet transaction | Save is blocked | P0 | Existing device coverage |
| 025 | Foreign-currency transaction online | Only currency codes leave device | P0 | Manual pending |
| 026 | Foreign-currency transaction offline | User gets recovery copy; no partial save | P1 | Manual pending |
| 027 | Shared mobile-money message | Review is required before saving | P0 | Existing device coverage |
| 028 | Duplicate imported reference | Second save is rejected | P0 | Existing device coverage |
| 029 | Refund linked to expense | Net spend and balance update correctly | P0 | Existing device coverage |
| 030 | Commitment payment link | Existing expense is linked without a second transaction | P0 | Existing device coverage |
| 031 | Insights with no data | Empty/insufficient state is honest | P1 | Existing device coverage |
| 032 | Insights with seven-day history | Recorded-spend estimate appears with basis | P1 | Manual pending |
| 033 | Money Outlook with hidden balances | Amounts remain masked | P0 | Existing device coverage |
| 034 | Daily reminder permission denied | App remains usable and explains limitation | P1 | Manual pending |
| 035 | App lock enabled | Sensitive screens require unlock | P0 | Existing device coverage |
| 036 | Biometric unavailable | PIN fallback remains available | P0 | Manual pending |
| 037 | Delete all local data | Workspace, settings and investments are cleared | P0 | Manual pending |
| 038 | Rotate/recreate activity | No duplicate navigation destination | P1 | Manual pending |
| 039 | Process death during navigation | App recovers without corrupting local state | P1 | Manual pending |
| 040 | Existing unit suite after feature | All JVM tests pass | P0 | Automated: `testDebugUnitTest` |
| 041 | Open empty Investments screen | Guidance and Add action are clear | P0 | Device: `InvestmentEndToEndTest` |
| 042 | Add holding with all required fields | One active holding appears | P0 | Device: `InvestmentEndToEndTest` |
| 043 | Add holding with zero current value | Holding saves and remains visible | P0 | Automated repository test pending device |
| 044 | Add holding with positive gain | Summary gain/loss is exact minor-unit arithmetic | P0 | Automated: `InvestmentSummaryTest` |
| 045 | Add holding with loss | Summary shows a negative value without advice | P0 | Automated repository test pending device |
| 046 | Add holding with blank name | Save is blocked with field guidance | P0 | Build-only |
| 047 | Add holding with blank provider | Save is blocked with field guidance | P0 | Build-only |
| 048 | Add holding with blank cost basis | Save is blocked | P0 | Build-only |
| 049 | Add holding with blank current value | Save is blocked | P0 | Build-only |
| 050 | Add holding with future valuation date | Save is blocked in UI and repository | P0 | Automated repository test pending device |
| 051 | Add holding over maximum money | Save is rejected without overflow | P0 | Automated unit/repository test pending device |
| 052 | Aggregate values at Long overflow boundary | Summary throws/rejects rather than wraps | P0 | Automated: `InvestmentSummaryTest` |
| 053 | Provider name at max length | Save succeeds | P1 | Build-only |
| 054 | Provider name over max length | Save is rejected | P1 | Automated validator coverage pending |
| 055 | Note at max length | Save succeeds | P1 | Build-only |
| 056 | Note over max length | Save is rejected | P1 | Automated validator coverage pending |
| 057 | Edit name/provider/category | Same holding ID is retained | P0 | Build-only |
| 058 | Edit visible cost/current values | New snapshot values replace old values | P0 | Build-only |
| 059 | Edit while values are hidden | Existing values are preserved and not exposed | P0 | Build-only |
| 060 | Archive active holding | It leaves active summary but remains in history | P0 | Device: `InvestmentEndToEndTest`, `RepositoryIntegrationTest` |
| 061 | Restore archived holding | It returns to active summary | P1 | Build-only |
| 062 | Soft-delete holding | It is absent from active UI and summary while its local tombstone prevents accidental recreation | P1 | Automated validator/repository coverage pending |
| 063 | Edit deleted holding ID | Repository rejects recreation of tombstone | P0 | Automated repository coverage pending |
| 064 | Multiple providers | Each holding remains separately identifiable | P1 | Build-only |
| 065 | Same name at different providers | Rows remain distinct by stable ID | P1 | Build-only |
| 066 | Asset categories render accessibly | Category label is understandable | P1 | Build-only |
| 067 | Unknown restored asset enum | It restores as Other, not a crash | P1 | Automated backup coverage pending |
| 068 | Holding value is not wallet balance | Wallet total remains unchanged | P0 | Device: `RepositoryIntegrationTest`, `InvestmentEndToEndTest` |
| 069 | Holding value is not cash-flow analytics | Expense/income totals remain unchanged | P0 | Device: `RepositoryIntegrationTest`, `InvestmentEndToEndTest` |
| 070 | Holding is not added to Money Outlook | Outlook calculation remains liquid-wallet based | P0 | Automated repository test pending device |
| 071 | Investment details while locked | Values remain masked | P0 | Manual pending |
| 072 | Fresh PIN before revealing values | Reveal requires existing security control | P0 | Manual pending |
| 073 | Secure window on investment dialog | Ordinary screenshots/capture are blocked | P0 | Manual physical/device pending |
| 074 | Add button touch target | Target is comfortably tappable | P1 | Manual pending |
| 075 | TalkBack labels for Add investment | Button has a meaningful label | P1 | Manual TalkBack pending |
| 076 | TalkBack labels for masked values | It announces hidden state, not amount | P0 | Manual TalkBack pending |
| 077 | TalkBack labels for provider/category | Context is read in order | P1 | Manual TalkBack pending |
| 078 | Keyboard on investment form | Fields remain visible and scrollable | P0 | Manual pending |
| 079 | Large font scale | Labels reflow without clipping | P0 | Manual pending |
| 080 | Compact screen/landscape | Form and cards remain reachable | P1 | Manual pending |
| 081 | Cancel add dialog | No record is created | P0 | Build-only |
| 082 | Back from add dialog | Dialog closes without data loss | P0 | Build-only |
| 083 | Double tap Save | One holding is created | P0 | Manual pending |
| 084 | Rapid Add/Back CTA | No stale page is composited below destination | P0 | Build-only |
| 085 | Restore while current holdings exist | Restore replaces holdings atomically | P0 | Automated backup test pending device |
| 086 | Failed restore validation | Current data remains unchanged | P0 | Automated validator coverage pending |
| 087 | Old version 1 backup | It restores without investments | P0 | Existing instrumentation coverage |
| 088 | Old version 2 backup | It restores without investments | P0 | Automated compatibility coverage pending |
| 089 | New version 3 backup | Active and archived investments round-trip; soft-deleted rows follow existing export policy | P0 | Automated backup test pending device |
| 090 | Excel export | Investments sheet includes values, dates and archive state | P0 | Build-only/manual pending |
| 091 | CSV export contract | Existing transaction columns remain unchanged | P1 | Existing unit coverage |
| 092 | Backup summary | Investment count is shown accurately | P1 | Automated backup test pending device |
| 093 | v4→v5 migration | Existing transactions and amounts are unchanged | P0 | Android migration test compiled; device pending |
| 094 | v1→v5 migration | All forward migrations apply without data loss | P0 | Device: `DatabaseMigrationTest` |
| 095 | v2→v5 migration | Optional fields and new table are valid | P0 | Device: `DatabaseMigrationTest` |
| 096 | v3→v5 migration | Commitment currency backfill and investments table coexist | P0 | Device: `DatabaseMigrationTest` |
| 097 | 10,000 transaction stress dataset | Existing performance gate remains within baseline | P0 | Device: `LargeDatasetPerformanceTest` |
| 098 | Large investment list | Scrolling and summary remain responsive | P1 | JVM summary stress exists; device/UI stress pending |
| 099 | Offline/network inspection | No investment name, value or provider is sent | P0 | Manual/network inspection pending |
| 100 | Release artifact and Play declarations | Signed AAB, privacy URL, financial declaration and Data Safety match behavior | P0 | Owner/Play Console pending |

## Exit criteria

- All P0 scenarios have executed evidence on a disposable emulator or approved physical device;
  no P0 defect remains open.
- Migration tests run from every supported prior schema, not merely compile.
- Backup round-trip is verified with active, archived and tombstoned holdings.
- TalkBack, large-font, keyboard, compact-screen and secure-window checks are run manually.
- The signed release AAB is verified and Play Console declarations match the final artifact.
- Performance claims are recorded from actual runs; no threshold is inferred from source review.

## Source-review risk identified before device execution

The QA review identified a modal-feedback risk for overlength name/provider/note values: repository
validation failures were previously assigned to a screen-level banner rendered behind the dialog.
The dialog now receives and displays that save failure inline, and the affected cases are scenarios
76, 78, and 85. This is a source-level fix; those scenarios still require disposable-device
execution before release sign-off.
