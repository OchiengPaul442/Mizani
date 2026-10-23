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
import com.mizani.money.core.security.PinHasher
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.local.BudgetLineEntity
import com.mizani.money.data.local.CategoryEntity
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
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

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
 * User-controlled portability: a human-readable CSV and a versioned, passphrase-encrypted
 * JSON backup. Restore is all-or-nothing (validated first, then applied in one Room
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
            "date,type,amount,currency,fee,wallet,to_wallet,category,income_source,payee,note,linked_transaction\n",
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
                )
                builder.append(escaped.joinToString(",")).append('\n')
            }
        return builder.toString()
    }

    suspend fun buildBackup(passphrase: String): ByteArray {
        require(passphrase.length >= 8) { "Use at least 8 characters for the backup passphrase" }
        val bundle = repository.exportBundle()
        val json = encodeBundle(bundle).toString()
        return encrypt(json.toByteArray(Charsets.UTF_8), passphrase)
    }

    suspend fun inspectBackup(bytes: ByteArray, passphrase: String): BackupSummary {
        val bundle = decodeBundle(decrypt(bytes, passphrase))
        return summarize(bundle)
    }

    suspend fun restore(bytes: ByteArray, passphrase: String): BackupSummary {
        val bundle = decodeBundle(decrypt(bytes, passphrase))
        repository.restoreBundle(bundle)
        return summarize(bundle)
    }

    fun suggestedCsvName(today: LocalDate = LocalDate.now()): String =
        "mizani-transactions-${today}.csv"

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

    // --------------------------------------------------------------- encryption

    private fun encrypt(plain: ByteArray, passphrase: String): ByteArray {
        val salt = PinHasher.newSalt(16)
        val key = PinHasher.hash(passphrase, salt, iterations = 100_000)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return MAGIC + salt + iv + encrypted
    }

    private fun decrypt(bytes: ByteArray, passphrase: String): ByteArray {
        if (bytes.size > MAX_BACKUP_BYTES) throw BackupException("Backup is too large")
        if (bytes.size < MAGIC.size + 16 + 12 + 16) throw BackupException("This is not a valid Mizani backup")
        if (!bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupException("Unsupported backup format")
        }
        return try {
            val salt = bytes.copyOfRange(MAGIC.size, MAGIC.size + 16)
            val iv = bytes.copyOfRange(MAGIC.size + 16, MAGIC.size + 28)
            val encrypted = bytes.copyOfRange(MAGIC.size + 28, bytes.size)
            val key = PinHasher.hash(passphrase, salt, iterations = 100_000)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted)
        } catch (error: Exception) {
            throw BackupException("Incorrect passphrase or damaged backup", )
        }
    }

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
        type = WalletType.entries.firstOrNull { it.name == json.optString("type") } ?: WalletType.OTHER,
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
        type = CategoryType.entries.firstOrNull { it.name == json.optString("type") } ?: CategoryType.EXPENSE,
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
        kind = IncomeSourceKind.entries.firstOrNull { it.name == json.optString("kind") } ?: IncomeSourceKind.OTHER,
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
    }

    private fun decodeTransaction(json: JSONObject) = TransactionEntity(
        id = json.getString("id"),
        type = TransactionType.entries.firstOrNull { it.name == json.optString("type") } ?: TransactionType.EXPENSE,
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
        periodType = BudgetPeriodType.entries.firstOrNull { it.name == json.optString("periodType") } ?: BudgetPeriodType.MONTHLY,
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
        rolloverRule = RolloverRule.entries.firstOrNull { it.name == json.optString("rolloverRule") } ?: RolloverRule.NONE,
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
        type = RecurringType.entries.firstOrNull { it.name == json.optString("type") } ?: RecurringType.BILL,
        amountMinor = json.optLong("amountMinor"),
        categoryId = json.optNullableString("categoryId"),
        walletId = json.optNullableString("walletId"),
        frequency = Frequency.entries.firstOrNull { it.name == json.optString("frequency") } ?: Frequency.MONTHLY,
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
        private const val VERSION = 1
        private const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
        private val MAGIC = "MZNB1".toByteArray(Charsets.US_ASCII)
    }
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
