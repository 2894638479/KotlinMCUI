package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.align.Alignable
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.sumOf
import io.github.u2894638479.kotlinmcui.modifier.contentMinHeight
import io.github.u2894638479.kotlinmcui.modifier.contentMinWidth
import io.github.u2894638479.kotlinmcui.modifier.paddingHeight
import io.github.u2894638479.kotlinmcui.modifier.paddingWidth
import kotlin.run

interface DslComponentAlign: DslComponentMetadata {
    val rect: MutRect

    val contentMinWidth get() = instance.modifier.contentMinWidth
    val contentMinHeight get() = instance.modifier.contentMinHeight

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

    val alignableVertical get() = instance.run {
        object : Alignable {
            override val minSize get() = outerMinHeight
            override val size get() = modifier.height + modifier.paddingHeight
            override val weight get() = modifier.weight
            override var low get() = rect.top - modifier.paddingTop
                set(value) { rect.top = value + modifier.paddingTop }
            override var high get() = rect.bottom - modifier.paddingBottom
                set(value) { rect.bottom = value - modifier.paddingBottom }
            override val align get() = modifier.alignment.vertical
        }
    }
}

val DslComponent.outerMinWidth get() = contentMinWidth + modifier.paddingWidth
val DslComponent.outerMinHeight get() = contentMinHeight + modifier.paddingHeight

val DslComponent.childrenSumWidth get() = children.sumOf { it.outerMinWidth }
val DslComponent.childrenSumHeight get() = children.sumOf { it.outerMinHeight }

val DslComponent.childrenMaxWidth get() = children.maxOfOrNull { it.outerMinWidth } ?: 0.px
val DslComponent.childrenMaxHeight get() = children.maxOfOrNull { it.outerMinHeight } ?: 0.px

fun DslComponent.contentMinSize(axis: Axis) = when(axis) {
    Axis.Horizontal -> contentMinWidth
    Axis.Vertical -> contentMinHeight
}

fun DslComponent.outerMinSize(axis: Axis) = when(axis) {
    Axis.Horizontal -> outerMinWidth
    Axis.Vertical -> outerMinHeight
}

fun DslComponent.alignable(axis: Axis) = when(axis) {
    Axis.Horizontal -> alignableHorizontal
    Axis.Vertical -> alignableVertical
}