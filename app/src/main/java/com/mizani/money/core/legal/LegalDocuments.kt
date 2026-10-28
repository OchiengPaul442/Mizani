package com.mizani.money.core.legal

enum class LegalDocumentId { TERMS, PRIVACY }

data class LegalSection(
    val heading: String,
    val paragraphs: List<String>,
)

data class LegalDocument(
    val id: LegalDocumentId,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val introduction: String,
    val sections: List<LegalSection>,
)

/**
 * User-facing legal drafts. Keep the matching Markdown copies in docs/ aligned with these
 * sections. Versions identify legal-document revisions and are independent of app releases.
 * Increment one only when that document's user-facing legal meaning changes; existing users then
 * accept the revised document once before opening their ledger.
 */
object LegalDocuments {
    const val EFFECTIVE_DATE = "28 October 2026"
    const val PRIVACY_EFFECTIVE_DATE = "28 October 2026"
    const val TERMS_VERSION = "2026-10-28-4"
    const val PRIVACY_VERSION = "2026-10-28-3"
    const val PRIVACY_CONTACT = "paul.ochieng.dev@gmail.com"
    const val PROVIDER_NAME = "Paul Ochieng"
    const val PROVIDER_ADDRESS_DRAFT =
        "Kampala, Uganda (provisional; full physical or postal address required before public release)"

    val terms = LegalDocument(
        id = LegalDocumentId.TERMS,
        title = "Terms of Service",
        version = TERMS_VERSION,
        effectiveDate = EFFECTIVE_DATE,
        introduction =
            "These Terms apply when you install or use Mizani. By tapping “Agree and continue” " +
                "during setup, or after accepting a new version in the app, you agree to these Terms. " +
                "Read them before proceeding.",
        sections = listOf(
            LegalSection("1. Provider and contact", listOf(
                "Provider: $PROVIDER_NAME. Address: $PROVIDER_ADDRESS_DRAFT. Contact: $PRIVACY_CONTACT.",
            )),
            LegalSection("2. Who may use Mizani", listOf(
                "You must be able to enter a binding agreement under the law where you live. If you are " +
                    "below the age of legal capacity, use Mizani only with permission from a parent or legal guardian.",
            )),
            LegalSection("3. What the app provides", listOf(
                "Mizani is a personal budgeting and record-keeping tool. It helps you organize financial " +
                    "information that you choose to enter and view summaries on your device. It is not a bank, " +
                    "payment service, lender, investment adviser, credit-scoring service, accounting service, " +
                    "or tax service. It does not move, hold, or safeguard money for you.",
                "The app does not provide financial, investment, legal, accounting, or tax advice. Its displays " +
                    "are not a recommendation or a promise that funds will be available. Check important " +
                    "decisions against your own records and appropriate professional advice.",
            )),
            LegalSection("4. Your information and device", listOf(
                "Mizani does not keep an online copy of your financial records. They are encrypted and stored " +
                    "on your device. You are responsible for securing the device and keeping exports and backups " +
                    "safe. The Privacy Notice explains optional exchange-rate and update checks.",
                "Mizani uses Android's secure-window setting to block ordinary screenshots, screen recordings, " +
                    "and capture on non-secure displays. This platform control cannot stop a camera or a " +
                    "compromised device, and its behavior depends on Android and the device maker.",
                "If you turn on a daily reminder, Mizani checks the ledger on this device and can show a generic " +
                    "notification when no income, expense, or refund has been recorded that day. It does not send your " +
                    "ledger to a reminder service.",
                "Enter information you are entitled to use. Avoid putting another person’s sensitive details " +
                    "in a payee or note unless you have a valid reason and permission to do so.",
            )),
            LegalSection("5. Currency conversion", listOf(
                "If you record a transaction in a currency different from your workspace currency, Mizani may " +
                    "request the latest available daily reference rate for that currency pair from Frankfurter. " +
                    "The request contains the currency codes, not the amount, payee, note, or transaction date. " +
                    "The app saves the rate and its published date with the converted transaction.",
                "Reference rates can be delayed, revised, unavailable, or different from the rate applied by a " +
                    "bank, card network, cash exchange, or payment provider. They do not include fees or " +
                    "margins. Review the displayed conversion and correct it if needed. Do not rely on it for " +
                    "settlement, trading, tax, or regulatory reporting.",
            )),
            LegalSection("6. Your responsibilities", listOf(
                "You are responsible for the accuracy and lawful use of information you enter, for checking " +
                    "the app’s results, and for making any financial decisions. Do not misuse the app, interfere " +
                    "with its security, or use it in a way that breaks applicable law or another person’s rights.",
            )),
            LegalSection("7. Exports and backups", listOf(
                "A CSV export and an Excel workbook are human-readable and are not encrypted. The workbook " +
                    "contains multiple sheets of your financial records for analysis. A Mizani backup is encrypted " +
                    "with the passphrase you choose; if you lose that passphrase, the backup cannot be restored. " +
                    "Files you save outside Mizani may be copied, backed up, or processed by the destination " +
                    "service under its own terms. The app cannot remove copies outside its private storage.",
                "Automatic device backup is disabled. Deleting all local data cannot be undone without a " +
                    "backup. Uninstalling Mizani can permanently remove your records. Export a backup first " +
                    "if you want to keep a copy.",
            )),
            LegalSection("8. App rights and changes", listOf(
                "These Terms govern use of official Mizani app releases. Rights to use, modify, and distribute " +
                    "Mizani's covered source code and executable forms are granted under the Apache License, " +
                    "Version 2.0, as described in the repository's LICENSE and NOTICE files; these Terms do not " +
                    "narrow those rights. That license does not grant rights to the Mizani name, logo, or third-party " +
                    "materials, which remain subject to their own notices and terms.",
                "We may update, change, or discontinue app features. If a change materially revises these Terms, " +
                    "the app will show the revised Terms and ask you to accept them before continuing to use " +
                    "the app. You can stop using Mizani and delete your local data at any time.",
            )),
            LegalSection("9. Service limits and legal rights", listOf(
                "Mizani is provided without a promise that it will always be available, error-free, or suitable " +
                    "for every device or purpose. To the extent allowed by law, the provider is not responsible " +
                    "for indirect loss caused by relying on an entry, summary, conversion rate, lost device, or " +
                    "export. Nothing in these Terms limits a right or remedy that applicable law does not allow " +
                    "us to limit, including mandatory consumer protections.",
            )),
            LegalSection("10. Applicable law and questions", listOf(
                "Mandatory laws that apply where you live continue to apply. No governing-law choice in this " +
                    "draft removes those protections. For questions about these Terms, contact $PRIVACY_CONTACT.",
            )),
        ),
    )

