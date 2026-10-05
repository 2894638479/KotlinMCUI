package io.github.u2894638479.kotlinmcui.widget

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.DslBackendTriangles
import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.backend.fillRect
import io.github.u2894638479.kotlinmcui.backend.triangles
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
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.*
import kotlin.reflect.KMutableProperty0

/** Which palette a [Controls.ColorEdit] shows. */
enum class ColorPicker { Square, Triangle }

/** One channel of a colour, as edited by [Controls.ColorSlider]. */
enum class ColorChannel { Red, Green, Blue, Alpha, Hue, Saturation, Value }

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
internal fun triangleWeights(x: Double, y: Double): Triple<Double,Double,Double>? =
    rawWeights(x,y).takeIf { it.isInside() }

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

/** A view of one channel of [color] as a 0..1 value, for a slider to edit. */
internal fun channelView(color: StableRW<Color>, channel: ColorChannel): StableRW<Double> = object : StableRW<Double> {
    override fun getValue(): Double {
        val value = color.value
        return when(channel) {
            ColorChannel.Red -> value.rDouble
            ColorChannel.Green -> value.gDouble
            ColorChannel.Blue -> value.bDouble
            ColorChannel.Alpha -> value.aDouble
            ColorChannel.Hue -> value.hDouble
            ColorChannel.Saturation -> value.sDouble
            ColorChannel.Value -> value.vDouble
        }
    }

    override fun setValue(value: Double) {
        val current = color.value
        val v = value.coerceIn(0.0,1.0)
        color.value = when(channel) {
            ColorChannel.Red -> current.change(r = v)
            ColorChannel.Green -> current.change(g = v)
            ColorChannel.Blue -> current.change(b = v)
            ColorChannel.Alpha -> current.change(a = v)
            ColorChannel.Hue -> current.changeHSV(h = v)
            ColorChannel.Saturation -> current.changeHSV(s = v)
            ColorChannel.Value -> current.changeHSV(v = v)
        }
    }
}

/** The single letter a slider is labelled with; language-neutral, like the numeric readouts. */
internal fun ColorChannel.letter() = when(this) {
    ColorChannel.Red -> "r"
    ColorChannel.Green -> "g"
    ColorChannel.Blue -> "b"
    ColorChannel.Alpha -> "a"
    ColorChannel.Hue -> "h"
    ColorChannel.Saturation -> "s"
    ColorChannel.Value -> "v"
}

