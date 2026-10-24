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
 * sections. Increment the applicable version whenever its user-facing meaning changes; the app
 * then asks existing users to accept the revised documents before opening their ledger.
 */
object LegalDocuments {
    const val EFFECTIVE_DATE = "25 October 2026"
    const val TERMS_VERSION = "2026-10-25"
    const val PRIVACY_VERSION = "2026-10-25"
    const val PRIVACY_CONTACT = "paul.ochieng.dev@gmail.com"
    const val PROVIDER_NAME_RELEASE_PLACEHOLDER =
        "Mizani app publisher (legal name required before public release)"

    val terms = LegalDocument(
        id = LegalDocumentId.TERMS,
        title = "Terms of Service",
        version = TERMS_VERSION,
        effectiveDate = EFFECTIVE_DATE,
        introduction =
            "These Terms apply when you install or use Mizani. By tapping “Agree and continue” " +
                "during setup, or “Agree and continue” after a terms update, you agree to these Terms. " +
                "Read them before proceeding.",
        sections = listOf(
            LegalSection("1. Provider and contact", listOf(
                "Provider: $PROVIDER_NAME_RELEASE_PLACEHOLDER. Privacy and legal contact: $PRIVACY_CONTACT. " +
                    "The provider’s full legal identity must be completed before public release.",
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
                "Mizani currently works without an account or a Mizani server. Your ledger is stored in an " +
                    "encrypted database on your device. You are responsible for securing the device and for " +
                    "keeping any exports or backups safe. The Privacy Notice explains the app’s local storage " +
                    "and the limited network request used for currency conversion.",
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
                    "Files you save or share may be copied, backed up, or processed by the storage or sharing " +
                    "service you choose. The app cannot remove copies outside its private storage.",
                "Automatic Android backup is disabled for the app. Deleting all local data cannot be undone " +
                    "without a backup. Uninstalling the app can permanently remove your ledger and its " +
                    "device-bound database key. Export a backup first if you want to keep a copy.",
            )),
            LegalSection("8. App rights and changes", listOf(
                "The app and its branding are protected by applicable intellectual-property laws. These Terms " +
                    "give you a personal, non-exclusive, non-transferable right to use the app for lawful personal " +
                    "budgeting while you comply with these Terms. Third-party components remain under their own " +
                    "licenses.",
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
        effectiveDate = EFFECTIVE_DATE,
        introduction =
            "This Notice describes what Mizani processes, where it is stored, and the choices available to " +
                "you. Mizani is designed to keep your financial ledger on your device.",
        sections = listOf(
            LegalSection("1. Who is responsible", listOf(
                "Provider/controller: $PROVIDER_NAME_RELEASE_PLACEHOLDER. Contact: $PRIVACY_CONTACT. " +
                    "The provider’s full legal identity and any required local representative must be completed " +
                    "before public release.",
            )),
            LegalSection("2. Information stored on your device", listOf(
                "The information you enter can include wallet names and balances; transaction types, dates, " +
                    "amounts, fees, currencies, payees and notes; budgets and categories; income sources; goals " +
                    "and contributions; recurring items; and debt records. Free-text names and notes may contain " +
                    "personal information if you choose to put it there.",
                "Mizani also stores settings such as your selected country, primary currency, theme and app-lock " +
                    "preferences. If you set an app PIN, the app stores a salted PIN hash, not the PIN itself. " +
                    "The app stores the versions of these documents you accepted and the acceptance time.",
            )),
            LegalSection("3. Why the app uses this information", listOf(
                "The app uses the information locally to create and update your ledger, show the features you " +
                    "request, calculate your on-device summaries, apply your settings, protect app access, and " +
                    "create an export or backup when you request one. The acceptance record documents which " +
                    "Terms and Notice were shown to you. Applicable law may also require certain records to be " +
                    "kept or disclosed.",
                "Mizani does not require a name, email address, phone number, bank login, or account. It does not " +
                    "include advertising, analytics, crash-reporting, or generative-AI services in this build.",
            )),
            LegalSection("4. Currency-rate request", listOf(
                "When you use conversion for a foreign-currency transaction, the app sends the source and " +
                    "workspace currency codes to the public Frankfurter API over HTTPS. It does not send the " +
                    "transaction amount, date, payee, note, wallet name, or ledger. The app receives the " +
                    "reference rate and published rate date, then stores them with that transaction on your device.",
                "Frankfurter states that its public API does not collect or log personal data, IP addresses, or " +
                    "request URLs, and that its public service uses Cloudflare for caching and denial-of-service " +
                    "protection with aggregate traffic statistics. The request still travels over your internet " +
                    "connection, which is operated by your network provider. See Frankfurter’s current " +
                    "information at https://frankfurter.dev/.",
            )),
            LegalSection("5. Storage, security and retention", listOf(
                "The financial ledger is held in a SQLCipher-encrypted database. Its database key is protected " +
                    "with a non-exportable Android Keystore key. Some app settings are stored separately in " +
                    "Android DataStore; the ledger’s database encryption does not mean every app setting is " +
                    "stored in that database. No security measure can guarantee that a device or data is safe " +
                    "in every circumstance.",
                "Mizani has no server-side copy of your ledger. Local records remain in the app until you delete " +
                    "all local data, clear the app’s storage, or uninstall it. Android automatic backup and " +
                    "device-transfer backup are disabled for the app. Uninstalling or losing access to the " +
                    "device-bound key may make local data unrecoverable.",
            )),
            LegalSection("6. Exports and other services", listOf(
                "CSV exports and Excel workbooks are unencrypted and may expose your financial details to anyone " +
                    "who can access the file. The workbook includes multiple sheets such as transactions, wallets, " +
                    "budgets, recurring schedules, goals and debts. A Mizani backup is encrypted with the " +
                    "passphrase you choose. When you save, share, or restore a file, Android’s file picker and any " +
                    "destination service you select may process or store it under their own terms and privacy " +
                    "notice. Mizani cannot delete those external copies for you.",
            )),
            LegalSection("7. Sharing, sale and tracking", listOf(
                "The app does not upload your ledger to a Mizani account or backend. It does not sell ledger " +
                    "information, use it for advertising, or send it to analytics or AI services. Apart from the " +
                    "currency-code request described above and a file destination you select, the app has no " +
                    "configured service that receives your ledger. The operating system, app marketplace, and " +
                    "network providers may process technical information under their own terms.",
            )),
            LegalSection("8. Your choices and privacy rights", listOf(
                "You can view and correct entries in the app, export your ledger from Settings, and delete local " +
                "data from Settings. Deletion does not remove exports or backups already saved elsewhere. You " +
                    "can stop the currency-rate request by not using foreign-currency conversion; the rest of " +
                    "the ledger remains local.",
                "Depending on the law where you live, you may also have rights to request access, correction, " +
                    "deletion, restriction, objection, or portability, and to complain to a data-protection " +
                    "authority. To contact the provider, email $PRIVACY_CONTACT. The provider has no online " +
                    "ledger account to search; please do not email financial details unless needed to handle " +
                    "your request.",
            )),
            LegalSection("9. Children", listOf(
                "Mizani is not designed to collect information from children. Anyone below the age of legal " +
                    "capacity should use it only with permission from a parent or legal guardian.",
            )),
            LegalSection("10. Updates", listOf(
                "If this Notice changes materially, the app will show the revised Notice and ask you to accept " +
                    "the updated documents before you continue. You can read the current Terms and Notice from " +
                    "Settings at any time. Contact $PRIVACY_CONTACT with a privacy question.",
            )),
        ),
    )

    fun isCurrentAcceptance(termsVersion: String?, privacyVersion: String?): Boolean =
        termsVersion == TERMS_VERSION && privacyVersion == PRIVACY_VERSION
}
