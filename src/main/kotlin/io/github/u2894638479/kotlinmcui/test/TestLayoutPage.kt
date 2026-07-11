package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.decorator.background
import io.github.u2894638479.kotlinmcui.dsl.forEachWithId
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.static
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.dsl.withScale
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.StableRO
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
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
            Column {
                val scaleProp = local { 1 }
                val decimalPart = local { 0.0 }
                val scale by StableRO { scaleProp.value + decimalPart.value }
                Row(Modifier.height(20.scaled).padding(3.scaled)) {
                    Slider(Modifier,Axis.Horizontal,1..7,scaleProp) {}
                    Slider(Modifier,Axis.Horizontal,0.0..1.0,decimalPart) {}
                }.Box {
                    TextFlatten {
                        "scale: ${String.format("%.2f",scale)}".emit()
                    }
                }
                withScale(scale) {
                    TestPage()
                }
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