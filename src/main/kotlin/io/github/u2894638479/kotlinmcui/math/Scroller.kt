package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.outerMinSize
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.functions.animatable
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.rect.Bound
import io.github.u2894638479.kotlinmcui.math.rect.bound
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScope
import kotlin.collections.ifEmpty
import kotlin.collections.sumOf
import kotlin.getValue
import kotlin.math.sign
import kotlin.run

interface Scroller: DslScaleContext, Bound {
    companion object {
        val empty = object : Scroller {
            override val items: List<Item> = emptyList()
            override val low get() = 0.px
            override val high get() = 0.px
            override var offset get() = 0.0
                set(value) {}
            override var rawScroll get() = 0.0
                set(value) {}
            override var scroll get() = 0.0
                set(value) {}
            override var scrollIndex get() = 0
                set(value) {}
            override val scale get() = 1.0
        }
        context(ctx: DslScaleContext,_: DslDataStoreContext,instance: DslComponent)
        fun scroller(children: DslChild.List, axis: Axis, scrollProp: StableRWProperty<Double>?):Scroller
        = object : Scroller, Bound by instance.rect.bound(axis) {
            override val scale get() = ctx.scale
            override val items = children.mapView {
                object : Item {
                    override val identity get() = it.identity
                    override val size get() = it.run { outerMinSize(axis) }
                }
            }
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
    }
    interface Item {
        val size: Measure
        val identity: DslId
    }
    val items: List<Item>
    /*
    rawScroll: 实时滚动值。为了和scroll（可为animatable）做区分
    offset、scrollIndex: 显示4~6元素时，防止元素0~3的大小变化导致漂移。另lazy时无法检测0~3的大小。
    所以按照可见的首个元素的位置作为offset，不依赖前面的元素。
     */
    var offset: Double
    var rawScroll: Double
    var scroll: Double
    var scrollIndex: Int

    fun spaceBefore(): Double {
        items.ifEmpty { return 0.0 }
        val scroll = scroll
        val (beginIndex,offset) = calculateIndex(scroll)
        val (endIndex,_) = calculateIndex(scroll + size)
        if(beginIndex == 0) return scroll - offset
        val avgSize = items.subList(beginIndex,endIndex + 1).sumOf { it.size.unscaled } / (endIndex + 1 - beginIndex)
        return avgSize * beginIndex + scroll - offset
    }

    fun spaceAfter(): Double {
        items.ifEmpty { return 0.0 }
        val scroll = scroll
        val (beginIndex,_) = calculateIndex(scroll)
        val (endIndex,offset) = calculateIndex(scroll + size)
        if(endIndex == items.size - 1) return items.last().size.unscaled - (scroll + size - offset)
        val avgSize = items.subList(beginIndex,endIndex + 1).sumOf { it.size.unscaled } / (endIndex + 1 - beginIndex)
        return avgSize * (items.size - endIndex) - (scroll + size - offset)
    }



    fun scroll(measure: Measure) = scroll(measure.unscaled)
    fun scrollTo(id: DslId, align: Align) = scrollTo(items.indexOfFirst { it.identity == id },align)
    fun scrollToId(id:Any?,align: Align) = scrollTo(items.indexOfFirst { it.identity.topElement == id },align)

    fun calculateIndex(scroll: Double): Pair<Int, Double> {
        val items = items.ifEmpty { return 0 to 0.0 }
        var scrollIndex = scrollIndex
        var offset = offset
        scrollIndex = scrollIndex.coerceIn(items.indices)
        while(scroll - offset < 0) {
            if(scrollIndex == 0) {
                break
            } else {
                scrollIndex--
                offset -= items[scrollIndex].size.unscaled
            }
        }
        while (scroll - offset > 0) {
            if(scrollIndex >= items.size - 1) break
            if(scroll - offset <= items[scrollIndex].size.unscaled) break
            offset += items[scrollIndex].size.unscaled
            scrollIndex++
        }
        return scrollIndex to offset
    }

    fun updateIndex(): IntRange {
        val scroll = scroll
        val (beginIndex,offset) = calculateIndex(scroll)
        scrollIndex = beginIndex
        this.offset = offset
        val size = size
        var endIndex = scrollIndex
        var sum = offset - scroll
        while (sum < size) {
            if (endIndex >= items.size) break
            sum += items[endIndex].size.unscaled
            endIndex++
        }
        return beginIndex..<endIndex
    }

    // 把滚轮值限制到正确的范围，防止滚到画面外
    fun updateScroll() {
        val (index,offset) = calculateIndex(rawScroll)
        val size = size
        if(index == 0 && rawScroll < offset) rawScroll = offset

        var i = index
        var sum = offset - rawScroll
        while (sum < size) {
            if (i >= items.size) {
                rawScroll -= (size - sum)
                val (index,offset) = calculateIndex(rawScroll)
                if(index == 0 && rawScroll < offset) rawScroll = offset
                break
            }
            sum += items[i].size.unscaled
            i++
        }

        scroll = rawScroll
    }


    fun scroll(value: Double): Double {
        updateScroll()
        val before = rawScroll
        rawScroll += value
        updateScroll()
        val after = rawScroll
        return (value - (after - before))
    }

    fun scrollTo(align: Align) {
        var scrollIndex = scrollIndex
        var offset = offset
        val items = items
        when(align) {
            Align.LOW -> {
                while(scrollIndex > 0) {
                    offset -= items[scrollIndex].size.unscaled
                    scrollIndex--
                }
                rawScroll = offset
            }
            Align.MID -> {
                val mid = scrollIndex / 2
                scrollTo(mid, Align.MID)
                if(scrollIndex % 2 == 1) {
                    rawScroll += items[mid].size.unscaled / 2
                }
            }
            Align.HIGH -> {
                val size = size
                if(size <= 0) return
                while(scrollIndex < items.size) {
                    offset += items[scrollIndex].size.unscaled
                    scrollIndex++
                }
                rawScroll = offset
            }
        }
    }

    fun scrollTo(index: Int,align: Align) {
        if(index !in items.indices) return
        var scrollIndex = scrollIndex
        var offset = offset
        val items = items
        when(align) {
            Align.LOW -> {
                while(scrollIndex != index) {
                    val sign = (index - scrollIndex).sign
                    offset += sign * items[scrollIndex].size.unscaled
                    scrollIndex += sign
                }
                rawScroll = offset
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
}

val Scroller.size get() = (high - low).unscaled