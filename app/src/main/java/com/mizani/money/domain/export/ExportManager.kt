package com.mizani.money.domain.export

import android.content.Context
import com.mizani.money.core.model.BudgetPeriodType
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.Frequency
import com.mizani.money.core.model.IncomeSourceKind
import com.mizani.money.core.model.RecurringType
import com.mizani.money.core.model.RolloverRule
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.CategoryEntity
import com.mizani.money.data.local.CommitmentEntity
import com.mizani.money.data.local.CommitmentPaymentEntity
import com.mizani.money.data.local.DebtEntity
import com.mizani.money.data.local.GoalContributionEntity
import com.mizani.money.data.local.GoalEntity
import com.mizani.money.data.local.IncomeSourceEntity
import com.mizani.money.data.local.RecurringRuleEntity
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.local.WalletEntity
import com.mizani.money.data.local.WorkspaceEntity
import com.mizani.money.data.repo.FinanceRepository
import com.mizani.money.data.repo.RestoreBundle
import com.mizani.money.data.repo.BackupValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.io.OutputStream
import java.math.BigDecimal

class BackupException(message: String) : Exception(message)

data class BackupSummary(
    val createdAt: Long,
    val currencyCode: String,
    val walletCount: Int,
    val transactionCount: Int,
    val categoryCount: Int,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val incomeMinor: Long,
    val expenseMinor: Long,
)

/**
 * User-controlled portability: human-readable CSV and Excel exports, plus a versioned,
 * passphrase-encrypted JSON backup. Restore is all-or-nothing (validated first, then applied in one Room
 * transaction) so a failed import can never partially overwrite the existing ledger.
 */
