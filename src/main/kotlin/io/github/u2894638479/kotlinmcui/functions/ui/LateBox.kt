package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.alignable
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild.Companion.buildThis
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl

context(ctx: DslContext)
fun LateBox(modifier: Modifier = Modifier,id:Any? = null,content:context(DslContext) Rect.() -> Unit) = collect(
    object : DslScope by DslScopeImpl(newChildId(id ?: content::class),modifier,ctx,{}) {
        override fun build() {}
        override fun layoutHorizontal() {}
        override fun layoutVertical() {
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