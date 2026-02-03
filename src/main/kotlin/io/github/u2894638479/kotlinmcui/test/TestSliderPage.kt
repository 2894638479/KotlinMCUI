package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.animatable
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.Slider
import io.github.u2894638479.kotlinmcui.functions.ui.SliderVertical
import io.github.u2894638479.kotlinmcui.functions.ui.TextAutoFold
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.getValue
import kotlin.math.roundToInt

context(ctx: DslContext)
fun TestSliderPage() = Row {
    Column {
        val valueProp by remember(30).property
        val value by valueProp
        TextFlatten { "current slider value:$value".emit() }
        Slider(
            Modifier.height(20.scaled),
            valueProp.remap({ it / 100.0 }, { (it * 100).toInt() })
        ) { TextFlatten { "value:$value".emit() } }

        val levelProp by remember(3).property
        val level by levelProp
        TextFlatten { "level:$level".emit() }
        Slider(
            Modifier.height(20.scaled),
            levelProp.remap({ it / 5.0 }, { (it * 5).roundToInt() })
        ) { TextFlatten { "level:$level".emit() } }

        val floatProp by remember(0.4f).property
        val float by floatProp
        TextFlatten { "float".emit() }
        Slider(
            Modifier.height(20.scaled),
            floatProp.remap({ it.toDouble() }, { it.toFloat() })
        ) { TextFlatten { "value:$float".emit() } }

        TextFlatten { "animatable?".emit() }
        val animatableProp by animatable(0.8).property
        val animatable by animatableProp
        Slider(Modifier.height(20.scaled), animatableProp) { TextFlatten { "animatable:$animatable".emit() } }
    }
    val prop by 0.6.remember.property
    val value by prop
    SliderVertical(Modifier.width(20.scaled), prop) {
        TextAutoFold {
            "Vertical?\n$value".emit()
        }
    }
}