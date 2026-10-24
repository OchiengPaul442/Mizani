# Mizani Privacy Notice

**Version:** 2026-10-25  
**Effective:** 25 October 2026  
**Provider/controller:** Mizani app publisher (legal name required before public release)  
**Contact:** paul.ochieng.dev@gmail.com

This Notice describes what Mizani processes, where it is stored, and the choices available to you. Mizani is designed to keep your financial ledger on your device.

## 1. Who is responsible

Provider/controller: Mizani app publisher (legal name required before public release). Contact: paul.ochieng.dev@gmail.com. The provider’s full legal identity and any required local representative must be completed before public release.

## 2. Information stored on your device

The information you enter can include wallet names and balances; transaction types, dates, amounts, fees, currencies, payees and notes; budgets and categories; income sources; goals and contributions; recurring items; and debt records. Free-text names and notes may contain personal information if you choose to put it there.

Mizani also stores settings such as your selected country, primary currency, theme and app-lock preferences. If you set an app PIN, the app stores a salted PIN hash, not the PIN itself. The app stores the versions of these documents you accepted and the acceptance time.

## 3. Why the app uses this information

The app uses the information locally to create and update your ledger, show the features you request, calculate your on-device summaries, apply your settings, protect app access, and create an export or backup when you request one. The acceptance record documents which Terms and Notice were shown to you. Applicable law may also require certain records to be kept or disclosed.

Mizani does not require a name, email address, phone number, bank login, or account. It does not include advertising, analytics, crash-reporting, or generative-AI services in this build.

## 4. Currency-rate request

When you use conversion for a foreign-currency transaction, the app sends the source and workspace currency codes to the public Frankfurter API over HTTPS. It does not send the transaction amount, date, payee, note, wallet name, or ledger. The app receives the reference rate and published rate date, then stores them with that transaction on your device.

Frankfurter states that its public API does not collect or log personal data, IP addresses, or request URLs, and that its public service uses Cloudflare for caching and denial-of-service protection with aggregate traffic statistics. The request still travels over your internet connection, which is operated by your network provider. See Frankfurter’s current information at <https://frankfurter.dev/>.

## 5. Storage, security and retention

The financial ledger is held in a SQLCipher-encrypted database. Its database key is protected with a non-exportable Android Keystore key. Some app settings are stored separately in Android DataStore; the ledger’s database encryption does not mean every app setting is stored in that database. No security measure can guarantee that a device or data is safe in every circumstance.

Mizani has no server-side copy of your ledger. Local records remain in the app until you delete all local data, clear the app’s storage, or uninstall it. Android automatic backup and device-transfer backup are disabled for the app. Uninstalling or losing access to the device-bound key may make local data unrecoverable.

## 6. Exports and other services

CSV exports and Excel workbooks are unencrypted and may expose your financial details to anyone who can access the file. The workbook includes multiple sheets such as transactions, wallets, budgets, recurring schedules, goals and debts. A Mizani backup is encrypted with the passphrase you choose. When you save, share, or restore a file, Android’s file picker and any destination service you select may process or store it under their own terms and privacy notice. Mizani cannot delete those external copies for you.

## 7. Sharing, sale and tracking

The app does not upload your ledger to a Mizani account or backend. It does not sell ledger information, use it for advertising, or send it to analytics or AI services. Apart from the currency-code request described above and a file destination you select, the app has no configured service that receives your ledger. The operating system, app marketplace, and network providers may process technical information under their own terms.

## 8. Your choices and privacy rights

You can view and correct entries in the app, export your ledger from Settings, and delete local data from Settings. Deletion does not remove exports or backups already saved elsewhere. You can stop the currency-rate request by not using foreign-currency conversion; the rest of the ledger remains local.

Depending on the law where you live, you may also have rights to request access, correction, deletion, restriction, objection, or portability, and to complain to a data-protection authority. To contact the provider, email paul.ochieng.dev@gmail.com. The provider has no online ledger account to search; please do not email financial details unless needed to handle your request.

## 9. Children

Mizani is not designed to collect information from children. Anyone below the age of legal capacity should use it only with permission from a parent or legal guardian.

## 10. Updates

If this Notice changes materially, the app will show the revised Notice and ask you to accept the updated documents before you continue. You can read the current Terms and Notice from Settings at any time. Contact paul.ochieng.dev@gmail.com with a privacy question.
