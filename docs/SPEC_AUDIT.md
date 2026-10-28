# Financial algorithms specification audit

This audit compares the attached `Mizani_Financial_Algorithms_Spec.md` with the current Android
implementation. The specification is treated as product requirements, not as evidence that a
feature already exists.

## Current architecture

- Room + SQLCipher is the local source of truth. Repository writes validate domain invariants and
  commit atomically; the UI does not maintain independent wallet balances.
- Money is persisted as `Long` minor units with decimal conversion rates. Transfers, fees, refunds,
  adjustments and linked commitment payments already have dedicated ledger behavior.
- Shared/pasted messages are parsed in memory, reviewed in the transaction editor, and reduced to a
  provider/reference identity when saved. No SMS permission or background SMS reader is used.
- Commitments are finite obligations with partial payment links to existing expense transactions.
  A link does not create a second expense. Foreign commitment targets retain a dated rate and a
  workspace-currency equivalent.
- Money Outlook currently provides a conservative 30-day reserve snapshot and a recorded-spending
  or user-target estimate. It is not yet the full chronological runway simulator in section D.

## Implemented in this phase: import review

- Provider detection now covers MTN MoMo, Airtel Money, M-Pesa, Orange Money, Moov, Wave, Telecel,
  Tigo, Vodafone Cash, EcoCash, OPay, PalmPay, Moniepoint, HaloPesa and TNM Mpamba.
- Provider references accept `Ref`, transaction IDs, receipt IDs and plain `ID:` values. Messages
  without a stable reference use a deterministic SHA-256 digest of normalized text for exact
  re-import detection; the raw message is not persisted.
- ISO timestamps such as `2026-10-28 13:45:16` and existing day-first dates are accepted. Amount
  and fee parsing remains decimal/minor-unit based.
- The supplied MTN MoMo Virtual Card example parses as a UGX 10,000 incoming transaction, fee 0,
  source `MTN MOMO VIRTUAL CARD`, date 28 October 2026 and reference `44604050982`.
- Incoming import reviews no longer inherit an arbitrary recent income category. The user must
  choose what the receipt represents; the review explains that it may be income, a transfer,
  reimbursement, loan proceeds or returned money.
- Imported transactions in a supported foreign currency now use the existing conversion-aware
  editor instead of being rejected before review. Saving still requires a valid dated rate.

## Remaining specification gaps

These are intentionally not represented as complete:

1. Import does not yet expose a confidence enum, provider-reported balance reconciliation, or a
   separate event/posting table for complex fee-inclusive versus fee-exclusive confirmations.
2. There is no bulk bank/mobile-money statement importer. The supported workflow remains
   user-initiated paste/share plus review.
3. Commitments do not yet have generated recurrence occurrences, household sharing, explicit
   overpayment credits, or a dedicated allocation table for split/loan principal-interest
   postings. Existing linked payments are safe for the current one-expense-per-commitment flow.
4. Money Outlook does not yet implement the section-D chronological daily recurrence, confirmed
   future inflows, multi-currency per-currency forecasts, or prefix-minimum discretionary daily
   allowance. The current reserve view must not be described as that simulator.

## Recommended delivery order

1. Keep the Phase 1 import review and add provider fixtures for each supported country, including
   duplicate, same-amount/different-reference, reversal and fee-inclusive cases.
2. Add an immutable import/event identity and provider balance-check metadata without changing the
   existing canonical ledger rows.
3. Add idempotent commitment occurrences and allocation records, then implement loan principal /
   interest splits and explicit overpayment handling.
4. Build the Money Outlook chronological projection as a pure calculation layer, with a separate
   safe-to-spend snapshot and explicit insufficient-data/FX-unavailable states.
5. Add bulk statement import only after the event identity and review model can guarantee atomic,
   idempotent multi-row commits.

