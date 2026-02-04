package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.text.DslRenderableChar

@DslContextMarker
class DslTextBuilderContext(private val ctx: DslContext): DslScaleContext by ctx, DslDataStoreContext by ctx {
    private val chars = mutableListOf(mutableListOf<DslRenderableChar>())

    fun String.emit(
        color: Color = Color.WHITE,
        size: Measure = 9.scaled,
        style: DslCharStyle = DslCharStyle()
    ) {
        codePoints().forEach {
            if(it == '\n'.code) chars.add(mutableListOf())
            else chars.last().add(DslRenderableChar(it, style, color, size))
        }
    }

    fun enter() = chars.add(mutableListOf())

    fun toChars(): List<List<DslRenderableChar>> = chars
}