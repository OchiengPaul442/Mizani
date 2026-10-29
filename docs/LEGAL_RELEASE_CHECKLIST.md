# Legal release checklist

The in-app Terms and Privacy Notice are product-specific drafts. This review checked their statements against the current app behavior and public guidance available on 30 October 2026. It is not a legal opinion or confirmation that the operator has met every obligation.

Complete these items before public distribution:

- Confirm Paul Ochieng is authorized to license all original Mizani source code under Apache-2.0. Keep third-party materials, brand assets, and trademarks outside that grant unless their owners separately authorize it.
- Review the generated **Settings > About & privacy > Open-source licenses** list against the exact release dependencies and include any notices not discovered from Maven metadata. Confirm the SQLCipher Community Edition attribution and license text remain available in app and release materials.
- Confirm the Storyset illustration downloads and modifications are permitted for the intended app distribution. Retain the original download and license evidence, and keep the required attribution available in the in-app Third-party notices screen.
- Confirm Paul Ochieng is the correct legal provider/controller name and use the owner-confirmed
  release address, “Kampala, Uganda.” Confirm who is the data controller for the app.
- Confirm `paul.ochieng.dev@gmail.com` is monitored for privacy requests, complaints, and legal notices. Add any required local privacy contact or representative.
- Have counsel review the Terms, Privacy Notice, onboarding consent, and actual data flows for every intended market: Uganda, Kenya, Tanzania, Rwanda, Nigeria, Ghana, South Africa, and any market selected as “Other.” Limit launch markets if the review is incomplete.
- For Uganda, confirm the operator's current registration and reporting obligations directly with the Personal Data Protection Office (PDPO). The PDPO's published guidance describes registration for data controllers, collectors, and processors, subject to exemptions made by notice; its 2024 decision also describes registration as mandatory unless a specific exemption is gazetted. The PDPO's knowledge center announced an annual compliance-report deadline of 23 November 2026 for registered controllers and processors. Confirm the current requirements and whether they apply to this operator: [PDPO registration guidance](https://pdpo.go.ug/media//2022/01/20102021105143-Registration_Classification_and_Guidance_Notes.pdf), [PDPO decision](https://pdpo.go.ug/media/2025/07/Ssekamwa-Frank-3-Ors-vs-Google-LLC-PDPO-Decision-18th-July-2024.pdf), [PDPO knowledge center](https://pdpo.go.ug/knowledge-center?resourceId=1faaec8c-c601-4fa9-b65b-b02510a3cfaf).
- Decide and document the legal basis, rights-request process, retention/deletion approach, breach response, any Data Protection Officer or representative requirement, and cross-border transfer requirements for each launch market.
- Verify the current Google Play privacy/Data Safety disclosures, store developer identity, and production dependency/network behavior before release. Recheck Frankfurter and GitHub privacy terms when their integrations change.
- Confirm the app's secure-window behavior on supported Android versions and devices. Android's `FLAG_SECURE` blocks ordinary screenshot and non-secure display capture, but it is a platform control and cannot guarantee protection against external cameras, compromised devices, or every OS implementation. See [Android's guidance](https://developer.android.com/security/fraud-prevention/activities).
- Keep runtime documents and Markdown copies aligned. Increment the relevant version in `app/src/main/java/com/mizani/money/core/legal/LegalDocuments.kt` when user-facing meaning changes; the app then requests renewed acceptance.

Do not publish while the provider identity/address, public privacy URL, or other release checklist items remain incomplete.
