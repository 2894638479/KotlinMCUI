package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.component.nextFocusable
import io.github.u2894638479.kotlinmcui.component.nextFocusableList
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.Overlay
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.math.maxOf
import io.github.u2894638479.kotlinmcui.math.minOf
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.vertices
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.modifier.Modifier

/**
 * The top-level screen component that orchestrates the build-layout-render cycle.
 *
 * Each frame, [render] calls [build] to reconstruct the entire UI tree (including
 * overlay collection and tooltip creation), detects hover, and dispatches rendering
 * to children. Focus navigation (arrow keys, Tab, Home, End) is handled in [keyDown].
 */
class DslScreen private constructor(
    val dataStore: DslDataStore,
    val dslFunction: DslFunction,
    val delegate: DslScope
) : DslScope by delegate {
    constructor(dataStore: DslDataStore,dslFunction: DslFunction):this(
        dataStore,dslFunction,
        with(DslContext(dataStore)) {
            withScale(0.0) {
                DslScopeImpl(DslId(null), Modifier, this,{})
            }
        },
    )

    fun close() { dataStore.onClose() }

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton): Boolean {
        dataStore.focused = instance.testHit(mouse) { it.takeIf { it.focusable } }?.identity
        return delegate.mouseDown(mouseButton)
    }

    context(eventModifier: KeyModifiers)
    override fun keyDown(key: Key, scanCode: Int): Boolean {
        if(delegate.keyDown(key, scanCode)) return true
        val focused = when(key) {
            Key.LEFT -> instance.nextFocusableList(dataStore.focused,true) { it.viewHorizontal }
            Key.RIGHT -> instance.nextFocusableList(dataStore.focused) { it.viewHorizontal }
            Key.UP -> instance.nextFocusableList(dataStore.focused,true) { it.viewVertical }
            Key.DOWN -> instance.nextFocusableList(dataStore.focused) { it.viewVertical }
            Key.TAB -> if(eventModifier.shift) instance.nextFocusable(dataStore.focused) { it.viewSequential.asReversed() }
                else instance.nextFocusable(dataStore.focused) { it.viewSequential }
            Key.HOME -> instance.nextFocusable(null) { it.viewSequential }
            Key.END -> instance.nextFocusable(null) { it.viewSequential.asReversed() }
            else -> return false
        }
        if(dataStore.focused != focused?.identity) {
            dataStore.focused = focused?.identity
            dataStore.keyboardNarration = focused?.narration
            return true
        }
        return false
    }

    override fun globalFocusChanged(newFocus: DslId?) {
        fun DslChild.Group.f():Unit = forEach { it.children.f();it.globalFocusChanged(newFocus) }
        instance.children.f()
    }

    override fun globalHoverChanged(newHover: DslId?) {
        fun DslChild.Group.f():Unit = forEach { it.children.f();it.globalHoverChanged(newHover) }
        instance.children.f()
    }

    init { instance = this }


    private var overlays = DslChild.Group()
    override var children = DslChild.Group()
    override fun build() {
        children = DslChild.Group()
        overlays = DslChild.Group()
        val ctx = DslContext(dataStore)
        context(ctx) {
            ctx.withScale(dataStore.scale) {
                ctx.withIdentity(DslId(null)) {
                    var overlays = DslChild.Group()
                    ctx.withChildren(children) {
                        ctx.withOverlays(overlays) {
                            dslFunction()
                            children.forEach {
                                it.attachInstance()
                                it.build()
                            }
                        }
                    }
                    layoutHorizontal()
                    layoutVertical()
                    do {
                        this.overlays.collectAll(overlays)
                        val temp = overlays
                        overlays = DslChild.Group()
                        ctx.withOverlays(overlays) {
                            temp.forEach {
                                it.attachInstance()
                                it.build()
                                it.layoutHorizontal()
                                it.layoutVertical()
                            }
                        }
                    } while (overlays.isNotEmpty())


                    children = DslChild.Group().also { it.collectAll(this.overlays + children) }
                    val tooltips = DslChild.Group()
                    ctx.withOverlays(tooltips) {
                        Tooltip()
                    }

                    tooltips.forEach {
                        it.attachInstance()
                        it.build()
                        it.layoutHorizontal()
                        it.layoutVertical()
                    }
                    children = DslChild.Group().apply { collectAll(tooltips + children) }
                }
            }
        }
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() = dataStore.frame { buildFinish ->
        build()

        val hovered = instance.testHit(mouse) { it.takeIf { it.highlightable } }
        dataStore.hovered = hovered?.identity
        dataStore.mouseNarration = hovered?.narration ?: instance.testHit(mouse) { it.takeIf { it.narratable } }?.narration
        buildFinish()
        delegate.render()
    }

    context(ctx: DslContext)
    private fun Tooltip() {
        var focusedRect: Rect? = null
        var rawFocusedRect: Rect? = null
        var transforms = Transform.empty
        val hovered = dataStore.dslScreen.testHit(dataStore.mouse) { it.tooltip }
        val focusedId = dataStore.focused
        val focused = if(focusedId == null) null else dataStore.dslScreen.testHit { it.takeIf { it.identity == focusedId } }
        val func = hovered ?: focused?.tooltip ?: return
        if(focusedId != null && hovered == null) focusedRect = run {
            fun getTransform(component: DslComponent): Transform? {
                if (component.identity == focusedId) return component.transform
                val child = component.children.firstNotNullOfOrNull { getTransform(it) } ?: return null
                return component.transform * child
            }

            val transform = getTransform(dataStore.dslScreen) ?: return@run null
            val rect = focused?.rect ?: return@run null
            if (transform.isEmpty) return@run rect
            val vertices = rect.vertices.map { transform * it }
            if (dataStore.debug) {
                transforms = transform
                rawFocusedRect = rect
            }
            Rect(vertices.minOf { it.x }, vertices.minOf { it.y }, vertices.maxOf { it.x }, vertices.maxOf { it.y })
        }
        val outline: Rect = ctx.dataStore.dslScreen.rect
        val align = focusedRect?.let { OverlayAlign.AutoToRect(it,outline) } ?: OverlayAlign.AutoToPoint(dataStore.mouse,outline)
        Overlay(Modifier,align) { func() }
    }
}