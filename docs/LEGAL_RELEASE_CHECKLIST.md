# Legal release checklist

The app copy is a product-specific draft, not a determination that the provider has met every legal obligation. Complete these items before public distribution:

- Replace “Mizani app publisher (legal name required before public release)” with the operator’s full legal name and business address. Confirm who acts as controller for this app.
- Confirm `paul.ochieng.dev@gmail.com` is monitored for privacy requests, complaints, and legal notices. Add any required local privacy contact or representative.
- Have counsel review the Terms, Privacy Notice, onboarding consent, and data flows for each intended market: Uganda, Kenya, Tanzania, Rwanda, Nigeria, Ghana, South Africa, and Other. Decide whether to limit “Other” until a jurisdiction review is complete.
- Assess controller/processor registration, representative or Data Protection Officer requirements, lawful bases, data-subject request handling, breach response, and cross-border transfer rules for each launch market. Uganda’s Personal Data Protection Office says the general registration requirement applies to data controllers, collectors, and processors unless a category is exempted by notice; confirm the current position directly with the regulator.
- Verify the current Google Play privacy/Data Safety disclosures, store developer identity, and the exact behavior of production dependencies and network traffic before release.
- Confirm the provider’s real legal identity and contact route are present in both the app and store listing. Do not publish while these marked fields remain.

The runtime documents are defined in `app/src/main/java/com/mizani/money/core/legal/LegalDocuments.kt`; update the Markdown copies here when changing those documents and increment their version constants to trigger re-consent.
