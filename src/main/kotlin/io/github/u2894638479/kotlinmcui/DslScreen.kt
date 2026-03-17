package io.github.u2894638479.kotlinmcui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.*
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslFrameContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslOnCloseContext
import io.github.u2894638479.kotlinmcui.context.DslTopContext
import io.github.u2894638479.kotlinmcui.functions.DslTopFunction
import io.github.u2894638479.kotlinmcui.functions.executeContext
import io.github.u2894638479.kotlinmcui.functions.ui.MouseTipComponent
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.maxOf
import io.github.u2894638479.kotlinmcui.math.minOf
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.*
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.math.transform.Transform.Companion.isEmpty
import io.github.u2894638479.kotlinmcui.math.transform.Transform.Companion.plus
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslChild.Companion.buildThis
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import org.lwjgl.glfw.GLFW

class DslScreen private constructor(
    val dataStore: DslDataStore,
    val dslFunction: DslTopFunction,
    val delegate: DslScope
) : DslScope by delegate {
    constructor(dataStore: DslDataStore,dslFunction: DslTopFunction):this(
        dataStore,dslFunction,DslScopeImpl(DslId(null), Modifier,
            DslContext(DslId(null),dataStore, DslChild.List.empty,dataStore,
                DslFrameContext(ULong.MAX_VALUE,0L)),{}),
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

    override fun globalFocusChanged(newFocus: DslId?) {
        fun DslChild.List.f():Unit = forEach { it.children.f();it.globalFocusChanged(newFocus) }
        instance.children.f()
    }

    override fun globalHoverChanged(newHover: DslId?) {
        fun DslChild.List.f():Unit = forEach { it.children.f();it.globalHoverChanged(newHover) }
        instance.children.f()
    }

    init { instance = this }

    private val frameContext = object: DslFrameContext {
        override var frameBeginNano = 0L
        override var frameIndex = ULong.MAX_VALUE
    }

    override fun build() {
        dataStore.onClose = dataStore.defaultOnClose
        val buildChildren = DslChild.List()
        DslTopContext(identity, dataStore, buildChildren, dataStore, frameContext){
            val defaultOnClose = dataStore.onClose
            val executeContext = context(DslIdContext(identity), DslDataStoreContext(dataStore)) { executeContext }
            val onCloseContext = object : DslOnCloseContext, DslExecuteContext by executeContext {
                override fun defaultOnClose() = defaultOnClose()
            }
            dataStore.onClose = { it(onCloseContext) }
        }.run { dslFunction() }
        buildChildren.forEach { it.attachInstance();it.build() }

        val children = instance.children
        var list = children.toSet()
        while(list.isNotEmpty()) {
            list.forEach { it.attachInstance();it.build() }
            list = children.subtract(list)
        }
        buildChildren.forEach { children.collect(it) }
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        frameContext.frameIndex++
        frameContext.frameBeginNano = System.nanoTime()
        instance.children.clear()
        val tooltipPlaceHolder = DslComponentImpl(DslId(Unit),Modifier)
        val tooltip = instance.children.collect(tooltipPlaceHolder)
        build()
        layoutHorizontal()
        layoutVertical()
        tooltip.change(buildTooltip().apply { attachInstance();build();layoutHorizontal();layoutVertical() })

        val hovered = instance.testHit(mouse) { it.takeIf { it.highlightable } }
        dataStore.hovered = hovered?.identity
        dataStore.mouseNarration = hovered?.narration ?: instance.testHit(mouse) { it.takeIf { it.narratable } }?.narration
        delegate.render()
    }

    private fun buildTooltip() = run {
        val ctx = DslContext(identity,dataStore,instance.children,dataStore,frameContext)
        val delegate = DslScopeImpl(identity + {},Modifier,ctx,{})
        object : DslComponent by delegate, MouseTipComponent {
            var focusedRect: Rect? = null
            override fun build() {
                val hovered = dataStore.dslScreen.testHit(dataStore.mouse) { it.tooltip }
                val focusedId = dataStore.focused
                val focused = if(focusedId == null) null else dataStore.dslScreen.testHit { it.takeIf { it.identity == focusedId } }
                val func = hovered ?: focused?.tooltip ?: return
                if(focusedId != null && hovered == null) focusedRect = run {
                    fun getTransform(component: DslComponent): Transform? {
                        if(component.identity == focusedId) return component.transform
                        val child = component.children.firstNotNullOfOrNull { getTransform(it) } ?: return null
                        return component.transform + child
                    }
                    val transform = getTransform(dataStore.dslScreen) ?: return@run null
                    val rect = focused?.rect ?: return@run null
                    if(transform.isEmpty) return@run rect
                    val vertices = rect.vertices.map { transform.transform(it) }
                    Rect(vertices.minOf { it.x },vertices.minOf { it.y },vertices.maxOf { it.x },vertices.maxOf { it.y })
                }

                instance.children.buildThis(ctx,func)
                instance.children.forEach { it.attachInstance();it.build() }
            }
            override fun layoutHorizontal() {
                val focusedRect = focusedRect
                val boundH = if(focusedRect != null) layoutFocusH(focusedRect, dataStore.dslScreen.rect,instance.childrenMaxWidth)
                else layoutMouseH(dataStore.mouse, dataStore.dslScreen.rect,instance.childrenMaxWidth)
                instance.rect.bound(Axis.Horizontal) copyFrom boundH
                delegate.layoutHorizontal()
            }
            override fun layoutVertical() {
                val focusedRect = focusedRect
                val boundV = if(focusedRect != null) layoutFocusV(focusedRect, dataStore.dslScreen.rect,instance.childrenMaxHeight)
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
    }
}