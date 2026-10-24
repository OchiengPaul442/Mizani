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
- Transfers between a user's wallets are not expenditure or income. Fees are expenditure.
- Expected income is not received income. Recurring income rules never create cash until
  the user marks them received.
- Savings contributions are not ordinary spending when money merely moves between the
  user's own wallets.
- Refunds reduce net spending in the original category. Adjustments never count as income
  or spending.
- Deletions are soft (tombstone) so analytics and future sync never resurrect money history.
- Financial writes happen in Room transactions with validation at the repository boundary.

## Analytics rules

- All financial analytics are deterministic, explainable, locally computed and tested.
- Every metric that could mislead has an in-app “How this is calculated” explanation.
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

- SMS import is policy-gated and absent; core budgeting never depends on it.
- No bank APIs, payment processing, lending, credit scoring, investments or tax advice.
- No cloud sync implementation in this build; the UI states this and offers encrypted backup.
- Single base currency per workspace; expense and income entry may be converted from another
  supported currency using the latest available daily Frankfurter reference rate.
- No split transactions yet.
