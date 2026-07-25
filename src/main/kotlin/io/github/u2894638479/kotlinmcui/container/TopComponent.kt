package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.component.nextFocusable
import io.github.u2894638479.kotlinmcui.component.nextFocusableList
import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.input.PreeditInfo
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.math.rect.copyFrom

/**
 * The singleton root of the entire component tree.
 *
 * ### Backend convention
 * The backend must, each frame, set [rect] to the full screen dimensions
 * and call [DslComponent.render] on this component. The render method copies
 * the rect to all overlay children before dispatching rendering.
 *
 * [modifier], [identity], and [scale] are not meaningful at the root level
 * and will throw if accessed.
 */
object TopComponent: DslScope {
    override val rect = MutRect()
    override val modifier get() = error("modifier is not available on topComponent")
    override var instance: DslComponent get() = this
        set(value) {}
    override val identity get() = error("identity is not available on topComponent")
    override val scale get() = error("scale is not available on topComponent")
    override val children by lazy {
        DslChild.Group().also {
            it.collect(dslBackend.create("Dsl Overlay") { DslEntryLoader.overlays() }.dslScreen)
        }
    }

    override fun globalFocusChanged(newFocus: DslId?, source: FocusSource) {
        fun DslChild.Group.f():Unit = forEach { it.children.f();it.globalFocusChanged(newFocus, source) }
        instance.children.f()
    }

    override fun globalHoverChanged(newHover: DslId?) {
        fun DslChild.Group.f():Unit = forEach { it.children.f();it.globalHoverChanged(newHover) }
        instance.children.f()
    }

    var focused: DslId? = null
        private set

    internal fun setFocus(newFocus: DslId?, source: FocusSource) {
        focused = newFocus
        globalFocusChanged(newFocus, source)
    }
    var hovered: DslId? = null
        internal set(value) {
            field = value
            globalHoverChanged(value)
        }
    var keyboardNarration: String? = null
        internal set(value) {
            if(value != field) {
                field = value
                value?.let { dslBackend.narrate(it) }
            }
        }
    var mouseNarration: String? = null
        internal set(value) {
            if(value != field) {
                field = value
                value?.let { dslBackend.narrate(it) }
            }
        }
    val mouse get() = dslBackend.mouse

    /** The current IME preedit state. Updated by [globalPreedit]. */
    var preedit = PreeditInfo.empty
        internal set

    /** Whether the currently focused component accepts IME input (has a non-null [DslComponentMetadata.preeditArea]). */
    val preeditUsing get() = testHit { it.takeIf { it !== this && it.identity == focused } }?.preeditArea != null

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton): Boolean {
        setFocus(instance.testHit(mouse) { it.takeIf { it.focusable } }?.identity, FocusSource.MOUSE)
        return super.mouseDown(mouseButton)
    }

    context(eventModifier: KeyModifiers)
    override fun keyDown(key: Key, scanCode: Int): Boolean {
        if(super.keyDown(key, scanCode)) return true
        val next = when(key) {
            Key.LEFT -> instance.nextFocusableList(focused,true) { it.viewHorizontal }
            Key.RIGHT -> instance.nextFocusableList(focused) { it.viewHorizontal }
            Key.UP -> instance.nextFocusableList(focused,true) { it.viewVertical }
            Key.DOWN -> instance.nextFocusableList(focused) { it.viewVertical }
            Key.TAB -> if(eventModifier.shift) instance.nextFocusable(focused) { it.viewSequential.asReversed() }
            else instance.nextFocusable(focused) { it.viewSequential }
            Key.HOME -> instance.nextFocusable(null) { it.viewSequential }
            Key.END -> instance.nextFocusable(null) { it.viewSequential.asReversed() }
            else -> return false
        }
        if(this.focused != next?.identity) {
            setFocus(next?.identity, FocusSource.KEYBOARD)
            this.keyboardNarration = next?.narration
            return true
        }
        return false
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        children.forEach { it.rect copyFrom rect }
        children.asReversed().forEach { it.render() }
        val hovered = instance.testHit(mouse) { it.takeIf { it.highlightable } }
        this.hovered = hovered?.identity
        mouseNarration = hovered?.narration ?: instance.testHit(mouse) { it.takeIf { it.narratable } }?.narration
    }

    /**
     * Dispatch IME preedit state. When [info] is empty, broadcast to all
     * components (composition ended). When non-empty, send only to the
     * focused component. If the component does not have [selfRenderPreedit],
     * also notify the platform of the cursor position via [setPreeditArea].
     */
    override fun globalPreedit(info: PreeditInfo) {
        preedit = info
        if(info.isEmpty) testHit {
            if(it !== this) { it.globalPreedit(info) };null
        } else {
            val component = testHit { it.takeIf { it.identity == focused } } ?: return
            component.globalPreedit(info)
            if(component.selfRenderPreedit) component.preeditArea?.let { dslBackend.setPreeditArea(it) }
        }
    }
}