    val privacy = LegalDocument(
        id = LegalDocumentId.PRIVACY,
        title = "Privacy Notice",
        version = PRIVACY_VERSION,
        effectiveDate = PRIVACY_EFFECTIVE_DATE,
        introduction =
            "This Notice explains how Mizani stores your information and the choices you have.",
        sections = listOf(
            LegalSection("1. Who is responsible", listOf(
                "Provider: $PROVIDER_NAME. Address: $PROVIDER_ADDRESS_DRAFT. Contact: $PRIVACY_CONTACT.",
            )),
            LegalSection("2. Information stored on your device", listOf(
                "The information you enter can include wallet names and balances; transaction types, dates, " +
                    "amounts, fees, currencies, payees and notes; budgets and categories; income sources; goals " +
                    "and contributions; recurring items; and debt records. Free-text names and notes may contain " +
                    "personal information if you choose to put it there.",
                "If you choose to paste or share a supported mobile-money confirmation, Mizani reads its text " +
                    "on this device. The original message is discarded after review; only the transaction you " +
                    "confirm and its provider reference are stored in your ledger.",
                "Mizani also stores settings such as your selected country, primary currency, theme and app-lock " +
                    "preferences, optional reminder settings and time, and cached currency rates. App-lock details " +
                    "stay on this device. The app records which versions of these documents you accepted and when.",
            )),
            LegalSection("3. Why the app uses this information", listOf(
                "Mizani uses your information to provide the budgeting features you choose, protect your records, " +
                    "and create exports or backups when you request them. Optional daily and commitment reminders " +
                    "check your local ledger and use generic notifications without names or amounts; they do not " +
                    "send ledger information off the device.",
                "Mizani does not require a name, email address, phone number, bank login, or account. Your " +
                    "ledger is not used for advertising or usage tracking.",
            )),
            LegalSection("4. Optional requests", listOf(
                "When you convert a foreign-currency transaction, Mizani sends the two currency codes to " +
                    "Frankfurter to get the latest available reference rate. It does not send the amount, date, " +
                    "payee, note, wallet name, or other ledger details. The rate and date are saved with the " +
                    "transaction on your phone.",
                "Frankfurter says it neither collects nor logs personal data, IP addresses, or request details. Your network " +
                    "provider still handles the connection. Read more at https://frankfurter.dev/.",
                "If you installed Mizani outside Google Play, you can choose to check for updates. Mizani requests " +
                    "public release information from GitHub and compares the release version on your device. It " +
                    "does not send your installed version or ledger. GitHub may receive your IP address and request " +
                    "details under its Privacy Statement. You can skip the check.",
            )),
            LegalSection("5. Storage, security and retention", listOf(
                "Your financial records are encrypted and stored on this phone. Device security helps protect " +
                    "access to them. No security measure can guarantee that a device or data is safe in every " +
                    "circumstance.",
                "Mizani sets Android's secure-window protection on app screens, dialogs and menus to block " +
                    "ordinary screenshots, screen recordings, and non-secure display capture. Android and device " +
                    "behavior varies; this cannot prevent capture with an external camera or on a compromised device.",
                "Mizani keeps no online copy of your ledger. Local records remain in the app until you delete all " +
                    "local data, clear the app’s storage, or uninstall it. Delete all data also clears saved currency " +
                    "rates, settings and reminder schedules. Automatic device backups are disabled. " +
                    "Uninstalling the app or losing access to this phone may make local data unrecoverable.",
            )),
            LegalSection("6. Exports and other services", listOf(
                "CSV exports and Excel workbooks are unencrypted and may expose your financial details to anyone " +
                    "who can access the file. The workbook includes multiple sheets such as transactions, wallets, " +
                    "budgets, recurring schedules, goals, debts and commitments. A Mizani backup is encrypted with the " +
                    "passphrase you choose. When you save or restore a file, Android’s file picker and any " +
                    "destination service you select may process or store it under their own terms and privacy " +
                    "notice. Mizani cannot delete those external copies for you.",
            )),
            LegalSection("7. Other services", listOf(
                "The provider does not receive or sell an online copy of your ledger, or use it for advertising. " +
                    "External services receive only the information described above or a file you choose to " +
                    "save. The operating system, app marketplace, and network providers may process " +
                    "technical information under their own terms.",
            )),
            LegalSection("8. Your choices and privacy rights", listOf(
                "You can view and correct entries in the app, export your ledger from Settings, and delete local " +
                    "data from Settings. Deletion does not remove exports or backups already saved elsewhere. " +
                    "Currency-rate requests happen only when you convert a foreign-currency transaction. Update " +
                    "checks are optional and available when you install outside Google Play. Neither sends ledger " +
                    "entries. Optional reminders use local settings and check local records only. Shared or pasted " +
                    "transaction text is processed on this device and is not retained as a message.",
                "Privacy rights depend on the law that applies where you live. That law may give you rights to " +
                    "request access, correction or deletion, object to certain processing, or complain to a data " +
                    "protection authority. You can manage records stored only on this device in the app. For a " +
                    "request about information the provider handles, email $PRIVACY_CONTACT. Do not email ledger " +
                    "details unless needed to handle your request.",
            )),
            LegalSection("9. Children", listOf(
                "Mizani is not designed to collect information from children. Anyone below the age of legal " +
                    "capacity should use it only with permission from a parent or legal guardian.",
            )),
            LegalSection("10. Changes", listOf(
                "If this Notice changes materially, the app will show it and ask you to accept " +
                    "the new documents before you continue. You can read the Terms and Notice from " +
                    "Settings at any time. Contact $PRIVACY_CONTACT with a privacy question.",
            )),
        ),
    )

    fun isCurrentAcceptance(termsVersion: String?, privacyVersion: String?): Boolean =
        termsVersion == TERMS_VERSION && privacyVersion == PRIVACY_VERSION
}
