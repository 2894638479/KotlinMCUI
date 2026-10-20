package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.outline
import io.github.u2894638479.kotlinmcui.dsl.decorator.rotate
import io.github.u2894638479.kotlinmcui.dsl.decorator.shear
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.dsl.ui.ColorRect
import io.github.u2894638479.kotlinmcui.dsl.ui.Item
import io.github.u2894638479.kotlinmcui.dsl.ui.Row
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.widget.Presets

/**
 * The cases that break a draw sorter, not the cases that make a pretty page.
 *
 * A backend that reorders draws to merge batches is only correct while two rules hold: draws whose pixels
 * are disjoint may swap, and nothing else may. Every block below is a place where the obvious shortcut
 * fails - a translucent stack over text, an opaque fill declared *above* a label, an outline that spills
 * onto its neighbour, a glyph shadow that reaches one pixel past its box, a rotated translucent row whose
 * bounding box is far larger than what it paints, and an item icon that a vertex-level sorter cannot move
 * at all. The page is not meant to look good; it is meant to change pixels the day the sorter errs.
 */
context(ctx: DslContext)
fun TestOrderPage() = ScrollableColumn {
    // Two translucent sheets over a label: swapping either sheet with the text changes the blend.
    Box(Modifier.height(48.scaled)) {
        Presets.Text { "under the glass" }
        ColorRect(Modifier, Color(255, 0, 0, 96)) {}
        ColorRect(Modifier, Color(0, 0, 255, 96)) {}
    }

    // The fill is declared first, so it is drawn last and hides the label. A sorter that groups all
    // fills before all text would uncover text this page is meant to keep buried.
    Box(Modifier.height(40.scaled)) {
        ColorRect(Modifier, Color(40, 40, 40)) {}
        Presets.Text { "hidden under an opaque fill" }
    }

    // The outline is drawn outside the child's rect and lands on the green neighbour.
    Row(Modifier.height(40.scaled)) {
        Box(Modifier.weight(1.0)) { Presets.Text { "outline" } }.outline(2.scaled, 6.scaled, Color(255, 255, 0))
        ColorRect(Modifier.weight(1.0), Color.GREEN) {}
    }

    // A glyph shadow is drawn one pixel right and down of the glyph, past the glyph's own box.
    Row(Modifier.height(24.scaled)) {
        Presets.Text { "shadow" }
        ColorRect(Modifier.weight(1.0), Color.WHITE) {}
    }

    // Rotated and sheared translucent bands: the bounding box of each is much larger than its pixels,
    // which may only cost merges - it may never let a swap through.
    Row(Modifier.height(70.scaled)) {
        ColorRect(Modifier.weight(1.0), Color.RED) {}
        ColorRect(Modifier.weight(1.0), Color(0, 255, 0, 128)) {}
        ColorRect(Modifier.weight(1.0), Color(0, 0, 255, 128)) {}
    }.rotate(0.35).shear(0.4, 0.0)

    // An item draws through its own renderer, so a vertex-level display list cannot replay it: it has to
    // be a barrier, and the label after it has to stay under it.
    Row(Modifier.height(28.scaled)) {
        Item(Modifier.size(24.scaled, 24.scaled), "minecraft:diamond_pickaxe", 1, 0.5, id = "order-item")
        Presets.Text { "item over text" }
    }
}
