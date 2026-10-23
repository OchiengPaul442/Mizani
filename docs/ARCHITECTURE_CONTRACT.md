# Product architecture contract (Mizani V1)

This is an offline-first financial application.

## Canonical data and identity

- The local encrypted database is the canonical data source. UI layers read from it; the
  network (when it exists) would only feed it.
- No account is required. Guest mode does not depend on any server response.
- Before an account exists, financial data must not be synchronized to an application
  backend. This build has no backend endpoint and does not declare the INTERNET permission.
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
- No AI model or inference runtime is shipped in the APK, and no model download or network
  permission is added for it.
- No financial payload in prompts: only sanitized facts with stable IDs and category labels may be
  sent to a local model, and nothing leaves the device.

## Security and privacy

- No advertising SDK; no financial telemetry; no financial values in logs.
- Guest financial data never leaves the device.
- Android backup is disabled and all data domains are excluded from backup/transfer rules.
- Database bytes without the device Keystore key must not be sufficient to read data.
- The app lock PIN is a salted PBKDF2-HMAC-SHA256 hash; verification is constant-time.
- No secrets, service-role keys or API keys in the APK.

## Explicit V1 boundaries

- SMS import is policy-gated and absent; core budgeting never depends on it.
- No bank APIs, payment processing, lending, credit scoring, investments or tax advice.
- No cloud sync implementation in this build; the UI states this and offers encrypted backup.
- Single base currency per workspace; no live exchange rates.
- No split transactions yet.
