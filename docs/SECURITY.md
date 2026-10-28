# Security and data recovery

Mizani stores personal financial records. Its protections are designed to reduce exposure on a lost device, prevent accidental data replacement, and make recovery possible through a user-managed encrypted backup. No mobile app can guarantee protection on a rooted, compromised, or actively monitored device.

## Protections in the app

- The Room database is encrypted with SQLCipher. A random database key is wrapped by an AES-GCM key in Android Keystore. If the wrapped key cannot be saved, database startup fails closed. If the key is missing while database files remain, Mizani refuses to create a new key that would orphan those records.
- Android cloud backup and device transfer are disabled for app data. A database file must not be restored without its matching Keystore key.
- The optional app PIN is stored as a salted PBKDF2-HMAC-SHA256 verifier and checked with a constant-time comparison. Failed-attempt backoff is persisted. The PIN is an access barrier; device encryption and Android Keystore protect the database at rest.
- Each CSV export, Excel export, and encrypted-backup creation requires a fresh confirmation with the app PIN, or device biometrics when the user has enabled them. PIN confirmation uses the same persisted failed-attempt backoff as app unlock. A user without a PIN must set one up first; this also enables app lock when Mizani is reopened.
- App windows use `FLAG_SECURE` to prevent ordinary screenshots and non-secure displays from capturing financial screens. Android notes that this protection is not complete on older versions of Android ([platform guidance](https://developer.android.com/security/fraud-prevention/activities)).
- Encrypted backups use AES-GCM and a passphrase-derived key. New backups use the versioned `MZNB2` envelope with PBKDF2-HMAC-SHA256 at 600,000 iterations; `MZNB1` files remain readable for compatibility. This iteration count follows current OWASP PBKDF2 guidance ([Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)). Backup input is size-bounded, decoded values are validated, and replacement happens in one Room transaction.
- CSV and Excel exports are intentionally unencrypted for analysis. After fresh identity confirmation, the app warns before export and uses Android's document picker; save those files only in a location you trust.
- Network requests use HTTPS. Exchange-rate requests send currency codes, not transaction records. Update checks fetch public release metadata. Daily reminders contain no balances or transaction details and use private notification visibility.

## Recovery and data-loss precautions

1. Create an encrypted backup before uninstalling, resetting, or replacing a device. Keep the backup file and its passphrase in separate trusted places.
2. To check a backup, select it in **Data export & backup → Restore or move data** and enter its passphrase. Mizani shows a contents summary before changing the ledger.
3. Restore replaces the current local ledger. Confirm only after checking that the summary is for the intended backup. The restore validates the records before applying them in a database transaction.
4. Keep periodic backups if the records matter. Mizani has no cloud sync or server-side recovery. If the device and Keystore key are lost, the encrypted database cannot be recovered without a usable encrypted backup and its passphrase.
5. Protect CSV and Excel exports separately; they are not encrypted by Mizani.

## Limits

Encryption at rest does not protect data while the device is unlocked and the app is running. Malware with device-level access, a compromised operating system, screen-reading accessibility software, or someone who knows the backup passphrase may expose data. `FLAG_SECURE` is a platform deterrent, not a guarantee. Backups are user-managed, so Mizani cannot restore a lost file or forgotten passphrase.

## Security maintenance

For changes to database encryption, backup format, PIN verification, exported Android components, or personal-data handling, review the threat model and recovery behavior alongside code changes. Run JVM unit tests and a debug build before release. Instrumentation tests in this repository reset the app database and settings; run them only with disposable test data, never against a device whose Mizani ledger must be preserved.

This repository does not contain a release signing key, so `assembleRelease` produces an unsigned APK unless protected build inputs are configured. Put these values in the user's Gradle home `gradle.properties` or a protected CI Gradle home file, not the project `gradle.properties`:

| Gradle property | Value |
| --- | --- |
| `MIZANI_RELEASE_STORE_FILE` | Path to the private release keystore |
| `MIZANI_RELEASE_STORE_PASSWORD` | Keystore password |
| `MIZANI_RELEASE_KEY_ALIAS` | Signing key alias |
| `MIZANI_RELEASE_KEY_PASSWORD` | Signing key password |

Set all four properties or none; partial configuration fails the build. Gradle recommends user-home properties for credentials because their contents are not serialized into configuration-cache entries ([Gradle configuration-cache security guidance](https://docs.gradle.org/current/userguide/configuration_cache.html)). Keep the keystore and passwords out of the repository and command line. Coordinate signing-key rotation with the app distribution channel and verify that existing installs can update before rotating.
