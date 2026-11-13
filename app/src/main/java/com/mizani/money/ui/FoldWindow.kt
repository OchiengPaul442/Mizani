package com.mizani.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import com.mizani.money.R
import com.mizani.money.ui.components.MizaniMark
import com.mizani.money.ui.components.Spacing

/** The resolved fold posture for the current window. */
enum class FoldPosture {
    /** No separating hinge: the whole window is usable. */
    Flat,

    /** A vertical separating hinge: navigation leads, content trails. */
    Book,

    /** A horizontal separating hinge: content leads (top), navigation trails (bottom). */
    Tabletop,
}

/** An axis-aligned app pane expressed in window pixels. */
data class WindowPane(
    val leftPx: Int,
    val topPx: Int,
    val widthPx: Int,
    val heightPx: Int,
)

/**
 * The fold-aware window layout. [leadingPane] and [trailingPane] never overlap and never extend
 * past the window; the hinge is the gap between them. [trailingPane] is null in [FoldPosture.Flat]
 * because there is no second pane.
 *
 * [usableSplit] and [compactSplit] are resolved against the density that was current when the
 * layout was computed. They describe whether both panes are large enough for a real two-pane
 * layout, or only a compact icon-only navigation pane.
 */
data class FoldWindowLayout(
    val posture: FoldPosture,
    val windowWidthPx: Int,
    val windowHeightPx: Int,
    val leadingPane: WindowPane,
    val trailingPane: WindowPane?,
    val hingeWidthPx: Int,
    val hingeHeightPx: Int,
    val usableSplit: Boolean = false,
    val compactSplit: Boolean = false,
) {
    /** The pane that owns the app content. Book -> trailing, Tabletop -> leading, Flat -> full window. */
    val contentPane: WindowPane
        get() {
            if (posture == FoldPosture.Flat) return leadingPane
            val default = when (posture) {
                FoldPosture.Book -> trailingPane ?: leadingPane
                FoldPosture.Tabletop -> leadingPane
                FoldPosture.Flat -> leadingPane
            }
            // A real two-pane split, or one whose smaller pane can still host icon-only
            // navigation, keeps the natural orientation. Only a genuinely too-small pane gives
            // the content the larger side and leaves navigation/backdrop in the smaller one.
            if (usableSplit || compactSplit) return default
            val trailing = trailingPane ?: return default
            return when (posture) {
                FoldPosture.Book -> if (leadingPane.widthPx >= trailing.widthPx) leadingPane else trailing
                FoldPosture.Tabletop -> if (leadingPane.heightPx >= trailing.heightPx) leadingPane else trailing
                FoldPosture.Flat -> default
            }
        }

    /** The pane reserved for navigation chrome, or null when there is no second pane. */
    val navigationPane: WindowPane?
        get() {
            if (posture == FoldPosture.Flat) return null
            val content = contentPane
            return if (content == leadingPane) trailingPane else leadingPane
        }

    val isHalfOpen: Boolean
        get() = posture != FoldPosture.Flat
}

fun WindowPane.leftDp(density: Density): Dp = with(density) { leftPx.toDp() }

fun WindowPane.topDp(density: Density): Dp = with(density) { topPx.toDp() }

fun WindowPane.widthDp(density: Density): Dp = with(density) { widthPx.toDp() }

fun WindowPane.heightDp(density: Density): Dp = with(density) { heightPx.toDp() }

fun Int.toDp(density: Density): Dp = with(density) { toDp() }

/** Minimum pane/window extents for a comfortable two-pane split. */
private const val BOOK_MIN_PANE_DP = 300
private const val BOOK_MIN_WINDOW_DP = 600
private const val TABLETOP_MIN_PANE_DP = 260
private const val TABLETOP_MIN_WINDOW_DP = 480
private const val COMPACT_MIN_PANE_DP = 180

/** Comfortable breathing room between a fold-aware dialog and its content pane edges. */
private val DialogPaneMargin = 24.dp

