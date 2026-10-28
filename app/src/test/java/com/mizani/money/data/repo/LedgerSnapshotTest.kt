package com.mizani.money.data.repo

import com.mizani.money.core.model.TransactionType
import com.mizani.money.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerSnapshotTest {

    @Test
    fun netExpenseValuesExcludeRefundsForCommitmentChoices() {
        val snapshot = LedgerSnapshot(
            transactions = listOf(
                transaction("expense", TransactionType.EXPENSE, 1_000),
                transaction("partial", TransactionType.EXPENSE, 1_000),
                transaction("refund", TransactionType.REFUND, 400, linkedTransactionId = "partial"),
                transaction("full", TransactionType.EXPENSE, 500),
                transaction("full-refund", TransactionType.REFUND, 500, linkedTransactionId = "full"),
            ),
        )

        assertEquals(
            mapOf("expense" to 1_000L, "partial" to 600L, "full" to 0L),
            snapshot.netExpenseMinorById(),
        )
    }

    private fun transaction(
        id: String,
        type: TransactionType,
        amountMinor: Long,
        linkedTransactionId: String? = null,
    ) = TransactionEntity(
        id = id,
        type = type,
        amountMinor = amountMinor,
        currencyCode = "UGX",
        walletId = "wallet",
        linkedTransactionId = linkedTransactionId,
        localDateEpochDay = 0L,
        occurredAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
