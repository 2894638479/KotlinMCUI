package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.align.Alignable
import io.github.u2894638479.kotlinmcui.modifier.contentMinHeight
import io.github.u2894638479.kotlinmcui.modifier.contentMinWidth
import io.github.u2894638479.kotlinmcui.modifier.paddingHeight
import io.github.u2894638479.kotlinmcui.modifier.paddingWidth

interface DslComponentAlign {
    val rect: Rect

    context(instance: DslComponent)
    val contentMinWidth get() = instance.modifier.contentMinWidth
    context(instance: DslComponent)
    val contentMinHeight get() = instance.modifier.contentMinHeight

    context(instance: DslComponent)
    val outerMinWidth get() = instance.contentMinWidth + instance.modifier.paddingWidth
    context(instance: DslComponent)
    val outerMinHeight get() = instance.contentMinHeight + instance.modifier.paddingHeight

    context(instance: DslComponent)
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

    context(instance: DslComponent)
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