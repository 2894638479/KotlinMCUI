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
                DslScopeImpl(DslId(dataStore), Modifier, this,{})
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
            }
        }
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() = dataStore.frame { buildFinish ->
        build()
        buildFinish()
        delegate.render()
    }

    /**
     * Render the default IME preedit preview box.
     *
     * Only shown when the focused component accepts IME input
     * ([DslComponentMetadata.imeArea] is non-null) but does not handle
     * rendering internally ([DslComponentMetadata.selfRenderPreedit] is false).
     * The box is rendered in the overlay layer so it is not clipped by
     * parent scissor or constrained by component bounds.
     */
    context(ctx: DslContext)
    private fun ImeBox() {
        val component = testHit { it.takeIf { it.identity == TopComponent.focused } } ?: return
        val rect = component.imeArea?.transformedMaxBound(component.relativeTransform(this) ?: return) ?: return
        if(TopComponent.preedit.isEmpty) return
        if(component.selfRenderPreedit) return
        Overlay(Modifier, OverlayAlign.AutoToRect(rect,this.rect)) {
            Box {
                TextFlatten(Modifier.padding(3.scaled)) { TopComponent.preedit.toString().emit(color = Color.BLACK) }
            }.background(Color.WHITE).outline(color = Color.GREEN).change {
                object : DslComponent by it {
                    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
                    override fun <RP> render() {
                        it.render()
                        dslBackend.setImeArea(instance.rect)
                    }
                }
            }
        }
    }

    context(ctx: DslContext)
    private fun Tooltip() {
        val hovered = testHit(dslBackend.mouse) { it.tooltip }
        val focusedId = TopComponent.focused
        val focused = if(focusedId == null) null else testHit { it.takeIf { it.identity == focusedId } }
        val func = hovered ?: focused?.tooltip ?: return
        val focusedRect = if(focusedId != null && hovered == null) run {
            focused?.rect?.transformedMaxBound(focused.relativeTransform(this) ?: return@run null)
        } else null
        val outline: Rect = rect
        val align = focusedRect?.let { OverlayAlign.AutoToRect(it,outline) } ?: OverlayAlign.AutoToPoint(dslBackend.mouse,outline)
        Overlay(Modifier,align) { func() }
    }
}