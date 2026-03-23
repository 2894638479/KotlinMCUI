package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.decorator.rotate
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.functions.ui.Spacer
import io.github.u2894638479.kotlinmcui.math.animate.swing
import io.github.u2894638479.kotlinmcui.utils.Simple
import kotlin.time.Duration.Companion.seconds

context(ctx: DslContext)
fun TestAnimationPage() = ScrollableColumn {
    Row {
        Spacer {}
        Simple.Button("test") {}.rotate(swing(1.seconds) * 0.3)
        Spacer {}
    }
}