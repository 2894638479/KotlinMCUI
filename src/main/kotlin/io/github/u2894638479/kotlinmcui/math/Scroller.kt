package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.outerMinSize
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.rect.Bound
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.math.rect.size
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.prop.setValue
import kotlin.collections.sumOf
import kotlin.math.sign

// ── Extensions ───────────────────────────────────────────────────────────────

// ── Extensions ───────────────────────────────────────────────────────────────

/** Scroll by a [Measure] value. */
fun Scroller.scroll(measure: Measure) = scroll(measure.unscaled)

/**
 * Scroll by [value] pixels and return the unconsumed amount
 * (clamped at boundaries). May load intermediate items in lazy mode.
 */
fun Scroller.scroll(value: Double): Double {
    updateScroll()
    val before = rawScroll
    rawScroll += value
    updateScroll()
    val after = rawScroll
    return (value - (after - before))
}

/** Scroll to the item identified by [id]. */
fun Scroller.scrollTo(id: DslId, align: Align) = scrollTo(items.indexOfFirst { it.identity == id }, align)

/**
 * Scroll so that item [index] is aligned according to [align].
 * May load intermediate items in lazy mode.
 */
fun Scroller.scrollTo(index: Int, align: Align) {
    if (index !in items.indices) return
    var idx = scrollIndex
    var off = offset
    when (align) {
        Align.LOW -> {
            while (idx != index) {
                val step = (index - idx).sign.toInt()
                off += step * items[idx].size.unscaled
                idx += step
            }
            rawScroll = off
        }
        Align.MID -> {
            scrollTo(index, Align.LOW)
            scroll(-(high - low - items[index].size).unscaled / 2)
        }
        Align.HIGH -> {
            scrollTo(index, Align.LOW)
            scroll(-(high - low - items[index].size).unscaled)
        }
    }
}

/** Scroll the current visible item according to [align]. */
fun Scroller.scrollTo(align: Align) {
    var idx = scrollIndex
    var off = offset
    when (align) {
        Align.LOW -> {
            while (idx > 0) { idx--; off -= items[idx].size.unscaled }
            rawScroll = off
        }
        Align.MID -> {
            val mid = idx / 2
            scrollTo(mid, Align.MID)
            if (idx % 2 == 1) rawScroll += items[mid].size.unscaled / 2
        }
        Align.HIGH -> {
            val viewport = size.unscaled
            if (viewport <= 0) return
            while (idx < items.size) { off += items[idx].size.unscaled; idx++ }
            rawScroll = off
        }
    }
}

/**
 * Scroll the item at [index] into the viewport according to [constraint].
 */
fun Scroller.scrollToView(index: Int, constraint: ScrollConstraint) {
    if (index !in items.indices) return
    val viewport = size.unscaled
    var itemOffset = offset
    var i = scrollIndex
    while (i != index) {
        val step = (index - i).sign.toInt()
        itemOffset += step * items[i].size.unscaled
        i += step
    }
    val itemSize = items[index].size.unscaled
    val itemTop = itemOffset
    val itemBottom = itemTop + itemSize
    when (constraint) {
        ScrollConstraint.MINIMAL -> {
            if (itemTop >= scroll && itemBottom <= scroll + viewport) return
            if (itemTop < scroll) scroll(itemTop - scroll)
            else scroll(itemBottom - (scroll + viewport))
        }
        ScrollConstraint.CENTER -> scrollTo(index, Align.MID)
    }
}

/** Scroll the item identified by [id] into the viewport. */
fun Scroller.scrollToView(id: DslId, constraint: ScrollConstraint) =
    scrollToView(items.indexOfFirst { it.identity == id }, constraint)

// ── Interface ────────────────────────────────────────────────────────────────

/**
 * A scroll controller managing scroll position, item visibility, and boundary
 * clamping for virtualised lists.
 *
 * Supports both fully-loaded lists ([companion.scroller]) and lazy lists
 * ([io.github.u2894638479.kotlinmcui.dsl.ui.LazyColumn] creates its own).
 *
 * ### Design rationale
 * [rawScroll] tracks the real-time position (may differ from [scroll] when
 * animation is involved). [offset] and [scrollIndex] anchor to the first
 * visible item so that changing sizes of items above the viewport does not
 * cause drift.
 *
 * ### First-frame behaviour
 * A scroller declared as `local { Scroller.empty }` is [empty] in the first
 * frame. Scroll state is only valid from the second frame onward.
 */
interface Scroller : DslScaleContext, Bound {
    companion object {
        val empty = object : Scroller {
            override val items: List<Item> = emptyList()
            override val low get() = 0.px
            override val high get() = 0.px
            override var offset get() = 0.0; set(value) {}
            override var rawScroll get() = 0.0; set(value) {}
            override var scroll get() = 0.0; set(value) {}
            override var scrollIndex get() = 0; set(value) {}
            override val scale get() = 1.0
        }

        /**
         * Create a scroller for a fully-loaded component list (e.g. [io.github.u2894638479.kotlinmcui.dsl.ui.Scrollable]).
         * All items are always available — [spaceBefore] and [spaceAfter] compute
         * exact values by summing item sizes.
         */
        context(ctx: DslScaleContext, _: DslDataStoreContext, _: DslIdContext)
        fun scroller(instance: DslComponent, axis: Axis, scrollProp: StableRW<Double> = local.animatable { 0.0 }): Scroller =
            object : Scroller, Bound by instance.rect.bound(axis) {
                override val scale get() = ctx.scale
                override val items = instance.children.mapView {
                    object : Item {
                        override val identity get() = it.identity
                        override val size get() = it.outerMinSize(axis)
                    }
                }
                override var offset by local { 0.0 }
                override var rawScroll by local { 0.0 }
                override var scroll by scrollProp
                override var scrollIndex by local { 0 }

                // Exact: all items are loaded, sum directly.
                override fun spaceBefore(): Double {
                    items.ifEmpty { return 0.0 }
                    val s = scroll
                    val (beginIndex, offset) = calculateIndex(s)
                    return items.subList(0, beginIndex).sumOf { it.size.unscaled } + s - offset
                }

                override fun spaceAfter(): Double {
                    items.ifEmpty { return 0.0 }
                    val s = scroll
                    val viewport = size.unscaled
                    val (endIndex, offset) = calculateIndex(s + viewport)
                    val trailingSize = items.subList(endIndex, items.size).sumOf { it.size.unscaled }
                    return trailingSize - (s + viewport - offset)
                }
            }
    }

