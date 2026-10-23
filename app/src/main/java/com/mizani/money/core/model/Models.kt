package com.mizani.money.core.model

import java.util.UUID

enum class WalletType(val label: String) {
    CASH("Cash"),
    MOBILE_MONEY("Mobile money"),
    BANK("Bank"),
    CARD("Card"),
    SAVINGS("Savings"),
    DEBT("Debt"),
    OTHER("Other"),
}

enum class CategoryType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
}

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    REFUND("Refund"),
    ADJUSTMENT("Adjustment"),
}

enum class BudgetPeriodType(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    FOUR_WEEK("Four weeks"),
    CUSTOM("Custom dates"),
}

enum class RolloverRule(val label: String) {
    NONE("Do not roll over"),
    POSITIVE("Roll over leftover only"),
    BOTH("Roll over leftover and overspend"),
}

enum class RecurringType(val label: String) {
    BILL("Bill or commitment"),
    INCOME("Expected income"),
}

enum class Frequency(val label: String) {
    WEEKLY("Every week"),
    BIWEEKLY("Every 2 weeks"),
    MONTHLY("Every month"),
    QUARTERLY("Every 3 months"),
    YEARLY("Every year"),
}

enum class IncomeSourceKind(val label: String) {
    SALARY("Salary"),
    BUSINESS("Business"),
    FREELANCE("Freelance"),
    GIG("Casual / gig work"),
    REMITTANCE("Remittance"),
    FARMING("Farming"),
    OTHER("Other"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class InsightKind {
    BUDGET_PACE,
    UPCOMING_COMMITMENT,
    FEE_TREND,
    INCOME_CONCENTRATION,
    UNFUNDED_PRIORITY,
    SPENDING_TREND,
    SAVINGS_PROGRESS,
    DATA_COMPLETENESS,
    ESSENTIAL_COVERAGE,
}

enum class InsightTone {
    NEUTRAL,
    POSITIVE,
    ATTENTION,
}

data class Insight(
    val id: String,
    val kind: InsightKind,
    val tone: InsightTone,
    val title: String,
    val body: String,
    val explanation: String,
    val actionLabel: String? = null,
)

data class CountryPack(
    val code: String,
    val name: String,
    val currencyCode: String,
    val walletSuggestions: List<WalletSuggestion>,
    val mobileMoneyProviders: List<String>,
)

data class WalletSuggestion(
    val name: String,
    val type: WalletType,
)

data class DefaultCategory(
    val key: String,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val colorHex: String,
    val essential: Boolean,
)

object DefaultCategories {
    const val FEES_KEY = "mobile_money_fees"

    val all: List<DefaultCategory> = listOf(
        DefaultCategory("housing", "Housing & rent", CategoryType.EXPENSE, "home", "#087F72", true),
        DefaultCategory("food", "Food & groceries", CategoryType.EXPENSE, "food", "#B78331", true),
        DefaultCategory("transport", "Transport", CategoryType.EXPENSE, "transport", "#3F6F76", false),
        DefaultCategory("fuel", "Fuel", CategoryType.EXPENSE, "fuel", "#7A6A4F", false),
        DefaultCategory("mobile_data", "Mobile & data", CategoryType.EXPENSE, "phone", "#4F6FA8", true),
        DefaultCategory(FEES_KEY, "Mobile money fees", CategoryType.EXPENSE, "fees", "#865C19", false),
        DefaultCategory("utilities", "Utilities", CategoryType.EXPENSE, "bolt", "#2F8F80", true),
        DefaultCategory("education", "School & education", CategoryType.EXPENSE, "school", "#7E57A8", true),
        DefaultCategory("health", "Healthcare", CategoryType.EXPENSE, "health", "#B5564F", true),
        DefaultCategory("family", "Family support", CategoryType.EXPENSE, "family", "#A0626E", false),
        DefaultCategory("debt", "Debt payments", CategoryType.EXPENSE, "debt", "#8C4A3C", true),
        DefaultCategory("savings", "Savings", CategoryType.EXPENSE, "savings", "#2F7D58", false),
        DefaultCategory("giving", "Giving & donations", CategoryType.EXPENSE, "giving", "#9A7B2E", false),
        DefaultCategory("business", "Business expenses", CategoryType.EXPENSE, "business", "#45637A", false),
        DefaultCategory("personal_care", "Personal care", CategoryType.EXPENSE, "personal", "#A8698C", false),
        DefaultCategory("entertainment", "Entertainment", CategoryType.EXPENSE, "entertainment", "#6A6FA8", false),
        DefaultCategory("other_expense", "Other", CategoryType.EXPENSE, "other", "#6D6D6D", false),
        DefaultCategory("salary", "Salary", CategoryType.INCOME, "income", "#087F72", true),
        DefaultCategory("business_income", "Business income", CategoryType.INCOME, "business", "#2F8F80", true),
        DefaultCategory("freelance", "Freelance income", CategoryType.INCOME, "freelance", "#3F6F76", true),
        DefaultCategory("gifts", "Gifts & other income", CategoryType.INCOME, "giving", "#B78331", false),
    )

    fun feesId(): String = defaultEntityId("category", FEES_KEY)

    fun defaultEntityId(entity: String, key: String): String =
        UUID.nameUUIDFromBytes("mizani:$entity:$key".toByteArray()).toString()
}

object CountryPacks {
    val uganda = CountryPack(
        code = "UG",
        name = "Uganda",
        currencyCode = "UGX",
        walletSuggestions = listOf(
            WalletSuggestion("MTN MoMo", WalletType.MOBILE_MONEY),
            WalletSuggestion("Airtel Money", WalletType.MOBILE_MONEY),
            WalletSuggestion("Cash", WalletType.CASH),
            WalletSuggestion("Bank account", WalletType.BANK),
        ),
        mobileMoneyProviders = listOf("MTN MoMo", "Airtel Money"),
    )
    val kenya = CountryPack(
        code = "KE",
        name = "Kenya",
        currencyCode = "KES",
        walletSuggestions = listOf(
            WalletSuggestion("M-Pesa", WalletType.MOBILE_MONEY),
            WalletSuggestion("Airtel Money", WalletType.MOBILE_MONEY),
            WalletSuggestion("Cash", WalletType.CASH),
            WalletSuggestion("Bank account", WalletType.BANK),
        ),
        mobileMoneyProviders = listOf("M-Pesa", "Airtel Money"),
    )
    val tanzania = CountryPack(
        code = "TZ", name = "Tanzania", currencyCode = "TZS",
        walletSuggestions = listOf(
            WalletSuggestion("M-Pesa", WalletType.MOBILE_MONEY),
            WalletSuggestion("Tigo Pesa", WalletType.MOBILE_MONEY),
            WalletSuggestion("Airtel Money", WalletType.MOBILE_MONEY),
            WalletSuggestion("Cash", WalletType.CASH),
        ),
        mobileMoneyProviders = listOf("M-Pesa", "Tigo Pesa", "Airtel Money"),
    )
    val rwanda = CountryPack(
        code = "RW", name = "Rwanda", currencyCode = "RWF",
        walletSuggestions = listOf(
            WalletSuggestion("MTN MoMo", WalletType.MOBILE_MONEY),
            WalletSuggestion("Airtel Money", WalletType.MOBILE_MONEY),
            WalletSuggestion("Cash", WalletType.CASH),
            WalletSuggestion("Bank account", WalletType.BANK),
        ),
        mobileMoneyProviders = listOf("MTN MoMo", "Airtel Money"),
    )
    val nigeria = CountryPack(
        code = "NG", name = "Nigeria", currencyCode = "NGN",
        walletSuggestions = listOf(
            WalletSuggestion("Cash", WalletType.CASH),
            WalletSuggestion("Bank account", WalletType.BANK),
            WalletSuggestion("OPay", WalletType.MOBILE_MONEY),
        ),
        mobileMoneyProviders = listOf("OPay", "PalmPay", "Moniepoint"),
    )
    val ghana = CountryPack(
        code = "GH", name = "Ghana", currencyCode = "GHS",
        walletSuggestions = listOf(
            WalletSuggestion("MTN MoMo", WalletType.MOBILE_MONEY),
            WalletSuggestion("Telecel Cash", WalletType.MOBILE_MONEY),
            WalletSuggestion("Cash", WalletType.CASH),
            WalletSuggestion("Bank account", WalletType.BANK),
        ),
        mobileMoneyProviders = listOf("MTN MoMo", "Telecel Cash", "AirtelTigo Money"),
    )
    val southAfrica = CountryPack(
        code = "ZA", name = "South Africa", currencyCode = "ZAR",
        walletSuggestions = listOf(
            WalletSuggestion("Cash", WalletType.CASH),
            WalletSuggestion("Bank account", WalletType.BANK),
            WalletSuggestion("Savings", WalletType.SAVINGS),
        ),
        mobileMoneyProviders = listOf("MTN MoMo", "Capitec Pay"),
    )
    val other = CountryPack(
        code = "OT", name = "Other / not listed", currencyCode = "USD",
        walletSuggestions = listOf(
            WalletSuggestion("Cash", WalletType.CASH),
            WalletSuggestion("Mobile money", WalletType.MOBILE_MONEY),
            WalletSuggestion("Bank account", WalletType.BANK),
        ),
        mobileMoneyProviders = listOf("Mobile money"),
    )

    val all: List<CountryPack> =
        listOf(uganda, kenya, tanzania, rwanda, nigeria, ghana, southAfrica, other)

    fun byCode(code: String): CountryPack = all.firstOrNull { it.code == code } ?: other

    fun currencyFor(countryCode: String): String = byCode(countryCode).currencyCode
}

object Ids {
    fun newId(): String = UUID.randomUUID().toString()
}

object AppInfo {
    const val NAME = "Mizani"
    const val TAGLINE = "Your money, in balance."
    const val VERSION = "1.0.0"
}
