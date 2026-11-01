package com.mizani.money.domain.engagement

import com.mizani.money.core.model.TransactionType
import com.mizani.money.data.local.TransactionEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyMomentumTest {
    private val today = LocalDate.of(2026, 11, 1)

    @Test
    fun `one tracked day earns points and starts a recognition tier`() {
        val momentum = DailyMomentumCalculator.calculate(
            listOf(transaction(today)),
            today,
        )

        assertEquals(10, momentum.points)
        assertEquals(1, momentum.trackedDays)
        assertEquals(1, momentum.currentStreak)
        assertEquals(1, momentum.bestStreak)
        assertTrue(momentum.todayTracked)
        assertEquals(MomentumTier.STARTING_OUT, momentum.tier)
        assertEquals(20, momentum.pointsToNextTier)
        assertEquals(2, momentum.trackedDaysToNextTier)
    }

    @Test
    fun `streaks count consecutive tracked dates but points count unique days`() {
        val transactions = listOf(
            transaction(today.minusDays(3)),
            transaction(today.minusDays(2)),
            transaction(today.minusDays(1)),
            transaction(today),
            transaction(today),
            transaction(today.minusDays(5)),
        )

        val momentum = DailyMomentumCalculator.calculate(transactions, today)

        assertEquals(50, momentum.points)
        assertEquals(5, momentum.trackedDays)
        assertEquals(4, momentum.currentStreak)
        assertEquals(4, momentum.bestStreak)
        assertEquals(MomentumTier.FIRST_STEPS, momentum.tier)
        assertEquals(2, momentum.trackedDaysToNextTier)
    }

    @Test
    fun `deleted adjustments and future entries do not create progress`() {
        val deleted = transaction(today).copy(deletedAt = 123L)
        val adjustment = transaction(today.minusDays(1)).copy(type = TransactionType.ADJUSTMENT)
        val future = transaction(today.plusDays(1))

        val momentum = DailyMomentumCalculator.calculate(
            listOf(deleted, adjustment, future),
            today,
        )

        assertEquals(0, momentum.points)
        assertEquals(0, momentum.trackedDays)
        assertEquals(0, momentum.currentStreak)
        assertEquals(0, momentum.bestStreak)
        assertFalse(momentum.todayTracked)
        assertEquals(MomentumTier.STARTING_OUT, momentum.tier)
    }

    @Test
    fun `missing today resets current streak without erasing best streak`() {
        val transactions = listOf(
            transaction(today.minusDays(4)),
            transaction(today.minusDays(3)),
            transaction(today.minusDays(1)),
        )

        val momentum = DailyMomentumCalculator.calculate(transactions, today)

        assertEquals(0, momentum.currentStreak)
        assertEquals(2, momentum.bestStreak)
        assertFalse(momentum.todayTracked)
    }

    @Test
    fun `all user activity types count once per calendar day`() {
        val transactions = listOf(
            transaction(today).copy(type = TransactionType.INCOME),
            transaction(today).copy(type = TransactionType.TRANSFER),
            transaction(today).copy(type = TransactionType.REFUND),
            transaction(today).copy(type = TransactionType.ADJUSTMENT),
        )

        val momentum = DailyMomentumCalculator.calculate(transactions, today)

        assertEquals(1, momentum.trackedDays)
        assertEquals(10, momentum.points)
    }

    private fun transaction(date: LocalDate): TransactionEntity = TransactionEntity(
        id = "id-$date",
        type = TransactionType.EXPENSE,
        amountMinor = 1_000L,
        currencyCode = "UGX",
        walletId = "wallet",
        localDateEpochDay = date.toEpochDay(),
        occurredAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
