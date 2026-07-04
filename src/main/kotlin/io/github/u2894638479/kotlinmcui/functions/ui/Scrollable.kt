package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.alignable
import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.component.outerMinSize
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.functions.*
import io.github.u2894638479.kotlinmcui.functions.decorator.scissor
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.math.rect.overlap
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import org.lwjgl.glfw.GLFW


context(ctx: DslContext)
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    scrollerProp: StableRW<Scroller>? = null,
    scrollProp: StableRW<Double>? = null,
    sensitivity: Double = 30.0,
    id:Any? = null,
    function: DslFunction
) = Scrollable(modifier,Axis.Vertical,scrollerProp,scrollProp,sensitivity,id,function)


context(ctx: DslContext)
fun ScrollableRow(
    modifier: Modifier = Modifier,
    scrollerProp: StableRW<Scroller>? = null,
    scrollProp: StableRW<Double>? = null,
    sensitivity: Double = 30.0,
    id:Any? = null,
    function: DslFunction
) = Scrollable(modifier,Axis.Horizontal,scrollerProp,scrollProp,sensitivity,id,function)


context(ctx: DslContext)
fun Scrollable(
    modifier: Modifier = Modifier,
    axis: Axis,
    scrollerProp: StableRW<Scroller>? = null,
    scrollProp: StableRW<Double>? = null,
    sensitivity: Double = 30.0,
    id:Any? = null,
    function: DslFunction
) = withId(id ?: function::class) {
    val delegate = DslScopeImpl(identity, modifier, ctx, function)
    collect(
        object : DslScope by delegate {
            var scroller by scrollerProp ?: local<Scroller> { Scroller.empty }

            override val contentMinWidth by lazy {
                max(if(axis == Axis.Horizontal) 0.px else instance.childrenMaxWidth,super.contentMinWidth)
            }

            override val contentMinHeight by lazy {
                max(if(axis == Axis.Vertical) 0.px else instance.childrenMaxHeight,super.contentMinHeight)
            }

            private fun layoutAxis() {
                val scroller = Scroller.scroller(instance,axis,scrollProp)
                scroller.updateScroll()
                scroller.updateIndex()
                val rect = instance.rect
                val move = scroller.run { scroll - offset + children.take(scrollIndex).sumOf { it.outerMinSize(axis).unscaled } }
                Aligner.close(Align.LOW).align(rect.bound(axis).expand(low = move.scaled), children.map { it.alignable(axis) })
                this.scroller = scroller
            }

            override fun layoutVertical() {
                if(axis != Axis.Vertical) return delegate.layoutVertical()
                layoutAxis()
                children.forEach { it.layoutVertical() }
            }

            override fun layoutHorizontal() {
                if(axis != Axis.Horizontal) return delegate.layoutHorizontal()
                layoutAxis()
                children.forEach { it.layoutHorizontal() }
            }

            context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                val rect = instance.rect
                children.asReversed().forEach {
                    if (it.rect.overlap(rect)) {
                        it.render()
                    }
                }
            }

            context(mouse: Position)
            override fun mouseScrollVertical(amount: Double): Double {
                val remain = delegate.mouseScrollVertical(amount)
                if(axis != Axis.Vertical && !backend.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) return remain
                return scroller.scroll(remain * -sensitivity) / -sensitivity
            }

            context(mouse: Position)
            override fun mouseScrollHorizontal(amount: Double): Double {
                val remain = delegate.mouseScrollHorizontal(amount)
                if(axis != Axis.Horizontal) return remain
                return scroller.scroll(remain * -sensitivity) / -sensitivity
            }

            override val viewHorizontal get() = listOf(children)
            override val viewVertical get() = children.mapView { listOf(it) }
        }
    ).scissor()
}