/**
 * Pure fold layout resolution. A missing, non-separating, or degenerate feature yields a flat full
 * window. Bounds are clamped to the window and pane sizes are never negative.
 */
fun foldWindowLayout(
    windowWidthPx: Int,
    windowHeightPx: Int,
    feature: FoldFeatureBounds?,
    density: Density,
): FoldWindowLayout {
    val width = windowWidthPx.coerceAtLeast(0)
    val height = windowHeightPx.coerceAtLeast(0)
    if (feature == null || !feature.separating) {
        return flatLayout(width, height)
    }

    return if (feature.vertical) {
        val rawLeft = feature.leftPx.coerceIn(0, width)
        val rawRight = feature.rightPx.coerceIn(0, width)
        val hingeLeft = minOf(rawLeft, rawRight)
        val hingeRight = maxOf(rawLeft, rawRight)
        val leading = WindowPane(0, 0, hingeLeft, height)
        val trailing = WindowPane(hingeRight, 0, (width - hingeRight).coerceAtLeast(0), height)
        if (leading.widthPx == 0 || trailing.widthPx == 0) {
            flatLayout(width, height)
        } else {
            FoldWindowLayout(
                posture = FoldPosture.Book,
                windowWidthPx = width,
                windowHeightPx = height,
                leadingPane = leading,
                trailingPane = trailing,
                hingeWidthPx = (hingeRight - hingeLeft).coerceAtLeast(0),
                hingeHeightPx = 0,
            ).withSplitQuality(density)
        }
    } else {
        val rawTop = feature.topPx.coerceIn(0, height)
        val rawBottom = feature.bottomPx.coerceIn(0, height)
        val hingeTop = minOf(rawTop, rawBottom)
        val hingeBottom = maxOf(rawTop, rawBottom)
        val leading = WindowPane(0, 0, width, hingeTop)
        val trailing = WindowPane(0, hingeBottom, width, (height - hingeBottom).coerceAtLeast(0))
        if (leading.heightPx == 0 || trailing.heightPx == 0) {
            flatLayout(width, height)
        } else {
            FoldWindowLayout(
                posture = FoldPosture.Tabletop,
                windowWidthPx = width,
                windowHeightPx = height,
                leadingPane = leading,
                trailingPane = trailing,
                hingeWidthPx = 0,
                hingeHeightPx = (hingeBottom - hingeTop).coerceAtLeast(0),
            ).withSplitQuality(density)
        }
    }
}

/** Resolves the density-dependent split quality for a half-open layout. */
private fun FoldWindowLayout.withSplitQuality(density: Density): FoldWindowLayout {
    val trailing = trailingPane ?: return copy(usableSplit = false, compactSplit = false)
    val (leadingExtent, trailingExtent, windowExtent, minPaneDp, minWindowDp) = when (posture) {
        FoldPosture.Book -> listOf(
            leadingPane.widthPx,
            trailing.widthPx,
            windowWidthPx,
            BOOK_MIN_PANE_DP,
            BOOK_MIN_WINDOW_DP,
        )
        FoldPosture.Tabletop -> listOf(
            leadingPane.heightPx,
            trailing.heightPx,
            windowHeightPx,
            TABLETOP_MIN_PANE_DP,
            TABLETOP_MIN_WINDOW_DP,
        )
        FoldPosture.Flat -> return copy(usableSplit = false, compactSplit = false)
    }
    val leadingExtentDp = with(density) { leadingExtent.toDp() }
    val trailingExtentDp = with(density) { trailingExtent.toDp() }
    val windowExtentDp = with(density) { windowExtent.toDp() }
    val usable = windowExtentDp >= minWindowDp.dp &&
        leadingExtentDp >= minPaneDp.dp &&
        trailingExtentDp >= minPaneDp.dp
    val compact = !usable && minOf(leadingExtentDp, trailingExtentDp) >= COMPACT_MIN_PANE_DP.dp
    return copy(usableSplit = usable, compactSplit = compact)
}

