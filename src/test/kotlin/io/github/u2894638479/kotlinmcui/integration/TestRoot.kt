package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.DslRuntime
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.input.PreeditInfo
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.MutRect

/**
 * The process owner root, driven the way the cases used to drive the deleted `TopComponent` object.
 *
 * This is a fixture, not a framework type: each member is a one-hop delegate into
 * [DslRuntime.ownerRoot] or the root's state. Production code takes the root through
 * `DslRuntime.ownerRoot` itself; nothing in `main` refers to this object.
 */
internal object TestRoot {
    private val root: DslRoot get() = DslRuntime.ownerRoot
    private val state get() = root.state

    /** The fixture backend seen as a root provider, the way production derives it from the published backend. */
    val provider get() = TestRootProvider

    val rect: MutRect get() = root.rect
    val children: DslChild.Group get() = root.children
    val instance: DslComponent get() = root.component.instance

    val focused: DslId? get() = state.focused
    var hovered: DslId?
        get() = state.hovered
        set(value) { state.setHovered(value) }
    var preedit: PreeditInfo
        get() = state.preedit
        set(value) { state.setPreedit(value) }
    var renderFailure: Throwable?
        get() = state.renderFailure
        set(value) { state.noteRenderFailure(value) }
    val frameIndex: Long get() = state.frameIndex

    fun setFocus(newFocus: DslId?, source: FocusSource = FocusSource.CODE) = state.setFocus(newFocus, source)

    fun globalPreedit(info: PreeditInfo) = root.component.globalPreedit(info)

    context(eventModifier: KeyModifiers, mouse: Position)
    fun mouseDown(mouseButton: MouseButton) = root.component.mouseDown(mouseButton)

    context(eventModifier: KeyModifiers)
    fun keyDown(key: Key, scanCode: Int) = root.component.keyDown(key, scanCode)

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    fun <RP> render() = root.component.render<RP>()
}