class ExportManager(
    private val context: Context,
    private val repository: FinanceRepository,
) {

    suspend fun buildCsv(): String {
        val snapshot = repository.snapshot.first()
        val currency = Currencies.byCode(snapshot.currencyCode)
        val wallets = snapshot.wallets.associateBy { it.id }
        val categories = snapshot.categories.associateBy { it.id }
        val sources = snapshot.incomeSources.associateBy { it.id }
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.append(
            "date,type,amount,currency,fee,wallet,to_wallet,category,income_source,payee,note,linked_transaction," +
                "original_amount,original_currency,original_fee,exchange_rate,exchange_rate_date,source_provider,source_reference\n",
        )
        snapshot.transactions
            .sortedWith(compareBy<TransactionEntity> { it.localDateEpochDay }.thenBy { it.occurredAt })
            .forEach { transaction ->
                val escaped = listOf(
                    csvEscape(LocalDate.ofEpochDay(transaction.localDateEpochDay).toString()),
                    csvEscape(transaction.type.name),
                    csvEscapeNumeric(amountText(transaction.amountMinor, currency.code)),
                    csvEscape(transaction.currencyCode),
                    csvEscapeNumeric(amountText(transaction.feeMinor, currency.code)),
                    csvEscape(wallets[transaction.walletId]?.name ?: ""),
                    csvEscape(transaction.toWalletId?.let { wallets[it]?.name } ?: ""),
                    csvEscape(transaction.categoryId?.let { categories[it]?.name } ?: ""),
                    csvEscape(transaction.incomeSourceId?.let { sources[it]?.name } ?: ""),
                    csvEscape(transaction.payee),
                    csvEscape(transaction.note),
                    csvEscape(transaction.linkedTransactionId ?: ""),
                    csvEscapeNumeric(
                        transaction.sourceAmountMinor?.let {
                            amountText(it, transaction.sourceCurrencyCode ?: transaction.currencyCode)
                        } ?: "",
                    ),
                    csvEscape(transaction.sourceCurrencyCode ?: ""),
                    csvEscapeNumeric(
                        transaction.sourceFeeMinor?.let {
                            amountText(it, transaction.sourceCurrencyCode ?: transaction.currencyCode)
                        } ?: "",
                    ),
                    csvEscapeNumeric(transaction.exchangeRate ?: ""),
                    csvEscape(transaction.exchangeRateDate ?: ""),
                    csvEscape(transaction.sourceProvider ?: ""),
                    csvEscape(transaction.sourceReference ?: ""),
                )
                builder.append(escaped.joinToString(",")).append('\n')
            }
        return builder.toString()
    }

    /** Writes a multi-sheet, Excel-compatible workbook without formula evaluation or macros. */
    suspend fun writeExcel(output: OutputStream) {
        val snapshot = repository.snapshot.first()
        val workspaceCurrency = snapshot.currencyCode
        val wallets = snapshot.wallets.associateBy { it.id }
        val categories = snapshot.categories.associateBy { it.id }
        val sources = snapshot.incomeSources.associateBy { it.id }
        val budgets = snapshot.budgets.associateBy { it.id }
        val goals = snapshot.goals.associateBy { it.id }

        val sheets = listOf(
            ExcelSheet(
                name = "Overview",
                headers = listOf("Field", "Value"),
                rows = listOf(
                    listOf(ExcelCell.Text("Workspace"), ExcelCell.Text(snapshot.workspace?.name ?: "My money")),
                    listOf(ExcelCell.Text("Primary currency"), ExcelCell.Text(workspaceCurrency)),
                    listOf(ExcelCell.Text("Exported on"), ExcelCell.Date(LocalDate.now())),
                    listOf(ExcelCell.Text("Transactions"), ExcelCell.Number(snapshot.transactions.size.toString())),
                    listOf(ExcelCell.Text("Recurring schedules"), ExcelCell.Number(snapshot.recurring.size.toString())),
                    listOf(ExcelCell.Text("Wallets"), ExcelCell.Number(snapshot.wallets.size.toString())),
                    listOf(ExcelCell.Text("Budgets"), ExcelCell.Number(snapshot.budgets.size.toString())),
                    listOf(
                        ExcelCell.Text("How to read this workbook"),
                        ExcelCell.Text("Transactions are entries recorded in Mizani. Recurring schedules are forecasts; they do not affect balances until recorded."),
                    ),
                    listOf(
                        ExcelCell.Text("Records included"),
                        ExcelCell.Text("Archived and soft-deleted records retained in this workspace are included and marked in their sheets."),
                    ),
                    listOf(
                        ExcelCell.Text("File protection"),
                        ExcelCell.Text("This workbook is not encrypted. Keep it in a private location."),
                    ),
                    listOf(
                        ExcelCell.Text("Long text"),
                        ExcelCell.Text("Excel limits a cell to 32,767 characters. Longer text is truncated to fit."),
                    ),
                ),
            ),
            ExcelSheet(
                name = "Transactions",
                headers = listOf(
                    "ID", "Date", "Type", "Amount", "Currency", "Fee", "Wallet ID", "Wallet",
                    "Transfer wallet ID", "Transfer wallet", "Category ID", "Category", "Fee category ID",
                    "Fee category", "Income source ID", "Income source", "Payee", "Note", "Linked transaction ID",
                    "Original amount", "Original currency", "Original fee", "Exchange rate", "Exchange rate date",
                    "Import source", "Import reference",
                    "Created at", "Updated at", "Deleted at",
                ),
                rows = snapshot.transactions
                    .sortedWith(compareBy<TransactionEntity> { it.localDateEpochDay }.thenBy { it.occurredAt })
                    .map { transaction ->
                        listOf(
                            text(transaction.id), date(transaction.localDateEpochDay), text(transaction.type.name),
                            money(transaction.amountMinor, transaction.currencyCode), text(transaction.currencyCode),
                            money(transaction.feeMinor, transaction.currencyCode), text(transaction.walletId),
                            text(wallets[transaction.walletId]?.name), text(transaction.toWalletId),
                            text(transaction.toWalletId?.let { wallets[it]?.name }), text(transaction.categoryId),
                            text(transaction.categoryId?.let { categories[it]?.name }), text(transaction.feeCategoryId),
                            text(transaction.feeCategoryId?.let { categories[it]?.name }), text(transaction.incomeSourceId),
                            text(transaction.incomeSourceId?.let { sources[it]?.name }), text(transaction.payee),
                            text(transaction.note), text(transaction.linkedTransactionId),
                            transaction.sourceAmountMinor?.let { money(it, transaction.sourceCurrencyCode ?: transaction.currencyCode) }
                                ?: text(""),
                            text(transaction.sourceCurrencyCode), transaction.sourceFeeMinor?.let {
                                money(it, transaction.sourceCurrencyCode ?: transaction.currencyCode)
                            } ?: text(""),
                            decimal(transaction.exchangeRate), text(transaction.exchangeRateDate),
                            text(transaction.sourceProvider), text(transaction.sourceReference),
                            timestamp(transaction.createdAt), timestamp(transaction.updatedAt),
                            timestamp(transaction.deletedAt),
                        )
                    },
            ),
            ExcelSheet(
                name = "Recurring",
                headers = listOf(
                    "ID", "Name", "Type", "Amount", "Currency", "Category ID", "Category", "Wallet ID",
                    "Wallet", "Frequency", "Next date", "Essential", "Note", "Archived", "Deleted at",
                ),
                rows = snapshot.recurring.sortedBy { it.nextDueEpochDay }.map { rule ->
                    listOf(
                        text(rule.id), text(rule.name), text(rule.type.name), money(rule.amountMinor, workspaceCurrency),
                        text(workspaceCurrency), text(rule.categoryId), text(rule.categoryId?.let { categories[it]?.name }),
                        text(rule.walletId), text(rule.walletId?.let { wallets[it]?.name }), text(rule.frequency.label),
                        date(rule.nextDueEpochDay), text(if (rule.essential) "Yes" else "No"), text(rule.note),
                        text(if (rule.archived) "Yes" else "No"), timestamp(rule.deletedAt),
                    )
                },
            ),
            ExcelSheet(
                name = "Wallets",
                headers = listOf(
                    "ID", "Name", "Type", "Opening balance", "Currency", "Color", "Icon", "Sort order", "Archived", "Deleted at",
                ),
                rows = snapshot.wallets.sortedBy { it.sortOrder }.map { wallet ->
                    listOf(
                        text(wallet.id), text(wallet.name), text(wallet.type.label),
                        money(wallet.openingBalanceMinor, wallet.currencyCode), text(wallet.currencyCode),
                        text(wallet.colorHex), text(wallet.icon), number(wallet.sortOrder),
                        text(if (wallet.archived) "Yes" else "No"), timestamp(wallet.deletedAt),
                    )
                },
            ),
            ExcelSheet(
                name = "Categories",
                headers = listOf("ID", "Name", "Type", "Icon", "Color", "Default", "Archived", "Deleted at"),
                rows = snapshot.categories.sortedBy { it.sortOrder }.map { category ->
                    listOf(
                        text(category.id), text(category.name), text(category.type.label), text(category.icon),
                        text(category.colorHex), text(if (category.isDefault) "Yes" else "No"),
                        text(if (category.archived) "Yes" else "No"), timestamp(category.deletedAt),
                    )
                },
            ),
            ExcelSheet(
                name = "Income Sources",
                headers = listOf("ID", "Name", "Kind", "Archived", "Deleted at"),
                rows = snapshot.incomeSources.sortedBy { it.name }.map { source ->
                    listOf(text(source.id), text(source.name), text(source.kind.label), text(if (source.archived) "Yes" else "No"), timestamp(source.deletedAt))
                },
            ),
            ExcelSheet(
                name = "Budgets",
                headers = listOf("ID", "Name", "Period", "Start date", "End date", "Deleted at"),
                rows = snapshot.budgets.sortedBy { it.startEpochDay }.map { budget ->
                    listOf(
                        text(budget.id), text(budget.name), text(budget.periodType.label),
                        date(budget.startEpochDay), date(budget.endEpochDay), timestamp(budget.deletedAt),
                    )
                },
            ),
            ExcelSheet(
                name = "Budget Lines",
                headers = listOf("ID", "Budget ID", "Budget", "Category ID", "Category", "Planned", "Funded", "Rollover", "Essential", "Priority", "Note"),
                rows = snapshot.budgetLines.map { line ->
                    listOf(
                        text(line.id), text(line.budgetId), text(budgets[line.budgetId]?.name), text(line.categoryId),
                        text(categories[line.categoryId]?.name), money(line.plannedMinor, workspaceCurrency),
                        money(line.fundedMinor, workspaceCurrency), text(line.rolloverRule.label),
                        text(if (line.essential) "Yes" else "No"), number(line.priority), text(line.note),
                    )
                },
            ),
            ExcelSheet(
                name = "Goals",
                headers = listOf("ID", "Name", "Target", "Allocated", "Target date", "Linked wallet ID", "Linked wallet", "Priority", "Note", "Archived", "Deleted at"),
                rows = snapshot.goals.sortedBy { it.priority }.map { goal ->
                    listOf(
                        text(goal.id), text(goal.name), money(goal.targetMinor, workspaceCurrency),
                        money(goal.allocatedMinor, workspaceCurrency), date(goal.targetEpochDay), text(goal.linkedWalletId),
                        text(goal.linkedWalletId?.let { wallets[it]?.name }), number(goal.priority), text(goal.note),
                        text(if (goal.archived) "Yes" else "No"), timestamp(goal.deletedAt),
                    )
                },
            ),
            ExcelSheet(
                name = "Contributions",
                headers = listOf("ID", "Goal ID", "Goal", "Amount", "Date", "Wallet ID", "Wallet", "Transfer transaction ID", "Note"),
                rows = snapshot.contributions.sortedBy { it.localDateEpochDay }.map { contribution ->
                    listOf(
                        text(contribution.id), text(contribution.goalId), text(goals[contribution.goalId]?.name),
                        money(contribution.amountMinor, workspaceCurrency), date(contribution.localDateEpochDay),
                        text(contribution.walletId), text(contribution.walletId?.let { wallets[it]?.name }),
                        text(contribution.transferTransactionId), text(contribution.note),
                    )
                },
            ),
            ExcelSheet(
                name = "Debts",
                headers = listOf("ID", "Name", "Lender", "Current balance", "Original amount", "Periodic payment", "Next date", "Interest rate percent", "Note", "Archived", "Deleted at"),
                rows = snapshot.debts.map { debt ->
                    listOf(
                        text(debt.id), text(debt.name), text(debt.lender), money(debt.currentBalanceMinor, workspaceCurrency),
                        debt.originalMinor?.let { money(it, workspaceCurrency) } ?: text(""),
                        money(debt.periodicPaymentMinor, workspaceCurrency), date(debt.nextDueEpochDay),
                        debt.interestRatePercent?.let { ExcelCell.Number(BigDecimal.valueOf(it).toPlainString()) } ?: text(""),
                        text(debt.note), text(if (debt.archived) "Yes" else "No"), timestamp(debt.deletedAt),
                    )
                },
            ),
            ExcelSheet(
                name = "Commitments",
                headers = listOf(
                    "ID", "Name", "Target", "Paid", "Remaining", "Due date", "Category ID", "Category",
                    "Reminder days before", "Archived",
                ),
                rows = snapshot.commitmentProgress().map { progress ->
                    val commitment = progress.commitment
                    listOf(
                        text(commitment.id), text(commitment.name), money(commitment.targetMinor, workspaceCurrency),
                        money(progress.paidMinor, workspaceCurrency), money(progress.remainingMinor, workspaceCurrency),
                        date(commitment.dueEpochDay), text(commitment.categoryId),
                        text(commitment.categoryId?.let { categories[it]?.name }), number(commitment.reminderDaysBefore),
                        text(if (commitment.archived) "Yes" else "No"),
                    )
                },
            ),
        )
        XlsxWorkbookWriter.write(output, sheets)
    }

    suspend fun buildBackup(passphrase: String): ByteArray {
        require(passphrase.length >= 12) { "Use a passphrase with at least 12 characters" }
        val bundle = repository.exportBundle()
        val plaintext = withContext(Dispatchers.Default) {
            encodeBundle(bundle).toString().toByteArray(Charsets.UTF_8)
        }
        return try {
            withContext(Dispatchers.Default) { BackupCrypto.encrypt(plaintext, passphrase) }
        } finally {
            plaintext.fill(0)
        }
    }

    suspend fun inspectBackup(bytes: ByteArray, passphrase: String): BackupSummary =
        withContext(Dispatchers.Default) { summarize(decodeBackup(bytes, passphrase)) }

    suspend fun restore(bytes: ByteArray, passphrase: String): BackupSummary {
        val bundle = withContext(Dispatchers.Default) { decodeBackup(bytes, passphrase) }
        repository.restoreBundle(bundle)
        return summarize(bundle)
    }

    private fun decodeBackup(bytes: ByteArray, passphrase: String): RestoreBundle {
        val plaintext = BackupCrypto.decrypt(bytes, passphrase)
        return try {
            decodeBundle(plaintext).also(BackupValidator::validate)
        } finally {
            plaintext.fill(0)
        }
    }

    fun suggestedCsvName(today: LocalDate = LocalDate.now()): String =
        "mizani-transactions-${today}.csv"

    fun suggestedExcelName(today: LocalDate = LocalDate.now()): String =
        "mizani-finances-${today}.xlsx"

    fun suggestedBackupName(today: LocalDate = LocalDate.now()): String =
        "mizani-backup-${today}.mizani"

    private fun summarize(bundle: RestoreBundle): BackupSummary {
        val transactions = bundle.transactions
        return BackupSummary(
            createdAt = bundle.workspace.updatedAt,
            currencyCode = bundle.workspace.currencyCode,
            walletCount = bundle.wallets.size,
            transactionCount = transactions.size,
            categoryCount = bundle.categories.size,
            firstDate = transactions.minOfOrNull { it.localDateEpochDay }?.let { LocalDate.ofEpochDay(it) },
            lastDate = transactions.maxOfOrNull { it.localDateEpochDay }?.let { LocalDate.ofEpochDay(it) },
            incomeMinor = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
            // Eligible spending: expense amounts + every fee (transfer and refund), net of refund amounts.
            expenseMinor = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountMinor + it.feeMinor } +
                transactions.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeMinor } -
                transactions.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor } +
                transactions.filter { it.type == TransactionType.REFUND }.sumOf { it.feeMinor },
        )
    }

    private fun text(value: String?): ExcelCell = ExcelCell.Text(value.orEmpty())

    private fun number(value: Int): ExcelCell.Number = ExcelCell.Number(value.toString())

    private fun decimal(value: String?): ExcelCell = value?.toBigDecimalOrNull()?.let {
        ExcelCell.Number(it.toPlainString())
    } ?: ExcelCell.Text("")

    private fun money(minor: Long, currencyCode: String): ExcelCell.Number =
        ExcelCell.Number(BigDecimal.valueOf(minor, Currencies.byCode(currencyCode).minorUnits).toPlainString())

    private fun date(epochDay: Long?): ExcelCell =
        epochDay?.let { ExcelCell.Date(LocalDate.ofEpochDay(it)) } ?: ExcelCell.Text("")

    private fun timestamp(epochMillis: Long?): ExcelCell = epochMillis?.let {
        ExcelCell.Text(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDateTime().toString())
    } ?: ExcelCell.Text("")

    // --------------------------------------------------------------- encryption

    // ------------------------------------------------------------------- json

    private fun encodeBundle(bundle: RestoreBundle): JSONObject = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("createdAt", System.currentTimeMillis())
        put("workspace", encodeWorkspace(bundle.workspace))
        put("wallets", bundle.wallets.map(::encodeWallet).toJsonArray())
        put("categories", bundle.categories.map(::encodeCategory).toJsonArray())
        put("incomeSources", bundle.incomeSources.map(::encodeIncomeSource).toJsonArray())
        put("transactions", bundle.transactions.map(::encodeTransaction).toJsonArray())
        put("budgets", bundle.budgets.map(::encodeBudget).toJsonArray())
        put("budgetLines", bundle.budgetLines.map(::encodeBudgetLine).toJsonArray())
        put("goals", bundle.goals.map(::encodeGoal).toJsonArray())
        put("contributions", bundle.contributions.map(::encodeContribution).toJsonArray())
        put("recurring", bundle.recurring.map(::encodeRecurring).toJsonArray())
        put("debts", bundle.debts.map(::encodeDebt).toJsonArray())
        put("commitments", bundle.commitments.map(::encodeCommitment).toJsonArray())
        put("commitmentPayments", bundle.commitmentPayments.map(::encodeCommitmentPayment).toJsonArray())
    }

    private fun decodeBundle(bytes: ByteArray): RestoreBundle {
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            throw BackupException("Backup contents are not readable")
        }
        if (root.optString("format") != FORMAT) throw BackupException("Unsupported backup format")
        val version = root.optInt("version", 0)
        if (version > VERSION) throw BackupException("This backup was created by a newer version of Mizani")
        if (version < 1) throw BackupException("Unsupported backup version")
        val recordCount = RECORD_ARRAY_NAMES.sumOf { root.optJSONArray(it)?.length()?.toLong() ?: 0L }
        if (recordCount > MAX_BACKUP_RECORDS) throw BackupException("Backup contains too many records")
        val bundle = RestoreBundle(
            workspace = decodeWorkspace(root.getJSONObject("workspace")),
            wallets = root.getJSONArray("wallets").mapObjects(::decodeWallet),
            categories = root.getJSONArray("categories").mapObjects(::decodeCategory),
            incomeSources = root.optJSONArray("incomeSources")?.mapObjects(::decodeIncomeSource) ?: emptyList(),
            transactions = root.getJSONArray("transactions").mapObjects(::decodeTransaction),
            budgets = root.getJSONArray("budgets").mapObjects(::decodeBudget),
            budgetLines = root.getJSONArray("budgetLines").mapObjects(::decodeBudgetLine),
            goals = root.getJSONArray("goals").mapObjects(::decodeGoal),
            contributions = root.getJSONArray("contributions").mapObjects(::decodeContribution),
            recurring = root.getJSONArray("recurring").mapObjects(::decodeRecurring),
            debts = root.getJSONArray("debts").mapObjects(::decodeDebt),
            commitments = root.optJSONArray("commitments")?.mapObjects(::decodeCommitment) ?: emptyList(),
            commitmentPayments = root.optJSONArray("commitmentPayments")?.mapObjects(::decodeCommitmentPayment) ?: emptyList(),
        )
        if (!Currencies.all.any { it.code == bundle.workspace.currencyCode }) {
            throw BackupException("Backup uses an unsupported currency")
        }
        return bundle
    }

    private fun encodeWorkspace(workspace: WorkspaceEntity) = JSONObject().apply {
        put("id", workspace.id)
        put("name", workspace.name)
        put("countryCode", workspace.countryCode)
        put("currencyCode", workspace.currencyCode)
        put("createdAt", workspace.createdAt)
        put("updatedAt", workspace.updatedAt)
        put("revision", workspace.revision)
        putNullable("deletedAt", workspace.deletedAt)
        putNullable("outlookDailyTargetMinor", workspace.outlookDailyTargetMinor)
    }

    private fun decodeWorkspace(json: JSONObject) = WorkspaceEntity(
        id = json.getString("id"),
        name = json.optString("name", "My money"),
        countryCode = json.optString("countryCode", "UG"),
        currencyCode = json.getString("currencyCode"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
        outlookDailyTargetMinor = json.optNullableLong("outlookDailyTargetMinor"),
    )

    private fun encodeWallet(wallet: WalletEntity) = JSONObject().apply {
        put("id", wallet.id)
        put("name", wallet.name)
        put("type", wallet.type.name)
        put("openingBalanceMinor", wallet.openingBalanceMinor)
        put("currencyCode", wallet.currencyCode)
        put("colorHex", wallet.colorHex)
        put("icon", wallet.icon)
        put("sortOrder", wallet.sortOrder)
        put("archived", wallet.archived)
        put("createdAt", wallet.createdAt)
        put("updatedAt", wallet.updatedAt)
        put("revision", wallet.revision)
        putNullable("deletedAt", wallet.deletedAt)
    }

    private fun decodeWallet(json: JSONObject) = WalletEntity(
        id = json.getString("id"),
        name = json.getString("name"),
        type = json.enumOrDefault("type", WalletType.OTHER),
        openingBalanceMinor = json.optLong("openingBalanceMinor"),
        currencyCode = json.optString("currencyCode", "UGX"),
        colorHex = json.optString("colorHex", "#2F6B5E"),
        icon = json.optString("icon", "wallet"),
        sortOrder = json.optInt("sortOrder"),
        archived = json.optBoolean("archived"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeCategory(category: CategoryEntity) = JSONObject().apply {
        put("id", category.id)
        put("name", category.name)
        put("type", category.type.name)
        put("icon", category.icon)
        put("colorHex", category.colorHex)
        put("isDefault", category.isDefault)
        put("archived", category.archived)
        put("sortOrder", category.sortOrder)
        put("createdAt", category.createdAt)
        put("updatedAt", category.updatedAt)
        put("revision", category.revision)
        putNullable("deletedAt", category.deletedAt)
    }

    private fun decodeCategory(json: JSONObject) = CategoryEntity(
        id = json.getString("id"),
        name = json.getString("name"),
        type = json.enumOrDefault("type", CategoryType.EXPENSE),
        icon = json.optString("icon", "category"),
        colorHex = json.optString("colorHex", "#6D6D6D"),
        isDefault = json.optBoolean("isDefault"),
        archived = json.optBoolean("archived"),
        sortOrder = json.optInt("sortOrder"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeIncomeSource(source: IncomeSourceEntity) = JSONObject().apply {
        put("id", source.id)
        put("name", source.name)
        put("kind", source.kind.name)
        put("archived", source.archived)
        put("createdAt", source.createdAt)
        put("updatedAt", source.updatedAt)
        put("revision", source.revision)
        putNullable("deletedAt", source.deletedAt)
    }

    private fun decodeIncomeSource(json: JSONObject) = IncomeSourceEntity(
        id = json.getString("id"),
        name = json.getString("name"),
        kind = json.enumOrDefault("kind", IncomeSourceKind.OTHER),
        archived = json.optBoolean("archived"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeTransaction(transaction: TransactionEntity) = JSONObject().apply {
        put("id", transaction.id)
        put("type", transaction.type.name)
        put("amountMinor", transaction.amountMinor)
        put("feeMinor", transaction.feeMinor)
        put("currencyCode", transaction.currencyCode)
        put("walletId", transaction.walletId)
        putNullable("toWalletId", transaction.toWalletId)
        putNullable("categoryId", transaction.categoryId)
        putNullable("feeCategoryId", transaction.feeCategoryId)
        putNullable("incomeSourceId", transaction.incomeSourceId)
        putNullable("linkedTransactionId", transaction.linkedTransactionId)
        put("note", transaction.note)
        put("payee", transaction.payee)
        put("localDateEpochDay", transaction.localDateEpochDay)
        put("occurredAt", transaction.occurredAt)
        put("createdAt", transaction.createdAt)
        put("updatedAt", transaction.updatedAt)
        put("revision", transaction.revision)
        putNullable("deletedAt", transaction.deletedAt)
        putNullable("sourceAmountMinor", transaction.sourceAmountMinor)
        putNullable("sourceFeeMinor", transaction.sourceFeeMinor)
        putNullable("sourceCurrencyCode", transaction.sourceCurrencyCode)
        putNullable("exchangeRate", transaction.exchangeRate)
        putNullable("exchangeRateDate", transaction.exchangeRateDate)
        putNullable("sourceProvider", transaction.sourceProvider)
        putNullable("sourceReference", transaction.sourceReference)
    }

    private fun decodeTransaction(json: JSONObject) = TransactionEntity(
        id = json.getString("id"),
        type = json.enumOrDefault("type", TransactionType.EXPENSE),
        amountMinor = json.optLong("amountMinor"),
        feeMinor = json.optLong("feeMinor"),
        currencyCode = json.optString("currencyCode", "UGX"),
        walletId = json.getString("walletId"),
        toWalletId = json.optNullableString("toWalletId"),
        categoryId = json.optNullableString("categoryId"),
        feeCategoryId = json.optNullableString("feeCategoryId"),
        incomeSourceId = json.optNullableString("incomeSourceId"),
        linkedTransactionId = json.optNullableString("linkedTransactionId"),
        note = json.optString("note"),
        payee = json.optString("payee"),
        localDateEpochDay = json.optLong("localDateEpochDay"),
        occurredAt = json.optLong("occurredAt"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
        sourceAmountMinor = json.optNullableLong("sourceAmountMinor"),
        sourceFeeMinor = json.optNullableLong("sourceFeeMinor"),
        sourceCurrencyCode = json.optNullableString("sourceCurrencyCode"),
        exchangeRate = json.optNullableString("exchangeRate"),
        exchangeRateDate = json.optNullableString("exchangeRateDate"),
        sourceProvider = json.optNullableString("sourceProvider"),
        sourceReference = json.optNullableString("sourceReference"),
    )

    private fun encodeBudget(budget: BudgetEntity) = JSONObject().apply {
        put("id", budget.id)
        put("name", budget.name)
        put("periodType", budget.periodType.name)
        put("startEpochDay", budget.startEpochDay)
        put("endEpochDay", budget.endEpochDay)
        put("createdAt", budget.createdAt)
        put("updatedAt", budget.updatedAt)
        put("revision", budget.revision)
        putNullable("deletedAt", budget.deletedAt)
    }

    private fun decodeBudget(json: JSONObject) = BudgetEntity(
        id = json.getString("id"),
        name = json.optString("name", "Budget"),
        periodType = json.enumOrDefault("periodType", BudgetPeriodType.MONTHLY),
        startEpochDay = json.optLong("startEpochDay"),
        endEpochDay = json.optLong("endEpochDay"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeBudgetLine(line: BudgetLineEntity) = JSONObject().apply {
        put("id", line.id)
        put("budgetId", line.budgetId)
        put("categoryId", line.categoryId)
        put("plannedMinor", line.plannedMinor)
        put("fundedMinor", line.fundedMinor)
        put("rolloverRule", line.rolloverRule.name)
        put("essential", line.essential)
        put("priority", line.priority)
        put("note", line.note)
    }

    private fun decodeBudgetLine(json: JSONObject) = BudgetLineEntity(
        id = json.getString("id"),
        budgetId = json.getString("budgetId"),
        categoryId = json.getString("categoryId"),
        plannedMinor = json.optLong("plannedMinor"),
        fundedMinor = json.optLong("fundedMinor"),
        rolloverRule = json.enumOrDefault("rolloverRule", RolloverRule.NONE),
        essential = json.optBoolean("essential"),
        priority = json.optInt("priority", 2),
        note = json.optString("note"),
    )

    private fun encodeGoal(goal: GoalEntity) = JSONObject().apply {
        put("id", goal.id)
        put("name", goal.name)
        put("targetMinor", goal.targetMinor)
        put("allocatedMinor", goal.allocatedMinor)
        putNullable("targetEpochDay", goal.targetEpochDay)
        putNullable("linkedWalletId", goal.linkedWalletId)
        put("priority", goal.priority)
        put("note", goal.note)
        put("icon", goal.icon)
        put("archived", goal.archived)
        put("createdAt", goal.createdAt)
        put("updatedAt", goal.updatedAt)
        put("revision", goal.revision)
        putNullable("deletedAt", goal.deletedAt)
    }

    private fun decodeGoal(json: JSONObject) = GoalEntity(
        id = json.getString("id"),
        name = json.getString("name"),
        targetMinor = json.optLong("targetMinor"),
        allocatedMinor = json.optLong("allocatedMinor"),
        targetEpochDay = json.optNullableLong("targetEpochDay"),
        linkedWalletId = json.optNullableString("linkedWalletId"),
        priority = json.optInt("priority", 2),
        note = json.optString("note"),
        icon = json.optString("icon", "savings"),
        archived = json.optBoolean("archived"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeContribution(contribution: GoalContributionEntity) = JSONObject().apply {
        put("id", contribution.id)
        put("goalId", contribution.goalId)
        put("amountMinor", contribution.amountMinor)
        put("localDateEpochDay", contribution.localDateEpochDay)
        putNullable("walletId", contribution.walletId)
        putNullable("transferTransactionId", contribution.transferTransactionId)
        put("note", contribution.note)
        put("createdAt", contribution.createdAt)
    }

    private fun decodeContribution(json: JSONObject) = GoalContributionEntity(
        id = json.getString("id"),
        goalId = json.getString("goalId"),
        amountMinor = json.optLong("amountMinor"),
        localDateEpochDay = json.optLong("localDateEpochDay"),
        walletId = json.optNullableString("walletId"),
        transferTransactionId = json.optNullableString("transferTransactionId"),
        note = json.optString("note"),
        createdAt = json.optLong("createdAt"),
    )

    private fun encodeRecurring(rule: RecurringRuleEntity) = JSONObject().apply {
        put("id", rule.id)
        put("name", rule.name)
        put("type", rule.type.name)
        put("amountMinor", rule.amountMinor)
        putNullable("categoryId", rule.categoryId)
        putNullable("walletId", rule.walletId)
        put("frequency", rule.frequency.name)
        put("nextDueEpochDay", rule.nextDueEpochDay)
        put("autoPost", rule.autoPost)
        put("essential", rule.essential)
        put("note", rule.note)
        put("archived", rule.archived)
        put("createdAt", rule.createdAt)
        put("updatedAt", rule.updatedAt)
        put("revision", rule.revision)
        putNullable("deletedAt", rule.deletedAt)
    }

    private fun decodeRecurring(json: JSONObject) = RecurringRuleEntity(
        id = json.getString("id"),
        name = json.getString("name"),
        type = json.enumOrDefault("type", RecurringType.BILL),
        amountMinor = json.optLong("amountMinor"),
        categoryId = json.optNullableString("categoryId"),
        walletId = json.optNullableString("walletId"),
        frequency = json.enumOrDefault("frequency", Frequency.MONTHLY),
        nextDueEpochDay = json.optLong("nextDueEpochDay"),
        autoPost = json.optBoolean("autoPost"),
        essential = json.optBoolean("essential", true),
        note = json.optString("note"),
        archived = json.optBoolean("archived"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeDebt(debt: DebtEntity) = JSONObject().apply {
        put("id", debt.id)
        put("name", debt.name)
        put("lender", debt.lender)
        put("currentBalanceMinor", debt.currentBalanceMinor)
        putNullable("originalMinor", debt.originalMinor)
        put("periodicPaymentMinor", debt.periodicPaymentMinor)
        putNullable("nextDueEpochDay", debt.nextDueEpochDay)
        putNullable("interestRatePercent", debt.interestRatePercent)
        put("note", debt.note)
        put("archived", debt.archived)
        put("createdAt", debt.createdAt)
        put("updatedAt", debt.updatedAt)
        put("revision", debt.revision)
        putNullable("deletedAt", debt.deletedAt)
    }

    private fun decodeDebt(json: JSONObject) = DebtEntity(
        id = json.getString("id"),
        name = json.getString("name"),
        lender = json.optString("lender"),
        currentBalanceMinor = json.optLong("currentBalanceMinor"),
        originalMinor = json.optNullableLong("originalMinor"),
        periodicPaymentMinor = json.optLong("periodicPaymentMinor"),
        nextDueEpochDay = json.optNullableLong("nextDueEpochDay"),
        interestRatePercent = if (json.isNull("interestRatePercent")) null else json.optDouble("interestRatePercent"),
        note = json.optString("note"),
        archived = json.optBoolean("archived"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeCommitment(commitment: CommitmentEntity) = JSONObject().apply {
        put("id", commitment.id)
        put("name", commitment.name)
        put("targetMinor", commitment.targetMinor)
        put("dueEpochDay", commitment.dueEpochDay)
        putNullable("categoryId", commitment.categoryId)
        put("reminderDaysBefore", commitment.reminderDaysBefore)
        put("archived", commitment.archived)
        put("createdAt", commitment.createdAt)
        put("updatedAt", commitment.updatedAt)
        put("revision", commitment.revision)
        putNullable("deletedAt", commitment.deletedAt)
    }

    private fun decodeCommitment(json: JSONObject) = CommitmentEntity(
        id = json.getString("id"),
        name = json.getString("name"),
        targetMinor = json.optLong("targetMinor"),
        dueEpochDay = json.optLong("dueEpochDay"),
        categoryId = json.optNullableString("categoryId"),
        reminderDaysBefore = json.optInt("reminderDaysBefore"),
        archived = json.optBoolean("archived"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
        deletedAt = json.optNullableLong("deletedAt"),
    )

    private fun encodeCommitmentPayment(payment: CommitmentPaymentEntity) = JSONObject().apply {
        put("id", payment.id)
        put("commitmentId", payment.commitmentId)
        put("transactionId", payment.transactionId)
        put("createdAt", payment.createdAt)
        put("updatedAt", payment.updatedAt)
        put("revision", payment.revision)
    }

    private fun decodeCommitmentPayment(json: JSONObject) = CommitmentPaymentEntity(
        id = json.getString("id"),
        commitmentId = json.getString("commitmentId"),
        transactionId = json.getString("transactionId"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        revision = json.optLong("revision", 1),
    )

    private fun amountText(minor: Long, currencyCode: String): String {
        val currency = Currencies.byCode(currencyCode)
        return MoneyFormat.toInput(minor, currency).ifBlank { "0" }
    }

    private fun csvEscape(value: String): String = com.mizani.money.domain.export.csvEscape(value)

    private fun List<JSONObject>.toJsonArray(): JSONArray = JSONArray().also { array ->

        forEach { array.put(it) }
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    companion object {
        private const val FORMAT = "mizani-backup"
        private const val VERSION = 2
        private const val MAX_BACKUP_RECORDS = 100_000L
        private val RECORD_ARRAY_NAMES = listOf(
            "wallets", "categories", "incomeSources", "transactions", "budgets", "budgetLines",
            "goals", "contributions", "recurring", "debts", "commitments", "commitmentPayments",
        )
    }
}

private inline fun <reified T : Enum<T>> JSONObject.enumOrDefault(name: String, default: T): T {
    val encoded = optString(name, default.name)
    return enumValues<T>().firstOrNull { it.name == encoded }
        ?: throw BackupException("Backup contains an unsupported value for $name")
}

/**
 * CSV field escaper shared by [ExportManager] for free-text cells. Neutralizes spreadsheet
 * formula injection by prefixing a single quote to a cell whose first non-whitespace
 * character is `=`, `+`, `-`, or `@`, or that begins with a tab/CR/LF (a leading whitespace
 * character defeats a naive `first()`-only check and can strip away to expose a formula).
 * Then applies standard RFC-4180 quoting (double-quote wrapping with internal quotes doubled)
 * for delimiters, quotes, and newlines. Declared at package level as `internal` so it can be
 * unit-tested directly from the same package without an [ExportManager] instance or reflection.
 */
internal fun csvEscape(value: String): String {
    val triggerChars = charArrayOf('=', '+', '-', '@')
    val whitespaceChars = charArrayOf(' ', '\t', '\r', '\n')
    val leading = value.trimStart { it in whitespaceChars }
    val startsWithWhitespace = value.isNotEmpty() && value[0] in charArrayOf('\t', '\r', '\n')
    val neutralized = if (startsWithWhitespace || (leading.isNotEmpty() && leading[0] in triggerChars)) {
        "'$value"
    } else {
        value
    }
    return if (neutralized.contains(',') ||
        neutralized.contains('"') ||
        neutralized.contains('\n') ||
        neutralized.contains('\r')
    ) {
        "\"${neutralized.replace("\"", "\"\"")}\""
    } else {
        neutralized
    }
}

/**
 * CSV escaper for numeric cells (amount/fee). Unlike [csvEscape], it never applies formula
 * neutralization, so a negative value such as `-500` is emitted verbatim as a number rather
 * than being turned into a text cell with a leading apostrophe. RFC-4180 quoting
 * (double-quote wrapping with internal quotes doubled) is still applied when the value
 * contains a delimiter, quote, or newline.
 */
internal fun csvEscapeNumeric(value: String): String {
    return if (value.contains(',') ||
        value.contains('"') ||
        value.contains('\n') ||
        value.contains('\r')
    ) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }
}
