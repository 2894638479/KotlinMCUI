package io.github.u2894638479.kotlinmcui.widget

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.backend.fillRect
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.dsl.translate
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.scroll.Scroller
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.*
import kotlin.reflect.KMutableProperty0

/**
 * Colour picker widgets.
 *
 * The top-level geometry and hex helpers in this file are `internal` because the widget tests assert them
 * directly (the test source set is associated with this module, so it sees `internal`); everything else here
 * is `private`. Keep the split that way: a helper that no test and no other file needs belongs to neither.
 */

/** Which palette a [Colors.ColorEdit] shows. */
enum class ColorPicker { Square, Triangle }

/** The channels the editor edits, in the order its strip shows them. [ColorProp] carries each one's name and reading. */
private val channels: List<ColorProp<Double>> = listOf(RDouble, GDouble, BDouble, ADouble, HDouble, SDouble, VDouble)

/**
 * The colour at [x] (saturation, 0..1) and [y] (from the top, so value is `1 - y`) of the
 * saturation/value square of [hue].
 *
 * The square is drawn as one quad whose corners are white, the pure hue, black and black, so
 * this is exactly what the backend interpolates between them.
 */
internal fun squareColor(hue: Double, x: Double, y: Double, alpha: Double): Color =
    Color.ofHSV(hue, x, 1.0 - y, alpha)

/** The point of the 0..1 square nearest to [x],[y]. The square palette *is* the rectangle. */
internal fun nearestOnUnitSquare(x: Double, y: Double) = x.coerceIn(0.0,1.0) to y.coerceIn(0.0,1.0)

/**
 * The corners of the palettes' triangles in 0..1 rectangle space: the first hue on top, the second
 * bottom-left, the third bottom-right.
 */
private val TRIANGLE_CORNERS = arrayOf(0.5 to 0.0,0.0 to 1.0,1.0 to 1.0)

/**
 * The point of that triangle nearest to [x],[y]: the closest of the three edge projections, which
 * covers the corners as well. Dropping the negative barycentric weights and renormalising is *not*
 * the same thing - it drifts along an edge instead of staying at the closest point.
 */
