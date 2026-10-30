# Mizani

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/brand/logo-lockup-on-dark.svg">
    <img src="docs/brand/logo-lockup-on-light.svg" alt="Mizani — Your money, in balance." width="360">
  </picture>
</p>

<p align="center">
  <strong>Clear money tracking for everyday life.</strong><br>
  Track income, expenses and balances across cash, mobile money, bank wallets and currencies.
</p>

<p align="center">
  <a href="https://github.com/OchiengPaul442/budgeting-application/actions/workflows/android-ci.yml"><img src="https://github.com/OchiengPaul442/budgeting-application/actions/workflows/android-ci.yml/badge.svg?branch=main&amp;event=push" alt="Build and tests workflow status"></a>
  <img src="https://img.shields.io/badge/Android-API%2024%2B-087F72?logo=android&logoColor=white" alt="Android API 24 and newer">
  <img src="https://img.shields.io/badge/Kotlin-087F72?logo=kotlin&logoColor=white" alt="Built with Kotlin">
</p>

Mizani is an Android budgeting app designed to work offline. It needs no account: your ledger stays on your device, and the core tracking features work without a connection. Country setup includes presets for Uganda, Kenya, Tanzania, Rwanda, Nigeria, Ghana and South Africa

## What you can do

- **Track money in and out.** Record income and expenses, with transfers, refunds and balance
  adjustments for a more accurate ledger.
- **Import mobile money confirmations.** Paste or share a confirmation from MTN MoMo, Airtel
  Money, M-Pesa, Orange Money, Moov, Wave, Telecel, Tigo, EcoCash, OPay, PalmPay, Moniepoint and
  other supported African networks. Review the suggested expense or income and edit it before
  saving; incoming money asks you to choose its income category. Import works on-device, keeps
  only a provider reference (or an exact-message digest when no reference is supplied) to catch
  duplicates, and never reads SMS in the background.
- **Organize your money.** Use wallets for cash, mobile money and bank accounts; group records by
  category; search and filter activity.
- **Track investments manually.** Record where a holding is placed, its broad asset category, cost
  basis and latest value. Investment snapshots stay encrypted on the device, do not connect to
  providers or place orders, and do not create duplicate ledger activity.
- **Understand your cash flow.** Review income, spending, category breakdowns, wallet balances and
  trends in Insights. Charts include accessible text summaries.
- **Check wallets quickly.** Active wallets appear on Home. Their balances stay hidden until you
  confirm with your app PIN or enabled biometrics. If you have no PIN, set up app lock first.
- **Track one-off commitments.** Add a target and due date for items such as school fees in the
  workspace or another supported currency, link payments already recorded as expenses, and
  optionally receive a generic local reminder. Foreign targets retain the daily reference rate
  used to convert them for workspace-currency planning.
- **Plan with Money Outlook.** See an estimate based on liquid wallet balances, recorded spending
  and unpaid commitments due within 30 days. Expected income is excluded. If there is not enough
  recorded history, enter a daily spending target instead.
- **Record different currencies.** Enter income or expenses in another supported currency. When
  online, Mizani fetches the latest available daily reference rate and saves the source amount,
  converted amount, rate and rate date. Reference rates can differ from bank, card or mobile money
  settlement rates.
- **Keep and move your records.** Export transactions to CSV or Excel, or create and restore a
  Mizani backup encrypted with a passphrase. CSV and Excel files are not encrypted.
- **Get a gentle reminder.** An optional daily local reminder defaults to 8:00 p.m. and can be
  changed in Settings. You can also opt in to local money check-ins for weekly spending, expected
  income, investment valuation, debt payments and savings goals. Check-ins are frequency-limited,
  use generic private notification copy, and show no names or amounts on the lock screen.
- **Protect access.** Use an optional six digit app PIN or biometric unlock.

## Brand

The Mizani identity is built around balance, clarity and trust. The interface uses the official
mark and restrained palette:

| Color      | Hex                  | Use                                      |
| ---------- | -------------------- | ---------------------------------------- |
| Deep Teal  | <code>#102A32</code> | Brand mark and small brand details       |
| Emerald    | <code>#087F72</code> | Primary actions and selected states      |
| Mint       | <code>#76D7C2</code> | Dark-theme emphasis and positive accents |
| Warm Gold  | <code>#E8BF6A</code> | Small highlights                         |
| Soft White | <code>#F7FAF8</code> | Light background                         |
| Ink        | <code>#17353D</code> | Primary text                             |

Dark app surfaces use layered charcoal or warm-slate tones; theme mode and accent remain separately
customizable.

See the [brand guide](docs/brand/BRAND_GUIDE.md) and [design system](docs/DESIGN_SYSTEM.md) for logo use, color, typography, spacing and component guidance.

## Data and privacy

The ledger and manual investment records are stored locally in a database encrypted with SQLCipher. Its key is protected with
Android Keystore. Mizani has no user account, cloud sync, advertising or analytics SDK. Money check-ins are calculated on-device
from the local ledger and do not upload financial data or provide investment or debt advice.

Internet access is used when you request a currency rate or check for an app update. Rate requests
send currency codes only, not transaction amounts or details. Play-installed builds use Google
Play's in-app update flow: Play checks availability and Mizani can prompt while the app is active;
Google Play controls download and installation. Sideloaded builds use the optional public GitHub
release check and do not silently download or install updates.

