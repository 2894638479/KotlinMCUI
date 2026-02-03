package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier

class DslScopeImpl(
    override val identity: DslId,
    override val modifier: Modifier,
    val ctx: DslContext,
    val dslFunction: DslFunction,
    override val alignerHorizontal: Aligner = Aligner.simplePlace,
    override val alignerVertical: Aligner = Aligner.simplePlace,
) : DslScope {
    override val rect = Rect()
    override val children = DslChild.List()

    context(instance: DslComponent)
    override fun build() {
        context(ctx.change(dslIdentity = instance.identity, dslChildren = children),dslFunction)
        children.forEach { it.run { build() } }
    }

    context(instance: DslComponent)
    override fun clear() { children.clear() }
}