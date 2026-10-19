package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
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
 * The fake backend's own root.
 *
 * This is the fixture's root, not the framework's: the runtime no longer hands any root out, so the
 * fixture builds one from the fake platform's pieces and every one-hop delegate below reads it.
 * Nothing in `main` refers to this object.
 */
internal object TestRoot {
    internal val root: DslRoot by lazy { DslRoot(TestInput, TestRenderer, TestScreenFactory, TestUtils) }
    private val state get() = root.state

    /**
     * A fresh root with the fake platform's pieces.
     *
     * Cases that need a second (or throwaway) tree build one of these directly instead of asking the
     * runtime for the process root.
     */
    fun newRoot(): DslRoot = DslRoot(
        input = TestInput,
        renderer = TestRenderer,
        screenFactory = TestScreenFactory,
        rootUtils = TestUtils,
    )

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

    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    fun <RP> render() = root.component.render<RP>()
}
