package com.mizani.money.domain.export

import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.BudgetEntity
import com.mizani.money.data.repo.LedgerSnapshot
import java.time.LocalDate

/** Builds a user-reviewed, plan-only message for the Android sharesheet. */
object BudgetShareText {
    fun build(budget: BudgetEntity, snapshot: LedgerSnapshot, currency: Currency): String {
        val lines = snapshot.budgetLinesFor(budget.id)
            .sortedWith(compareBy({ it.priority }, { snapshot.categoryName(it.categoryId) }))
        val plannedTotal = lines.sumOf { it.plannedMinor }
        val start = LocalDate.ofEpochDay(budget.startEpochDay)
        val end = LocalDate.ofEpochDay(budget.endEpochDay)
        val budgetName = singleLine(budget.name).ifBlank { "Budget" }

        return buildString {
            appendLine("Mizani budget: $budgetName")
            appendLine("Period: ${DateText.medium(start)} to ${DateText.medium(end)}")
            appendLine("Planned total: ${MoneyFormat.format(plannedTotal, currency)}")
            appendLine("Category limits:")
            if (lines.isEmpty()) {
                appendLine("- No category limits")
            } else {
                lines.forEach { line ->
                    val categoryName = singleLine(snapshot.categoryName(line.categoryId))
                        .ifBlank { "Uncategorised" }
                    appendLine("- $categoryName: ${MoneyFormat.format(line.plannedMinor, currency)}")
                }
            }
        }.trimEnd()
    }

    private fun singleLine(value: String): String = value.replace(Regex("\\s+"), " ").trim()
}
