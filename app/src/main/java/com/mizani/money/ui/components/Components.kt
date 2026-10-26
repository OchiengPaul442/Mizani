package com.mizani.money.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.mizani.money.R
import com.mizani.money.core.model.InsightTone
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.Currencies
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.data.local.TransactionEntity
import com.mizani.money.data.repo.LedgerSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import android.provider.Settings
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

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
    val fieldHeight = 56.dp
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
    iconRes: Int? = null,
    illustrationAsset: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    // Layered translucent brand circles behind a themed icon tile, with a small floating
    // accent sparkle. Compact variant drops the circles and the CTA gap for use inside cards.
    val ringSize = when {
        illustrationAsset != null -> 176.dp
        compact -> 64.dp
        else -> 96.dp
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = if (compact) Spacing.lg else Spacing.xxl, horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
            if (!compact) {
                Box(
                    Modifier.size(ringSize).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                )
                Box(
                    Modifier.size((ringSize.value * 0.6f).dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                )
            }
            if (illustrationAsset != null) {
                StorysetIllustration(
                    assetName = illustrationAsset,
                    modifier = Modifier.fillMaxSize().padding(Spacing.xs),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                    } else {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            if (!compact && illustrationAsset == null) {
                Icon(
                    painter = painterResource(R.drawable.ic_ph_sparkle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp).align(Alignment.TopEnd).padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(if (compact) Spacing.md else Spacing.lg))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * A brief, dependency-free success celebration: an animated check drawn on a brand circle with a
 * handful of confetti chips. Runs for ~650ms then calls [onFinished]. The whole visual is skipped
 * (only the polite "Saved" announcement fires) when the system animator duration scale is 0, so
 * users who disable animations aren't delayed. Trigger haptic feedback once at the start.
 */
@Composable
fun MizaniCelebration(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val animatorScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    val showVisual = animatorScale != 0f
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (showVisual) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(650)
        }
        onFinished()
    }

    // The "Saved" live-region node is always composed (never gated behind an early return), so
    // the polite announcement fires for screen readers even when the visual is skipped.
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 550, easing = EaseOutCubic),
        label = "celebrate",
    )
    val circleColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (showVisual) {
                    Modifier
                        .background(bgColor)
                        // Consume every pointer event so taps during the celebration cannot
                        // reach the form beneath (e.g. a double-tapped Save button).
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }
                } else {
                    Modifier
                },
            )
            // Polite live region announces the save without interrupting reading.
            .semantics(mergeDescendants = true) {
                contentDescription = "Saved"
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        if (showVisual) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val radius = size.minDimension / 2f
                val cx = center.x
                val cy = center.y
                val sweep = (progress.coerceIn(0f, 1f)) * 360f
                drawArc(
                    color = circleColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = size.minDimension * 0.12f, cap = StrokeCap.Round),
                )
                // Confetti chips: deterministic offsets so layout is stable across devices.
                val confetti = listOf(
                    Offset(-1.4f, -0.6f) to Color(0xFF0A9C8B),
                    Offset(1.3f, -0.9f) to Color(0xFF2ECC71),
                    Offset(-1.1f, 1.2f) to Color(0xFF3F6F76),
                    Offset(1.5f, 1.0f) to Color(0xFF76D7C2),
                    Offset(0.2f, -1.5f) to Color(0xFF087F72),
                )
                val pop = progress.coerceIn(0f, 1f)
                confetti.forEach { (frac, color) ->
                    val dist = radius * (1.2f + 0.6f * pop)
                    val px = cx + frac.x * dist
                    val py = cy + frac.y * dist
                    val chip = radius * 0.10f
                    val point = Offset(px.toFloat(), py.toFloat())
                    drawCircle(color = color.copy(alpha = (1f - pop * 0.3f).coerceIn(0f, 1f)), radius = chip, center = point)
                }
                // Check stroke reveals as the arc completes.
                if (progress > 0.45f) {
                    val reveal = ((progress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    val pw = radius * 0.45f
                    val strokeW = radius * 0.12f
                    val seg1 = Path().apply {
                        moveTo(cx - pw * 0.5f, cy)
                        lineTo(cx - pw * 0.12f, cy + pw * 0.4f)
                    }
                    val seg2 = Path().apply {
                        moveTo(cx - pw * 0.12f, cy + pw * 0.4f)
                        lineTo(cx + pw * 0.55f, cy - pw * 0.38f)
                    }
                    drawPath(seg1, color = circleColor, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    if (reveal > 0.4f) {
                        drawPath(seg2, color = circleColor, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
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
    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier.height(Spacing.fieldHeight),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "$label · ${DateText.medium(date)}",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp),
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                securePolicy = SecureFlagPolicy.SecureOn,
            ),
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
    tone: InsightTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
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
        if (transaction.sourceAmountMinor != null && transaction.sourceCurrencyCode != null) {
            val sourceCurrency = Currencies.byCode(transaction.sourceCurrencyCode)
            append(", entered as ")
            append(MoneyFormat.format(transaction.sourceAmountMinor, sourceCurrency))
            transaction.exchangeRateDate?.let { append(", rate dated $it") }
        }
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
            if (transaction.sourceAmountMinor != null && transaction.sourceCurrencyCode != null) {
                Text(
                    MoneyFormat.format(
                        transaction.sourceAmountMinor,
                        Currencies.byCode(transaction.sourceCurrencyCode),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
