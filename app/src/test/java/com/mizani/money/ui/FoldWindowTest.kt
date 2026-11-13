package com.mizani.money.ui

import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldWindowTest {

    private val density = Density(1f)

    @Test
    fun `without a separating feature the full window is flat`() {
        val layout = foldWindowLayout(1200, 800, feature = null, density = density)

        assertEquals(FoldPosture.Flat, layout.posture)
        assertEquals(WindowPane(0, 0, 1200, 800), layout.leadingPane)
        assertNull(layout.trailingPane)
        assertEquals(WindowPane(0, 0, 1200, 800), layout.contentPane)
        assertNull(layout.navigationPane)
        assertFalse(layout.usableSplit)
        assertFalse(layout.compactSplit)
    }

    @Test
    fun `a non-separating feature keeps the window flat`() {
        val layout = foldWindowLayout(
            1200,
            800,
            FoldFeatureBounds(580, 0, 620, 800, vertical = true, separating = false),
            density,
        )

        assertEquals(FoldPosture.Flat, layout.posture)
        assertNull(layout.trailingPane)
    }

    @Test
    fun `vertical hinge resolves book panes and selects trailing content`() {
        val layout = foldWindowLayout(
            1200,
            800,
            FoldFeatureBounds(580, 0, 620, 800, vertical = true, separating = true),
            density,
        )

        assertEquals(FoldPosture.Book, layout.posture)
        assertEquals(WindowPane(0, 0, 580, 800), layout.leadingPane)
        assertEquals(WindowPane(620, 0, 580, 800), layout.trailingPane)
        assertEquals(40, layout.hingeWidthPx)
        assertEquals(WindowPane(620, 0, 580, 800), layout.contentPane)
        assertEquals(WindowPane(0, 0, 580, 800), layout.navigationPane)
        assertTrue(layout.usableSplit)
        assertFalse(layout.compactSplit)
    }

    @Test
    fun `horizontal hinge resolves tabletop panes and selects leading content`() {
        val layout = foldWindowLayout(
            1200,
            800,
            FoldFeatureBounds(0, 380, 1200, 420, vertical = false, separating = true),
            density,
        )

        assertEquals(FoldPosture.Tabletop, layout.posture)
        assertEquals(WindowPane(0, 0, 1200, 380), layout.leadingPane)
        assertEquals(WindowPane(0, 420, 1200, 380), layout.trailingPane)
        assertEquals(40, layout.hingeHeightPx)
        assertEquals(WindowPane(0, 0, 1200, 380), layout.contentPane)
        assertEquals(WindowPane(0, 420, 1200, 380), layout.navigationPane)
        assertTrue(layout.usableSplit)
        assertFalse(layout.compactSplit)
    }

    @Test
    fun `rotation swaps book content to the leading top pane in tabletop`() {
        val layout = foldWindowLayout(
            800,
            1200,
            FoldFeatureBounds(0, 580, 800, 620, vertical = false, separating = true),
            density,
        )

        assertEquals(FoldPosture.Tabletop, layout.posture)
        assertEquals(WindowPane(0, 0, 800, 580), layout.contentPane)
        assertEquals(WindowPane(0, 620, 800, 580), layout.navigationPane)
        assertTrue(layout.usableSplit)
    }

    @Test
    fun `degenerate panes at the window edges are flat`() {
        assertEquals(
            FoldPosture.Flat,
            foldWindowLayout(
                1200, 800,
                FoldFeatureBounds(0, 0, 40, 800, vertical = true, separating = true),
                density,
            ).posture,
        )
        assertEquals(
            FoldPosture.Flat,
            foldWindowLayout(
                1200, 800,
                FoldFeatureBounds(1160, 0, 1200, 800, vertical = true, separating = true),
                density,
            ).posture,
        )
        assertEquals(
            FoldPosture.Flat,
            foldWindowLayout(
                1200, 800,
                FoldFeatureBounds(0, 0, 1200, 40, vertical = false, separating = true),
                density,
            ).posture,
        )
        assertEquals(
            FoldPosture.Flat,
            foldWindowLayout(
                1200, 800,
                FoldFeatureBounds(0, 760, 1200, 800, vertical = false, separating = true),
                density,
            ).posture,
        )
    }

    @Test
    fun `features outside the window are flat`() {
        assertEquals(
            FoldPosture.Flat,
            foldWindowLayout(
                1200, 800,
                FoldFeatureBounds(1400, 0, 1500, 800, vertical = true, separating = true),
                density,
            ).posture,
        )
        assertEquals(
            FoldPosture.Flat,
            foldWindowLayout(
                1200, 800,
                FoldFeatureBounds(-100, 0, -50, 800, vertical = true, separating = true),
                density,
            ).posture,
        )
        assertEquals(
            FoldPosture.Flat,
            foldWindowLayout(
                1200, 800,
                FoldFeatureBounds(0, 900, 1200, 950, vertical = false, separating = true),
                density,
            ).posture,
        )
    }

    @Test
    fun `a too-small leading book pane gives content the larger trailing pane`() {
        val layout = foldWindowLayout(
            500,
            800,
            FoldFeatureBounds(100, 0, 110, 800, vertical = true, separating = true),
            density,
        )

        assertEquals(FoldPosture.Book, layout.posture)
        assertFalse(layout.usableSplit)
        assertFalse(layout.compactSplit)
        assertEquals(WindowPane(110, 0, 390, 800), layout.contentPane)
        assertEquals(WindowPane(0, 0, 100, 800), layout.navigationPane)
    }

    @Test
    fun `a too-small trailing book pane gives content the larger leading pane`() {
        val layout = foldWindowLayout(
            500,
            800,
            FoldFeatureBounds(400, 0, 410, 800, vertical = true, separating = true),
            density,
        )

        assertEquals(FoldPosture.Book, layout.posture)
        assertFalse(layout.usableSplit)
        assertFalse(layout.compactSplit)
        assertEquals(WindowPane(0, 0, 400, 800), layout.contentPane)
        assertEquals(WindowPane(410, 0, 90, 800), layout.navigationPane)
    }

    @Test
    fun `a narrow but usable-height sidebar is a compact split`() {
        val layout = foldWindowLayout(
            600,
            800,
            FoldFeatureBounds(200, 0, 210, 800, vertical = true, separating = true),
            density,
        )

        assertEquals(FoldPosture.Book, layout.posture)
        assertFalse(layout.usableSplit)
        assertTrue(layout.compactSplit)
        assertEquals(WindowPane(210, 0, 390, 800), layout.contentPane)
        assertEquals(WindowPane(0, 0, 200, 800), layout.navigationPane)
    }

    @Test
    fun `a too-small leading tabletop pane gives content the larger bottom pane`() {
        val layout = foldWindowLayout(
            800,
            500,
            FoldFeatureBounds(0, 170, 800, 180, vertical = false, separating = true),
            density,
        )

        assertEquals(FoldPosture.Tabletop, layout.posture)
        assertFalse(layout.usableSplit)
        assertFalse(layout.compactSplit)
        assertEquals(WindowPane(0, 180, 800, 320), layout.contentPane)
        assertEquals(WindowPane(0, 0, 800, 170), layout.navigationPane)
    }

    @Test
    fun `split quality follows density not raw pixels`() {
        val layout = foldWindowLayout(
            1200,
            800,
            FoldFeatureBounds(580, 0, 620, 800, vertical = true, separating = true),
            Density(2f),
        )

        assertEquals(FoldPosture.Book, layout.posture)
        assertFalse(layout.usableSplit)
        assertTrue(layout.compactSplit)
    }
}