/** Adapter from the platform [WindowLayoutInfo] to the pure fold resolution. */
fun foldWindowLayout(
    windowWidthPx: Int,
    windowHeightPx: Int,
    windowLayoutInfo: WindowLayoutInfo?,
    density: Density,
): FoldWindowLayout = foldWindowLayout(
    windowWidthPx,
    windowHeightPx,
    windowLayoutInfo?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull { it.isSeparating }
        ?.let { feature ->
            FoldFeatureBounds(
                leftPx = feature.bounds.left,
                topPx = feature.bounds.top,
                rightPx = feature.bounds.right,
                bottomPx = feature.bounds.bottom,
                vertical = feature.orientation == FoldingFeature.Orientation.VERTICAL,
                separating = feature.isSeparating,
            )
        },
    density,
)

/**
 * Remembers the fold layout for the current window. The window size comes from
 * [LocalWindowInfo], which reflects the actual app window (including split-screen), rather than
 * the device configuration.
 */
@Composable
fun rememberFoldWindowLayout(windowLayoutInfo: WindowLayoutInfo?): FoldWindowLayout {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val widthPx = containerSize.width
    val heightPx = containerSize.height
    return remember(windowLayoutInfo, widthPx, heightPx, density.density) {
        foldWindowLayout(widthPx, heightPx, windowLayoutInfo, density)
    }
}

val LocalFoldWindowLayout = staticCompositionLocalOf<FoldWindowLayout?> { null }

/** Identifies which side of a two-pane fold layout a composable occupies. */
enum class FoldPaneRole { Leading, Trailing }

/**
 * The safe-drawing sides that belong to a pane. The hinge-facing side is deliberately excluded so
 * content never renders underneath the hinge.
 */
fun FoldWindowLayout.paneInsetsSides(role: FoldPaneRole): WindowInsetsSides? = when (posture) {
    FoldPosture.Book -> if (role == FoldPaneRole.Leading) {
        WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Start
    } else {
        WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End
    }
    FoldPosture.Tabletop -> if (role == FoldPaneRole.Leading) {
        WindowInsetsSides.Top + WindowInsetsSides.Start + WindowInsetsSides.End
    } else {
        WindowInsetsSides.Bottom + WindowInsetsSides.Start + WindowInsetsSides.End
    }
    FoldPosture.Flat -> null
}

/** Applies the side-specific safe-drawing insets for one fold pane. */
@Composable
fun Modifier.foldPaneWindowInsets(layout: FoldWindowLayout, role: FoldPaneRole): Modifier {
    val sides = layout.paneInsetsSides(role) ?: return this
    return windowInsetsPadding(WindowInsets.safeDrawing.only(sides))
}

/** Branded surface for the pane that has no app content. */
@Composable
fun FoldBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MizaniMark(size = 56, rounded = true)
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Private by design",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Places pre-main content in the content pane and a branded backdrop in the unused pane. Flat
 * windows keep their existing full-window treatment.
 */
@Composable
fun FoldAwareScreen(content: @Composable () -> Unit) {
    val foldLayout = LocalFoldWindowLayout.current
    if (foldLayout == null || !foldLayout.isHalfOpen) {
        Box(Modifier.fillMaxSize()) { content() }
        return
    }

    val density = LocalDensity.current
    val navPane = foldLayout.navigationPane
    val contentPane = foldLayout.contentPane

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        navPane?.let { nav ->
            Box(
                modifier = Modifier
                    .offset(x = nav.leftDp(density), y = nav.topDp(density))
                    .size(nav.widthDp(density), nav.heightDp(density)),
            ) { FoldBackdrop() }
        }
        // Panes are already bounded by the hinge; screens apply their own safe-drawing insets
        // (Onboarding, LegalConsent, StartupError) so the pane must not add a second inset pass.
        Box(
            modifier = Modifier
                .offset(x = contentPane.leftDp(density), y = contentPane.topDp(density))
                .size(contentPane.widthDp(density), contentPane.heightDp(density)),
        ) { content() }
    }
}

/**
 * Constrains dialog content to the fold content pane, centered, with a comfortable margin. In a
 * flat window this is a pass-through so existing dialog behavior is unchanged. The margin is
 * clamped to 0.dp so a pane smaller than the margin can never produce a negative constraint.
 */
