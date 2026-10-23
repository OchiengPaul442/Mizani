package com.mizani.money.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.mizani.money.R
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.domain.analytics.CategorySlice
import kotlin.math.roundToInt

/**
 * Donut chart backed by MPAndroidChart's [PieChart].
 *
 * The chart itself is transparent and unlabelled: all text (center total, legend,
 * accessibility) is rendered with Compose so it follows the theme, font scaling and
 * localization. Slice colors come from the category data (brand palette).
 *
 * The caller controls sizing/responsiveness via [modifier] (typically
 * `Modifier.weight(...).aspectRatio(1f)` or a fixed height).
 */
@Composable
fun MizaniDonutChart(
    slices: List<CategorySlice>,
    totalMinor: Long,
    currency: Currency,
    modifier: Modifier = Modifier,
    centerLabel: String? = null,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val positive = slices.filter { it.amountMinor > 0 }
    val sum = positive.sumOf { it.amountMinor }
    val hasSlices = positive.isNotEmpty() && sum > 0

    val noSpendingText = stringResource(R.string.chart_no_spending)
    val sliceTemplate = stringResource(R.string.donut_slice_summary)
    val summaryPrefix = stringResource(R.string.donut_summary_prefix)
    val description = if (!hasSlices) {
        noSpendingText
    } else {
        // Text equivalent of the chart: name, amount and share for every slice.
        summaryPrefix + " " + positive.joinToString(", ") {
            sliceTemplate.format(
                it.name,
                MoneyFormat.format(it.amountMinor, currency),
                (it.share * 100).roundToInt(),
            )
        }
    }

    // Stable key of the current data so the entrance animation runs once per data
    // change instead of on every recomposition (theme change, parent refresh, ...).
    val animationGuard = remember { DonutAnimationGuard() }
    val dataKey = positive.joinToString("|") { "${it.categoryId}:${it.amountMinor}:${it.colorHex}" }

    Box(
        modifier = modifier.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (!hasSlices) {
            // Neutral placeholder ring: no blank PieChart instance for empty periods.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 1.5.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
            }
            Text(
                noSpendingText,
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.72f).padding(horizontal = 4.dp),
            )
        } else {
            AndroidView(
                factory = { context ->
                    PieChart(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        // `this.` is required: an outer local `val description: String`
                        // (the a11y summary) shadows Chart's `description` member.
                        this.description.text = ""
                        legend.setEnabled(false)
                        setDrawCenterText(false)
                        setDrawEntryLabels(false)
                        setDrawHoleEnabled(true)
                        setHoleRadius(70f)
                        setHoleColor(android.graphics.Color.TRANSPARENT)
                        setTransparentCircleRadius(74f)
                        setTransparentCircleColor(android.graphics.Color.TRANSPARENT)
                        setTransparentCircleAlpha(0)
                        // Rounded slice ends require the hole and no under-hole drawing.
                        setDrawRoundedSlices(true)
                        setTouchEnabled(false)
                        setRotationEnabled(false)
                        setHighlightPerTapEnabled(false)
                        setExtraOffsets(0f, 0f, 0f, 0f)
                        setMinOffset(0f)
                        setNoDataText("")
                        minimumHeight = 0
                    }
                },
                update = { chart ->
                    // Re-read theme colors on every update so theme changes propagate.
                    chart.setNoDataTextColor(onSurfaceVariant.toArgb())
                    // Rebuild data only when slices/colors change; re-setting data on
                    // every recomposition would cancel the running entrance animation.
                    if (animationGuard.lastKey != dataKey) {
                        val entries = positive.map {
                            // Explicit String label: `null` alone is ambiguous between
                            // PieEntry(float, String), (float, Object) and (float, Drawable).
                            PieEntry((it.amountMinor.toDouble() / sum * 100.0).toFloat(), null as String?)
                        }
                        val dataSet = PieDataSet(entries, "")
                        dataSet.setColors(positive.map { parseHexColor(it.colorHex).toArgb() })
                        dataSet.setSliceSpace(1.5f)
                        dataSet.setDrawValues(false)
                        chart.data = PieData(dataSet)
                        animationGuard.lastKey = dataKey
                        chart.animateY(700, Easing.EaseOutCubic)
                        chart.invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            // Center overlay rendered in Compose: theme colors + font scaling, kept
            // inside the 70% hole so it never clips against the slices.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.62f),
            ) {
                Text(
                    centerLabel ?: stringResource(R.string.chart_spent),
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    MoneyFormat.format(totalMinor, currency),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Remembers the last animated data key without triggering recomposition. */
private class DonutAnimationGuard {
    var lastKey: String? = null
}
