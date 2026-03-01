package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.modifier.Modifier

class DslComponentImpl(
    override val identity: DslId,
    override val modifier: Modifier,
    override val rect: MutRect = MutRect()
): DslComponent {
    private var _instance: DslComponent? = null
    override var instance: DslComponent get() = _instance ?: error("using `instance` before initialize")
        set(value) { _instance = value }
}