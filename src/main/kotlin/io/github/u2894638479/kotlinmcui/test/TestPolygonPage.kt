package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.backend.fillPolygon
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.Row
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.dsl.ui.defaultBackground
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.widget.Presets
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws regular polygons through the triangle-list primitive, which no framework component
 * uses: the polygons below are painted by a component that calls the backend it was given,
 * exactly the way user code would.
 */
context(ctx: DslContext)
fun TestPolygonPage() {
    Column(Modifier.padding(10.scaled)) {
        Presets.Text { "polygon: hexagon" }
        Row(Modifier.size(240.scaled, 110.scaled)) {
            Polygon(Modifier.size(100.scaled, 100.scaled), "hexagon", 6)
        }
        Presets.Text { "polygon: triangle" }
        Row(Modifier.size(240.scaled, 70.scaled)) {
            Polygon(Modifier.size(60.scaled, 60.scaled), "triangle", 3)
        }
    }.defaultBackground()
}

private val polygonPalette = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE)

/**
 * A regular polygon painted as a triangle fan.
 */
context(ctx: DslContext)
private fun Polygon(modifier: Modifier, id: Any, sides: Int) = Spacer(modifier, id).change {
    object : DslComponent by it {
        context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            val rect = instance.rect
            val centerX = (rect.left + rect.right) / 2
            val centerY = (rect.top + rect.bottom) / 2
            val radius = minOf(rect.right - rect.left, rect.bottom - rect.top) / 2
            val points = (0 until sides).map { i ->
                val angle = i * 2.0 * PI / sides - PI / 2
                Position(centerX + radius * cos(angle), centerY + radius * sin(angle))
            }
            backend.fillPolygon(points, points.indices.map { polygonPalette[it % polygonPalette.size] })
        }
    }
}
