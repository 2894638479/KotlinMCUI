package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.component.alignable
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.container.DslChild.Companion.buildThis
import io.github.u2894638479.kotlinmcui.container.DslScope
import io.github.u2894638479.kotlinmcui.container.DslScopeImpl

context(ctx: DslContext)
fun LateBox(modifier: Modifier = Modifier,id:Any? = null,content:context(DslContext) Rect.() -> Unit) = collect(
    object : DslScope by DslScopeImpl(newChildId(id ?: content::class),modifier,ctx,{}) {
        /** Frame the content was last executed in, so a second layout pass is a no-op. */
        private var builtFrame = ULong.MAX_VALUE

        override fun build() {}
        override fun layoutHorizontal() {}
        override fun layoutVertical() {
            // Some parents lay their children out more than once per frame; running
            // the content again would collect the same children a second time.
            if(builtFrame == ctx.dataStore.frameIndex) return
            builtFrame = ctx.dataStore.frameIndex

            val children = instance.children
            children.buildThis(ctx) { instance.rect.content() }
            children.forEach { it.attachInstance();it.build() }
            Aligner.simplePlace.align(rect.bound(Axis.Horizontal),children.map { it.alignable(Axis.Horizontal) })
            children.forEach { it.layoutHorizontal() }
            Aligner.simplePlace.align(rect.bound(Axis.Vertical),children.map { it.alignable(Axis.Vertical) })
            children.forEach { it.layoutVertical() }
        }
    }
)