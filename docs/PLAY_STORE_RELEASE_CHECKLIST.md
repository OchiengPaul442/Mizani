# Play Store release checklist

Status as of 30 October 2026: **NO-GO until the owner completes the external Play Console,
legal, signing, and device-testing gates below.** This document separates repository evidence from
actions that cannot be performed from the source tree.

The policy-specific evidence review is tracked in [Google Play policy audit](GOOGLE_PLAY_POLICY_AUDIT.md).

## Release identity

| Item | Value/status |
| --- | --- |
| App | Mizani |
| Application ID | `com.mizani.money` |
| Current version | `1.7.0` |
| Current derived version code | `1007000`; confirm it is greater than the latest Play version code |
| Provider | Paul Ochieng; confirm legal authority to publish |
| Privacy contact | `paul.ochieng.dev@gmail.com`; confirm it is monitored |
| Release address | Kampala, Uganda; owner-confirmed release value |
| Privacy policy candidate | `https://github.com/OchiengPaul442/budgeting-application/blob/main/docs/PRIVACY_NOTICE.md`; use only after pushing the current policy and confirming the file is publicly accessible without login |

## Completed repository gates

- [x] Single-module offline-first Android architecture is preserved.
- [x] Target SDK is 37; minimum SDK is 24.
- [x] Release build uses R8/resource shrinking and an Android App Bundle output.
- [x] Local financial records use encrypted storage and user-managed encrypted backups.
- [x] Automatic app-data backup and device transfer are disabled.
- [x] No account, cloud sync, advertising, analytics, SMS, contacts, location, camera, or storage feature was found.
- [x] The app has an in-app local-data deletion flow.
- [x] The in-app Terms and Privacy Notice now use the supplied provider, email, address, and 30 October 2026 document revisions.
- [x] Navigation regression fix is covered by unit/build checks and a compiled Android UI regression test.
- [x] Manual offline investment snapshots are isolated from ledger transactions, protected by the existing value-visibility control, and included in encrypted backups and Excel exports.

## Mandatory owner actions before upload

### 1. Verify the developer account

- [ ] Complete Google Play developer identity verification.
- [ ] Confirm the developer account type: personal or organization.
- [ ] Ensure the developer name, provider name, support email, and legal information are consistent.
- [ ] Complete any payments-profile or organization verification requested by Play Console.
- [ ] If this is a newly created personal account, confirm whether Play requires the closed-test requirement before production access.

### 2. Complete Play Console declarations

- [ ] Register `com.mizani.money` and create the app entry.
- [ ] Complete the Financial features declaration for this budgeting and manual investment-record app. Review the exact Play category for portfolio management and do not claim trading, brokerage connectivity, or advice that the app does not provide.
- [ ] Complete the Data Safety form from the actual release behavior. Review local-only ledger data, exchange-rate currency-code requests, non-Play update checks, and user-selected exports/backups.
- [ ] Complete the Content rating questionnaire; do not leave the app unrated.
- [ ] Declare target audience and whether the app contains ads. The source indicates no ads.
- [ ] Provide app-access instructions: no account is required; reviewers can use a fresh workspace, complete onboarding, and use Settings for backup, reminders, privacy documents, and local-data deletion.
- [ ] Add the final public privacy-policy URL and verify it from an incognito browser.
- [ ] Add a support email and support/contact page.

### 3. Finish legal and privacy approval

- [ ] Confirm Paul Ochieng is the correct legal provider/data controller.
- [x] Apply the owner-confirmed release address `Kampala, Uganda` consistently in the app and public legal documents.
- [ ] Confirm the email inbox is monitored for privacy requests and legal notices.
- [ ] Review Uganda PDPO obligations and any requirements for Kenya, Tanzania, Rwanda, Nigeria, Ghana, and South Africa before selecting those markets.
- [ ] Confirm the public Privacy Notice exactly matches the in-app copy and the Data Safety answers.
- [ ] Retain Storyset asset evidence and verify the in-app attribution.
- [ ] Review the generated open-source license list against the exact release dependency graph.

### 4. Configure and verify signing

- [ ] Keep the upload/release keystore and passwords outside the repository.
- [ ] Configure all four `MIZANI_RELEASE_*` Gradle properties in protected Gradle user-home or CI secrets.
- [ ] Run `:app:signingReport` and confirm the release variant has a signing configuration.
- [ ] Build the AAB with `:app:bundleRelease`.
- [ ] Verify the AAB with `jarsigner`; it must not report `jar is unsigned`.
- [ ] Enroll in or confirm Google Play App Signing.
- [ ] Verify the upload key can update the same application ID and signing lineage.
- [ ] Confirm the uploaded version code is higher than every existing Play artifact.

### 5. Complete release QA

- [ ] Install the signed release build on a clean device.
- [ ] Test an upgrade over the previous signed build.
- [ ] Test API 24, a current Android device, API 35/36+, ARM64, and a 16 KB page-size environment.
- [ ] Exercise onboarding, offline startup, expense/income/transfer entry, editing, deletion, import, manual investment create/edit/archive/value masking, exports, backup restore, app lock, biometric unlock, reminders, and local-data deletion.
- [ ] Exercise TalkBack, large fonts, compact screens, landscape, keyboard/IME flows, and back navigation.
- [ ] Test the Play in-app update flow from an internal-test track: a normal release uses the flexible
  flow, a priority-4-or-higher release uses the immediate flow, cancellation is non-destructive, and
  a downloaded flexible update resumes and completes after returning to the app.
- [ ] Test the exact Play reviewer journey using disposable data only.
- [ ] Review the Play pre-launch report and resolve all blocking issues.

## Release and rollback gates

- [ ] Store listing has final icon, feature graphic, screenshots, short description, full description, category, support email, privacy URL, and accurate claims.
- [ ] Store claims do not imply banking, lending, payments, investment advice, cloud recovery, or guaranteed financial outcomes.
- [ ] Internal testing is complete and feedback is recorded.
- [ ] Closed testing is complete if required by the developer account.
- [ ] Release is staged initially rather than sent to 100% immediately.
- [ ] Android vitals, crash reports, reviews, and support email have an owner.
- [ ] Rollback/halting criteria are documented: stop rollout for crashes, data loss, failed restore, broken upgrade, or critical privacy/security defects.
- [ ] Encrypted backup and the signed previous production artifact are retained before rollout.

## Commands for final evidence

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
.\gradlew.bat :app:lintRelease --no-daemon
.\gradlew.bat :app:signingReport --no-daemon --no-configuration-cache
.\gradlew.bat :app:bundleRelease --no-daemon
jarsigner -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab
```

Production status becomes **GO** only after all mandatory owner actions, signed-artifact checks,
device QA, and Play Console review gates are complete. Source changes alone cannot verify a Google
developer account, accept Play declarations, or publish the app.
