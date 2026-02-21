package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun TestColorPage() = ScrollableColumn {
    val colorProp by Color.WHITE.remember.property
    var color by colorProp
    ColorRect(Modifier.height(50.scaled),color) {}
    Row(Modifier.height(20.scaled)) {
        Button(color = color) {  }
    }

    Slider(Modifier.height(20.scaled),Axis.Horizontal,0..255,
        colorProp.remap({ it.r.toInt() }, { color.change(r = it) })
    ) { TextFlatten { "r:${color.r}".emit() } }
    Slider(Modifier.height(20.scaled),Axis.Horizontal,0..255,
        colorProp.remap({ it.g.toInt() }, { color.change(g = it) })
    ) { TextFlatten { "g:${color.g}".emit() } }
    Slider(Modifier.height(20.scaled),Axis.Horizontal,0..255,
        colorProp.remap({ it.b.toInt() }, { color.change(b = it) })
    ) { TextFlatten { "b:${color.b}".emit() } }
    Slider(Modifier.height(20.scaled),Axis.Horizontal,0..255,
        colorProp.remap({ it.a.toInt() }, { color.change(a = it) })
    ) { TextFlatten { "a:${color.a}".emit() } }
    Slider(Modifier.height(20.scaled),Axis.Horizontal,
        colorProp.remap({ it.hDouble }, { color.changeHSV(h = it) })
    ) { TextFlatten { "h:${color.hDouble}".emit() } }
    Slider(Modifier.height(20.scaled),Axis.Horizontal,
        colorProp.remap({ it.sDouble }, { color.changeHSV(s = it) })
    ) { TextFlatten { "s:${color.sDouble}".emit() } }
    Slider(Modifier.height(20.scaled),Axis.Horizontal,
        colorProp.remap({ it.vDouble }, { color.changeHSV(v = it) })
    ) { TextFlatten { "v:${color.vDouble}".emit() } }
}