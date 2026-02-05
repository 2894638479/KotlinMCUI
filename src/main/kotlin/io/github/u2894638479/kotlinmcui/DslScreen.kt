package io.github.u2894638479.kotlinmcui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.nextFocusable
import io.github.u2894638479.kotlinmcui.component.nextFocusableList
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
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
    constructor(dataStore: DslDataStore,dslFunction: DslFunction):this(
        DslScopeImpl(
            DslId(null),
            Modifier,
            DslContext(DslId(null), dataStore, DslChild.List(), dataStore),
            dslFunction,
        ),dataStore
    )

    fun close() { dataStore.onClose() }
    fun init(rect: Rect){
        this.rect.copyFrom(rect)
    }

    context(instance: DslComponent)
    override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
        dataStore.focused = testHit(mouse) { it.takeIf { it.focusable } }?.identity
        return delegate.mouseDown(mouse, mouseButton)
    }

    context(instance: DslComponent)
    override fun keyDown(key: Int, scanCode: Int, eventModifier: EventModifier): Boolean {
        if(delegate.keyDown(key, scanCode, eventModifier)) return true
        dataStore.focused = when(key) {
            GLFW.GLFW_KEY_LEFT -> instance.nextFocusableList(dataStore.focused,true) { it.run { viewHorizontal } }?.identity
            GLFW.GLFW_KEY_RIGHT -> instance.nextFocusableList(dataStore.focused) { it.run { viewHorizontal } }?.identity
            GLFW.GLFW_KEY_UP -> instance.nextFocusableList(dataStore.focused,true) { it.run { viewVertical } }?.identity
            GLFW.GLFW_KEY_DOWN -> instance.nextFocusableList(dataStore.focused) { it.run { viewVertical } }?.identity
            GLFW.GLFW_KEY_TAB -> if(eventModifier.shift) instance.nextFocusable(dataStore.focused) { it.run { viewSequential.asReversed() } }?.identity
                else instance.nextFocusable(dataStore.focused) { it.run { viewSequential } }?.identity
            else -> return false
        }
        return true
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        dataStore.newFrame()
        clear()
        build()
        layoutHorizontal()
        layoutVertical()
        dataStore.tooltip = testHit(mouse) { it.tooltip }
        dataStore.hovered = testHit(mouse) { it.takeIf { it.highlightable } }?.identity
        dataStore.narration = testHit(mouse) { it.narration }
        delegate.render(mouse)
    }
}