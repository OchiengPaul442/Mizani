# Mizani production QA plan

## Objective / acceptance
- Audit every implemented Android surface and improve confirmed user-facing, financial, accessibility, privacy, and reliability defects without changing the app ID or schema unnecessarily.
- Centralize/rewrite exposed copy where practical; remove misleading demo/debug/unfinished content.
- Prove affected behavior with focused tests plus debug build; report all unverified areas honestly in `MIZANI_PRODUCTION_QA_REVIEW.md`.

## Scope / ownership
- Baseline/inventory: `app/src/main/java/com/mizani/money/**`, resources, tests, docs.
- Financial correctness/security: `core/money`, `domain/analytics`, `data/repo`, local security/export; independent reviewer after edits.
- UX/content/accessibility: `feature/**`, `ui/**`, `res/values/strings.xml`; no overlapping worker edits.
- Report/state: root QA report and `.opencode/runtime/**` owned by architect.

## Task order
1. Establish code/test baseline and inventory all screens, strings, flows, and confirmed risks.
2. Fix highest-impact correctness/privacy/interaction issues in bounded slices.
3. Professionalize copy and accessibility/layout consistency without broad redesign.
4. Implement PIN-attempt throttling only after the current lock-screen edits settle; obtain independent security review.
5. Run unit/build/instrumented checks available; review final diff and affected flows.
6. Publish severity-based QA report with fixed/partial/not fixed/not verified status.

## Verification
- `:app:testDebugUnitTest`, `:app:assembleDebug`; focused Android tests where emulator/tooling is available.
- Static searches for hardcoded/debug/placeholder user-facing content and sensitive logging.
- Real Android UI verification only if an emulator/device is available to the browser/worker environment.

## Confirmed implementation leads
- Primary/secondary Compose flows are implemented under `feature/**`; most copy is hardcoded while `res/values/strings.xml` only contains app name/tagline.
- Confirmed financial defect: refund fees are included by `feesMinor` but omitted from wallet refund balance change, category spend/forecast, transaction summaries, and backup summary (`FinanceModels.kt`, `Analytics.kt`, `TransactionsScreen.kt`, `ExportManager.kt`).
- Confirmed feedback defect: `SettingsScreen.kt` treats the `Result<Unit>` returned by `AppViewModel.enableLock` as a successful `runCatching` result without inspecting the nested failure.
- Confirmed product/documentation drift: `docs/VERIFICATION.md` says category reordering is not exposed, but `CategoriesScreen` exposes move controls; prior build/test claims remain stale until fresh commands run.
- Potential risks to verify/fix in bounded work: repository cross-reference/category/currency validation, restore bundle invariants, empty/stat handling for adjustment-only ledgers, and locale/accessibility coverage.

## Status
E2E AND RELEASE SMOKE GREEN — 87 JVM tests; all 5 instrumented classes passed on API 36.1; debug APK 31,141,566 bytes; unsigned release APK 10,663,390 bytes with R8/resource shrinking, debug-signed local smoke passed; docs/report synchronized; emulator stopped and generated screenshots/test APKs/logs removed. Production signing and localization remain release blockers; manual TalkBack/9-step acceptance, PIN wall-clock/atomicity, and real-spreadsheet CSV remain unverified.