/** The readout of one channel in its natural range: bytes, degrees, or percent. */
internal fun ColorChannel.readout(value: Double) = when(this) {
    ColorChannel.Red, ColorChannel.Green, ColorChannel.Blue, ColorChannel.Alpha -> (value * 255).toInt().toString()
    ColorChannel.Hue -> "${(value * 360).toInt()}°"
    ColorChannel.Saturation, ColorChannel.Value -> "${(value * 100).toInt()}%"
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
private fun <RP> DslBackendRenderer<RP>.outline(rect: Rect, thickness: Measure, color: Color) {
    fillRect(Rect(rect.left,rect.top,rect.right,rect.top + thickness),color)
    fillRect(Rect(rect.left,rect.bottom - thickness,rect.right,rect.bottom),color)
    fillRect(Rect(rect.left,rect.top,rect.left + thickness,rect.bottom),color)
    fillRect(Rect(rect.right - thickness,rect.top,rect.right,rect.bottom),color)
}

/** The small ring that marks where in a palette the current colour sits. */
context(renderParam: RP)
private fun <RP> DslBackendRenderer<RP>.paletteMarker(rect: Rect, x: Double, y: Double, unit: Measure) {
    val centerX = rect.left + rect.width * x.coerceIn(0.0,1.0)
    val centerY = rect.top + rect.height * y.coerceIn(0.0,1.0)
    val marker = Rect(centerX - unit * 3,centerY - unit * 3,centerX + unit * 3,centerY + unit * 3)
    outline(marker,unit,Color(0,0,0,200))
    outline(Rect(centerX - unit,centerY - unit,centerX + unit,centerY + unit),unit,Color.WHITE)
}

context(ctx: DslContext)
fun Controls.ColorSquare(
    prop: StableRW<Color>,
    modifier: Modifier = Modifier,
    id: Any,
): DslChild {
    val hue = local(newChildId(id)) { prop.value.hDouble }
    val dragging = local(newChildId(id)) { false }
    val input = dslBackend
    val unit = (ctx.scale * 2).px
    return collect(object : DslComponent by DslComponentImpl(newChildId(id),modifier.padding(unit * 3),ctx) {
        /**
         * Apply the colour under [mouse]. A press out of the palette is refused; a drag may leave
         * it ([clamp]), and is then held at the nearest point of the square.
         */
        private fun pick(mouse: Position, clamp: Boolean = false): Color? {
            val rect = instance.rect
            if(rect.width <= 0.px || rect.height <= 0.px) return null
            var x = (mouse.x - rect.left) / rect.width
            var y = (mouse.y - rect.top) / rect.height
            if(x < 0 || x > 1 || y < 0 || y > 1) {
                if(!clamp) return null
                val nearest = nearestOnUnitSquare(x,y)
                x = nearest.first
                y = nearest.second
            }
            val color = squareColor(hue.value,x,y,prop.value.aDouble)
            prop.value = color
            return color
        }

        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            val color = prop.value
            // A release outside the component (or outside the game window) never arrives, so the
            // drag is reconciled with the button that is actually held.
            if(dragging.value && !input.isMouseDown(MouseButton.LEFT)) dragging.value = false
            if(color.sDouble > 0.0) hue.value = color.hDouble
            val pure = Color.ofHSV(hue.value,1.0,1.0)
            backend.renderQuad(instance.rect,Color.WHITE,pure,Color.BLACK,Color.BLACK)
            backend.paletteMarker(instance.rect,color.sDouble,1.0 - color.vDouble,unit)
        }

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton): Boolean {
            if(mouseButton != MouseButton.LEFT) return false
            // Arm the drag only for a press this palette actually takes. A press elsewhere still
            // reaches it first (the tree stops at the first component returning true), and a flag
            // armed there would make this component swallow that other press's release.
            if(pick(mouse) == null) return false
            dragging.value = true
            return true
        }

        context(mouse: Position)
        override fun mouseMove() {
            if(!dragging.value) return
            if(!input.isMouseDown(MouseButton.LEFT)) {
                dragging.value = false
                return
            }
            pick(mouse, clamp = true)
        }

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseUp(mouseButton: MouseButton): Boolean {
            if(mouseButton != MouseButton.LEFT || !dragging.value) return false
            dragging.value = false
            return true
        }
    })
}

/**
 * The classic hue triangle: the pure hue on top, white bottom-left, black bottom-right.
 *
 * Drawing needs the optional [DslBackendTriangles] capability; without it the picker still works
 * (pressing sets the colour) but nothing is drawn — [Controls.ColorEdit] hides the mode instead of
 * offering an invisible area.
 */
