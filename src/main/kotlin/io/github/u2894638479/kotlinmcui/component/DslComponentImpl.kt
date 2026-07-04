package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.modifier.Modifier

class DslComponentImpl(
    override val identity: DslId,
    override val modifier: Modifier,
    ctx: DslContext,
    override val rect: MutRect = MutRect()
): DslComponent {
    override val scale = ctx.scale
    private var _instance: DslComponent? = null
    override var instance: DslComponent get() = _instance ?: error("using `instance` before initialize")
        set(value) { if(_instance == null) _instance = value else error("`instance` is set twice") }
}