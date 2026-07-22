package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.alignable
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.component.contentMinSize
import io.github.u2894638479.kotlinmcui.component.outerMinSize
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.decorator.scissor
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.container.DslChild.Companion.buildThis
import io.github.u2894638479.kotlinmcui.container.DslScope
import io.github.u2894638479.kotlinmcui.container.DslScopeImpl

// ── Public API ───────────────────────────────────────────────────────────────

context(ctx: DslContext)
fun LazyColumn(
    modifier: Modifier = Modifier,
    scrollerProp: StableRW<Scroller> = local { Scroller.empty },
    scrollProp: StableRW<Double> = local.animatable { 0.0 },
    sensitivity: Double = 30.0,
    id: Any? = null,
    function: DslFunction
) = LazyScrollable(Axis.Vertical, modifier, scrollerProp, scrollProp, sensitivity, id, function)

context(ctx: DslContext)
fun LazyRow(
    modifier: Modifier = Modifier,
    scrollerProp: StableRW<Scroller> = local { Scroller.empty },
    scrollProp: StableRW<Double> = local.animatable { 0.0 },
    sensitivity: Double = 30.0,
    id: Any? = null,
    function: DslFunction
) = LazyScrollable(Axis.Horizontal, modifier, scrollerProp, scrollProp, sensitivity, id, function)

// ── Implementation ───────────────────────────────────────────────────────────

/**
 * A scrollable container that only builds and renders children within the
 * visible region. Children outside the viewport are never loaded — their
 * size is discovered on first access and cached thereafter.
 */
context(ctx: DslContext)
private fun LazyScrollable(
    axis: Axis,
    modifier: Modifier = Modifier,
    scrollerProp: StableRW<Scroller> = local { Scroller.empty },
    scrollProp: StableRW<Double> = local.animatable { 0.0 },
    sensitivity: Double = 30.0,
    id: Any? = null,
    function: DslFunction
): DslChild = withId(id ?: function::class) {
    val delegate = DslScopeImpl(identity, modifier, ctx, function)
    collect(object : DslScope by delegate {
        var scroller by scrollerProp
        private var visibleChildren: List<DslComponent> = emptyList()

        override fun build() {
            instance.children.buildThis(ctx, function)
            instance.children.forEach { it.attachInstance() }
        }

        // Cross-axis (e.g. horizontal for LazyColumn, vertical for LazyRow):
        // minimum content size is 0 so the parent can shrink us.
        override val contentMinWidth by lazy {
            if (axis == Axis.Horizontal) super.contentMinWidth else 0.px
        }
        override val contentMinHeight by lazy {
            if (axis == Axis.Vertical) super.contentMinHeight else 0.px
        }

        override fun layoutVertical() = layoutAxis(Axis.Vertical)
        override fun layoutHorizontal() = layoutAxis(Axis.Horizontal)

        private fun layoutAxis(layoutAxis: Axis) {
            // Only handle layout on the scroll axis; delegate cross-axis.
            if (layoutAxis != axis) return delegate.run {
                if (axis == Axis.Vertical) layoutHorizontal() else layoutVertical()
            }

            val scroller = createLazyScroller()
            scroller.updateScroll()
            val visibleIndices = scroller.updateIndex()
            visibleChildren = if (visibleIndices.isEmpty()) emptyList()
            else children.subList(visibleIndices.first, visibleIndices.last + 1)

            val rect = instance.rect
            val move = (scroller.scroll - scroller.offset).scaled
            val low = rect.bound(axis).low - move
            val high = rect.bound(axis).high
            Aligner.close(Align.LOW).align(low, high, visibleChildren.map { it.alignable(axis) })
            if (axis == Axis.Vertical) visibleChildren.forEach { it.layoutVertical() }
            else visibleChildren.forEach { it.layoutHorizontal() }
            this.scroller = scroller
        }

        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() = visibleChildren.asReversed().forEach { it.render() }

        context(mouse: Position)
        override fun mouseScrollVertical(amount: Double): Double =
            if (axis == Axis.Vertical) handleScroll(amount) else delegate.mouseScrollVertical(amount)

        context(mouse: Position)
        override fun mouseScrollHorizontal(amount: Double): Double =
            if (axis == Axis.Horizontal) handleScroll(amount) else delegate.mouseScrollHorizontal(amount)

        context(mouse: Position)
        private fun handleScroll(amount: Double): Double {
            val remain = (if (axis == Axis.Vertical)
                delegate.mouseScrollVertical(amount) else delegate.mouseScrollHorizontal(amount)) * -sensitivity
            scroller.run {
                updateScroll()
                val before = rawScroll
                scroll(remain)
                updateScroll()
                val after = rawScroll
                return (remain - (after - before)) / -sensitivity
            }
        }

        override val viewHorizontal get() = if (axis == Axis.Horizontal) children.mapView { listOf(it) } else listOf(children)
        override val viewVertical get() = if (axis == Axis.Vertical) children.mapView { listOf(it) } else listOf(children)

        // ── Scroller factory ─────────────────────────────────────────────

        /**
         * Create a scroller whose [items] are lazily loaded. Accessing
         * `items[i]` triggers build + layout of child [i] and caches the
         * result for subsequent access.
         */
        private fun createLazyScroller(): Scroller = object : Scroller {
            override val scale = ctx.scale
            override val low by instance.rect.bound(axis)::low
            override val high by instance.rect.bound(axis)::high

            override val items = object : AbstractList<Scroller.Item>() {
                private val cache = MutableList<Scroller.Item?>(children.size) { null }
                override val size by cache::size
                override fun get(index: Int) = cache[index] ?: loadItem(index).also { cache[index] = it }
            }

            override var offset by local { 0.0 }
            override var rawScroll by local { 0.0 }
            override var scroll by scrollProp
            override var scrollIndex by local { 0 }
        }

        private fun loadItem(index: Int): Scroller.Item {
            val child = children[index]
            val size: Measure = child.run {
                build()
                val crossAxis = if (axis == Axis.Vertical) Axis.Horizontal else Axis.Vertical
                Aligner.simplePlace.align(rect.bound(crossAxis), listOf(alignable(crossAxis)))
                layoutAxis(crossAxis)
                outerMinSize(axis)
            }
            return object : Scroller.Item {
                override val identity get() = child.identity
                override val size = size
            }
        }
    }).scissor()
}
