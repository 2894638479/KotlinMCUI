package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.decorator.rotate
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.functions.ui.Spacer
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.utils.Simple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

context(ctx: DslContext)
fun TestAnimationPage() = ScrollableColumn {
    Row {
        Spacer {}
        var anim by local.animatable(1.seconds) { 0.0 }
        local {
            launch {
                var i = false
                while (true) {
                    anim = if(i) 0.3 else -0.3
                    i = !i
                    delay(1.seconds)
                }
            }
        }
        Simple.Button("test") {}.rotate(anim)
        Spacer {}
    }
}