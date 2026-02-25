package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.*
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.functions.*
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
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
import io.github.u2894638479.kotlinmcui.prop.*
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import org.lwjgl.glfw.GLFW
import kotlin.collections.asReversed
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.sumOf
import kotlin.collections.take


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

            var lazyWidth = Measure.AUTO
            context(instance: DslComponent)
            override val contentMinWidth get() = ::lazyWidth.lazy {
                max(if(axis == Axis.Horizontal) 0.px else instance.childrenMaxWidth,super.contentMinWidth)
            }

            var lazyHeight = Measure.AUTO
            context(instance: DslComponent)
            override val contentMinHeight get() = ::lazyHeight.lazy {
                max(if(axis == Axis.Vertical) 0.px else instance.childrenMaxHeight,super.contentMinHeight)
            }

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