package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.align.Alignable
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.sumOf
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.modifier.contentMinHeight
import io.github.u2894638479.kotlinmcui.modifier.contentMinWidth
import io.github.u2894638479.kotlinmcui.modifier.paddingHeight
import io.github.u2894638479.kotlinmcui.modifier.paddingWidth

/**
 * Alignment and layout measurement capabilities.
 *
 * Provides content sizing derived from the modifier chain and adapts the
 * component into [Alignable] instances consumable by [io.github.u2894638479.kotlinmcui.math.align.Aligner] strategies.
 */
interface DslComponentAlign: DslComponentMetadata {
    /** The mutable rectangle defining this component's position and size. */
    val rect: MutRect

    /** Minimum content width from the modifier chain. */
    val contentMinWidth get() = instance.modifier.contentMinWidth

    /** Minimum content height from the modifier chain. */
    val contentMinHeight get() = instance.modifier.contentMinHeight

    /** An [Alignable] adapter for horizontal layout, incorporating padding. */
    val alignableHorizontal get() = instance.run {
        object : Alignable {
            override val minSize get() = outerMinWidth
            override val size get() = modifier.width + modifier.paddingWidth
            override val weight get() = modifier.weight
            override var low get() = rect.left - modifier.paddingLeft
                set(value) { rect.left = value + modifier.paddingLeft }
            override var high get() = rect.right + modifier.paddingRight
                set(value) { rect.right = value - modifier.paddingRight }
            override val align get() = modifier.alignment.horizontal
        }
    }

    /** An [Alignable] adapter for vertical layout, incorporating padding. */
    val alignableVertical get() = instance.run {
        object : Alignable {
            override val minSize get() = outerMinHeight
            override val size get() = modifier.height + modifier.paddingHeight
            override val weight get() = modifier.weight
            override var low get() = rect.top - modifier.paddingTop
                set(value) { rect.top = value + modifier.paddingTop }
            override var high get() = rect.bottom + modifier.paddingBottom
                set(value) { rect.bottom = value - modifier.paddingBottom }
            override val align get() = modifier.alignment.vertical
        }
    }
}

/** Total width including padding from the modifier. */
val DslComponentAlign.outerMinWidth get() = contentMinWidth + modifier.paddingWidth

/** Total height including padding from the modifier. */
val DslComponentAlign.outerMinHeight get() = contentMinHeight + modifier.paddingHeight

/** Sum of all children's outer minimum widths. */
val DslComponent.childrenSumWidth get() = children.sumOf { it.outerMinWidth }

/** Sum of all children's outer minimum heights. */
val DslComponent.childrenSumHeight get() = children.sumOf { it.outerMinHeight }

/** Maximum of children's outer minimum widths, or `0.px` if no children. */
val DslComponent.childrenMaxWidth get() = children.maxOfOrNull { it.outerMinWidth } ?: 0.px

/** Maximum of children's outer minimum heights, or `0.px` if no children. */
val DslComponent.childrenMaxHeight get() = children.maxOfOrNull { it.outerMinHeight } ?: 0.px

/** Content minimum size along the given [axis]. */
fun DslComponentAlign.contentMinSize(axis: Axis) = when(axis) {
    Axis.Horizontal -> contentMinWidth
    Axis.Vertical -> contentMinHeight
}

/** Outer minimum size (content + padding) along the given [axis]. */
fun DslComponentAlign.outerMinSize(axis: Axis) = when(axis) {
    Axis.Horizontal -> outerMinWidth
    Axis.Vertical -> outerMinHeight
}

/** The [Alignable] adapter for the given [axis]. */
fun DslComponentAlign.alignable(axis: Axis) = when(axis) {
    Axis.Horizontal -> alignableHorizontal
    Axis.Vertical -> alignableVertical
}
