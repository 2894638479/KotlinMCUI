package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap

context(ctx: DslContext)
fun TestSliderPage() = Row {
    Column {
        val valueProp = local { 30.0 }
        val value by valueProp
        TextFlatten { "current slider value:$value".emit() }
        Slider(Modifier.height(20.scaled),Axis.Horizontal,0.0..100.0,valueProp) {
            TextFlatten { "value:$value".emit() }
        }

        val levelProp = local { 3 }
        val level by levelProp
        TextFlatten { "level:$level".emit() }
        Slider(Modifier.height(20.scaled),Axis.Horizontal,0..5,levelProp) {
            TextFlatten { "level:$level".emit() }
        }

        val level2Prop = local { 3 }
        val level2 by level2Prop
        TextFlatten { "step 2:$level2".emit() }
        Slider(Modifier.height(20.scaled),Axis.Horizontal,0..11 step 2,level2Prop) {
            TextFlatten { "step 2:$level2".emit() }
        }

        val floatProp = local { 0.4f }
        val float by floatProp
        TextFlatten { "float".emit() }
        Slider(Modifier.height(20.scaled),Axis.Horizontal,
            floatProp.remap({ it.toDouble() }, { it.toFloat() })
        ) { TextFlatten { "value:$float".emit() } }

        TextFlatten { "animatable?".emit() }
        val animatableProp = local.animatable { 0.8 }
        val animatable by animatableProp
        Slider(Modifier.height(20.scaled),Axis.Horizontal, animatableProp) { TextFlatten { "animatable:$animatable".emit() } }
    }
    val prop = local { 0.6 }
    val value by prop
    Slider(Modifier.width(20.scaled),Axis.Vertical, prop) {
        TextAutoFold {
            "Vertical?\n$value".emit()
        }
    }
}