Encrypted backups are intended for recovery and moving to another device. CSV and Excel exports are
readable files, so choose a trusted place to save them. See the [security and recovery
guide](docs/SECURITY.md) and [privacy notice](docs/PRIVACY_NOTICE.md).

## Build and run

### Requirements

- Android Studio with a full JDK/JBR supported by the project’s Gradle and Android Gradle Plugin versions.
- Android SDK Platform 37 and Android SDK Platform-Tools.
- Internet access for the first Gradle dependency and wrapper downloads.

The repository’s root <code>gradle.properties</code> currently includes a Windows Android Studio
JBR discovery path. If your JDK is installed elsewhere, update that setting locally to your full
JDK/JBR path. Keep machine-specific paths out of commits. Android Studio can create
<code>local.properties</code> for the SDK location; that file is intentionally ignored by Git.

### Build a debug APK

Clone the repository and build with the Gradle wrapper:

```powershell
git clone https://github.com/OchiengPaul442/budgeting-application.git
cd budgeting-application
.\gradlew.bat :app:assembleDebug
```

On macOS or Linux, use <code>./gradlew</code> in place of <code>.\gradlew.bat</code>.

The APK is written to <code>app/build/outputs/apk/debug/app-debug.apk</code>. Install it on a
connected device or emulator with:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run checks

The **Build & Tests** badge reports one GitHub Actions run for the debug APK and JVM unit tests.
It runs on pull requests and pushes to <code>main</code>. Instrumentation tests remain a local
check because they need a disposable Android emulator and clear its app data.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebugAndroidTest
```

To run the Android instrumentation suite, connect a disposable emulator and use:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

> **Test data warning:** Instrumentation tests clear the app’s local database and settings. Run
> them only on a disposable emulator or test profile, never on a device containing a ledger you
> need to keep.

### Release builds

To measure the distribution build instead of the larger development APK, run:

```powershell
.\gradlew.bat :app:assembleRelease
```

The output is <code>app/build/outputs/apk/release/app-release-unsigned.apk</code>. Release builds
shrink code and resources and package only ARM native libraries; debug builds also include emulator
ABIs and development code. The release APK must be signed before Android can install or distribute
it. For this repository, release signing is configured with the four
<code>MIZANI_RELEASE_*</code> Gradle properties.

Set <code>mizaniVersionName</code> in <code>gradle.properties</code> using
<code>MAJOR.MINOR.PATCH</code>; Gradle derives Android’s <code>versionCode</code> from it. A release
build is unsigned unless all four <code>MIZANI*RELEASE*\*</code> signing properties are configured.
Keep signing credentials outside the repository. See [security guidance](docs/SECURITY.md) before
preparing a release. Track pending changes in the [changelog](CHANGELOG.md); during release
preparation, move them into a dated section matching the Gradle version. Use the [Play Store
release checklist](docs/PLAY_STORE_RELEASE_CHECKLIST.md) for the account, legal, signing, QA,
listing, and staged-rollout gates.

## Project structure

Mizani is a single Android app module with package boundaries:

| Package                 | Responsibility                                                    |
| ----------------------- | ----------------------------------------------------------------- |
| <code>core</code>       | Money, currencies, user-shared message parsing, country defaults, dates and security policy |
| <code>data/local</code> | Room entities and encrypted database                              |
| <code>data/repo</code>  | Validated data access, preferences and exchange-rate lookup       |
| <code>domain</code>     | Analytics, CSV/Excel export and encrypted backup                  |
| <code>feature</code>    | Compose screens for onboarding, Home, Activity, Insights, Bills & commitments, Investments and More |
| <code>ui</code>         | Navigation, theme and reusable components                         |

Start with the [project structure](docs/PROJECT_STRUCTURE.md) and [architecture
contract](docs/ARCHITECTURE_CONTRACT.md) when changing the app.

## Project documentation

- [Changelog](CHANGELOG.md)
- [Agent instructions](AGENTS.md)
- [Source code license](LICENSE)
- [Attribution notice](NOTICE)
- [Terms of Service](docs/TERMS_OF_SERVICE.md)
- [Privacy Notice](docs/PRIVACY_NOTICE.md)
- [Security and data recovery](docs/SECURITY.md)
- [Architecture contract](docs/ARCHITECTURE_CONTRACT.md)
- [Design system](docs/DESIGN_SYSTEM.md)
- [Project structure](docs/PROJECT_STRUCTURE.md)
- [Third-party notices](docs/THIRD_PARTY_NOTICES.md)
- [Brand guide](docs/brand/BRAND_GUIDE.md)
- [Illustration and asset credits](docs/ASSET_CREDITS.md)

## License

Mizani's original source code and executable forms of that code are licensed
under Apache-2.0. You may fork, modify, and redistribute them under the terms
in [LICENSE](LICENSE). Preserve the original developer and project attribution
in [NOTICE](NOTICE), and mark files you modify as required by the license.

The Apache license does not cover third-party libraries or artwork, or grant
rights to Mizani's name, logo, or other trademarks. Their separate terms are in
[Third-party notices](docs/THIRD_PARTY_NOTICES.md) and [Asset credits](docs/ASSET_CREDITS.md).
The [Terms of Service](docs/TERMS_OF_SERVICE.md) govern use of official Mizani
app releases and do not narrow rights granted by Apache-2.0.
