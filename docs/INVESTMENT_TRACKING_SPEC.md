# Investment tracking MVP

## Decision

Mizani now supports a manual, offline-first investment snapshot. A user can record an investment
holding, where it is held, its broad asset category, the amount put in, its latest manually entered
value, the valuation date, and an optional note.

This is record-keeping only. Mizani does not connect to a broker, place orders, fetch market prices,
calculate tax, recommend an allocation, or provide investment advice. A holding is not a ledger
transaction and never changes a wallet balance. If a user moves money into an investment account,
they record that movement separately as an existing transfer so the same movement is not counted
twice.

## Why these fields

Public investor-education guidance emphasizes keeping transaction and cost-basis records, while
asset allocation and diversification depend on the types and locations of holdings. The MVP therefore
captures the minimum useful audit trail without pretending to be a brokerage ledger:

| Field | Purpose | Storage rule |
| --- | --- | --- |
| Name | Identifies the holding in the user's words | Required, max 80 characters |
| Provider / where held | Records the bank, fund manager, platform, property or other location | Required, max 80 characters |
| Asset category | Enables understandable grouping without recommending a category | Fixed enum, unknown values restore as `Other` |
| Cost basis | Records the user's entered amount put into the holding | Positive workspace-currency minor units |
| Current value | Records the latest manual valuation | Zero or positive workspace-currency minor units |
| Valuation date | Makes a manual value interpretable | Today or an earlier local date |
| Note | Optional context | Max 240 characters, local only |

The summary shows current value and the arithmetic difference from cost basis. It is explicitly not
presented as a rate of return, performance forecast, tax result, or recommendation.

## Data and compatibility invariants

- The encrypted Room database remains the source of truth.
- Money is stored as `Long` minor units and a currency code; no floating-point money is used.
- Every write is validated at `FinanceRepository` and committed inside a Room transaction.
- Holding currency must match the workspace currency.
- Active summaries exclude archived and soft-deleted holdings. Archived records remain restorable and
  portable.
- Adding, editing, archiving, deleting, or restoring a holding does not create or modify a
  `TransactionEntity`.
- Aggregates use checked addition/subtraction so overflow fails loudly rather than wrapping.
- Room migration 4→5 creates the additive `investment_holdings` table and indexes; no old table or
  record is changed.
- Version 1 and 2 encrypted backups remain readable. Backup format version 3 adds an optional
  `investments` array and preserves archived holdings; deleted rows follow the existing export
  policy and are excluded from user backups after soft deletion.
- Excel and encrypted backups include investments. The existing CSV remains a transaction ledger
  export so its established column contract is unchanged.
- App lock and secure-window protections cover investment values. Values remain masked until the
  user confirms with the existing PIN/biometric balance-visibility control.

## User flow

1. Open **More → Investments**.
2. If needed, set up or confirm app lock before revealing values.
3. Tap **Add investment** and enter the name, provider/location, asset category, cost basis, current
   value, and valuation date.
4. Save. The holding appears in the encrypted local list and the investment summary updates.
5. Tap a holding to update its manual valuation or archive it. Back and cancel leave existing data
   unchanged.

The screen states that the feature is manual and separate from the ledger. This wording is a safety
requirement, not marketing copy: it prevents users from assuming that adding a holding records a
cash movement or connects to the named provider.

## Explicit non-goals for this release

- No brokerage credentials, bank APIs, account aggregation, market-data API, or cloud sync.
- No buy/sell/dividend/fee event history, units, lots, realized gains, tax-lot selection, or cost-basis
  automation.
- No risk score, rebalancing suggestion, suitability assessment, guaranteed-return language, or
  personalized financial advice.
- No addition of investment value to wallet balances, Money Outlook, cash-flow totals, or spending
  analytics.

If trade history is later required, it must be a separate design: investment events and the canonical
cash-flow transaction must be created or edited together in one repository transaction, with explicit
rules for refunds, deletion, restoration, fees, dividends, and transfers.

## Research references

- [Investor.gov: Asset Allocation and Diversification](https://www.investor.gov/introduction-investing/getting-started/asset-allocation)
- [FINRA: Keep Up With Recordkeeping](https://syndication.finra.org/content/keep-recordkeeping)
- [FINRA: Returns, Explained](https://syndication.finra.org/content/how-are-your-investments-doing-returns-explained)
- [Google Play: Financial features declaration](https://support.google.com/googleplay/android-developer/answer/13849271?hl=en-en)

Google Play requires the Financial features declaration even when an app does not provide financial
features. This enhancement is an investment/portfolio-record feature, so the owner must review the
financial-feature selection and Data Safety answers in Play Console for the exact release artifact.
