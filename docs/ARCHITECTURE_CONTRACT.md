# Product architecture contract (Mizani V1)

This is an offline-first financial application.

## Canonical data and identity

- The local encrypted database is the canonical data source. UI layers read from it; the
  network (when it exists) would only feed it.
- No account is required. Guest mode does not depend on any server response.
- Before an account exists, financial data must not be synchronized to an application
  backend. There is no backend endpoint; internet access is used only for optional public
  exchange-rate lookups.
- Creating an account would enable synchronization; it must never replace the local
  database. Local export and deletion remain available.

## Technology

- Kotlin + Jetpack Compose, Material 3, Navigation Compose.
- Room for relational local state, with exportable schema and no destructive migrations.
- SQLCipher (`net.zetetic:sqlcipher-android`) for the encrypted database.
- Android Keystore, AES-GCM-wrapped random 256-bit database passphrase.
- DataStore for non-sensitive preferences and the salted PIN hash.
- AndroidX BiometricPrompt for biometric unlock.
- Manual dependency injection (`AppContainer`) for V1; no Hilt.

## Money and ledger rules

- Money is `Long` minor units + currency code. Never persist money as Float or Double.
- Wallets and ledger amounts use the workspace currency. A foreign-currency transaction also
  stores the original amount, currency, decimal rate and provider date; conversion uses decimal
  arithmetic and rounds to the workspace currency's minor unit.
- The exchange-rate request contains only the currency pair. Transaction amounts, payees and notes
  stay on device. Rates are daily reference rates, not bank/card settlement quotes.
- Commitment targets may be denominated in a currency other than the workspace currency. The
  commitment stores its saved daily reference rate and workspace-currency equivalent so progress,
  reserves, exports and restores remain deterministic without re-fetching a rate.
- Transfers between a user's wallets are not expenditure or income. Fees are expenditure.
- Expected income is not received income. Recurring income rules never create cash until
  the user marks them received.
- Savings contributions are not ordinary spending when money merely moves between the
  user's own wallets.
- Refunds reduce net spending in the original category. Adjustments never count as income
  or spending.
- Deletions are soft (tombstone) so analytics and future sync never resurrect money history.
- Financial writes happen in Room transactions with validation at the repository boundary.
- Commitment payments associate one active expense with one obligation; they never create a
  second ledger row. Linked refunds reduce the paid total. Deleting an expense removes its link;
  restoring that expense does not relink it. Transfers and non-expense transactions cannot be
  linked. Archiving an obligation preserves its history but removes it from Outlook reserves and
  reminder scheduling.
- Money Outlook subtracts unpaid obligations due within 30 days from active liquid wallets,
  excludes expected income, and uses recorded daily expense history only after seven observed
  days; otherwise it uses the user's optional daily target.

## Analytics rules

- All financial analytics are deterministic, explainable, locally computed and tested.
- Show the source or basis of estimates in concise in-app copy when it affects interpretation.
  Keep detailed methodology in supporting documentation. Add an in-app explanation only when it
  materially helps users understand a decision-relevant metric.
- Insufficient history is shown honestly: no runway/trend/savings-rate claims without data.
- Charts are always accompanied by a textual interpretation and TalkBack descriptions.
- No opaque financial health score, no creditworthiness claim and no AI-generated advice.

## AI boundary

- Deterministic, locally computed analytics remain the only source of every number shown in the app.
- Any future AI is an optional layer: local-only, opt-in, fact-constrained and verifier-gated. A
  response is discarded unless every claim is traceable to a Kotlin-supplied fact and no new number
  appears. Deterministic copy is shown whenever the model is absent or verification fails.
- No AI model or inference runtime is shipped in the APK, and no AI model download or network
  access is added.
- No financial payload in prompts: only sanitized facts with stable IDs and category labels may be
  sent to a local model, and nothing leaves the device.

## Security and privacy

- No advertising SDK; no financial telemetry; no financial values in logs.
- Guest financial data never leaves the device. The optional rate request sends only currency
  codes over HTTPS to Frankfurter.
- Android backup is disabled and all data domains are excluded from backup/transfer rules.
- Database bytes without the device Keystore key must not be sufficient to read data.
- The app lock PIN is a salted PBKDF2-HMAC-SHA256 hash; verification is constant-time.
- No secrets, service-role keys or API keys in the APK.

## Explicit V1 boundaries

- Transaction import is user-initiated through shared or pasted text for supported African
  mobile-money confirmations (including MTN MoMo, Airtel Money, M-Pesa, Orange Money, Moov, Wave,
  Telecel, Tigo, EcoCash, OPay, PalmPay and Moniepoint). Never request broad SMS access or monitor
  messages in the background. Parse locally, require review before saving, discard the raw message,
  and retain only a normalized provider reference—or a deterministic exact-message digest when no
  provider reference exists—for deduplication. Incoming money must be assigned an explicit income
  category during import review because the message cannot prove whether it is salary, a transfer,
  a reimbursement, a loan or a gift.
- Manual investment snapshots are supported for record-keeping only: name, provider/location,
  broad asset category, cost basis, current manual value, valuation date and note are stored in the
  encrypted local database. They do not create ledger transactions, alter wallet balances, fetch
  market prices, connect to providers, execute trades, or provide investment/tax advice. A future
  trade-history feature must define canonical ledger links and transactionally update both domains.
- No cloud sync implementation in this build; the UI states this and offers encrypted backup.
- Single base currency per workspace; expense and income entry may be converted from another
  supported currency using the latest available daily Frankfurter reference rate. Bills and
  commitments may also use another supported currency and retain the rate used for their target.
- No split transactions yet.
