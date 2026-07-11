package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentEvent
import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.rect.copyFrom

val topComponent: DslComponent = object: DslScope {
    override val rect = MutRect()
    override val modifier get() = error("")
    override var instance: DslComponent get() = this
        set(value) {}
    override val identity get() = error("")
    override val scale get() = error("")
    override val children = DslChild.List().also {
        it.collect(dslBackend.create("Dsl Overlay") { DslEntryLoader.overlays() }.dslScreen)
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        children.forEach { it.rect copyFrom rect }
        children.asReversed().forEach { it.render() }
    }
}