internal fun nearestOnUnitTriangle(x: Double, y: Double): Pair<Double,Double> {
    var nearestX = 0.0
    var nearestY = 0.0
    var nearestDistance = Double.MAX_VALUE
    for(index in TRIANGLE_CORNERS.indices) {
        val (ax,ay) = TRIANGLE_CORNERS[index]
        val (bx,by) = TRIANGLE_CORNERS[(index + 1) % TRIANGLE_CORNERS.size]
        val dx = bx - ax
        val dy = by - ay
        val ratio = (((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)).coerceIn(0.0,1.0)
        val px = ax + dx * ratio
        val py = ay + dy * ratio
        val distance = (x - px) * (x - px) + (y - py) * (y - py)
        if(distance < nearestDistance) {
            nearestDistance = distance
            nearestX = px
            nearestY = py
        }
    }
    return nearestX to nearestY
}

/** The three weights of [x],[y] without asking whether the point is inside; negative means outside. */
private fun rawWeights(x: Double, y: Double): Triple<Double,Double,Double> {
    val hue = 1.0 - y
    val white = 0.5 + 0.5 * y - x
    return Triple(hue,white,1.0 - hue - white)
}

private fun Triple<Double,Double,Double>.isInside() =
    first >= -1e-6 && second >= -1e-6 && third >= -1e-6

/**
 * The barycentric weights of the point [x],[y] (0..1 of the picker rectangle) inside the hue
 * triangle — pure hue on top, white bottom-left, black bottom-right — or `null` when the point
 * is outside it.
 */
private fun triangleWeights(x: Double, y: Double): Triple<Double,Double,Double>? =
    rawWeights(x,y).takeIf { it.isInside() }

/** The point of the unit triangle: on it, or - while dragging - its nearest edge. */
private fun unitTrianglePoint(x: Double, y: Double, clamp: Boolean) =
    if(triangleWeights(x,y) != null) x to y else if(clamp) nearestOnUnitTriangle(x,y) else null

/**
 * The colour at [x],[y] **on** the triangle, or `null` when the point is outside it.
 *
 * The three corners are mixed linearly because that is what the backend does across a triangle,
 * so the colour picked is the colour seen. White and black contribute to every channel equally,
 * which is why the mix can be written per channel as `hueWeight * pureHue + whiteWeight`.
 */
internal fun triangleColor(hue: Double, x: Double, y: Double, alpha: Double): Color? {
    val (hueWeight,whiteWeight) = triangleWeights(x,y) ?: return null
    val pure = Color.ofHSV(hue,1.0,1.0)
    return Color(
        pure.rDouble * hueWeight + whiteWeight,
        pure.gDouble * hueWeight + whiteWeight,
        pure.bDouble * hueWeight + whiteWeight,
        alpha,
    )
}

/**
 * Where the marker of [color] belongs inside the triangle, in 0..1 rectangle space.
 *
 * The pure hue contributes one channel at 1, one at 0 and one in between, so the smallest
 * channel is the white weight and the difference between the largest and the smallest is the
 * hue weight; the rest is black.
 */
internal fun triangleMarker(color: Color): Pair<Double,Double> {
    val max = maxOf(color.rDouble,color.gDouble,color.bDouble)
    val min = minOf(color.rDouble,color.gDouble,color.bDouble)
    val hueWeight = (max - min).coerceIn(0.0,1.0)
    val blackWeight = (1.0 - hueWeight - min).coerceIn(0.0,1.0)
    return (0.5 * hueWeight + blackWeight) to (1.0 - hueWeight)
}

/**
 * Where the marker of [color] belongs in the hue triangle, in 0..1 rectangle space.
 *
 * The colour there *is* the mixture of the three corners, so its three components are the weights;
 * they are normalised by their sum because a mixture scaled by its value is the same point. A black
 * colour has no position at all, and the centre - its nearest point - is used instead.
 */
internal fun hueTriangleMarker(color: Color): Pair<Double,Double> {
    val sum = color.rDouble + color.gDouble + color.bDouble
    if(sum <= 0.0) return 0.5 to 2.0 / 3
    val red = color.rDouble / sum
    val blue = color.bDouble / sum
    return (0.5 * red + blue) to (1.0 - red)
}

/** `#rrggbb`, or `#rrggbbaa` while the colour is not opaque. */
internal fun formatColorHex(color: Color) =
    if(color.aInt == 255) "#%02x%02x%02x".format(color.rInt,color.gInt,color.bInt)
    else "#%02x%02x%02x%02x".format(color.rInt,color.gInt,color.bInt,color.aInt)

/**
 * Parses `#rgb`, `#rgba`, `#rrggbb` or `#rrggbbaa`; the `#` is optional and case does not
 * matter. Returns `null` for anything else, so a half-typed value simply leaves the colour
 * alone. Channels are in the order the field shows them, so alpha — when present — is last.
 */
internal fun parseColorHex(text: String): Color? {
    val digits = text.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    if(digits.length != 3 && digits.length != 4 && digits.length != 6 && digits.length != 8) return null
    if(!digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    fun byte(index: Int,width: Int) = digits.substring(index,index + width).toInt(16).let { if(width == 1) it * 17 else it }
    return when(digits.length) {
        3 -> Color(byte(0,1),byte(1,1),byte(2,1))
        4 -> Color(byte(0,1),byte(1,1),byte(2,1),byte(3,1))
        6 -> Color(byte(0,2),byte(2,2),byte(4,2))
        else -> Color(byte(0,2),byte(2,2),byte(4,2),byte(6,2))
    }
}

/** Draw the outline of [rect] as four filled edges, so the middle stays visible. */
context(renderParam: RP)
private fun <RP> DslRootRenderer<RP>.outline(rect: Rect, thickness: Measure, color: Color) {
    fillRect(Rect(rect.left,rect.top,rect.right,rect.top + thickness),color)
    fillRect(Rect(rect.left,rect.bottom - thickness,rect.right,rect.bottom),color)
    fillRect(Rect(rect.left,rect.top,rect.left + thickness,rect.bottom),color)
    fillRect(Rect(rect.right - thickness,rect.top,rect.right,rect.bottom),color)
}

/** The small ring that marks where in a palette the current colour sits. */
context(renderParam: RP)
private fun <RP> DslRootRenderer<RP>.paletteMarker(rect: Rect, x: Double, y: Double, unit: Measure) {
    val centerX = rect.left + rect.width * x.coerceIn(0.0,1.0)
    val centerY = rect.top + rect.height * y.coerceIn(0.0,1.0)
    val marker = Rect(centerX - unit * 3,centerY - unit * 3,centerX + unit * 3,centerY + unit * 3)
    outline(marker,unit,Color(0,0,0,200))
    outline(Rect(centerX - unit,centerY - unit,centerX + unit,centerY + unit),unit,Color.WHITE)
}

/** The apex, bottom-left and bottom-right of [rect], written into [out] as the triangle-list primitive wants them. */
private fun Rect.writeTriangleVertices(out: FloatArray) {
    val left = left.pixelsOrElse { 0f }
    val right = right.pixelsOrElse { 0f }
    val top = top.pixelsOrElse { 0f }
    val bottom = bottom.pixelsOrElse { 0f }
    out[0] = (left + right) / 2
    out[1] = top
    out[2] = left
    out[3] = bottom
    out[4] = right
    out[5] = bottom
}

/**
 * Draw that triangle with the three corner colours.
 *
 * The scratch arrays come from the caller, which takes them from **its own root**: they must not be
 * shared between roots, because two roots can be drawing on two threads at once.
 */
context(backend: DslRootRenderer<RP>, renderParam: RP)
private fun <RP> DslRootRenderer<RP>.paletteTriangle(
    rect: Rect, apex: Color, left: Color, right: Color,
    vertices: FloatArray, colors: IntArray,
) {
    if(rect.width <= 0.px || rect.height <= 0.px) return
    rect.writeTriangleVertices(vertices)
    colors[0] = apex.rgbaInt
    colors[1] = left.rgbaInt
    colors[2] = right.rgbaInt
    this.renderTriangles(vertices,colors)
}

/**
 * What every palette shares: a press is taken only when it lands on the shape, a drag follows the
 * pointer and is held at the shape's nearest point once it leaves, and the drag ends either on a
 * release or — because a release outside the window never arrives — when the button is no longer
 * held.
 *
 * A subclass supplies the shape: [pointAt] maps the pointer into it, [colorAt] turns a point of it
 * into a colour, and its own `render` draws it after having called [reconcileDrag].
 */
private abstract class Palette(
    delegate: DslComponent,
    private val prop: StableRW<Color>,
    private val dragging: StableRW<Boolean>,
) : DslComponent by delegate {

    /** Where [x],[y] (0..1 across the palette) is on the shape: its nearest point when [clamp], null when a press there is not ours. */
    protected abstract fun pointAt(x: Double, y: Double, clamp: Boolean): Pair<Double,Double>?

    /** The colour a point [pointAt] accepted stands for, or null when it is not on the shape after all. */
    protected abstract fun colorAt(x: Double, y: Double): Color?

    private fun pick(mouse: Position, clamp: Boolean): Color? {
        val rect = instance.rect
        if(rect.width <= 0.px || rect.height <= 0.px) return null
        val point = pointAt((mouse.x - rect.left) / rect.width,(mouse.y - rect.top) / rect.height,clamp) ?: return null
        val color = colorAt(point.first,point.second) ?: return null
        prop.value = color
        return color
    }

    /** End the drag when the button is no longer held; called once a frame while rendering. */
    protected fun reconcileDrag() {
        if(dragging.value && instance.dataStore?.root?.input?.isMouseDown(MouseButton.LEFT) != true) dragging.value = false
    }

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton): Boolean {
        if(mouseButton != MouseButton.LEFT) return false
        // Arm the drag only for a press this palette actually takes: a press elsewhere still reaches
        // here first, and a flag armed there would make this component swallow that press's release.
        if(pick(mouse,clamp = false) == null) return false
        dragging.value = true
        return true
    }

    context(mouse: Position)
    override fun mouseMove() {
        if(!dragging.value) return
        if(instance.dataStore?.root?.input?.isMouseDown(MouseButton.LEFT) != true) {
            dragging.value = false
            return
        }
        pick(mouse,clamp = true)
    }

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseUp(mouseButton: MouseButton): Boolean {
        if(mouseButton != MouseButton.LEFT || !dragging.value) return false
        dragging.value = false
        return true
    }
}

/** The hue circle is not linear in RGB, but each 60° sector between two primary hues is. */
private const val HUE_SECTORS = 6

/**
 * The colour editing widgets, all built from the public DSL.
 *
 * The palettes and the composite editor share the [Palette] machinery; the channels are edited
 * through the core colour slider, so [ColorProp] stays the one channel abstraction.
 */
object Colors {


    context(ctx: DslContext)
    fun ColorSquare(
        prop: StableRW<Color>,
        modifier: Modifier = Modifier,
        id: Any,
    ): DslChild {
        val hue = local(newChildId(id)) { prop.value.hDouble }
        val unit = (ctx.scale * 2).px
        return collect(object : Palette(
            DslComponentImpl(newChildId(id),modifier.padding(unit * 3),ctx),
            prop,
            local(newChildId(id)) { false },
        ) {
            /** The square is the whole rectangle, so its nearest point is a plain coerce. */
            override fun pointAt(x: Double, y: Double, clamp: Boolean) =
                if(x in 0.0..1.0 && y in 0.0..1.0) x to y else if(clamp) nearestOnUnitSquare(x,y) else null

            override fun colorAt(x: Double, y: Double) = squareColor(hue.value,x,y,prop.value.aDouble)

            context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                reconcileDrag()
                val color = prop.value
                // The square has no hue of its own: it is the last chromatic one the colour had.
                if(color.sDouble > 0.0) hue.value = color.hDouble
                backend.renderQuad(instance.rect,Color.WHITE,Color.ofHSV(hue.value,1.0,1.0),Color.BLACK,Color.BLACK)
                backend.paletteMarker(instance.rect,color.sDouble,1.0 - color.vDouble,unit)
            }
        })
    }

    /**
     * The classic hue triangle: the pure hue on top, white bottom-left, black bottom-right.
     *
     * The triangle is drawn with [DslRootRenderer.renderTriangles], so every backend can
     * show it.
     */
    context(ctx: DslContext)
    fun ColorTriangle(
        prop: StableRW<Color>,
        modifier: Modifier = Modifier,
        id: Any,
    ): DslChild {
        val hue = local(newChildId(id)) { prop.value.hDouble }
        val unit = (ctx.scale * 2).px
        return collect(object : Palette(
            DslComponentImpl(newChildId(id),modifier.padding(unit * 3),ctx),
            prop,
            local(newChildId(id)) { false },
        ) {
            override fun pointAt(x: Double, y: Double, clamp: Boolean) = unitTrianglePoint(x,y,clamp)

            override fun colorAt(x: Double, y: Double) = triangleColor(hue.value,x,y,prop.value.aDouble)

            context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                reconcileDrag()
                val rect = instance.rect
                val color = prop.value
                if(color.sDouble > 0.0) hue.value = color.hDouble
                backend.paletteTriangle(
                    rect,Color.ofHSV(hue.value,1.0,1.0),Color.WHITE,Color.BLACK,
                    instance.dataStore!!.root.triangleVertices, instance.dataStore!!.root.triangleColors,
                )
                val (markerX,markerY) = triangleMarker(color)
                backend.paletteMarker(rect,markerX,markerY,unit)
            }
        })
    }

    /**
     * A hue bar: press or drag along it to move the hue, leaving saturation, value and alpha alone.
     *
     * Six quads, one per sector between neighbouring primary hues, are exact — the hue is nonlinear in
     * RGB over the whole circle, but linear inside a sector.
     */
    context(ctx: DslContext)
    fun ColorHue(
        prop: StableRW<Color>,
        modifier: Modifier = Modifier,
        axis: Axis = Axis.Horizontal,
        id: Any,
    ): DslChild {
        val unit = (ctx.scale * 2).px
        return collect(object : Palette(
            DslComponentImpl(newChildId(id),modifier.padding(unit * 3),ctx),
            prop,
            local(newChildId(id)) { false },
        ) {
            override fun pointAt(x: Double, y: Double, clamp: Boolean): Pair<Double,Double>? {
                // The bar is the whole rectangle: only one axis carries the hue, but a press beside the
                // bar must not move it, so the point has to be on both axes.
                if(!clamp && (x < 0 || x > 1 || y < 0 || y > 1)) return null
                return if(axis == Axis.Horizontal) x.coerceIn(0.0,1.0) to 0.5
                else 0.5 to y.coerceIn(0.0,1.0)
            }

            override fun colorAt(x: Double, y: Double) = prop.value.changeHSV(h = if(axis == Axis.Horizontal) x else y)

            context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                reconcileDrag()
                val rect = instance.rect
                repeat(HUE_SECTORS) { index ->
                    val from = index.toDouble() / HUE_SECTORS
                    val to = (index + 1).toDouble() / HUE_SECTORS
                    val begin = Color.ofHSV(from,1.0,1.0)
                    val end = Color.ofHSV(to,1.0,1.0)
                    // The corners are left-top, right-top, left-bottom, right-bottom, so the ramp has to
                    // run along the bar's own axis: along x when it is horizontal, along y otherwise.
                    if(axis == Axis.Horizontal) backend.renderQuad(
                        Rect(rect.left + rect.width * from,rect.top,rect.left + rect.width * to,rect.bottom),
                        begin,end,begin,end,
                    ) else backend.renderQuad(
                        Rect(rect.left,rect.top + rect.height * from,rect.right,rect.top + rect.height * to),
                        begin,begin,end,end,
                    )
                }
                val hue = prop.value.hDouble
                backend.paletteMarker(
                    rect,
                    if(axis == Axis.Horizontal) hue else 0.5,
                    if(axis == Axis.Horizontal) 0.5 else hue,
                    unit,
                )
            }
        })
    }

    /**
     * A triangular hue palette: the corners are the pure red, green and blue hues, and a point picks
     * their mixture, so the perimeter sweeps the whole hue circle and the middle darkens.
     *
     * Unlike the square (one quad) and the bar (six quads) this needs the optional triangle-list
     * capability, because a rectangle is never a triangle. What is drawn is the backend's linear
     * interpolation between the three corners, which is exactly the mixture picked here — normalising
     * it to full value instead (which would keep the middle bright) cannot be drawn that way at all.
     */
    context(ctx: DslContext)
    fun ColorHueTriangle(
        prop: StableRW<Color>,
        modifier: Modifier = Modifier,
        id: Any,
    ): DslChild {
        val unit = (ctx.scale * 2).px
        return collect(object : Palette(
            DslComponentImpl(newChildId(id),modifier.padding(unit * 3),ctx),
            prop,
            local(newChildId(id)) { false },
        ) {
            override fun pointAt(x: Double, y: Double, clamp: Boolean) = unitTrianglePoint(x,y,clamp)

            override fun colorAt(x: Double, y: Double): Color? {
                val (red,green,blue) = triangleWeights(x,y) ?: return null
                return Color(red,green,blue,prop.value.aDouble)
            }

            context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                reconcileDrag()
                val rect = instance.rect
                val alpha = prop.value.aDouble
                backend.paletteTriangle(
                    rect,
                    Color(1.0,0.0,0.0,alpha),
                    Color(0.0,1.0,0.0,alpha),
                    Color(0.0,0.0,1.0,alpha),
                    instance.dataStore!!.root.triangleVertices, instance.dataStore!!.root.triangleColors,
                )
                val (x,y) = hueTriangleMarker(prop.value)
                backend.paletteMarker(rect,x,y,unit)
            }
        })
    }

    /**
     * The colour as text, live both ways: it shows the colour whenever nobody is typing in it, and
     * applies every prefix that already parses while it is focused.
     */
    context(ctx: DslContext)
    fun ColorText(
        prop: StableRW<Color>,
        modifier: Modifier = Modifier,
        id: Any,
    ): DslChild {
        val text = local(newChildId(id)) { formatColorHex(prop.value) }
        val field = EditableText(modifier,text,id = newChildId(id))
        if(field.currentComponent().isFocused) parseColorHex(text.value)?.let { prop.value = it }
        else text.value = formatColorHex(prop.value)
        return field.editBoxBackground()
    }

    /**
     * A full colour editor: a preview, a hexadecimal field, one of two palettes with the alpha
     * alongside, and a strip with every channel.
     *
     * The palette is chosen with the button next to it and remembered per [id]. Every picker is always
     * offered: `renderTriangles` is a mandatory primitive (see ADR 0004), so there is no backend that
     * could not draw the triangle.
     */
    context(ctx: DslContext)
    fun ColorEdit(prop: KMutableProperty0<Color>, id: Any = prop) = ColorEdit(prop.property,ColorPicker.Square,id)

    context(ctx: DslContext)
    fun ColorEdit(
        prop: StableRW<Color>,
        picker: ColorPicker = ColorPicker.Square,
        id: Any = prop,
    ): DslChild {
        val mode = local(newChildId(id)) { picker }
        return Column(id = id) {
            Row(Modifier.padding(2.scaled)) {
                ColorRect(Modifier.size(20.scaled,20.scaled).padding(1.scaled),prop.value,newChildId(id))
                ColorText(prop,Modifier.weight(1.0),newChildId(id))
            }
            Row(Modifier.padding(2.scaled)) {
                when(mode.value) {
                    ColorPicker.Square -> ColorSquare(prop,Modifier.size(80.scaled,80.scaled),newChildId(id))
                    ColorPicker.Triangle -> ColorTriangle(prop,Modifier.size(80.scaled,80.scaled),newChildId(id))
                }
                // The palettes only move saturation and value, so the hue needs its own handle.
                ColorHue(prop,Modifier.size(14.scaled,80.scaled),Axis.Vertical,newChildId(id))
                Column(Modifier.padding(2.scaled)) {
                    Editors.EnumButton(
                        mode,
                        translate("kotlinmcui.widget.color.picker"),
                        { translate("kotlinmcui.widget.color.${it.name.lowercase()}") },
                        newChildId(id),
                    )
                    channelSlider(ADouble,prop,Axis.Horizontal,Modifier.width(60.scaled),newChildId(id),label = true)
                }
            }
            val scrollerProp = local(newChildId(id)) { Scroller.empty }
            ScrollableRow(Modifier.minHeight(50.scaled),scrollerProp) {
                channels.forEach { channel ->
                    channelSlider(channel,prop,Axis.Vertical,Modifier.width(20.scaled).padding(1.scaled),newChildId(id) + channel,label = false)
                }
            }
            if(scrollerProp.value.isScrollable()) ScrollBar(Modifier.height(10.scaled),scrollerProp,Axis.Horizontal) {}
        }
    }
}

/** A slider editing [channel] of [color]: [ColorProp] is the one channel abstraction, and the core colour slider already draws the ramp. */
context(ctx: DslContext)
private fun channelSlider(
    channel: ColorProp<Double>,
    color: StableRW<Color>,
    axis: Axis,
    modifier: Modifier,
    id: Any,
    label: Boolean,
) = Slider(modifier,axis,channel,color,id = id) {
    if(label) TextFlatten(Modifier.padding(h = 5.scaled)) {
        channel.name.emit()
        ": ".emit()
        channel.toString(color.value).emit()
    } else TextAutoFold { channel.name.take(1).emit() }
}

