package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.component.relativeTransform
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.decorator.background
import io.github.u2894638479.kotlinmcui.dsl.decorator.outline
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.dsl.ui.Overlay
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.transformedMaxBound
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding

/**
 * The top-level screen component that orchestrates the build-layout-render cycle.
 */
class DslScreen private constructor(
    val dataStore: DslDataStore,
    val dslFunction: DslFunction,
    val delegate: DslScope
) : DslScope by delegate {
    constructor(dataStore: DslDataStore, dslFunction: DslFunction) : this(
        dataStore, dslFunction,
        with(DslContext(dataStore)) {
            withScale(0.0) {
                DslScopeImpl(DslId(dataStore), Modifier, this, {})
            }
        },
    )

    fun close() { dataStore.onClose() }

    init { instance = this }

    private var overlays = DslChild.Group()
    override var children = DslChild.Group()

    override fun build() {
        children = DslChild.Group()
        overlays = DslChild.Group()
        val ctx = DslContext(dataStore)
        context(ctx) {
            ctx.withScale(dataStore.scale) {
                ctx.withIdentity(identity) {
                    val initialOverlays = buildMainTree()
                    buildOverlayTree(initialOverlays)
                    buildTooltips()
                }
            }
        }
    }

    /**
     * First phase: execute the DSL function, attach instances, run build and
     * layout on all children. Returns overlays created during this phase.
     */
    context(ctx: DslContext)
    private fun buildMainTree(): DslChild.Group {
        val batch = DslChild.Group()
        ctx.withChildren(children) {
            ctx.withOverlays(batch) {
                dslFunction()
                children.forEach {
                    it.attachInstance()
                    it.build()
                }
            }
        }
        layoutHorizontal()
        layoutVertical()
        return batch
    }

    /**
     * Second phase: iteratively build and layout overlay components. Overlay
     * builds can create more overlays, so this loops until no new overlays
     * are produced.
     */
    context(ctx: DslContext)
    private fun buildOverlayTree(initial: DslChild.Group) {
        overlays.collectAll(initial)
        var batch = initial
        while (batch.isNotEmpty()) {
            val next = DslChild.Group()
            ctx.withOverlays(next) {
                batch.forEach {
                    it.attachInstance()
                    it.build()
                    it.layoutHorizontal()
                    it.layoutVertical()
                }
            }
            overlays.collectAll(next)
            batch = next
        }
    }

    /**
     * Third phase: build tooltip-layer components (IME preedit box and
     * hover/focus tooltip). These are rendered on top of everything else.
     */
    context(ctx: DslContext)
    private fun buildTooltips() {
        children = DslChild.Group().also { it.collectAll(overlays + children) }
        val tooltips = DslChild.Group()
        ctx.withOverlays(tooltips) {
            ImeBox()
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

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() = dataStore.frame { buildFinish ->
        build()
        buildFinish()
        delegate.render()
    }

    /**
     * Render the default IME preedit preview box in the overlay layer,
     * positioned at the focused component's preedit cursor.
     */
    context(ctx: DslContext)
    private fun ImeBox() {
        val component = testHit { it.takeIf { it.identity == TopComponent.focused } } ?: return
        val rect = component.preeditArea?.transformedMaxBound(component.relativeTransform(this) ?: return) ?: return
        if (TopComponent.preedit.isEmpty) return
        if (component.selfRenderPreedit) return
        Overlay(Modifier, OverlayAlign.AutoToRect(rect, this.rect)) {
            Box {
                TextFlatten(Modifier.padding(3.scaled)) { TopComponent.preedit.toString().emit(color = Color.BLACK) }
            }.background(Color.WHITE).outline(color = Color.GREEN).change {
                object : DslComponent by it {
                    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
                    override fun <RP> render() {
                        it.render()
                        dslBackend.setPreeditArea(instance.rect)
                    }
                }
            }
        }
    }

    /**
     * Render a tooltip near the hovered or focused component. If a component
     * is focused but not hovered, the tooltip anchors to the component's rect.
     * Otherwise it anchors to the mouse position.
     */
    context(ctx: DslContext)
    private fun Tooltip() {
        val hovered = testHit(dslBackend.mouse) { it.tooltip }
        val focusedId = TopComponent.focused
        val focused = if (focusedId == null) null else testHit { it.takeIf { it.identity == focusedId } }
        val func = hovered ?: focused?.tooltip ?: return
        val focusedRect = if (focusedId != null && hovered == null) run {
            focused?.rect?.transformedMaxBound(focused.relativeTransform(this) ?: return@run null)
        } else null
        val outline: Rect = rect
        val align = focusedRect?.let { OverlayAlign.AutoToRect(it, outline) }
            ?: OverlayAlign.AutoToPoint(dslBackend.mouse, outline)
        Overlay(Modifier, align) { func() }
    }
}