    /** An item in the scroll list. Accessing [size] may trigger lazy loading. */
    interface Item {
        val size: Measure
        val identity: DslId
    }

    // ── Abstract state ───────────────────────────────────────────────────

    val items: List<Item>

    /**
     * The real-time scroll position. Separate from [scroll] which may be
     * animated (e.g. via [io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope.animatable]).
     */
    var rawScroll: Double

    /** The scroll position, possibly animated. Clamped to valid range by [updateScroll]. */
    var scroll: Double

    /**
     * The index and offset of the first visible item. Together with [scrollIndex],
     * this anchors the viewport so that size changes of items above the visible
     * range do not cause drift. In lazy mode, items above the viewport may not
     * be loaded — [offset] provides a reference point independent of them.
     */
    var offset: Double
    var scrollIndex: Int

    // ── Core algorithms (same for lazy and full) ──────────────────────────

    /**
     * Map a scroll position to an item index and local offset.
     * Walks from the current [scrollIndex] forward or backward until the
     * position is found. Only accesses items along the walk path.
     */
    fun calculateIndex(scroll: Double): Pair<Int, Double> {
        val items = items.ifEmpty { return 0 to 0.0 }
        var index = scrollIndex.coerceIn(items.indices)
        var offset = offset
        while (scroll - offset < 0) {
            if (index == 0) break
            index--
            offset -= items[index].size.unscaled
        }
        while (scroll - offset > 0) {
            if (index >= items.size - 1) break
            if (scroll - offset <= items[index].size.unscaled) break
            offset += items[index].size.unscaled
            index++
        }
        return index to offset
    }

    /** Update [scrollIndex] and [offset] and return the visible item range. */
    fun updateIndex(): IntRange {
        val s = scroll
        val (beginIndex, offset) = calculateIndex(s)
        scrollIndex = beginIndex
        this.offset = offset
        val viewport = size.unscaled
        var endIndex = beginIndex
        var sum = offset - s
        while (sum < viewport) {
            if (endIndex >= items.size) break
            sum += items[endIndex].size.unscaled
            endIndex++
        }
        return beginIndex..<endIndex
    }

    /** Clamp [rawScroll] to valid range and sync [scroll] to it. */
    fun updateScroll() {
        val (index, offset) = calculateIndex(rawScroll)
        val viewport = size.unscaled
        if (index == 0 && rawScroll < offset) rawScroll = offset

        var i = index
        var sum = offset - rawScroll
        while (sum < viewport) {
            if (i >= items.size) {
                rawScroll -= (viewport - sum)
                val (idx, off) = calculateIndex(rawScroll)
                if (idx == 0 && rawScroll < off) rawScroll = off
                break
            }
            sum += items[i].size.unscaled
            i++
        }

        scroll = rawScroll
    }

    // ── Overridable: lazy-safe defaults, full-list overrides for exact ─────

    /**
     * Estimate the scrollable space before the visible region.
     *
     * The default implementation estimates from the average size of visible
     * items — safe for lazy lists where items above the viewport may not be
     * loaded. Fully-loaded lists should override with exact summation.
     */
    fun spaceBefore(): Double {
        items.ifEmpty { return 0.0 }
        val s = scroll
        val (beginIndex, offset) = calculateIndex(s)
        val (endIndex, _) = calculateIndex(s + size.unscaled)
        if (beginIndex == 0) return s - offset
        val avgSize = items.subList(beginIndex, endIndex + 1).sumOf { it.size.unscaled } / (endIndex + 1 - beginIndex)
        return avgSize * beginIndex + s - offset
    }

    /**
     * Estimate the scrollable space after the visible region.
     *
     * The default implementation estimates from the average size of visible
     * items. Fully-loaded lists should override with exact summation.
     */
    fun spaceAfter(): Double {
        items.ifEmpty { return 0.0 }
        val s = scroll
        val (beginIndex, _) = calculateIndex(s)
        val viewport = size.unscaled
        val (endIndex, offset) = calculateIndex(s + viewport)
        if (endIndex == items.size - 1) return items.last().size.unscaled - (s + viewport - offset)
        val avgSize = items.subList(beginIndex, endIndex + 1).sumOf { it.size.unscaled } / (endIndex + 1 - beginIndex)
        return avgSize * (items.size - endIndex) - (s + viewport - offset)
    }

    // ── Derived ───────────────────────────────────────────────────────────

    fun isScrollable() = spaceBefore() + spaceAfter() > 0
}
