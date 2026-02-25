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
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.copyFrom
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
                override fun build(instance: DslComponent) {
                    dataStore.onClose = dataStore.defaultOnClose
                    val children = instance.children ?: return
                    context(
                        DslTopContext(instance.identity, dataStore, children, dataStore){
                            val onClose = dataStore.onClose
                            dataStore.onClose = { it(ctx) { onClose() } }
                        }, dslFunction
                    )
                    children.forEach { it.build(it) }
                }
            }
        },dataStore
    )

    fun close() { dataStore.onClose() }
    fun init(rect: Rect){
        this.rect.copyFrom(rect)
    }

    context(eventModifier: EventModifier, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton): Boolean {
        dataStore.focused = instance.testHit(mouse) { it.takeIf { it.focusable } }?.identity
        return delegate.mouseDown(mouseButton)
    }

    context(mouse: Position)
    override fun mouseMove() {
        dataStore.mouse = mouse
        return delegate.mouseMove()
    }

    context(eventModifier: EventModifier)
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

    override fun build(instance: DslComponent) {
        delegate.build(instance)
        instance.children?.sortBy { it !is MouseTipComponent }
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        dataStore.newFrame()
        instance.children?.clear()
        build(instance)
        layoutHorizontal()
        layoutVertical()
        val hovered = instance.testHit(mouse) { it.takeIf { it.highlightable } }
        dataStore.hovered = hovered?.identity
        dataStore.mouseNarration = hovered?.narration ?: instance.testHit(mouse) { it.takeIf { it.narratable } }?.narration
        delegate.render()
    }
}