package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.animatable
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap

context(ctx: DslContext)
fun TestSliderPage() = Row {
    Column {
        val valueProp by remember(30).property
        val value by valueProp
        TextFlatten { "current slider value:$value".emit() }
        SliderHorizontal(Modifier.height(20.scaled),0..100,valueProp) {
            TextFlatten { "value:$value".emit() }
        }

        val levelProp by remember(3).property
        val level by levelProp
        TextFlatten { "level:$level".emit() }
        SliderHorizontal(Modifier.height(20.scaled),0..5,levelProp) {
            TextFlatten { "level:$level".emit() }
        }

        val level2Prop by remember(3).property
        val level2 by level2Prop
        TextFlatten { "step 2:$level2".emit() }
        SliderHorizontal(Modifier.height(20.scaled),0..11 step 2,level2Prop) {
            TextFlatten { "step 2:$level2".emit() }
        }

        val floatProp by remember(0.4f).property
        val float by floatProp
        TextFlatten { "float".emit() }
        SliderHorizontal(Modifier.height(20.scaled),
            floatProp.remap({ it.toDouble() }, { it.toFloat() })
        ) { TextFlatten { "value:$float".emit() } }

        TextFlatten { "animatable?".emit() }
        val animatableProp by animatable(0.8).property
        val animatable by animatableProp
        SliderHorizontal(Modifier.height(20.scaled), animatableProp) { TextFlatten { "animatable:$animatable".emit() } }
    }
    val prop by 0.6.remember.property
    val value by prop
    SliderVertical(Modifier.width(20.scaled), prop) {
        TextAutoFold {
            "Vertical?\n$value".emit()
        }
    }
}