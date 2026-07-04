package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.decorator.background
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.functions.static
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.utils.Config
import io.github.u2894638479.kotlinmcui.utils.Simple

context(ctx: DslContext)
fun TestLayoutPage() = Column {
    val map by static { mapOf<String, DslFunction>(
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
            val n = local { 10 }
            fun color(n:Int) = when(n % 5) {
                0 -> Color.RED
                1 -> Color.BLUE
                2 -> Color.GREEN
                3 -> Color.WHITE
                else -> Color.BLACK
            }
            context(ctx:DslContext)
            fun func(n: Int) {
                if(n <= 0) return
                val f: DslFunction = {
                    ColorRect(Modifier.weight(0.5),color(n)) {}
                    func(n-1)
                }
                if(n % 2 == 0) Row { f() }
                else Column { f() }
            }
            Column {
                Config.Slider(n,0..100,"n") {}
                func(n.value)
            }
        },
        "LateBox" to {
            LateBox {
                if(width > height) TextFlatten { "W>H".emit() }
                else TextFlatten { "H>W".emit() }
                if(width > height) Row {
                    ColorRect(color = Color.RED) {}
                    ColorRect(color = Color.BLUE) {}
                } else Column {
                    ColorRect(color = Color.RED) {}
                    ColorRect(color = Color.BLUE) {}
                }
            }
        }
    ) }
    var chosen by local { map.entries.first() }
    Row(Modifier.weight(0.0)) {
        map.entries.forEachWithId {
            Simple.Button(it.key,chosen != it) { chosen = it }
        }
    }
    ShrinkBox {
        chosen.value()
    }
}