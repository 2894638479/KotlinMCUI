package io.github.u2894638479.kotlinmcui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.component.nextFocusable
import io.github.u2894638479.kotlinmcui.component.nextFocusableList
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslTopContext
import io.github.u2894638479.kotlinmcui.functions.DslTopFunction
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.functions.ui.MouseTipComponent
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.MutBound
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.copyFrom
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.width
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
                override fun build() {
                    dataStore.onClose = dataStore.defaultOnClose
                    val children = instance.children
                    DslTopContext(instance.identity, dataStore, children, dataStore){
                        val onClose = dataStore.onClose
                        dataStore.onClose = { it(ctx) { onClose() } }
                    }.run { dslFunction() }
                    children.forEach { it.attachInstance();it.build() }
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
            GLFW.GLFW_KEY_LEFT -> instance.nextFocusableList(dataStore.focused,true) { it.viewHorizontal }
            GLFW.GLFW_KEY_RIGHT -> instance.nextFocusableList(dataStore.focused) { it.viewHorizontal }
            GLFW.GLFW_KEY_UP -> instance.nextFocusableList(dataStore.focused,true) { it.viewVertical }
            GLFW.GLFW_KEY_DOWN -> instance.nextFocusableList(dataStore.focused) { it.viewVertical }
            GLFW.GLFW_KEY_TAB -> if(eventModifier.shift) instance.nextFocusable(dataStore.focused) { it.viewSequential.asReversed() }
                else instance.nextFocusable(dataStore.focused) { it.viewSequential }
            GLFW.GLFW_KEY_HOME -> instance.nextFocusable(null) { it.viewSequential }
            GLFW.GLFW_KEY_END -> instance.nextFocusable(null) { it.viewSequential.asReversed() }
            else -> return false
        }
        dataStore.focused = focused?.identity
        dataStore.keyboardNarration = focused?.narration
        return true
    }

    init { instance = this }
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        dataStore.newFrame()
        instance.children.clear()
        build()
        layoutHorizontal()
        layoutVertical()
        buildTooltip().currentComponent().run { attachInstance();build();layoutHorizontal();layoutVertical() }
        instance.children.sortBy { it !is MouseTipComponent }
        val hovered = instance.testHit(mouse) { it.takeIf { it.highlightable } }
        dataStore.hovered = hovered?.identity
        dataStore.mouseNarration = hovered?.narration ?: instance.testHit(mouse) { it.takeIf { it.narratable } }?.narration
        delegate.render()
    }

    private fun buildTooltip() = run {
        val ctx = DslContext(identity,dataStore,instance.children,dataStore)
        val delegate = DslScopeImpl(identity + {},Modifier,ctx,{})
        instance.children.collect(
            object : DslComponent by delegate, MouseTipComponent {
                var focused: DslComponent? = null
                override fun build() {
                    val focused = dataStore.focused?.let { id ->
                        dataStore.dslScreen.run {
                            testHit { it.takeIf { it.identity == id } }?.takeIf { it.focusable }
                        }
                    }
                    val hovered = dataStore.dslScreen.run {
                        testHit { it.takeIf { dataStore.mouse in it.rect }?.tooltip }
                    }
                    val func = focused?.tooltip ?: hovered ?: return

                    instance.children.buildThis(ctx,func)
                    instance.children.forEach { it.attachInstance();it.build() }
                    this.focused = focused?.takeIf { it.tooltip != null && it != hovered }
                }
                override fun layoutHorizontal() {
                    val focused = focused
                    val useFocus = focused != null
                    val boundH = if(useFocus) layoutFocusH(focused.rect, dataStore.dslScreen.rect,instance.childrenMaxWidth)
                    else layoutMouseH(dataStore.mouse, dataStore.dslScreen.rect,instance.childrenMaxWidth)
                    instance.rect.bound(Axis.Horizontal) copyFrom boundH
                    delegate.layoutHorizontal()
                }
                override fun layoutVertical() {
                    val focused = focused
                    val useFocus = focused != null
                    val boundV = if(useFocus) layoutFocusV(focused.rect, dataStore.dslScreen.rect,instance.childrenMaxHeight)
                    else layoutMouseV(dataStore.mouse, dataStore.dslScreen.rect,instance.childrenMaxHeight)
                    instance.rect.bound(Axis.Vertical) copyFrom boundV
                    delegate.layoutVertical()
                }

                fun layoutMouseH(mouse: Position, screen: Rect, width: Measure) = MutBound(0.px,0.px).also {
                    it.low = when {
                        screen.right - mouse.x >= width -> mouse.x
                        screen.left + width <= mouse.x -> mouse.x - width
                        else -> screen.right - width
                    }
                    it.high = it.low + width
                }
                fun layoutMouseV(mouse: Position, screen: Rect, height: Measure) = MutBound(0.px,0.px).also {
                    it.low = when {
                        mouse.y - screen.top >= height -> mouse.y - height
                        mouse.y + height <= screen.bottom -> mouse.y
                        else -> screen.top
                    }
                    it.high = it.low + height
                }

                fun layoutFocusH(focused: Rect, screen: Rect, width: Measure) = MutBound(0.px,0.px).also {
                    it.low = if(screen.right - focused.left >= width) focused.left else max(screen.left,focused.right - width)
                    it.high = it.low + width
                }
                fun layoutFocusV(focused: Rect, screen: Rect, height: Measure) = MutBound(0.px,0.px).also {
                    it.low = if(screen.bottom - focused.bottom >= height) focused.bottom else max(screen.top,focused.top - height)
                    it.high = it.low + height
                }
            }
        )
    }
}