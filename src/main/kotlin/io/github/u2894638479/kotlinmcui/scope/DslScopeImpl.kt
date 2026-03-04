package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild.Companion.buildThis

class DslScopeImpl(
    override val identity: DslId,
    override val modifier: Modifier,
    val ctx: DslContext,
    val dslFunction: DslFunction,
    val alignerHorizontal: Aligner = Aligner.simplePlace,
    val alignerVertical: Aligner = Aligner.simplePlace,
) : DslScope {
    override val rect = MutRect()
    override val children = DslChild.List()
    private var _instance: DslComponent? = null
    override var instance: DslComponent get() = _instance ?: error("using `instance` before initialize")
        set(value) { if(_instance == null) _instance = value else error("`instance` is set twice") }

    override fun layoutHorizontal() {
        val children = instance.children
        alignerHorizontal.align(instance.rect.bound(Axis.Horizontal),children.map { it.alignableHorizontal })
        children.forEach { it.layoutHorizontal() }
    }

    override fun layoutVertical() {
        val children = instance.children
        alignerVertical.align(instance.rect.bound(Axis.Vertical),children.map { it.alignableVertical })
        children.forEach { it.layoutVertical() }
    }

    override fun build() {
        instance.children.buildThis(ctx,dslFunction)
        instance.children.forEach { it.attachInstance();it.build() }
    }
}