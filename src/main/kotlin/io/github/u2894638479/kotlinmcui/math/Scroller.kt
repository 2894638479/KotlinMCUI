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
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.math.rect.size
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.prop.setValue
import kotlin.collections.sumOf
import kotlin.math.sign

// ── Interface ────────────────────────────────────────────────────────────────

/**
 * A scroll controller managing scroll position, item visibility, and boundary
 * clamping for virtualised lists.
 *
 * Supports both fully-loaded lists ([companion.scroller]) and lazy lists
 * ([io.github.u2894638479.kotlinmcui.dsl.ui.LazyColumn] creates its own).
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
         * Create a scroller for a fully-loaded component list
         * (e.g. [io.github.u2894638479.kotlinmcui.dsl.ui.Scrollable]).
         * All items are always available — [spaceBefore] and [spaceAfter]
         * compute exact values by summing item sizes.
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

    /** Real-time scroll position. Separate from [scroll] which may be animated. */
    var rawScroll: Double

    /** The scroll position, possibly animated. Clamped by [updateScroll]. */
    var scroll: Double

    /**
     * Offset and index of the first visible item. Anchors the viewport so that
     * size changes above the visible range do not cause drift.
     */
    var offset: Double
    var scrollIndex: Int

    // ── Overridable: lazy-safe defaults, full-list overrides for exact ─────

    /** Estimated space before the visible region (lazy-safe). Override for exact. */
    fun spaceBefore(): Double {
        items.ifEmpty { return 0.0 }
        val s = scroll
        val (beginIndex, offset) = calculateIndex(s)
        val (endIndex, _) = calculateIndex(s + size.unscaled)
        if (beginIndex == 0) return s - offset
        val avg = items.subList(beginIndex, endIndex + 1).sumOf { it.size.unscaled } / (endIndex + 1 - beginIndex)
        return avg * beginIndex + s - offset
    }

    /** Estimated space after the visible region (lazy-safe). Override for exact. */
    fun spaceAfter(): Double {
        items.ifEmpty { return 0.0 }
        val s = scroll
        val (beginIndex, _) = calculateIndex(s)
        val viewport = size.unscaled
        val (endIndex, offset) = calculateIndex(s + viewport)
        if (endIndex == items.size - 1) return items.last().size.unscaled - (s + viewport - offset)
        val avg = items.subList(beginIndex, endIndex + 1).sumOf { it.size.unscaled } / (endIndex + 1 - beginIndex)
        return avg * (items.size - endIndex) - (s + viewport - offset)
    }

    fun isScrollable() = spaceBefore() + spaceAfter() > 0

    /**
     * Index of the item identified by [identity], or `-1`.
     *
     * The default implementation walks [items]. Lazy scrollers override it so that
     * locating an item does not force every preceding item to load.
     */
    fun indexOf(identity: DslId): Int = items.indexOfFirst { it.identity == identity }
}

// ── Core algorithms ──────────────────────────────────────────────────────────

/**
 * Map a scroll position to an item index and local offset.
 * Walks from the current [Scroller.scrollIndex] forward or backward,
 * only accessing items along the walk path.
 */
fun Scroller.calculateIndex(scroll: Double): Pair<Int, Double> {
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

/** Update [Scroller.scrollIndex] and [Scroller.offset], return the visible range. */
fun Scroller.updateIndex(): IntRange {
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

/** Clamp [Scroller.rawScroll] to valid range and sync [Scroller.scroll] to it. */
fun Scroller.updateScroll() {
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

// ── Scrolling operations ─────────────────────────────────────────────────────

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

/**
 * Scroll to the start, middle or end of the whole content.
 *
 * [Align.LOW] goes to the first item, [Align.HIGH] to the last, and [Align.MID]
 * centres the item halfway through the list.
 */
fun Scroller.scrollTo(align: Align) {
    var idx = scrollIndex
    var off = offset
    when (align) {
        Align.LOW -> {
            while (idx > 0) { idx--; off -= items[idx].size.unscaled }
            rawScroll = off
        }
        Align.MID -> {
            return scrollTo(items.size / 2, Align.MID)
        }
        Align.HIGH -> {
            val viewport = size.unscaled
            if (viewport <= 0) return
            while (idx < items.size) { off += items[idx].size.unscaled; idx++ }
            rawScroll = off
        }
    }
    updateScroll()
}

/**
 * Scroll so that item [index] is aligned according to [align].
 * May load intermediate items in lazy mode.
 */
fun Scroller.scrollTo(index: Int, align: Align) {
    if (index !in items.indices) return

    var idx = scrollIndex
    var off = offset
    while (idx != index) {
        val step = (index - idx).sign.toInt()
        off += step * items[idx].size.unscaled
        idx += step
    }
    rawScroll = off

    when (align) {
        Align.LOW -> {}
        Align.MID -> {
            scrollTo(index, Align.LOW)
            scroll(-(high - low - items[index].size).unscaled / 2)
        }
        Align.HIGH -> {
            scrollTo(index, Align.LOW)
            scroll(-(high - low - items[index].size).unscaled)
        }
    }
    updateScroll()
}

/**
 * Scroll to the item identified by [id].
 *
 * Uses [Scroller.indexOf], so lazy scrollers can resolve the index without
 * loading the items in between.
 */
fun Scroller.scrollTo(id: DslId, align: Align) = scrollTo(indexOf(id), align)

/**
 * Scroll the item at [index] into the viewport according to [constraint].
 *
 * [ScrollConstraint.KeepIn] and [ScrollConstraint.KeepInRatio] do nothing when the
 * item is already visible and otherwise push the minimum amount needed to bring it
 * into view; [ScrollConstraint.AlignTo] always aligns the item.
 */
fun Scroller.scrollTo(index: Int, constraint: ScrollConstraint) {
    if (index !in items.indices) return
    when (constraint) {
        is ScrollConstraint.KeepIn,
        is ScrollConstraint.KeepInRatio -> {
            val viewport = size.unscaled
            val pad = when (constraint) {
                is ScrollConstraint.KeepIn -> constraint.padding.unscaled
                is ScrollConstraint.KeepInRatio -> viewport * constraint.ratio
                else -> 0.0
            }
            var itemOffset = offset
            var i = scrollIndex
            while (i != index) {
                val step = (index - i).sign
                itemOffset += step * items[i].size.unscaled
                i += step
            }
            val itemTop = itemOffset
            val itemBottom = itemTop + items[index].size.unscaled
            val rs = rawScroll
            if (itemTop >= rs + pad && itemBottom <= rs + viewport - pad) return
            if (itemTop < rs + pad) scroll(itemTop - (rs + pad))
            else scroll(itemBottom - (rs + viewport - pad))
        }
        is ScrollConstraint.AlignTo -> scrollTo(index, constraint.align)
    }
}

/** Scroll the item identified by [id] into the viewport. */
fun Scroller.scrollTo(id: DslId, constraint: ScrollConstraint) =
    scrollTo(indexOf(id), constraint)
