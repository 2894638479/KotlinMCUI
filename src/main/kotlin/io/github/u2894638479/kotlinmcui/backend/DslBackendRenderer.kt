package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.text.DslFont

interface DslBackendRenderer<RP> {
    context(renderParam: RP)
    fun fillRect(rect: Rect, color: Color)
    context(renderParam: RP)
    fun fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color)
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderButton(rect: Rect, highlighted: Boolean, active: Boolean, color: Color = Color.WHITE)
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderSlot(rect: Rect)
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderContainer(rect: Rect)
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderItem(rect: Rect, item: String, count: Int, damage: Double?, enchanted: Boolean)

    context(renderParam:RP)
    fun renderImage(image: ImageHolder, rect: Rect, uv: Rect, color: Color = Color.WHITE)
    context(ctx: DslScaleContext, renderParam: RP)
    fun renderDefaultBackground(rect: Rect)

    context(renderParam: RP)
    fun withScissor(rect: Rect, block: () -> Unit)
    fun getFont(name:String? = null): DslFont<RP>
}