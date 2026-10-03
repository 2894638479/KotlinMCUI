package io.github.u2894638479.kotlinmcui.math.align

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.MeasureArray
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Bound
import io.github.u2894638479.kotlinmcui.math.sumOf
import kotlin.collections.sumOf

fun Aligner.align(bound: Bound,alignable: List<Alignable>) = align(bound.low,bound.high,alignable)

/**
 * A 1D layout strategy that positions items within a given range.
 *
 * Each [Alignable] provides hints ([weight], [size], [minSize], [align]),
 * but individual strategies are free to use or ignore any of these.
 * For example, [close] ignores [weight], while [weightedStrictByMin] uses it
 * to distribute remaining space.
 *
 * Pre-built strategies are available in the [companion][Companion].
 * Implement this interface for custom layout behaviour.
 */
fun interface Aligner {
    /**
     * Layout [alignable] items within the range [[low], [high]].
     * Each item's [Alignable.low] and [Alignable.high] must be set.
     */
    fun align(low: Measure, high: Measure, alignable: List<Alignable>)

    companion object {
        val equalSpacing = Aligner { low,high,alignable ->
            alignable.ifEmpty { return@Aligner }
            val sum = alignable.sumOf { it.autoSizeMin }
            val space = (high - low - sum) / alignable.size
            var current = low + space/2
            alignable.forEach {
                it.low = current
                current += it.size.ifNan { it.minSize.ifNan { 0.px } }
                it.high = current
                current += space
            }
        }

        fun close(align: Align) = Aligner { low, high, alignable ->
            var current = when (align) {
                Align.LOW -> low
                Align.MID -> (high + low - alignable.sumOf { it.autoSizeMin }) / 2
                Align.HIGH -> high - alignable.sumOf { it.autoSizeMin }
            }
            alignable.forEach {
                it.low = current
                current += it.autoSizeMin
                it.high = current
            }
        }

        val simplePlace = Aligner { low, high, alignable ->
            alignable.forEach {
                val size = it.autoSize(high - low)
                when(it.align) {
                    Align.LOW -> {
                        it.low = low
                        it.high = low + size
                    }
                    Align.MID -> {
                        val l = (high + low - size) / 2
                        it.low = l
                        it.high = l + size
                    }
                    Align.HIGH -> {
                        it.high = high
                        it.low = high - size
                    }
                }
            }
        }

        /**
         * Fixed-size items keep their size; everything left over is shared by the
         * automatic items according to [Alignable.weight].
         *
         * When no weight is available, or when fixed-size items already use up the
         * whole range, automatic items fall back to their minimum size instead of
         * receiving a zero (or negative) slice.
         */
        val weighted = Aligner { low,high,alignable ->
            var fixedSum = 0.px
            var weightSum = 0.0
            alignable.forEach {
                if(it.size.isNumber) fixedSum += it.size else weightSum += it.weight
            }

            // Only the space left over by the fixed-size items can be distributed.
            val remain = (high - low) - fixedSum
            val factor = if(weightSum > 0.0 && remain > 0.px) remain / weightSum else null
            var current = low
            alignable.forEach {
                it.low = current
                current += if(it.size.isNumber) it.size
                    else factor?.let { f -> f * it.weight } ?: it.autoSizeMin
                it.high = current
            }
        }

        val weightedStrictByMin = Aligner { low,high,alignable ->
            var remainSpace : Measure
            var remainWeightSum : Double
            var remainCount : Int
            val result = MeasureArray(alignable.size) {
                alignable[it].autoSize(Measure.AUTO)
            }
            while (true) {
                remainSpace = high - low
                remainWeightSum = 0.0
                remainCount = 0
                alignable.forEachIndexed { i,it ->
                    val result = result[i]
                    if(result.isNumber) {
                        remainSpace -= result
                    } else {
                        remainCount++
                        remainWeightSum += it.weight
                    }
                }
                if(remainCount == 0) break
                if(remainSpace <= 0.px) {
                    for(i in result.indices) {
                        if(!result[i].isNumber) result[i] = alignable[i].minSize.ifNan { 0.px }
                    }
                    break
                }

                val factor = remainSpace / remainWeightSum
                var markDirty = false
                alignable.forEachIndexed { i,it ->
                    if(result[i].isNumber) return@forEachIndexed
                    val assume = if(factor.isFinite) factor * it.weight else 0.px
                    if(assume <= it.minSize) {
                        result[i] = it.minSize
                        markDirty = true
                    }
                }
                if(!markDirty) {
                    alignable.forEachIndexed { i,it ->
                        if(result[i].isNumber) return@forEachIndexed
                        result[i] = factor * it.weight
                    }
                    break
                }
            }
            var current = low
            alignable.forEachIndexed { i,it ->
                it.low = current
                current += result[i]
                it.high = current
            }
        }

        /**
         * Every item keeps at least its minimum size; the leftover space is shared
         * among the automatic items proportionally to [Alignable.weight].
         */
        val weightedExtend = Aligner { low,high,alignable ->
            val sum = alignable.sumOf { it.autoSizeMin }
            val remain = high - low - sum
            fun Alignable.exactWeight() = if(size.isNumber) 0.0 else weight
            val sumWeight = alignable.sumOf { it.exactWeight() }
            val factor = if(sumWeight > 0.0 && remain > 0.px) remain / sumWeight else 0.px
            var current = low
            alignable.forEach {
                it.low = current
                current += factor * it.exactWeight()
                current += it.autoSizeMin
                it.high = current
            }
        }

        /**
         * Like [weightedExtend], but the leftover space is split equally among the
         * automatic items, ignoring [Alignable.weight].
         */
        val equalWeightedExtend = Aligner { low,high,alignable ->
            val sum = alignable.sumOf { it.autoSizeMin }
            val remain = high - low - sum
            val weightCount = alignable.count { !it.size.isNumber }
            val space = if (weightCount == 0 || remain <= 0.px) 0.px else remain / weightCount
            var current = low
            alignable.forEach {
                it.low = current
                current += it.autoSizeMin
                if (!it.size.isNumber) current += space
                it.high = current
            }
        }
    }
}