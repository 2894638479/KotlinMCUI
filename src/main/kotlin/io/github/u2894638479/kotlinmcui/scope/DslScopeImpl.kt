package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.modifier.Modifier

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
    override var instance: DslComponent = this

    override fun layoutHorizontal() {
        val children = instance.children ?: return
        alignerHorizontal.align(instance.rect.bound(Axis.Horizontal),children.map { it.run { alignableHorizontal } })
        children.forEach { it.run { layoutHorizontal() } }
    }

    override fun layoutVertical() {
        val children = instance.children ?: return
        alignerVertical.align(instance.rect.bound(Axis.Vertical),children.map { it.run { alignableVertical } })
        children.forEach { it.run { layoutVertical() } }
    }

    override fun build(instance: DslComponent) {
        super.build(instance)
        val children = instance.children ?: return
        context(ctx.change(dslIdentity = instance.identity, dslChildren = children),dslFunction)
        children.forEach { it.build(it) }
    }
}