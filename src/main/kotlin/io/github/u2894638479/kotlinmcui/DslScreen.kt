package io.github.u2894638479.kotlinmcui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.nextFocusable
import io.github.u2894638479.kotlinmcui.component.nextFocusableList
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslTopContext
import io.github.u2894638479.kotlinmcui.functions.DslTopFunction
import io.github.u2894638479.kotlinmcui.functions.ui.MouseTipComponent
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import org.lwjgl.glfw.GLFW

class DslScreen private constructor(
    val delegate: DslScope,
    val dataStore: DslDataStore,
) : DslScope by delegate {
    constructor(dataStore: DslDataStore,dslFunction: DslTopFunction):this(
        Unit.run {
            val id = DslId(dataStore.title)
            val ctx = DslContext(id, dataStore, DslChild.List(), dataStore)
            val delegate = DslScopeImpl(id, Modifier, ctx,{})
            object: DslScope by delegate {
                context(instance: DslComponent)
                override fun build() {
                    dataStore.onClose = dataStore.defaultOnClose
                    context(
                        DslTopContext(instance.identity, dataStore, delegate.children, dataStore){
                            val onClose = dataStore.onClose
                            dataStore.onClose = { it(ctx) { onClose() } }
                        }, dslFunction
                    )
                    children.forEach { it.run { build() } }
                }
            }
        },dataStore
    )

    fun close() { dataStore.onClose() }
    fun init(rect: Rect){
        this.rect.copyFrom(rect)
    }

    context(instance: DslComponent, eventModifier: EventModifier)
    override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
        dataStore.focused = testHit(mouse) { it.takeIf { it.focusable } }?.identity
        return delegate.mouseDown(mouse, mouseButton)
    }

    context(instance: DslComponent)
    override fun mouseMove(mouse: Position) {
        dataStore.mouse = mouse
        return delegate.mouseMove(mouse)
    }

    context(instance: DslComponent, eventModifier: EventModifier)
    override fun keyDown(key: Int, scanCode: Int): Boolean {
        if(delegate.keyDown(key, scanCode)) return true
        val focused = when(key) {
            GLFW.GLFW_KEY_LEFT -> instance.nextFocusableList(dataStore.focused,true) { it.run { viewHorizontal } }
            GLFW.GLFW_KEY_RIGHT -> instance.nextFocusableList(dataStore.focused) { it.run { viewHorizontal } }
            GLFW.GLFW_KEY_UP -> instance.nextFocusableList(dataStore.focused,true) { it.run { viewVertical } }
            GLFW.GLFW_KEY_DOWN -> instance.nextFocusableList(dataStore.focused) { it.run { viewVertical } }
            GLFW.GLFW_KEY_TAB -> if(eventModifier.shift) instance.nextFocusable(dataStore.focused) { it.run { viewSequential.asReversed() } }
                else instance.nextFocusable(dataStore.focused) { it.run { viewSequential } }
            GLFW.GLFW_KEY_HOME -> instance.nextFocusable(null) { it.run { viewSequential } }
            GLFW.GLFW_KEY_END -> instance.nextFocusable(null) { it.run { viewSequential.asReversed() } }
            else -> return false
        }
        dataStore.focused = focused?.run { identity }
        dataStore.keyboardNarration = focused?.run { narration }
        return true
    }

    context(instance: DslComponent)
    override fun build() {
        delegate.build()
        children.sortBy { it !is MouseTipComponent }
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        dataStore.newFrame()
        clear()
        build()
        layoutHorizontal()
        layoutVertical()
        dataStore.tooltip = testHit(mouse) { it.tooltip }
        val hovered = testHit(mouse) { it.takeIf { it.highlightable } }
        dataStore.hovered = hovered?.identity
        dataStore.mouseNarration = hovered?.narration ?: testHit(mouse) { it.takeIf { it.narratable } }?.narration
        delegate.render(mouse)
    }
}