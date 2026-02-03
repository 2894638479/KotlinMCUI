package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentAlign
import io.github.u2894638479.kotlinmcui.component.DslComponentEvent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.animatable
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.identity
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.withId
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.size
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.scope.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import kotlin.collections.forEach
import kotlin.run

context(ctx: DslContext)
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    scrollerProp: StableRWProperty<Scroller>? = null,
    scrollProp: StableRWProperty<Double>? = null,
    sensitivity: Double = 30.0,
    scrollToFocused: Boolean = false,
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

            val lazyWidth by lazy { childrenMaxWidth }

            context(instance: DslComponent)
            override val contentMinWidth get() = Measure.max(lazyWidth, super.contentMinWidth)


            context(instance: DslComponent)
            override fun layoutVertical() {
                val scroller = object : Scroller {
                    override val scale get() = ctx.scale
                    override val items = children.mapView {
                        object : Scroller.Item {
                            override val identity get() = it.identity
                            override val size get() = it.run { outerMinHeight }
                        }
                    }
                    override val low by instance.rect::top
                    override val high by instance.rect::bottom
                    override var offset by 0.0.remember
                    override var rawScroll by 0.0.remember
                    override var scroll by scrollProp ?: run {
                        val prop by animatable(0.0).property
                        prop
                    }
                    override var scrollIndex by 0.remember
                    override fun spaceBefore(): Double {
                        items.ifEmpty { return 0.0 }
                        val scroll = scroll
                        val (beginIndex, offset) = calculateIndex(scroll)
                        return items.subList(0, beginIndex).sumOf { it.size.unscaled } + scroll - offset
                    }

                    override fun spaceAfter(): Double {
                        items.ifEmpty { return 0.0 }
                        val scroll = scroll
                        val (endIndex, offset) = calculateIndex(scroll + size)
                        return items.subList(endIndex, items.size).sumOf { it.size.unscaled } - (scroll + size - offset)
                    }
                }
                scroller.updateScroll()
                scroller.updateIndex()
                val rect = instance.rect
                val move = scroller.run { scroll - offset + children.take(scrollIndex).sumOf { it.run { outerMinHeight }.unscaled } }
                Aligner.close(Align.LOW).align(rect.top - move.scaled, rect.bottom, children.map { it.run { alignableVertical } })
                children.forEach { it.run { layoutVertical() } }
                this.scroller = scroller
            }

            context(backend: DslBackendRenderer<RP>, renderPara: RP, instance: DslComponent)
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
            override fun mouseScroll(mouse: Position, amount: Double): Double {
                if (mouse !in instance.rect) return amount
                val remain = delegate.mouseScroll(mouse, amount) * -sensitivity
                scroller.run {
                    updateScroll()
                    val before = rawScroll
                    scroll(remain)
                    updateScroll()
                    val after = rawScroll
                    return (remain - (after - before)) / -sensitivity
                }
            }

            context(instance: DslComponent)
            override fun <T> testHit(mouse: Position, get: context(DslComponent) (DslComponent) -> T?): T? {
                if (mouse !in instance.rect) return null
                return delegate.testHit(mouse, get)
            }

            context(instance: DslComponent)
            override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
                if (mouse !in instance.rect) return false
                return delegate.mouseDown(mouse, mouseButton)
            }

            context(instance: DslComponent)
            override fun focusChanged(newFocus: DslId?) {
                if (!scrollToFocused) return
                newFocus ?: return
                scroller.scrollTo(scroller.items.indexOfFirst { it.identity in newFocus }, Align.MID)
            }

            override val viewHorizontal get() = listOf(children)
            override val viewVertical get() = children.mapView { listOf(it) }
        }
    )
}