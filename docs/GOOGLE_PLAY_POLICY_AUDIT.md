# Google Play policy audit

**Audit date:** 8 November 2026
**Status:** No-go until the external Play Console and release-owner gates below are completed.

This is an engineering and release-readiness audit, not legal advice or a guarantee of Google Play
approval. Google Play's current policy pages and the final signed artifact control the result.

## Scope reviewed

- [Developer Policy Center](https://play.google/developer-content-policy/)
- [Financial Services](https://support.google.com/googleplay/android-developer/answer/9876821)
- [User Data](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Policy announcement: October 29, 2026](https://support.google.com/googleplay/android-developer/answer/18100766)
- [Target API Level Policy](https://support.google.com/googleplay/android-developer/answer/16561298)
- [SDK Requirements](https://support.google.com/googleplay/android-developer/answer/13323374)
- [Metadata](https://support.google.com/googleplay/android-developer/answer/9898842)
- [Content Ratings](https://support.google.com/googleplay/android-developer/answer/9898843)
- [Functionality, Content, and User Experience](https://support.google.com/googleplay/android-developer/answer/9898783)
- [Intellectual Property](https://support.google.com/googleplay/android-developer/answer/9888072)
- [Export compliance](https://support.google.com/googleplay/android-developer/answer/113770?hl=en)
- [Advance notice to App Review](https://support.google.com/googleplay/android-developer/answer/6320428)

## October 29, 2026 policy announcement review

The announcement gives developers at least 30 days from 29 October 2026 to update affected
apps. The repository and manifest were reviewed against each announced change. Mizani currently
does not use the affected restricted permissions or APIs, so no new permission, declaration, or
runtime flow is required in this source revision.

| Announced area | Mizani impact | Required handling |
| --- | --- | --- |
| Local Network permission | Not declared or used. The app has no device-to-device or local-network sync. | Do not add the permission as future desktop-sync preparation. Re-audit the transport and complete the Play declaration before introducing local-network functionality. |
| Manage Contacts permission | Not declared or used. | Keep contact selection out of the product unless a core use case is approved; prefer scoped platform pickers if a future feature needs contact data. |
| AppFunctions API | Not used. | No action. If added later, review agent-safety, deception, security-boundary, and cross-app behavior before release. |
| Subscription eligibility discounts | No subscriptions, trials, or eligibility-based discounts. | No action. Revisit pricing copy and eligibility verification before adding any paid recurring offer. |
| Location, All Files Access, Accessibility, SMS/call logs | None of these permissions or APIs are used. User-initiated imports and exports use scoped sharing/document-picker flows. | Preserve the current least-privilege design; do not replace the document picker with broad storage access or add background SMS/call-log access. |
| Play Age Signals API | No child/family-targeted experience is currently in scope. | Do not integrate it speculatively. Reassess if the target audience or family features change. |
| Trader status and Japan authorization | External Play Console/account obligations, not source behavior. | The release owner must complete trader-status steps where applicable, and Japan authorization by 1 December 2026 if the app is sold into or out of Japan. |
| February 2027 technical quality requirements | Release/build gate; source review alone cannot establish compliance. | Review the linked Play quality requirements and the final signed artifact before the affected deadline. |

### Source-level conclusion

The existing manifest remains intentionally limited to `INTERNET`, `USE_BIOMETRIC`, and
`POST_NOTIFICATIONS`, with a targeted mailto query. The app continues to use scoped Android
document pickers for exports and backups, encrypted local storage as its source of truth, and no
account, cloud sync, ads, analytics, contacts, location, SMS, call logs, accessibility service,
or AppFunctions integration. No runtime legal-document version bump is warranted because this
policy review does not change Mizani's user-facing data processing.

## Repository evidence

| Area | Evidence | Assessment |
| --- | --- | --- |
| Target API | `app/build.gradle.kts` targets and compiles with API 37 | Meets the current source-level target requirement; re-check before each release because Google changes the required API level. |
| Permissions | Manifest contains `INTERNET`, `USE_BIOMETRIC`, and `POST_NOTIFICATIONS`; email-app visibility is a targeted `<queries>` entry | No SMS, contacts, location, storage, camera, microphone, accessibility, or broad package visibility permission is declared. Verify the merged release manifest before upload. |
| Financial features | Local budgeting, transaction records, and manual investment snapshots; no bank connection, lending, payments, brokerage, trading, or advice | Financial features declaration is mandatory even when an app has no financial features. Declare the actual manual budgeting/investment-record behavior accurately; do not describe it as banking, lending, trading, or advice. |
| User data | SQLCipher/Keystore local storage, no account, cloud sync, advertising SDK, or analytics SDK; local-data deletion is available | In-app Privacy Notice and public policy are present. The final public URL and Play Data safety form still require owner verification. |
| Network and SDKs | HTTPS exchange-rate request sends currency codes only; GitHub metadata is used for sideload checks; Play App Update uses Play services for Play-installed update flows | No financial ledger payload is sent by these paths. Play SDK data handling must be reflected in the final Data safety review. |
| Feedback | User explicitly chooses the email app and reviews the message before sending; no automatic background email service | This is a user-initiated transfer. The selected email provider controls its own processing and retention. |
| Intellectual property | Storyset attribution and asset evidence are recorded in `docs/ASSET_CREDITS.md`; dependency notices are in `docs/THIRD_PARTY_NOTICES.md` | Retain original license/download evidence and confirm the intended distribution is covered. Advance-review notice is only relevant if one of Google's listed special scenarios applies. |
| Functionality | Unit, emulator, debug build, release lint, and local release bundle checks have passed | Play internal testing, pre-launch report, and physical-device coverage remain required. |
| Encryption/export | SQLCipher, Android Keystore, AES-GCM backups, and biometric/PIN controls are included | Complete Play Console export-compliance questions with the owner's legal/export assessment. Do not infer an exemption solely from the code audit. |

## Required owner actions before submission

1. Push the current Privacy Notice to the public repository and verify the exact URL in an
   incognito browser: `https://github.com/OchiengPaul442/budgeting-application/blob/main/docs/PRIVACY_NOTICE.md`.
2. Complete the Financial features declaration for every track, including testing tracks. Explain
   that Mizani is an on-device budgeting and manual record-keeping app and does not move money,
   lend, trade, connect to brokers, or provide financial advice.
3. Complete Data safety from the final signed artifact and every included SDK. Reconcile the form
   with the Privacy Notice, including local-only financial records, user-selected feedback email,
   currency-code requests, GitHub sideload checks, and Google Play update metadata.
4. Complete the export-compliance questionnaire because the app uses encryption. Obtain legal or
   export counsel confirmation for the selected answer if required.
5. Complete the content-rating questionnaire and repeat it if the app's content or features change.
6. Complete store metadata honestly: title, developer identity, category, description, screenshots,
   icon, support email, privacy URL, target audience, ads declaration, and financial-feature claims.
   Do not use ranking claims, misleading keywords, or unlicensed logos/artwork.
7. Configure the protected release signing inputs, build and verify the signed AAB, and confirm the
   application ID, signing lineage, and monotonically higher version code can update the same Play
   listing.
8. Run a signed internal-test release on supported Android devices. Verify onboarding, offline
   behavior, encrypted backup/restore, app lock, data deletion, feedback handoff, Play in-app
   updates, and the Play pre-launch report using disposable data.
9. Review local-law, privacy, financial-record, and export obligations for every country selected
   in Play Console. Source inspection cannot establish those legal requirements.
10. Complete the policy-announcement checks in the release checklist: trader status where
    applicable, Japan authorization where applicable, and the February 2027 technical-quality
    review before the next affected release.

## Release decision

The source tree is technically prepared for Play review, but the app is **not yet Play-compliant
by evidence** until the owner completes the external declarations, public-policy verification,
signed-artifact checks, and Play internal-test gates. No production publication or external review
was performed during this audit.