context(ctx: DslContext)
fun Controls.ColorTriangle(
    prop: StableRW<Color>,
    modifier: Modifier = Modifier,
    id: Any,
): DslChild {
    val hue = local(newChildId(id)) { prop.value.hDouble }
    val dragging = local(newChildId(id)) { false }
    val input = dslBackend
    val unit = (ctx.scale * 2).px
    return collect(object : DslComponent by DslComponentImpl(newChildId(id),modifier.padding(unit * 3),ctx) {
        /** See the square's picker: a drag out of the triangle is held at its nearest edge. */
        private fun pick(mouse: Position, clamp: Boolean = false): Color? {
            val rect = instance.rect
            if(rect.width <= 0.px || rect.height <= 0.px) return null
            var x = (mouse.x - rect.left) / rect.width
            var y = (mouse.y - rect.top) / rect.height
            if(triangleWeights(x,y) == null) {
                if(!clamp) return null
                val nearest = nearestOnUnitTriangle(x,y)
                x = nearest.first
                y = nearest.second
            }
            val color = triangleColor(hue.value,x,y,prop.value.aDouble) ?: return null
            prop.value = color
            return color
        }

        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            val rect = instance.rect
            val color = prop.value
            if(dragging.value && !input.isMouseDown(MouseButton.LEFT)) dragging.value = false
            if(color.sDouble > 0.0) hue.value = color.hDouble
            val triangle = backend.triangles()
            if(triangle != null && rect.width > 0.px && rect.height > 0.px) {
                val left = rect.left.pixelsOrElse { 0f }
                val right = rect.right.pixelsOrElse { 0f }
                val top = rect.top.pixelsOrElse { 0f }
                val bottom = rect.bottom.pixelsOrElse { 0f }
                val pure = Color.ofHSV(hue.value,1.0,1.0)
                triangle.renderTriangles(
                    floatArrayOf((left + right) / 2,top,left,bottom,right,bottom),
                    intArrayOf(pure.rgbaInt,Color.WHITE.rgbaInt,Color.BLACK.rgbaInt),
                )
            }
            val (markerX,markerY) = triangleMarker(color)
            backend.paletteMarker(rect,markerX,markerY,unit)
        }

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton): Boolean {
            if(mouseButton != MouseButton.LEFT) return false
            // Arm the drag only for a press this palette actually takes. A press elsewhere still
            // reaches it first (the tree stops at the first component returning true), and a flag
            // armed there would make this component swallow that other press's release.
            if(pick(mouse) == null) return false
            dragging.value = true
            return true
        }

        context(mouse: Position)
        override fun mouseMove() {
            if(!dragging.value) return
            if(!input.isMouseDown(MouseButton.LEFT)) {
                dragging.value = false
                return
            }
            pick(mouse, clamp = true)
        }

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseUp(mouseButton: MouseButton): Boolean {
            if(mouseButton != MouseButton.LEFT || !dragging.value) return false
            dragging.value = false
            return true
        }
    })
}

/** The hue circle is not linear in RGB, but each 60° sector between two primary hues is. */
private const val HUE_SECTORS = 6

/**
 * A hue bar: press or drag along it to move the hue, leaving saturation, value and alpha alone.
 *
 * Six quads, one per sector between neighbouring primary hues, are exact — the hue is nonlinear in
 * RGB over the whole circle, but linear inside a sector.
 */
context(ctx: DslContext)
fun Controls.ColorHue(
    prop: StableRW<Color>,
    modifier: Modifier = Modifier,
    axis: Axis = Axis.Horizontal,
    id: Any,
): DslChild {
    val dragging = local(newChildId(id)) { false }
    val input = dslBackend
    val unit = (ctx.scale * 2).px
    return collect(object : DslComponent by DslComponentImpl(newChildId(id),modifier.padding(unit * 3),ctx) {
        private fun pick(mouse: Position, clamp: Boolean = false): Color? {
            val rect = instance.rect
            if(rect.width <= 0.px || rect.height <= 0.px) return null
            val x = (mouse.x - rect.left) / rect.width
            val y = (mouse.y - rect.top) / rect.height
            // The bar is the whole rectangle: only one axis carries the hue, but both have to be on
            // the bar, or a press beside it would move the hue.
            if(!clamp && (x < 0 || x > 1 || y < 0 || y > 1)) return null
            val hue = if(axis == Axis.Horizontal) x.coerceIn(0.0,1.0) else y.coerceIn(0.0,1.0)
            val color = prop.value.changeHSV(h = hue)
            prop.value = color
            return color
        }

        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            val rect = instance.rect
            if(dragging.value && !input.isMouseDown(MouseButton.LEFT)) dragging.value = false
            repeat(HUE_SECTORS) { index ->
                val from = index.toDouble() / HUE_SECTORS
                val to = (index + 1).toDouble() / HUE_SECTORS
                val begin = Color.ofHSV(from,1.0,1.0)
                val end = Color.ofHSV(to,1.0,1.0)
                // The corners are left-top, right-top, left-bottom, right-bottom, so the gradient has
                // to run along the bar's own axis: along x when it is horizontal, along y otherwise.
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

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton): Boolean {
            if(mouseButton != MouseButton.LEFT) return false
            if(pick(mouse) == null) return false
            dragging.value = true
            return true
        }

        context(mouse: Position)
        override fun mouseMove() {
            if(!dragging.value) return
            if(!input.isMouseDown(MouseButton.LEFT)) {
                dragging.value = false
                return
            }
            pick(mouse, clamp = true)
        }

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseUp(mouseButton: MouseButton): Boolean {
            if(mouseButton != MouseButton.LEFT || !dragging.value) return false
            dragging.value = false
            return true
        }
    })
}

