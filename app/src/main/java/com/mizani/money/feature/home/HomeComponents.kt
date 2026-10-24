package com.mizani.money.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.domain.analytics.UpcomingCommitment
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.parseHexColor
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun UpcomingList(
    upcoming: List<UpcomingCommitment>,
    currency: Currency,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        upcoming.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = parseHexColor(item.colorHex)
                // Day + month abbreviations read as one meaningful date, not two loose numbers.
                val dueLabel = stringResource(R.string.home_upcoming_tile, DateText.medium(item.dueDate))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(color.copy(alpha = 0.14f))
                        .semantics { contentDescription = dueLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            item.dueDate.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = color,
                        )
                        Text(
                            item.dueDate.month.name.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = color.copy(alpha = 0.8f),
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${if (item.dueDate.isBefore(today)) "Overdue · " else ""}${DateText.relative(item.dueDate, today)} · ${item.categoryName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    (if (item.isIncome) "+" else "") + MoneyFormat.format(item.amountMinor, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
        val next = upcoming.firstOrNull()
        if (next != null) {
            val days = ChronoUnit.DAYS.between(today, next.dueDate)
            val total = upcoming.filter { !it.isIncome }.sumOf { it.amountMinor }
            Text(
                (if (days < 0) "Overdue: ${next.name} by ${-days} ${if (days == -1L) "day" else "days"}. "
                else "Next: ${next.name} in $days ${if (days == 1L) "day" else "days"}. ") +
                    "Commitments listed here total ${MoneyFormat.format(total, currency)}.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
