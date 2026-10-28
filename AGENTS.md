# Mizani agent instructions

These instructions apply to the whole repository. Follow them alongside the user's request and any more specific instructions in a subdirectory.

## Understand the project before editing

- Read `README.md` for setup, build, and CI commands.
- Read `docs/ARCHITECTURE_CONTRACT.md` and `docs/PROJECT_STRUCTURE.md` before changing architecture, data flow, or package boundaries.
- Read `docs/SECURITY.md` and the relevant privacy/legal/verification documents when touching sensitive data, cryptography, exports, permissions, network access, release configuration, or legal copy.
- The product is a single-module, offline-first Android app using Kotlin, Jetpack Compose, Room, SQLCipher, DataStore, and manual dependency injection. Keep dependencies flowing through the existing package boundaries; do not add a backend, account system, cloud sync, analytics, or new Gradle modules without an explicit product decision.

## Preserve application invariants

- The encrypted local database is the source of truth. Financial data must not be sent over the network or written to logs. The optional exchange-rate request sends currency codes only.
- Represent persisted money as integer minor units plus a currency code. Do not use `Float` or `Double` for monetary calculations.
- Keep financial writes validated at the repository boundary and committed transactionally. UI code must not access DAOs or SQLite directly.
- Database changes require a forward migration, updated exported Room schema, and migration coverage. Never use a destructive migration or silently discard ledger or backup fields.
- Keep analytics deterministic, locally computed, and explained in the UI. Preserve accessibility descriptions and text alternatives for charts.
- Check app-lock, backup/restore, and test-data-reset behavior before changing related paths. See `docs/VERIFICATION.md`; Android instrumentation tests can clear app state and must use a disposable emulator/profile.

## Backward compatibility for feature work

- Before changing an existing financial flow, inspect its current data model, repository validation, analytics, navigation, exports, backup/restore behavior, and tests. Preserve existing user-visible behavior unless the feature explicitly changes it.
- Keep persisted enum values, Room column and table meanings, backup JSON field names, DataStore keys, and established navigation routes stable. Prefer additive optional fields and compatible defaults; never reinterpret or silently discard older user data.
- For every Room change, add a forward-only migration, check in the generated schema, and test migration from each supported prior schema with representative records. Never use a destructive migration or silently clear state.
- Keep older encrypted backups restorable when adding fields or entities. Validate both old-backup-to-new-app restore and new-backup round trips; retain unrelated legacy fields. Do not change CSV/XLSX columns or meanings without documenting the compatibility impact.
- When linking new features to ledger transactions, reuse the existing transaction instead of creating duplicate financial activity. Define how edits, soft deletion, restoration, refunds, transfers, and archival affect linked totals before shipping.
- Keep new permissions and notifications narrowly scoped. Do not request broad SMS access for user-initiated import; notifications must remain generic and private on the lock screen.
- For changes that touch existing financial data, verify both a fresh install and an upgrade/restore path. Run instrumentation classes separately on a disposable emulator because each class may reset app state; never clear an unidentified or non-disposable profile.

## Licensing, assets, and legal documents

- Mizani-authored source and executable forms are under Apache-2.0. Preserve the root `LICENSE` and `NOTICE`; retain the original developer and project attribution when distributing derivatives.
- Do not assume the project license covers third-party libraries, illustrations, fonts, or branding. Update `docs/THIRD_PARTY_NOTICES.md` or `docs/ASSET_CREDITS.md` when dependencies or assets change, and review the generated dependency-license screen for release builds.
- Storyset artwork has separate terms and requires user-accessible attribution while those assets remain in the app. Keep it in the dedicated Third-party notices screen rather than adding it to unrelated app content.
- Keep the runtime documents in `LegalDocuments.kt` aligned with `docs/TERMS_OF_SERVICE.md` and `docs/PRIVACY_NOTICE.md`. Increment the corresponding runtime document version whenever its user-facing meaning changes, and check `docs/LEGAL_RELEASE_CHECKLIST.md` before release. Do not present a provisional provider address as final.

## Changelog and version policy

- For every issue or pull request, assess whether it changes user-visible behavior, security/privacy, data compatibility, supported dependencies/platforms, legal notices, or the build/release process. Record each meaningful release change in `CHANGELOG.md` in the same change. If no entry is warranted, say why in the completion summary; do not add noise for formatting-only edits, test-only changes, or internal refactors with no user or release impact.
- Put pending work under `## [Unreleased]` using the most relevant Keep a Changelog heading: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, or `Security`. Write concise, impact-focused entries for people; include an issue/PR reference when one exists. Do not create extra categories or include confidential vulnerability details before coordinated disclosure.
- If an issue names a target release, keep its entry under `Unreleased` until that release is actually being cut; include the target or issue reference where useful. Do not invent a future version heading, release date, or changelog entry from an assumption.
- `mizaniVersionName` in root `gradle.properties` is the version source of truth. Use `MAJOR.MINOR.PATCH`; `app/build.gradle.kts` derives Android `versionCode`, so do not set `versionCode` independently. Keep the derived code monotonic and within Android's supported range.
- Follow SemVer intent when preparing a release: PATCH for compatible fixes, MINOR for backward-compatible features, and MAJOR for breaking changes, especially changes to ledger/backup compatibility or established user workflows. Do not bump the app version for every issue.
- Only when a release is explicitly requested/approved: choose the version, update `mizaniVersionName`, move the corresponding entries from `Unreleased` into `## [x.y.z] - YYYY-MM-DD`, leave a fresh `Unreleased` heading, and verify the release notes and derived version code agree. Do not publish, sign, tag, or upload a release unless explicitly authorized.
- Do not reconstruct missing historical notes or rewrite a released changelog section except to correct a documented error.

## Verification and handoff

- Use the Gradle wrapper and the commands documented in `README.md` and `.github/workflows/android-ci.yml`. CI currently runs `:app:testDebugUnitTest` and `:app:assembleDebug`; run focused checks for the code changed and report the exact commands and results.
- Use disposable test data for instrumentation tests. Never run tests that reset app state against a device or profile containing data the user needs.
- Keep changes scoped. Update relevant documentation and tests with behavior changes. Do not commit machine-specific paths, `local.properties`, signing keys, passwords, or other secrets.
- In the handoff, summarize behavior/documentation changes, changelog disposition, verification actually performed, and any known limitations. Never claim a test, build, release, or review passed unless it was actually performed.
