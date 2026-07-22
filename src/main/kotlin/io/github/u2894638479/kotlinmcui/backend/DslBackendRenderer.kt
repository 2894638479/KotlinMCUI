package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
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

    /**
     * Render a list of colored vertices as a filled polygon.
     *
     * This is the primitive fill operation — backends should implement this
     * directly. Order determines the shape: vertices are connected in sequence
     * to form a triangle fan where vertices[0] is the hub.
     */
    context(renderParam: RP)
    fun renderVertices(vertices: List<Pair<Position, Color>>)

    /**
     * Draw a gradient-filled rectangle. Default implementation delegates to
     * [renderVertices] with the four corner vertices.
     */
    context(renderParam: RP)
    fun fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color) {
        renderVertices(listOf(
            Position(rect.left, rect.top) to lt,
            Position(rect.right, rect.top) to rt,
            Position(rect.right, rect.bottom) to rb,
            Position(rect.left, rect.bottom) to lb,
        ))
    }

    /** Draw a solid filled rectangle. Default delegates to [fillRectGradient]. */
    context(renderParam: RP)
    fun fillRect(rect: Rect, color: Color) {
        fillRectGradient(rect, color, color, color, color)
    }

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
