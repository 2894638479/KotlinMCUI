package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.text.DslFont
import java.io.File

interface DslBackendRenderer<RP> {
    context(renderPara:RP)
    fun fillRect(rect: Rect, color: Color, z: Measure = 0.px)
    context(renderPara:RP)
    fun fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color)
    context(renderPara:RP)
    fun renderButton(rect: Rect, highlighted: Boolean, active: Boolean, color: Color = Color.WHITE)
    context(renderPara:RP)
    fun renderEditBox(rect: Rect, highlighted: Boolean, color: Color = Color.WHITE)

    context(renderPara: RP)
    fun withScissor(rect: Rect, block:()-> Unit)

    val guiScale: Double
    val isInGame: Boolean
    val defaultFont: DslFont<RP>
    fun getFont(name:String?): DslFont<RP>

    fun loadLocalImage(file: File): ImageHolder
    fun forceLoadLocalImage(file: File): ImageHolder

    context(renderPara:RP)
    fun renderImage(image: ImageHolder, rect: Rect, uv: Rect, color: Color = Color.WHITE)

    fun playButtonSound()

    context(ctx: DslContext)
    fun defaultBackground(id:Any): DslChild
}