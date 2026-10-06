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

    /**
     * Draw one quad, optionally textured.
     *
     * This is the only drawing primitive a backend has to implement: everything the
     * framework draws is a quad, and the helpers below are written in terms of it
     * (see [fillRectGradient], [fillRect], [renderImage]). Overriding those instead
     * would fork the contract, so a backend that wants a platform fast path for solid
     * rectangles should recognise the case here — an untextured quad whose four colours
     * are equal.
     *
     * The four corners of [rect] are shaded individually — [lt], [rt], [lb], [rb] are
     * left-top, right-top, left-bottom and right-bottom — and drawn as the two triangles
     * `(lt, rt, lb)` and `(rt, rb, lb)`.
     *
     * [image] and [uv] describe the texture: [uv] is the source rectangle inside the
     * image, in image pixel space (top-left origin). Pass `null` for both to draw colour
     * only.
     *
     * Every parameter is a value class or an existing reference, so a call allocates
     * nothing. Keep it that way: a per-vertex object graph here costs hundreds of bytes
     * per rectangle, and a frame draws hundreds of them.
     */
    context(renderParam: RP)
    fun renderQuad(
        rect: Rect,
        lt: Color,
        rt: Color,
        lb: Color,
        rb: Color,
        image: ImageHolder? = null,
        uv: Rect? = null,
    )

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
     * Render the default screen background (e.g. dirt texture in Minecraft).
     */
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

/**
 * Draw a gradient-filled rectangle.
 *
 * One of the drawing helpers written in terms of [DslBackendRenderer.renderQuad]. It is
 * an extension rather than an interface member so that the interface keeps a single
 * drawing primitive: a backend that wants its own solid-rectangle path does it inside
 * `renderQuad`, where every caller benefits. Allocates nothing.
 */
context(renderParam: RP)
fun <RP> DslBackendRenderer<RP>.fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color) =
    renderQuad(rect, lt, rt, lb, rb)

/** Draw a solid filled rectangle. See [fillRectGradient] for why this is an extension. */
context(renderParam: RP)
fun <RP> DslBackendRenderer<RP>.fillRect(rect: Rect, color: Color) =
    renderQuad(rect, color, color, color, color)

/**
 * Draw [image] into [rect], taking its pixels from [uv] (image pixel space) and tinting
 * them with [color]. See [fillRectGradient] for why this is an extension.
 */
context(renderParam: RP)
fun <RP> DslBackendRenderer<RP>.renderImage(image: ImageHolder, rect: Rect, uv: Rect, color: Color = Color.WHITE) =
    renderQuad(rect, color, color, color, color, image, uv)
