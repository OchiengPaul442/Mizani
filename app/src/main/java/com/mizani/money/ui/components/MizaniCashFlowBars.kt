package com.mizani.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.mizani.money.core.money.Currency
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.ui.theme.LocalMizaniThemeTokens
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

/** One month of grouped income/expense bars drawn by [MizaniCashFlowBars]. */
data class MonthBar(val label: String, val incomeMinor: Long, val expenseMinor: Long)

/**
 * Grouped cash-flow bars backed by MPAndroidChart v3.1.0's [BarChart].
 *
 * The Android view only draws bars and axis ticks. Everything a user needs to read
 * (legend, month amounts, accessibility description) is rendered with Compose, so it
 * follows the theme, font scaling and localization.
 *
 * Financial roles stay stable across user-selected accent themes: income = emerald,
 * expenses = warm gold. Values are plotted in major
 * currency units (`minor / scaleFactor`) so large balances never overflow a Float.
 *
 * Each month group is exactly one x-unit wide and centred on its integer index, with the
 * axis pinned to half a unit on both sides. That makes the tick spacing exactly 1 unit,
 * so every axis label lands on the centre of its month group (the renderer draws X labels
 * with `Paint.Align.CENTER`).
 */
@Composable
fun MizaniCashFlowBars(
    months: List<MonthBar>,
    currency: Currency,
    modifier: Modifier = Modifier,
) {
    val themeTokens = LocalMizaniThemeTokens.current
    val incomeColor = themeTokens.income
    val expenseColor = themeTokens.expense
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    // ComponentBase.setTextSize(...) expects dp; scale by fontScale so axis text
    // follows the user's accessibility font size.
    val axisTextSizeDp = 10f * LocalDensity.current.fontScale

    val a11yDescription = cashFlowDescription(months, currency)
    val incomeArgb = incomeColor.toArgb()
    val expenseArgb = expenseColor.toArgb()
    val gridArgb = gridColor.toArgb()
    val chartState = remember { BarsChartGuard() }
    val dataKey = remember(months, currency) {
        months.joinToString("|") { "${it.label}:${it.incomeMinor}:${it.expenseMinor}" } +
            "#${currency.scaleFactor}"
    }
    val xFormatter = remember(months) { MonthAxisFormatter(months.map { it.label }) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11yDescription },
    ) {
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    // No chart-side text: labels live in Compose, apart from the axis ticks.
                    this.description.isEnabled = false
                    legend.setEnabled(false)
                    setNoDataText("")
                    setTouchEnabled(false)
                    setDragEnabled(false)
                    setScaleEnabled(false)
                    setPinchZoom(false)
                    setDoubleTapToZoomEnabled(false)
                    setDrawBarShadow(false)
                    setDrawValueAboveBar(false)
                    setDrawGridBackground(false)
                    setFitBars(true)
                    setMinOffset(0f)
                    xAxis.apply {
                        setPosition(XAxis.XAxisPosition.BOTTOM)
                        setGranularity(1f)
                        setGranularityEnabled(true)
                        setDrawAxisLine(false)
                        setDrawGridLines(false)
                        setDrawLabels(true)
                        // Ticks already sit on group centres; centering would shift them
                        // by half a group.
                        setCenterAxisLabels(false)
                        setTextColor(axisTextColor.toArgb())
                        setTextSize(axisTextSizeDp)
                        setValueFormatter(xFormatter)
                    }
                    axisLeft.apply {
                        setDrawAxisLine(false)
                        setDrawGridLines(true)
                        setGridColor(gridArgb)
                        setAxisMinimum(0f)
                        setLabelCount(4, false)
                        setTextColor(axisTextColor.toArgb())
                        setTextSize(axisTextSizeDp)
                        setValueFormatter(CompactValueFormatter())
                    }
                    axisRight.setEnabled(false)
                }
            },
            update = { chart ->
                val axisText = axisTextColor.toArgb()
                // Re-read theme values on every update so dark-mode changes propagate.
                chart.xAxis.setTextColor(axisText)
                chart.xAxis.setTextSize(axisTextSizeDp)
                chart.xAxis.setValueFormatter(xFormatter)
                // The month count belongs to the data, so the pinned bounds follow it.
                chart.xAxis.setAxisMinimum(-AXIS_MARGIN)
                chart.xAxis.setAxisMaximum(months.size - AXIS_MARGIN)
                chart.xAxis.setLabelCount(months.size.coerceIn(2, 25), false)
                chart.axisLeft.setTextColor(axisText)
                chart.axisLeft.setTextSize(axisTextSizeDp)
                chart.axisLeft.setGridColor(gridArgb)
                chart.setNoDataTextColor(axisText)

                if (chartState.lastDataKey != dataKey) {
                    val scale = currency.scaleFactor.coerceAtLeast(1L).toDouble()
                    val incomeEntries = ArrayList<BarEntry>(months.size)
                    val expenseEntries = ArrayList<BarEntry>(months.size)
                    months.forEachIndexed { index, month ->
                        val centre = index.toFloat()
                        incomeEntries += BarEntry(centre - BAR_OFFSET, (month.incomeMinor / scale).toFloat())
                        expenseEntries += BarEntry(centre + BAR_OFFSET, (month.expenseMinor / scale).toFloat())
                    }
                    val incomeSet = BarDataSet(incomeEntries, "Income").apply {
                        color = incomeArgb
                        setDrawValues(false)
                    }
                    val expenseSet = BarDataSet(expenseEntries, "Expenses").apply {
                        color = expenseArgb
                        setDrawValues(false)
                    }
                    // Rebuild only on data change: re-setting data would restart the animation.
                    chart.data = BarData(incomeSet, expenseSet).apply { barWidth = BAR_WIDTH }
                    chartState.lastDataKey = dataKey
                    chartState.incomeArgb = incomeArgb
                    chartState.expenseArgb = expenseArgb
                    chart.animateY(ANIMATION_MS, Easing.EaseOutCubic)
                    chart.invalidate()
                } else if (chartState.incomeArgb != incomeArgb || chartState.expenseArgb != expenseArgb) {
                    chart.data?.dataSets?.forEachIndexed { index, set ->
                        val barSet = set as? BarDataSet ?: return@forEachIndexed
                        barSet.color = if (index == 0) incomeArgb else expenseArgb
                    }
                    chartState.incomeArgb = incomeArgb
                    chartState.expenseArgb = expenseArgb
                    chart.invalidate()
                }
            },
            modifier = Modifier.fillMaxWidth().height(196.dp),
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowLegendItem(incomeColor, "Income")
            Spacer(Modifier.width(Spacing.md))
            FlowLegendItem(expenseColor, "Expenses")
        }
    }
}

