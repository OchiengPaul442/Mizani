package com.mizani.money.data.repo

import com.mizani.money.core.model.InvestmentAssetType
import com.mizani.money.data.local.InvestmentHoldingEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class InvestmentSummaryTest {

    @Test
    fun summaryExcludesArchivedAndDeletedHoldings() {
        val snapshot = LedgerSnapshot(
            investments = listOf(
                holding("active", 1_000, 1_250),
                holding("archived", 9_000, 12_000, archived = true),
                holding("deleted", 20_000, 30_000, deletedAt = 1L),
            ),
        )

        assertEquals(1, snapshot.investmentSummary().holdingCount)
        assertEquals(1_000L, snapshot.investmentSummary().costBasisMinor)
        assertEquals(1_250L, snapshot.investmentSummary().currentValueMinor)
        assertEquals(250L, snapshot.investmentSummary().gainLossMinor)
    }

    @Test
    fun summaryAllowsZeroCurrentValueAndReportsLoss() {
        val summary = LedgerSnapshot(
            investments = listOf(holding("loss", 4_000, 0)),
        ).investmentSummary()

        assertEquals(-4_000L, summary.gainLossMinor)
    }

    @Test
    fun summaryHandlesTenThousandHoldingsWithoutChangingTotals() {
        val summary = LedgerSnapshot(
            investments = List(10_000) { index ->
                holding("holding-$index", costBasis = 100, currentValue = 125)
            },
        ).investmentSummary()

        assertEquals(10_000, summary.holdingCount)
        assertEquals(1_000_000L, summary.costBasisMinor)
        assertEquals(1_250_000L, summary.currentValueMinor)
        assertEquals(250_000L, summary.gainLossMinor)
    }

    @Test(expected = ArithmeticException::class)
    fun summaryRejectsAggregateOverflow() {
        LedgerSnapshot(
            investments = listOf(
                holding("a", costBasis = Long.MAX_VALUE, currentValue = 0),
                holding("b", costBasis = 1, currentValue = 0),
            ),
        ).investmentSummary()
    }

    private fun holding(
        id: String,
        costBasis: Long,
        currentValue: Long,
        archived: Boolean = false,
        deletedAt: Long? = null,
    ) = InvestmentHoldingEntity(
        id = id,
        name = id,
        provider = "Provider",
        assetType = InvestmentAssetType.FUNDS,
        costBasisMinor = costBasis,
        currentValueMinor = currentValue,
        currencyCode = "UGX",
        valuationDateEpochDay = 20_000,
        archived = archived,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
    )
}