/** A horizontal slider editing one channel of [prop], labelled with its value. */
context(ctx: DslContext)
fun Controls.ColorSlider(
    channel: ColorChannel,
    prop: StableRW<Color>,
    modifier: Modifier = Modifier,
    id: Any,
): DslChild {
    val view = channelView(prop,channel)
    return Slider(modifier,Axis.Horizontal,0.0..1.0,view,id = id) {
        TextFlatten(Modifier.padding(h = 5.scaled)) {
            channel.letter().emit()
            ": ".emit()
            channel.readout(view.value).emit()
        }
    }
}

/**
 * The colour as text, live both ways: it shows the colour whenever nobody is typing in it, and
 * applies every prefix that already parses while it is focused.
 */
context(ctx: DslContext)
fun Controls.ColorText(
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

/** A compact vertical slider for the channel strip of [Controls.ColorEdit]. */
context(ctx: DslContext)
private fun channelStripSlider(channel: ColorChannel, prop: StableRW<Color>, id: Any) = Slider(
    Modifier.width(20.scaled).padding(1.scaled),Axis.Vertical,
    object: StableRW<Double> {
        private val view = channelView(prop,channel)
        override fun getValue() = 1 - view.value
        override fun setValue(value: Double) { view.value = 1 - value }
    },
    id = id,
) {
    TextAutoFold { channel.letter().emit() }
}

/**
 * A full colour editor: a preview, a hexadecimal field, one of two palettes with the alpha
 * alongside, and a strip with every channel.
 *
 * The palette is chosen with the button next to it and remembered per [id]; the triangle is only
 * offered when the backend can draw one, because a picker nobody can see is worse than none.
 */
context(ctx: DslContext)
fun Controls.ColorEdit(prop: KMutableProperty0<Color>, id: Any = prop) = ColorEdit(prop.property,ColorPicker.Square,id)

context(ctx: DslContext)
fun Controls.ColorEdit(
    prop: StableRW<Color>,
    picker: ColorPicker = ColorPicker.Square,
    id: Any = prop,
): DslChild {
    val canDrawTriangle = dslBackend is DslBackendTriangles<*>
    val mode = local(newChildId(id)) { if(canDrawTriangle) picker else ColorPicker.Square }
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
                if(canDrawTriangle) Controls.EnumButton(
                    mode,
                    translate("kotlinmcui.widget.color.picker"),
                    { translate("kotlinmcui.widget.color.${it.name.lowercase()}") },
                    newChildId(id),
                )
                ColorSlider(ColorChannel.Alpha,prop,Modifier.width(60.scaled),newChildId(id))
            }
        }
        val scrollerProp = local(newChildId(id)) { Scroller.empty }
        ScrollableRow(Modifier.minHeight(50.scaled),scrollerProp) {
            ColorChannel.entries.forEach { channelStripSlider(it,prop,newChildId(id) + it) }
        }
        if(scrollerProp.value.isScrollable()) ScrollBar(Modifier.height(10.scaled),scrollerProp,Axis.Horizontal) {}
    }
}
