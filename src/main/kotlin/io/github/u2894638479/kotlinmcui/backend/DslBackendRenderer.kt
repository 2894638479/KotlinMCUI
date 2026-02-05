package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.text.DslFont
import java.io.File

interface DslBackendRenderer<RP> {
    context(renderParam: RP)
    fun fillRect(rect: Rect, color: Color, z: Measure = 0.px)
    context(renderParam: RP)
    fun fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color)
    context(renderParam: RP)
    fun renderButton(rect: Rect, highlighted: Boolean, active: Boolean, color: Color = Color.WHITE)
    context(renderParam: RP)
    fun renderEditBox(rect: Rect, highlighted: Boolean, color: Color = Color.WHITE)

    context(renderParam:RP)
    fun renderImage(image: ImageHolder, rect: Rect, uv: Rect, color: Color = Color.WHITE)
    context(ctx: DslScaleContext, renderParam: RP)
    fun renderDefaultBackground(rect: Rect)

    context(renderParam: RP)
    fun withScissor(rect: Rect, block: () -> Unit)

    val guiScale: Double
    val isInGame: Boolean
    val defaultFont: DslFont<RP>

    fun getFont(name:String?): DslFont<RP>

    fun loadLocalImage(file: File): ImageHolder
    fun forceLoadLocalImage(file: File): ImageHolder

    fun playButtonSound()
}