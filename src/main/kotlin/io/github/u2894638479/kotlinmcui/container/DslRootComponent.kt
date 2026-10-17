package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.component.nextFocusable
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.input.PreeditInfo
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.copyFrom

/**
 * The component that represents one root in the tree.
 *
 * It is not the root: the root owns the state, the viewport and the children, while this component is
 * the node the tree walks, the focus broadcast enters, and the backend drives. One root has exactly
 * one of these for its whole lifetime - that stability is what `instance` and the focused identity
 * rest on, so it must never be rebuilt while the root lives.
 *
 * Every piece of state it reads or writes goes through [DslRoot.state]; the tree it walks is
 * [DslRoot.children].
 */
internal class DslRootComponent(private val root: DslRoot) : DslScope {
    private val state get() = root.state

    override val rect get() = root.geometry.rect
    override val modifier get() = error("modifier is not available on a root component")
    override var instance: DslComponent get() = this
        set(value) {}
    override val identity get() = error("identity is not available on a root component")
    override val scale get() = error("scale is not available on a root component")
    override val children get() = root.children

    override fun globalFocusChanged(newFocus: DslId?, source: FocusSource) {
        fun DslChild.Group.f():Unit = forEach { it.children.f();it.globalFocusChanged(newFocus, source) }
        instance.children.f()
    }

    override fun globalHoverChanged(newHover: DslId?) {
        fun DslChild.Group.f():Unit = forEach { it.children.f();it.globalHoverChanged(newHover) }
        instance.children.f()
    }

    /** Whether the currently focused component accepts IME input (it has a [preeditArea]). */
    val preeditUsing get() = testHit { it.takeIf { it !== this && it.identity == state.focused } }?.preeditArea != null

    val mouse get() = dslBackend.mouse

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton): Boolean {
        acquireFocusFromInput { newFocus, source -> state.setFocus(newFocus, source) }
        return super.mouseDown(mouseButton)
    }

    context(eventModifier: KeyModifiers)
    override fun keyDown(key: Key, scanCode: Int): Boolean {
        if(super.keyDown(key, scanCode)) return true
        val next = when(key) {
            Key.LEFT -> instance.nextFocusable(state.focused,true) { it.viewHorizontal }
            Key.RIGHT -> instance.nextFocusable(state.focused) { it.viewHorizontal }
            Key.UP -> instance.nextFocusable(state.focused,true) { it.viewVertical }
            Key.DOWN -> instance.nextFocusable(state.focused) { it.viewVertical }
            Key.TAB -> if(eventModifier.shift) instance.nextFocusable(state.focused) { it.viewSequential.asReversed() }
            else instance.nextFocusable(state.focused) { it.viewSequential }
            Key.HOME -> instance.nextFocusable(null) { it.viewSequential }
            Key.END -> instance.nextFocusable(null) { it.viewSequential.asReversed() }
            else -> return false
        }
        if(state.focused != next?.identity) {
            state.setFocus(next?.identity, FocusSource.KEYBOARD)
            state.noteKeyboardNarration(next?.narration)
            return true
        }
        return false
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        state.advanceFrame()
        children.forEach { it.rect copyFrom rect }
        children.asReversed().forEach { child ->
            try {
                child.render()
            } catch(e: Throwable) {
                // Recorded for the preview session and rethrown: a development tool watching must
                // not change what the platform does with a broken page.
                state.noteRenderFailure(e)
                throw e
            }
        }
        val hovered = instance.testHit(mouse) { it.takeIf { it.highlightable } }
        state.setHovered(hovered?.identity)
        state.noteMouseNarration(hovered?.narration ?: instance.testHit(mouse) { it.takeIf { it.narratable } }?.narration)
    }

    /**
     * Dispatch IME preedit state. When [info] is empty, broadcast to all
     * components (composition ended). When non-empty, send only to the
     * focused component. If the component does not have [selfRenderPreedit],
     * also notify the platform of the cursor position via [setPreeditArea].
     */
    override fun globalPreedit(info: PreeditInfo) {
        state.setPreedit(info)
        if(info.isEmpty) testHit {
            if(it !== this) { it.globalPreedit(info) };null
        } else {
            val component = testHit { it.takeIf { it !== this && it.identity == state.focused } } ?: return
            component.globalPreedit(info)
            if(component.selfRenderPreedit) component.preeditArea?.let { dslBackend.setPreeditArea(it) }
        }
    }
}
