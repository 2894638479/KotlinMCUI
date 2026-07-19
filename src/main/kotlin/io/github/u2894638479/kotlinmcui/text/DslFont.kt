package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.math.Measure

/**
 * A font for text rendering.
 *
 * Provides glyph metrics and rendering. Each font has a [lineHeight] base size;
 * individual characters are scaled relative to this. [charAdvance] computes the
 * horizontal advance width for a styled character.
 *
 * @param RP The platform-specific render parameter type.
 */
interface DslFont<RP> {
    val lineHeight: Measure
    fun glyph(code:Int) : DslGlyph
    context(renderParam: RP)
    fun renderChar(char: DslRenderableChar, x: Measure, y: Measure, effectLeft: Measure, effectRight: Measure)

    fun charAdvance(char: DslRenderableChar) = glyph(char.code).advance(char.style) * (char.size / lineHeight)
    fun charHeight(char: DslRenderableChar) = char.size
}