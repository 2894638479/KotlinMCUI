package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.text.DslFont

/**
 * The rendering contract for a backend.
 *
 * All drawing operations use [context receivers] to implicitly receive the
 * render parameter ([RP]) — a platform-specific object such as a matrix stack
 * or pose stack. Methods drawing Minecraft-styled widgets also receive
 * [DslScaleContext] to adjust for the current GUI scale.
 *
 * Implementations should batch draw calls where possible and only flush when
 * [flush] is called or when the batch must be committed (e.g. before a scissor
 * or transform change).
 *
 * @param RP The platform-specific render parameter type.
 */
interface DslBackendRenderer<RP> {
    /** The current GUI scale factor (e.g. 2.0 for doubled resolution). */
    val guiScale: Double

    /** Flush any pending batched draw calls to the render target. */
    context(renderParam: RP)
    fun flush()

    /** Draw a solid filled rectangle. */
    context(renderParam: RP)
    fun fillRect(rect: Rect, color: Color)

    /**
     * Draw a gradient-filled rectangle.
     * @param lt Color at the left-top corner.
     * @param rt Color at the right-top corner.
     * @param lb Color at the left-bottom corner.
     * @param rb Color at the right-bottom corner.
     */
    context(renderParam: RP)
    fun fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color)

    /**
     * Render a Minecraft-style button background.
     * @param highlighted Whether the button is currently hovered or focused.
     * @param active Whether the button is interactive.
     */
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderButton(rect: Rect, highlighted: Boolean, active: Boolean, color: Color = Color.WHITE)

    /** Render a Minecraft-style item slot background. */
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderSlot(rect: Rect)

    /** Render a Minecraft-style container/inventory background. */
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderContainer(rect: Rect)

    /** Render a tooltip background. */
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderTooltip(rect: Rect)

    /**
     * Render a Minecraft item icon.
     * @param item The item identifier (e.g. `"minecraft:diamond"`).
     * @param count Stack size displayed on the icon.
     * @param damage Durability fraction (0.0 = full, 1.0 = broken), or null to hide.
     * @param enchanted Whether to render the enchantment glint effect.
     */
    context(renderParam: RP, ctx: DslScaleContext)
    fun renderItem(rect: Rect, item: String, count: Int, damage: Double?, enchanted: Boolean)

    /**
     * Render an image with optional UV mapping and tint.
     * @param uv The UV rectangle within the image texture, or the full image rect to use the entire texture.
     */
    context(renderParam: RP)
    fun renderImage(image: ImageHolder, rect: Rect, uv: Rect, color: Color = Color.WHITE)

    /** Render the default screen background (e.g. dirt texture in Minecraft). */
    context(ctx: DslScaleContext, renderParam: RP)
    fun renderDefaultBackground(rect: Rect)

    /**
     * Execute [block] with rendering clipped to [rect].
     * Nested scissor calls should intersect with the current clip region.
     */
    context(renderParam: RP)
    fun withScissor(rect: Rect, block: () -> Unit)

    /**
     * Execute [block] with the given transform matrix applied.
     * Nested transform calls should compose with the current transform.
     */
    context(renderParam: RP)
    fun withTransform(transform: Transform, block: () -> Unit)

    /**
     * Retrieve a font for text rendering.
     * @param name Optional font identifier, or `null` for the default font.
     */
    fun getFont(name: String? = null): DslFont<RP>
}
