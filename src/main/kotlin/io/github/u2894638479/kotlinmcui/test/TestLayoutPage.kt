package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.decorator.background
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.Box
import io.github.u2894638479.kotlinmcui.functions.ui.Button
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.SliderHorizontal
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.functions.ui.TextFoldable
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.minHeight
import io.github.u2894638479.kotlinmcui.modifier.minWidth
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import kotlin.collections.mapOf

context(ctx: DslContext)
fun TestLayoutPage() = Column {
    val map by remember { mapOf<String, DslFunction>(
        "general" to {
            Row {
                Column {
                    TextFlatten(Modifier.weight(0.0)) { "weight:0.0".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(1.0)) { "weight:1.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(2.0)) { "weight:2.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.weight(3.0)) { "weight:3.0".emit() }.background(Color.BLUE)
                }
                Column {
                    Row {
                        TextFlatten(Modifier.weight(0.0)) { "weight:0.0".emit() }.background(Color(100, 100, 100))
                        TextFlatten(Modifier.weight(1.0)) { "weight:1.0".emit() }.background(Color.RED)
                        TextFlatten(Modifier.weight(2.0)) { "weight:2.0".emit() }.background(Color.GREEN)
                        TextFlatten(Modifier.weight(3.0)) { "weight:3.0".emit() }.background(Color.BLUE)
                    }
                    TextFlatten(Modifier.padding(10.scaled)) { "padding:10.scaled".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.padding(20.scaled)) { "padding:20.scaled".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.padding(10.px)) { "padding:10.px".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.padding(20.px)) { "padding:20.px".emit() }.background(Color(100, 100, 100))
                }
                Column {
                    TextFlatten { "weight:1.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(Double.MAX_VALUE)) { "weight:Double.MAX_VALUE".emit() }.background(Color.GREEN)
                }
            }
        },
        "row" to {
            Column {
                Row {
                    TextFlatten(Modifier.width(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.weight(2.0)) { "2.0".emit() }.background(Color.BLUE)
                }
                Row {
                    TextFlatten(Modifier.width(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.weight(Double.MAX_VALUE)) { "Double.MAX_VALUE".emit() }.background(Color.BLUE)
                }
                Row {
                    TextFlatten(Modifier.weight(Double.MAX_VALUE)) { "Double.MAX_VALUE".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.width(Measure.AUTO_MIN)) { "min".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.BLUE)
                }
                Row {
                    TextFlatten(Modifier.width(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.width(100.scaled)) { "width(100.scaled)....................".emit() }.background(Color.BLUE)
                    TextFlatten(Modifier.minWidth(100.scaled)) { "minW(100.scaled)".emit() }
                }
                Row {
                    TextFlatten(Modifier.width(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.width(100.scaled)) { "width(100.scaled)".emit() }.background(Color.BLUE)
                    TextFlatten(Modifier.minWidth(100.scaled)) { "minW(100.scaled)....................".emit() }
                }
            }
        },
        "column" to {
            Row {
                Column {
                    TextFlatten(Modifier.height(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.weight(2.0)) { "2.0".emit() }.background(Color.BLUE)
                }
                Column {
                    TextFlatten(Modifier.height(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.weight(Double.MAX_VALUE)) { "Double.MAX_VALUE".emit() }.background(Color.BLUE)
                }
                Column {
                    TextFlatten(Modifier.weight(Double.MAX_VALUE)) { "Double.MAX_VALUE".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.height(Measure.AUTO_MIN)) { "min".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.BLUE)
                }
                Column {
                    TextFlatten(Modifier.height(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFoldable(Modifier.height(100.scaled)) { "height(100.scaled)\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n......".emit() }.background(Color.BLUE)
                    TextFlatten(Modifier.minHeight(100.scaled)) { "minH(100.scaled)".emit() }
                }
                Column {
                    TextFlatten(Modifier.height(Measure.AUTO_MIN)) { "min".emit() }.background(Color(100, 100, 100))
                    TextFlatten(Modifier.weight(0.0)) { "0.0".emit() }.background(Color.RED)
                    TextFlatten(Modifier.weight(1.0)) { "1.0".emit() }.background(Color.GREEN)
                    TextFlatten(Modifier.height(100.scaled)) { "height(100.scaled)".emit() }.background(Color.BLUE)
                    TextFoldable(Modifier.minHeight(100.scaled)) { "minH(100.scaled)\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n......".emit() }
                }
            }
        },
        "recursion" to {
            val n by remember(10).property
            SliderHorizontal(Modifier.height(20.scaled),0..100,n) {
                TextFlatten { "n=${n.value}".emit() }
            }
            fun color(n:Int) = when(n % 5) {
                0 -> Color.RED
                1 -> Color.BLUE
                2 -> Color.GREEN
                3 -> Color.WHITE
                else -> Color.BLACK
            }
            fun Modifier.padding(n:Int):Modifier {
                fun value(a:Int,b:Int) = if((n%4) == a || (n%4) == b) 5.px else 0.px
                return padding(value(0,1),value(1,2),value(2,3),value(3,0))
            }
            context(ctx:DslContext)
            fun func(n: Int) {
                if(n <= 0) return
                Box(Modifier.padding(n)) {
                    func(n-1)
                }.background(color(n))
            }
            func(n.value)
        }
    ) }
    var chosen by remember(map.entries.first())
    Row(Modifier.height(25.scaled)) {
        map.entries.forEachWithId {
            Button {
                TextFlatten { it.key.emit() }
            }.clickable(chosen != it) { chosen = it }
        }
    }
    chosen.value()
}