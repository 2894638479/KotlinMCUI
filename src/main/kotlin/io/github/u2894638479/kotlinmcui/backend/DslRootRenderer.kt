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
 * **Every draw reaches the screen in the order the calls were made.** A backend may batch, defer or
 * reorder freely, as long as the order it produces draws each pixel the same way the call order would:
 * draws whose pixels are disjoint may swap, and nothing else may. The framework states its layering by
 * calling in order - a container renders its children last-to-first, so the first declared child is on
 * top - and relies on this one property to keep it.
 *
 * @param RP The platform-specific render parameter type.
 */
interface DslRootRenderer<RP> {
    /** The current GUI scale factor (e.g. 2.0 for doubled resolution). */
    val guiScale: Double

    /**
     * Draw one quad, optionally textured.
     *
     * One of the two drawing primitives a backend has to implement (the other is
     * [renderTriangles]; ADR 0004 made that mandatory, so there is no "if the backend has
     * it" branch anywhere). Everything the framework draws is either a quad or a polygon
     * list, and the helpers below are written in terms of this one
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
     * Submit a triangle list; every three vertices form one triangle.
     *
     * [renderQuad] cannot express an arbitrary triangle: its four corners are the corners of a
     * rectangle, so any affine mapping of them is a parallelogram. Shapes that are not
     * parallelograms — a fan for a polygon, a slanted outline, a chart — therefore come
     * through here.
     *
     * @param vertices two floats `(x, y)` per vertex, or four `(x, y, u, v)` when [image]
     *   is given. Coordinates are in the same space as every other drawing call (the
     *   pixel space the framework measures in); UVs are image pixels from the top-left.
     *   Implementations read the array during the call, so a caller that draws every frame
     *   can keep reusing one buffer.
     * @param colors one packed RGBA colour per vertex, in the same order as [vertices].
     * @param image the texture for the UV-carrying form, or `null` for colour only.
     */
    context(renderParam: RP)
    fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder? = null)

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
 * One of the drawing helpers written in terms of [DslRootRenderer.renderQuad]. It is an
 * extension so every call site shares one implementation; a backend that wants its own
 * solid-rectangle fast path does it inside `renderQuad`, where every caller benefits.
 * Allocates nothing.
 */
context(renderParam: RP)
fun <RP> DslRootRenderer<RP>.fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color) =
    renderQuad(rect, lt, rt, lb, rb)

/** Draw a solid filled rectangle. See [fillRectGradient] for why this is an extension. */
context(renderParam: RP)
fun <RP> DslRootRenderer<RP>.fillRect(rect: Rect, color: Color) =
    renderQuad(rect, color, color, color, color)

/**
 * Draw [image] into [rect], taking its pixels from [uv] (image pixel space) and tinting
 * them with [color]. See [fillRectGradient] for why this is an extension.
 */
context(renderParam: RP)
fun <RP> DslRootRenderer<RP>.renderImage(image: ImageHolder, rect: Rect, uv: Rect, color: Color = Color.WHITE) =
    renderQuad(rect, color, color, color, color, image, uv)

/**
 * Fill a convex polygon as a triangle fan, one colour per vertex.
 *
 * Convenience for [DslRootRenderer.renderTriangles]: it rebuilds its arrays on every
 * call, which is fine for the shapes that need it — the framework itself never calls
 * this. A concave polygon, or one that needs texture coordinates, submits its own
 * triangles through [DslRootRenderer.renderTriangles].
 *
 * A vertex whose coordinates are not plain numbers (an unmeasured value) makes the whole
 * call draw nothing, like the other drawing entry points.
 */
context(renderParam: RP)
fun <RP> DslRootRenderer<RP>.fillPolygon(points: List<Position>, colors: List<Color>) {
    require(points.size >= 3) { "a polygon needs at least three vertices, got ${points.size}" }
    require(colors.size == points.size) {
        "one colour per vertex, got ${colors.size} for ${points.size} vertices"
    }
    val vertices = FloatArray((points.size - 2) * 6)
    val packed = IntArray((points.size - 2) * 3)
    var v = 0
    var c = 0
    for (i in 1 until points.size - 1) {
        // The fan's apex is the first vertex, so every triangle is (0, i, i + 1).
        val apexX = points[0].x.pixelsOrElse<Float> { return }
        val apexY = points[0].y.pixelsOrElse<Float> { return }
        val x0 = points[i].x.pixelsOrElse<Float> { return }
        val y0 = points[i].y.pixelsOrElse<Float> { return }
        val x1 = points[i + 1].x.pixelsOrElse<Float> { return }
        val y1 = points[i + 1].y.pixelsOrElse<Float> { return }
        vertices[v++] = apexX; vertices[v++] = apexY
        vertices[v++] = x0; vertices[v++] = y0
        vertices[v++] = x1; vertices[v++] = y1
        packed[c++] = colors[0].rgbaInt
        packed[c++] = colors[i].rgbaInt
        packed[c++] = colors[i + 1].rgbaInt
    }
    renderTriangles(vertices, packed)
}
