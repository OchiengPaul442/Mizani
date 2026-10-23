package com.mizani.money.data.local

import androidx.room.TypeConverter
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.IncomeSourceKind
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType

class Converters {
    @TypeConverter fun walletTypeToString(value: WalletType): String = value.name
    @TypeConverter fun stringToWalletType(value: String): WalletType =
        runCatching { WalletType.valueOf(value) }.getOrDefault(WalletType.OTHER)

    @TypeConverter fun categoryTypeToString(value: CategoryType): String = value.name
    @TypeConverter fun stringToCategoryType(value: String): CategoryType =
        runCatching { CategoryType.valueOf(value) }.getOrDefault(CategoryType.EXPENSE)

    @TypeConverter fun transactionTypeToString(value: TransactionType): String = value.name
    @TypeConverter fun stringToTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter fun budgetPeriodToString(value: BudgetPeriodType): String = value.name
    @TypeConverter fun stringToBudgetPeriod(value: String): BudgetPeriodType =
        runCatching { BudgetPeriodType.valueOf(value) }.getOrDefault(BudgetPeriodType.MONTHLY)

    @TypeConverter fun rolloverToString(value: RolloverRule): String = value.name
    @TypeConverter fun stringToRollover(value: String): RolloverRule =
        runCatching { RolloverRule.valueOf(value) }.getOrDefault(RolloverRule.NONE)

    @TypeConverter fun recurringTypeToString(value: RecurringType): String = value.name
    @TypeConverter fun stringToRecurringType(value: String): RecurringType =
        runCatching { RecurringType.valueOf(value) }.getOrDefault(RecurringType.BILL)

    @TypeConverter fun frequencyToString(value: Frequency): String = value.name
    @TypeConverter fun stringToFrequency(value: String): Frequency =
        runCatching { Frequency.valueOf(value) }.getOrDefault(Frequency.MONTHLY)

    @TypeConverter fun incomeKindToString(value: IncomeSourceKind): String = value.name
    @TypeConverter fun stringToIncomeKind(value: String): IncomeSourceKind =
        runCatching { IncomeSourceKind.valueOf(value) }.getOrDefault(IncomeSourceKind.OTHER)
}
