package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

interface DslGlyph {
    val normalAdvance: Measure
    val boldOffset: Measure
    val shadowOffset: Measure

    fun advance(style: DslCharStyle) = normalAdvance + if(style.isBold) boldOffset else 0.px
}