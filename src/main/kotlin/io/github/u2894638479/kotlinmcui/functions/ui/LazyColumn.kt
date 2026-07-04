package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.component.outerMinHeight
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.*
import io.github.u2894638479.kotlinmcui.functions.decorator.scissor
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.scope.DslChild.Companion.buildThis
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl


context(ctx: DslContext)
fun LazyColumn(
    modifier: Modifier = Modifier,
    scrollerProp: StableRW<Scroller>? = null,
    scrollProp: StableRW<Double>? = null,
    sensitivity: Double = 30.0,
    id:Any? = null,
    function: DslFunction
) = withId(id ?: function::class) {
    val delegate = DslScopeImpl(identity, modifier, ctx, function)
    collect(object : DslScope by delegate {
        var scroller by scrollerProp ?: local<Scroller> { Scroller.empty }

        override fun build() {
            instance.children.buildThis(ctx,function)
            instance.children.forEach { it.attachInstance() }
        }

        private var visibleChildren: List<DslComponent> = emptyList()

        override fun layoutVertical() {
            val scroller = object : Scroller {
                override val scale = ctx.scale
                override val items = object : AbstractList<Scroller.Item>() {
                    private val list = MutableList<Scroller.Item?>(children.size) { null }
                    override val size by list::size
                    override fun get(index: Int) = list[index] ?: object : Scroller.Item {
                        private val child = children[index]
                        override val identity by child::identity
                        override val size: Measure
                            get() = children[index].run {
                                build()
                                Aligner.simplePlace.align(rect.bound(Axis.Horizontal), listOf(alignableHorizontal))
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
                override var offset by local { 0.0 }
                override var rawScroll by local { 0.0 }
                override var scroll by scrollProp ?: local.animatable { 0.0 }
                override var scrollIndex by local { 0 }
            }
            scroller.updateScroll()
            val visibleIndices = scroller.updateIndex()
            visibleChildren = visibleIndices.let {
                if (it.isEmpty()) emptyList()
                else children.subList(it.first, it.last + 1)
            }
            val rect = instance.rect
            Aligner.close(Align.LOW).align(
                rect.top - (scroller.scroll - scroller.offset).scaled,
                rect.bottom,
                visibleChildren.map { it.alignableVertical })
            visibleChildren.forEach { it.layoutVertical() }
            this.scroller = scroller
        }

        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() = visibleChildren.asReversed().forEach { it.render() }

        context(mouse: Position)
        override fun mouseScrollVertical(amount: Double): Double {
            val remain = delegate.mouseScrollVertical(amount) * -sensitivity
            scroller.run {
                updateScroll()
                val before = rawScroll
                scroll(remain)
                updateScroll()
                val after = rawScroll
                return (remain - (after - before)) / -sensitivity
            }
        }

        override val viewHorizontal get() = listOf(children)
        override val viewVertical get() = children.mapView { listOf(it) }
    }).scissor()
}