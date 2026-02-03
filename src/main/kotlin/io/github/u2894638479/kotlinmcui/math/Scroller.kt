package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.align.Align
import kotlin.collections.sumOf
import kotlin.math.sign

interface Scroller: DslScaleContext {
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
    }
    interface Item {
        val size: Measure
        val identity: DslId
    }
    val items: List<Item>
    val low: Measure
    val high: Measure
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


    fun scroll(value: Double) { rawScroll += value }

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