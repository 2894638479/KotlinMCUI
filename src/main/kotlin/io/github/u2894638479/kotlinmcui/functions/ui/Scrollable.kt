package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.alignable
import io.github.u2894638479.kotlinmcui.component.outerMinSize
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.ctxBackend
import io.github.u2894638479.kotlinmcui.functions.identity
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.withId
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.math.rect.overlap
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.scope.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.scope.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import org.lwjgl.glfw.GLFW
import kotlin.collections.forEach
import kotlin.run


context(ctx: DslContext)
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    scrollerProp: StableRWProperty<Scroller>? = null,
    scrollProp: StableRWProperty<Double>? = null,
    sensitivity: Double = 30.0,
    id:Any? = null,
    function: DslFunction
) = Scrollable(modifier,Axis.Vertical,scrollerProp,scrollProp,sensitivity,id,function)


context(ctx: DslContext)
fun ScrollableRow(
    modifier: Modifier = Modifier,
    scrollerProp: StableRWProperty<Scroller>? = null,
    scrollProp: StableRWProperty<Double>? = null,
    sensitivity: Double = 30.0,
    id:Any? = null,
    function: DslFunction
) = Scrollable(modifier,Axis.Horizontal,scrollerProp,scrollProp,sensitivity,id,function)


context(ctx: DslContext)
fun Scrollable(
    modifier: Modifier = Modifier,
    axis: Axis,
    scrollerProp: StableRWProperty<Scroller>? = null,
    scrollProp: StableRWProperty<Double>? = null,
    sensitivity: Double = 30.0,
    id:Any? = null,
    function: DslFunction
) = withId(id ?: function::class) {
    val delegate = DslScopeImpl(identity, modifier, ctx, function)
    collect(
        object : DslScope by delegate {
            var scroller by scrollerProp ?: run {
                val prop by Scroller.empty.remember.property
                prop
            }

            val lazyWidth by lazy { if(axis == Axis.Horizontal) 0.px else childrenMaxWidth }
            context(instance: DslComponent)
            override val contentMinWidth get() = Measure.max(lazyWidth, super.contentMinWidth)

            val lazyHeight by lazy { if(axis == Axis.Vertical) 0.px else childrenMaxHeight }
            context(instance: DslComponent)
            override val contentMinHeight get() = Measure.max(lazyHeight, super.contentMinHeight)


            context(instance: DslComponent)
            private fun layoutAxis() {
                val scroller = Scroller.scroller(children,axis,scrollProp)
                scroller.updateScroll()
                scroller.updateIndex()
                val rect = instance.rect
                val move = scroller.run { scroll - offset + children.take(scrollIndex).sumOf { it.run { outerMinSize(axis) }.unscaled } }
                Aligner.close(Align.LOW).align(rect.bound(axis).expand(low = move.scaled), children.map { it.run { alignable(axis) } })
                this.scroller = scroller
            }

            context(instance: DslComponent)
            override fun layoutVertical() {
                if(axis != Axis.Vertical) return delegate.layoutVertical()
                layoutAxis()
                children.forEach { it.run { layoutVertical() } }
            }

            context(instance: DslComponent)
            override fun layoutHorizontal() {
                if(axis != Axis.Horizontal) return delegate.layoutHorizontal()
                layoutAxis()
                children.forEach { it.run { layoutHorizontal() } }
            }

            context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
            override fun <RP> render(mouse: Position) {
                val rect = instance.rect
                backend.withScissor(rect) {
                    children.asReversed().forEach {
                        if (it.rect.overlap(rect)) {
                            it.run { render(mouse) }
                        }
                    }
                }
            }

            context(instance: DslComponent)
            override fun mouseScrollVertical(mouse: Position, amount: Double): Double {
                val remain = delegate.mouseScrollVertical(mouse,amount)
                if((axis != Axis.Vertical && !ctxBackend.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) || mouse !in instance.rect) return remain
                return scroller.scroll(remain * -sensitivity) / -sensitivity
            }

            context(instance: DslComponent)
            override fun mouseScrollHorizontal(mouse: Position, amount: Double): Double {
                val remain = delegate.mouseScrollHorizontal(mouse,amount)
                if(axis != Axis.Horizontal || mouse !in instance.rect) return remain
                return scroller.scroll(remain * -sensitivity) / -sensitivity
            }

            context(instance: DslComponent)
            override fun <T> testHit(mouse: Position, get: context(DslComponent) (DslComponent) -> T?): T? {
                if (mouse !in instance.rect) return null
                return delegate.testHit(mouse, get)
            }

            context(instance: DslComponent, eventModifier: EventModifier)
            override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
                if (mouse !in instance.rect) return false
                return delegate.mouseDown(mouse, mouseButton)
            }

            override val viewHorizontal get() = listOf(children)
            override val viewVertical get() = children.mapView { listOf(it) }
        }
    )
}