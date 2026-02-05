package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
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
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue


context(ctx: DslContext)
fun LazyColumn(
    modifier: Modifier = Modifier,
    scrollerProp: StableRWProperty<Scroller>? = null,
    scrollProp: StableRWProperty<Double>? = null,
    sensitivity: Double = 30.0,
    scrollToFocused: Boolean = false,
    id:Any? = null,
    function: DslFunction
) = withId(id ?: function::class) {
    val delegate = DslScopeImpl(identity, modifier, ctx, function, alignerVertical = Aligner.close(Align.LOW))
    collect(object : DslScope by delegate {
        var scroller by scrollerProp ?: run {
            val prop by Scroller.empty.remember.property
            prop
        }

        context(instance: DslComponent)
        override fun build() {
            context(ctx.change(dslIdentity = instance.identity, dslChildren = children)) {
                function()
            }
        }

        private var visibleChildren: List<DslComponent> = emptyList()

        context(instance: DslComponent)
        override fun layoutVertical() {
            val scroller = object : Scroller {
                override val scale get() = ctx.scale
                override val items = object : AbstractList<Scroller.Item>() {
                    private val list = MutableList<Scroller.Item?>(children.size) { null }
                    override val size by list::size
                    override fun get(index: Int) = list[index] ?: object : Scroller.Item {
                        private val child = children[index]
                        override val identity by child::identity
                        override val size: Measure
                            get() = children[index].run {
                                build()
                                val rect = instance.rect
                                alignerHorizontal.align(rect.left, rect.right, listOf(alignableHorizontal))
                                layoutHorizontal()
                                outerMinHeight
                            }.also {
                                list[index] = object : Scroller.Item {
                                    override val identity get() = child.identity
                                    override val size = it
                                }
                            }
                    }.also { list[index] = it }
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
            }
            scroller.updateScroll()
            val visibleIndices = scroller.updateIndex()
            visibleChildren = visibleIndices.let {
                if (it.isEmpty()) emptyList()
                else children.subList(it.first, it.last + 1)
            }
            val rect = instance.rect
            alignerVertical.align(
                rect.top - (scroller.scroll - scroller.offset).scaled,
                rect.bottom,
                visibleChildren.map { it.run { alignableVertical } })
            visibleChildren.forEach { it.run { layoutVertical() } }
            this.scroller = scroller
        }

        context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
        override fun <RP> render(mouse: Position) = backend.withScissor(instance.rect) {
            visibleChildren.asReversed().forEach { it.run { render(mouse) } }
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
    })
}