package com.mizani.money.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.model.InsightTone
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.repo.LedgerSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    // Semantic tokens: one 16dp screen/card rhythm throughout.
    val screen = 16.dp
    val card = 16.dp
    val section = 16.dp
    val bottomFab = 96.dp
    val bottom = 32.dp
    val buttonHeight = 52.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MizaniTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            }
        },
        actions = actions,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
fun MizaniCard(
    modifier: Modifier = Modifier,
    // Default card surface is the theme surface: white in light, brand dark surface
    // (#12323A) in dark, so cards read as raised on the dark page (#0B2028).
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.card),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    val shape = MaterialTheme.shapes.medium
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Card(modifier = modifier.then(clickModifier), colors = colors, shape = shape) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Int = 6,
) {
    val safe = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(trackColor)
            // Exposed as a progress value so screen readers can state it; the nearby
            // text labels remain the primary human-readable signal.
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(safe, 0f..1f) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safe)
                .height(height.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun CategoryAvatar(
    icon: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val color = parseHexColor(colorHex)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MizaniIcons.category(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

@Composable
fun WalletAvatar(
    type: com.mizani.money.core.model.WalletType,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val color = parseHexColor(colorHex)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MizaniIcons.wallet(type),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((size * 0.48f).dp),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xxl, horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text("$label · ${DateText.medium(date)}")
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showPicker = false
                    },
                ) { Text("Choose") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    body: String,
    explanation: String,
    tone: InsightTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    var showExplanation by remember { mutableStateOf(false) }
    val container = when (tone) {
        InsightTone.ATTENTION -> MaterialTheme.colorScheme.secondaryContainer
        InsightTone.POSITIVE -> MaterialTheme.colorScheme.primaryContainer
        InsightTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val onContainer = when (tone) {
        InsightTone.ATTENTION -> MaterialTheme.colorScheme.onSecondaryContainer
        InsightTone.POSITIVE -> MaterialTheme.colorScheme.onPrimaryContainer
        InsightTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        contentColor = onContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Spacing.card)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = onContainer)
            Spacer(Modifier.height(Spacing.xs))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = onContainer.copy(alpha = 0.88f))
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button) { showExplanation = !showExplanation }
                    // 48dp minimum touch target (was ~26dp), so the expander is
                    // actually tappable without shrinking anything else.
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.how_calculated_show),
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer,
                )
                Icon(
                    if (showExplanation) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    // State is spelled out for screen readers, same pattern as MetricCard.
                    contentDescription = stringResource(
                        if (showExplanation) R.string.how_calculated_hide else R.string.how_calculated_show,
                    ),
                    modifier = Modifier.size(18.dp),
                    tint = onContainer,
                )
            }
            AnimatedVisibility(visible = showExplanation) {
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Spacing.sm))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    snapshot: LedgerSnapshot,
    currency: Currency,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val category = snapshot.category(transaction.categoryId)
    val title = when {
        transaction.type == TransactionType.TRANSFER -> "Transfer to ${snapshot.walletName(transaction.toWalletId)}"
        transaction.payee.isNotBlank() -> transaction.payee
        transaction.note.isNotBlank() -> transaction.note
        category != null -> category.name
        else -> transaction.type.label
    }
    val subtitle = when (transaction.type) {
        TransactionType.TRANSFER ->
            "${snapshot.walletName(transaction.walletId)} → ${snapshot.walletName(transaction.toWalletId)}"
        else -> {
            val date = LocalDate.ofEpochDay(transaction.localDateEpochDay)
            "${snapshot.walletName(transaction.walletId)} · ${DateText.short(date)}"
        }
    }
    val amountColor = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REFUND -> MaterialTheme.colorScheme.primary
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
        TransactionType.ADJUSTMENT -> MaterialTheme.colorScheme.onSurfaceVariant
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
    }
    val amountPrefix = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REFUND -> "+"
        TransactionType.EXPENSE, TransactionType.TRANSFER -> if (transaction.amountMinor >= 0) "−" else "+"
        TransactionType.ADJUSTMENT -> if (transaction.amountMinor >= 0) "+" else "−"
    }
    val semanticsText = buildString {
        append(transaction.type.label)
        append(", ")
        append(title)
        append(", ")
        append(amountPrefix)
        append(MoneyFormat.format(kotlin.math.abs(transaction.amountMinor), currency))
        append(", ")
        append(subtitle)
        if (transaction.feeMinor > 0) {
            append(", fee ")
            append(MoneyFormat.format(transaction.feeMinor, currency))
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp)
            .semantics { contentDescription = semanticsText },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = when (transaction.type) {
            TransactionType.TRANSFER -> MizaniIcons.wallet(
                snapshot.wallet(transaction.walletId)?.type ?: com.mizani.money.core.model.WalletType.OTHER,
            )
            else -> MizaniIcons.category(category?.icon ?: "other")
        }
        val color = parseHexColor(category?.colorHex ?: "#3D6E8C")
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(color.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (transaction.type == TransactionType.TRANSFER) {
                    Icon(
                        Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$amountPrefix${MoneyFormat.format(kotlin.math.abs(transaction.amountMinor), currency)}",
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                maxLines = 1,
            )
            if (transaction.feeMinor > 0) {
                Text(
                    "fee ${MoneyFormat.format(transaction.feeMinor, currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SmallPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        modifier = modifier,
        color = container,
        contentColor = contentColor,
        shape = CircleShape,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium,
        )
    }
}