@Composable
fun FoldAwareDialogHost(content: @Composable () -> Unit) {
    val foldLayout = LocalFoldWindowLayout.current
    if (foldLayout == null || !foldLayout.isHalfOpen) {
        content()
        return
    }

    val density = LocalDensity.current
    val pane = foldLayout.contentPane
    val paneWidth = pane.widthDp(density)
    val paneHeight = pane.heightDp(density)
    val dialogWidthMax = (paneWidth - DialogPaneMargin).coerceAtLeast(0.dp)
    val dialogHeightMax = (paneHeight - DialogPaneMargin).coerceAtLeast(0.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = pane.leftDp(density), top = pane.topDp(density)),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            modifier = Modifier
                .width(paneWidth)
                .height(paneHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = dialogWidthMax)
                    .heightIn(max = dialogHeightMax)
                    .clipToBounds(),
            ) { content() }
        }
    }
}

/**
 * The shared two-pane navigation layout. The content subtree is always composed in the same slot,
 * so the [androidx.navigation.compose.NavHost] and its saveable form state survive posture changes
 * between Flat, Book, and Tabletop. In Flat the sidebar and dock measure zero and content fills the
 * window; in Book the sidebar leads and content trails; in Tabletop content leads and the dock
 * trails. Each pane receives its own side-specific safe-drawing insets.
 */
@Composable
fun FoldNavigationLayout(
    layout: FoldWindowLayout,
    sidebar: @Composable () -> Unit,
    dock: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val navPane = layout.navigationPane
    val contentPane = layout.contentPane
    val contentRole = if (contentPane == layout.leadingPane) {
        FoldPaneRole.Leading
    } else {
        FoldPaneRole.Trailing
    }
    val navRole = if (navPane != null && navPane == layout.leadingPane) {
        FoldPaneRole.Leading
    } else {
        FoldPaneRole.Trailing
    }

    Layout(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        content = {
            Box(modifier = Modifier.foldPaneWindowInsets(layout, navRole)) {
                if (layout.posture == FoldPosture.Book) sidebar()
            }
            Box(modifier = Modifier.foldPaneWindowInsets(layout, navRole)) {
                if (layout.posture == FoldPosture.Tabletop) dock()
            }
            Box(modifier = Modifier.foldPaneWindowInsets(layout, contentRole)) { content() }
        },
        measurePolicy = { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight

            val sidebarConstraints = if (layout.posture == FoldPosture.Book && navPane != null) {
                Constraints.fixed(navPane.widthPx, navPane.heightPx)
            } else {
                Constraints.fixed(0, 0)
            }
            val sidebarPlaceable = measurables[0].measure(sidebarConstraints)

            val dockConstraints = if (layout.posture == FoldPosture.Tabletop && navPane != null) {
                Constraints.fixed(navPane.widthPx, navPane.heightPx)
            } else {
                Constraints.fixed(0, 0)
            }
            val dockPlaceable = measurables[1].measure(dockConstraints)

            val contentWidth = contentPane.widthPx.coerceIn(0, width)
            val contentHeight = contentPane.heightPx.coerceIn(0, height)
            val contentPlaceable = measurables[2].measure(Constraints.fixed(contentWidth, contentHeight))

            layout(width, height) {
                sidebarPlaceable.place(navPane?.leftPx ?: 0, navPane?.topPx ?: 0)
                dockPlaceable.place(navPane?.leftPx ?: 0, navPane?.topPx ?: 0)
                contentPlaceable.place(contentPane.leftPx, contentPane.topPx)
            }
        },
    )
}

private fun flatLayout(width: Int, height: Int): FoldWindowLayout = FoldWindowLayout(
    posture = FoldPosture.Flat,
    windowWidthPx = width,
    windowHeightPx = height,
    leadingPane = WindowPane(0, 0, width, height),
    trailingPane = null,
    hingeWidthPx = 0,
    hingeHeightPx = 0,
)