/** Text legend: colour is never the only signal, the series name is spelled out. */
@Composable
private fun FlowLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Screen-reader equivalent of the chart: every month's income and expenses. */
private fun cashFlowDescription(months: List<MonthBar>, currency: Currency): String {
    if (months.isEmpty()) return "Monthly cash flow chart. No months to show yet."
    return "Monthly cash flow chart. " + months.joinToString("; ") { month ->
        "${month.label}: income ${MoneyFormat.format(month.incomeMinor, currency)}, " +
            "expenses ${MoneyFormat.format(month.expenseMinor, currency)}"
    }
}

/** X ticks are the group centres (0, 1, 2, ...), one index per month. */
private class MonthAxisFormatter(private val labels: List<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String = labels.getOrNull(value.roundToInt()) ?: ""
}

/** Left axis labels in compact form: `950`, `12k`, `1.2M`, `3.4B`. */
private class CompactValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String = compactLabel(value.toDouble())
}

private fun compactLabel(value: Double): String {
    val magnitude = abs(value)
    return when {
        magnitude >= 1_000_000_000 -> trimmed(value / 1_000_000_000) + "B"
        magnitude >= 1_000_000 -> trimmed(value / 1_000_000) + "M"
        magnitude >= 1_000 -> trimmed(value / 1_000) + "k"
        // Sub-unit ticks (only possible on an empty, all-zero chart) stay readable.
        magnitude > 0 && magnitude < 1 -> trimmed(value)
        else -> value.roundToInt().toString()
    }
}

private fun trimmed(scaled: Double): String {
    val rounded = round(scaled * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

/** Remembers the last animated data key and the applied colors without recomposition. */
private class BarsChartGuard {
    var lastDataKey: String? = null
    var incomeArgb: Int = android.graphics.Color.TRANSPARENT
    var expenseArgb: Int = android.graphics.Color.TRANSPARENT
}

/** Width of one bar and its offset from the group centre (group width = 1 unit). */
private const val BAR_WIDTH = 0.34f
private const val BAR_OFFSET = 0.21f

/** Half a group of empty space pinned on each side of the x axis. */
private const val AXIS_MARGIN = 0.5f

private const val ANIMATION_MS = 700
