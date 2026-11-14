package com.mizani.money.ui

/** A display feature expressed in window pixels so the layout policy stays unit-testable. */
data class FoldFeatureBounds(
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
    val vertical: Boolean,
    val separating: Boolean,
)

data class SafeWindowPane(
    val leftPx: Int,
    val topPx: Int,
    val widthPx: Int,
    val heightPx: Int,
)

/** Shared window/font thresholds for responsive Compose decisions. */
object AdaptiveLayout {
    const val MEDIUM_WIDTH_DP = 600
    const val COMPACT_NAVIGATION_WIDTH_DP = 360
    const val STACKED_CONTENT_WIDTH_DP = 420
    const val LARGE_FONT_SCALE = 1.15f
    const val EXTRA_LARGE_FONT_SCALE = 1.30f

    /** Content width at which Home switches from one column to two. */
    const val EXPANDED_CONTENT_WIDTH_DP = 700

    /** Cap for single-column page content so cards stay readable on wide windows. */
    const val SINGLE_COLUMN_MAX_WIDTH_DP = 680

    /**
     * Selects the largest uninterrupted app pane when a separating fold/hinge crosses the
     * window. A continuous, non-separating fold does not force a smaller pane because content can
     * safely span it. Equal panes prefer the leading pane to keep navigation placement stable.
     */
    fun largestSafePane(
        windowWidthPx: Int,
        windowHeightPx: Int,
        feature: FoldFeatureBounds?,
    ): SafeWindowPane {
        val width = windowWidthPx.coerceAtLeast(0)
        val height = windowHeightPx.coerceAtLeast(0)
        if (feature == null || !feature.separating) {
            return SafeWindowPane(0, 0, width, height)
        }

        return if (feature.vertical) {
            val leftWidth = feature.leftPx.coerceIn(0, width)
            val rightStart = feature.rightPx.coerceIn(0, width)
            val rightWidth = (width - rightStart).coerceAtLeast(0)
            if (leftWidth == 0 || rightWidth == 0) {
                return SafeWindowPane(0, 0, width, height)
            }
            if (rightWidth > leftWidth) {
                SafeWindowPane(rightStart, 0, rightWidth, height)
            } else {
                SafeWindowPane(0, 0, leftWidth, height)
            }
        } else {
            val topHeight = feature.topPx.coerceIn(0, height)
            val bottomStart = feature.bottomPx.coerceIn(0, height)
            val bottomHeight = (height - bottomStart).coerceAtLeast(0)
            if (topHeight == 0 || bottomHeight == 0) {
                return SafeWindowPane(0, 0, width, height)
            }
            if (bottomHeight > topHeight) {
                SafeWindowPane(0, bottomStart, width, bottomHeight)
            } else {
                SafeWindowPane(0, 0, width, topHeight)
            }
        }
    }
}
