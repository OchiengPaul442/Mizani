package com.mizani.money.data.repo

import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.importing.ImportIdentifiers
import com.mizani.money.core.money.MoneyLimits

/**
 * Structural validation for a [RestoreBundle] before it replaces the local ledger.
 * Rejects bundles that would leave the restored workspace internally inconsistent.
 */
internal object BackupValidator {

    fun validate(bundle: RestoreBundle) {
        require(bundle.workspace.id.isNotBlank()) { "Backup has no workspace" }
        require(bundle.wallets.isNotEmpty()) { "Backup has no wallets" }
        require(bundle.wallets.map { it.id }.toSet().size == bundle.wallets.size) {
            "Backup contains duplicate wallet ids"
        }
        require(bundle.categories.map { it.id }.toSet().size == bundle.categories.size) {
            "Backup contains duplicate category ids"
        }
        require(bundle.incomeSources.map { it.id }.toSet().size == bundle.incomeSources.size) {
            "Backup contains duplicate income source ids"
        }
        require(bundle.transactions.map { it.id }.toSet().size == bundle.transactions.size) {
            "Backup contains duplicate transaction ids"
        }
        require(bundle.budgets.map { it.id }.toSet().size == bundle.budgets.size) {
            "Backup contains duplicate budget ids"
        }
        require(bundle.budgetLines.map { it.id }.toSet().size == bundle.budgetLines.size) {
            "Backup contains duplicate budget line ids"
        }
        require(
            bundle.budgetLines.map { it.budgetId to it.categoryId }.toSet().size == bundle.budgetLines.size
        ) {
            "Backup contains duplicate budget lines for the same budget and category"
        }
        require(bundle.goals.map { it.id }.toSet().size == bundle.goals.size) {
            "Backup contains duplicate goal ids"
        }
        require(bundle.contributions.map { it.id }.toSet().size == bundle.contributions.size) {
            "Backup contains duplicate contribution ids"
        }
        require(bundle.recurring.map { it.id }.toSet().size == bundle.recurring.size) {
            "Backup contains duplicate recurring ids"
        }
        require(bundle.debts.map { it.id }.toSet().size == bundle.debts.size) {
            "Backup contains duplicate debt ids"
        }
        require(bundle.commitments.map { it.id }.toSet().size == bundle.commitments.size) {
            "Backup contains duplicate commitment ids"
        }
        require(bundle.commitmentPayments.map { it.id }.toSet().size == bundle.commitmentPayments.size) {
            "Backup contains duplicate commitment payment ids"
        }
        require(bundle.commitmentPayments.map { it.transactionId }.toSet().size == bundle.commitmentPayments.size) {
            "A transaction cannot pay more than one commitment"
        }

        // Single base currency per workspace: every record must agree with the workspace.
        val workspaceCurrency = bundle.workspace.currencyCode
        bundle.wallets.forEach { wallet ->
            require(wallet.currencyCode == workspaceCurrency) { "Wallet currency does not match the workspace" }
        }

        val walletIds = bundle.wallets.map { it.id }.toSet()
        val categoryIds = bundle.categories.map { it.id }.toSet()
        val categoryTypeById = bundle.categories.associate { it.id to it.type }
        val transactionIds = bundle.transactions.map { it.id }.toSet()
        val incomeSourceIds = bundle.incomeSources.map { it.id }.toSet()
        require(bundle.workspace.outlookDailyTargetMinor == null || bundle.workspace.outlookDailyTargetMinor in 1..MoneyLimits.MAX_MINOR_AMOUNT) {
            "Money Outlook target is invalid"
        }
        val importReferences = mutableSetOf<Pair<String, String>>()

        bundle.transactions.forEach { transaction ->
            require(transaction.walletId in walletIds) { "Transaction references a missing wallet" }
            require(transaction.currencyCode == workspaceCurrency) { "Transaction currency does not match the workspace" }
            if (transaction.type == TransactionType.TRANSFER) {
                require(transaction.amountMinor > 0) { "Transfer amount must be positive" }
                val destination = transaction.toWalletId
                require(destination != null && destination in walletIds) {
                    "Transfer references a missing wallet"
                }
                require(destination != transaction.walletId) {
                    "Transfer must move between different wallets"
                }
            }
            if (transaction.type == TransactionType.REFUND) {
                require(transaction.amountMinor > 0) { "Refund amount must be positive" }
            }
            require(transaction.amountMinor != 0L) { "Transaction has a zero amount" }
            require(transaction.feeMinor >= 0) { "Transaction has a negative fee" }
            TransactionCurrencyRules.validate(
                workspaceCurrencyCode = workspaceCurrency,
                type = transaction.type,
                amountMinor = transaction.amountMinor,
                feeMinor = transaction.feeMinor,
                sourceAmountMinor = transaction.sourceAmountMinor,
                sourceFeeMinor = transaction.sourceFeeMinor,
                sourceCurrencyCode = transaction.sourceCurrencyCode,
                exchangeRate = transaction.exchangeRate,
                exchangeRateDate = transaction.exchangeRateDate,
            )
            if (transaction.categoryId != null) {
                require(transaction.categoryId in categoryIds) { "Transaction references a missing category" }
                // A category's type must agree with the transaction's nature.
                val categoryType = categoryTypeById[transaction.categoryId]
                if (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.REFUND) {
                    require(categoryType == CategoryType.EXPENSE) {
                        "Expense and refund transactions must reference an expense category"
                    }
                } else if (transaction.type == TransactionType.INCOME) {
                    require(categoryType == CategoryType.INCOME) {
                        "Income transactions must reference an income category"
                    }
                }
            }
            if (transaction.feeCategoryId != null) {
                require(transaction.feeCategoryId in categoryIds) { "Transaction references a missing fee category" }
                require(categoryTypeById[transaction.feeCategoryId] == CategoryType.EXPENSE) {
                    "Fee category must be an expense category"
                }
            }
            if (transaction.incomeSourceId != null) {
                require(transaction.incomeSourceId in incomeSourceIds) {
                    "Transaction references a missing income source"
                }
            }
            val provider = transaction.sourceProvider
            val reference = transaction.sourceReference
            require((provider == null) == (reference == null)) {
                "Imported transaction source and reference must be provided together"
            }
            if (provider != null && reference != null) {
                require(ImportIdentifiers.isValidProvider(provider)) { "Imported transaction source is invalid" }
                require(ImportIdentifiers.isValidReference(reference)) { "Imported transaction reference is invalid" }
                require(
                    importReferences.add(
                        ImportIdentifiers.normalizeProvider(provider) to ImportIdentifiers.normalizeReference(reference),
                    ),
                ) {
                    "Backup contains duplicate imported transaction references"
                }
            }
            if (transaction.linkedTransactionId != null) {
                require(transaction.linkedTransactionId in transactionIds) {
                    "Transaction references a missing linked transaction"
                }
            }
            if (transaction.type == TransactionType.INCOME) {
                require(transaction.amountMinor > 0) { "Income must be positive" }
            }
            if (transaction.type == TransactionType.EXPENSE) {
                require(transaction.amountMinor > 0) { "Expense must be positive" }
            }
        }
        val budgetIds = bundle.budgets.map { it.id }.toSet()
        bundle.budgetLines.forEach { line ->
            require(line.budgetId in budgetIds) { "Budget line references a missing budget" }
            require(line.categoryId in categoryIds) { "Budget line references a missing category" }
            require(line.plannedMinor >= 0 && line.fundedMinor >= 0) { "Budget line has a negative amount" }
        }
        val goalIds = bundle.goals.map { it.id }.toSet()
        bundle.contributions.forEach { contribution ->
            require(contribution.goalId in goalIds) { "Contribution references a missing goal" }
            require(contribution.amountMinor > 0) { "Contribution must be positive" }
            if (contribution.walletId != null) {
                require(contribution.walletId in walletIds) { "Contribution references a missing wallet" }
            }
            if (contribution.transferTransactionId != null) {
                require(contribution.transferTransactionId in transactionIds) {
                    "Contribution references a missing transfer"
                }
            }
        }
        bundle.recurring.forEach { rule ->
            if (rule.categoryId != null) {
                require(rule.categoryId in categoryIds) { "Recurring rule references a missing category" }
            }
            if (rule.walletId != null) {
                require(rule.walletId in walletIds) { "Recurring rule references a missing wallet" }
            }
        }
        bundle.goals.forEach { goal ->
            if (goal.linkedWalletId != null) {
                require(goal.linkedWalletId in walletIds) { "Goal references a missing wallet" }
            }
        }
        bundle.debts.forEach { debt ->
            require(debt.currentBalanceMinor >= 0) { "Debt balance cannot be negative" }
            require(debt.periodicPaymentMinor >= 0) { "Debt payment cannot be negative" }
        }
        val commitmentIds = bundle.commitments.map { it.id }.toSet()
        bundle.commitments.forEach { commitment ->
            require(commitment.targetMinor in 1..MoneyLimits.MAX_MINOR_AMOUNT) {
                "Commitment target is invalid"
            }
            require(commitment.reminderDaysBefore in 0..30) { "Commitment reminder is invalid" }
            if (commitment.categoryId != null) {
                require(commitment.categoryId in categoryIds) { "Commitment references a missing category" }
                require(categoryTypeById[commitment.categoryId] == CategoryType.EXPENSE) {
                    "Commitment category must be an expense category"
                }
            }
        }
        bundle.commitmentPayments.forEach { payment ->
            require(payment.commitmentId in commitmentIds) { "Commitment payment references a missing commitment" }
            require(payment.transactionId in transactionIds) { "Commitment payment references a missing transaction" }
            require(bundle.transactions.first { it.id == payment.transactionId }.type == TransactionType.EXPENSE) {
                "Only an expense can pay a commitment"
            }
        }
    }
}
