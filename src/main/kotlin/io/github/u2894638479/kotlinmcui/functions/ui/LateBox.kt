package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.alignable
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScope

context(ctx: DslContext)
fun LateBox(modifier: Modifier = Modifier,id:Any? = null,content:context(DslContext) Rect.() -> Unit) = collect(
    object : DslScope {
        override val children = DslChild.List()
        override val rect = MutRect()
        override val identity = newChildId(id ?: content::class)
        override val modifier = modifier

        context(instance: DslComponent)
        override fun layoutVertical() {
            context(ctx.change(dslIdentity = identity, dslChildren = children)) { instance.rect.content() }
            children.forEach { it.run { build() } }
            Aligner.simplePlace.align(rect.bound(Axis.Horizontal),children.map { it.run { alignable(Axis.Horizontal) } })
            children.forEach { it.run { layoutHorizontal() } }
            Aligner.simplePlace.align(rect.bound(Axis.Vertical),children.map { it.run { alignable(Axis.Vertical) } })
            children.forEach { it.run { layoutVertical() } }
        }

        context(instance: DslComponent)
        override fun clear() = children.clear()
    }
)