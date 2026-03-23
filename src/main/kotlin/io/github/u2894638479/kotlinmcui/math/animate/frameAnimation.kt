package io.github.u2894638479.kotlinmcui.math.animate

import io.github.u2894638479.kotlinmcui.context.DslFrameContext
import kotlin.time.Duration

// swing between -1..1
context(ctx: DslFrameContext)
fun swing(cycle: Duration, interpolator: Interpolator = Interpolator.default): Double {
    val cycleNano = cycle.inWholeNanoseconds
    val remain = ctx.frameBeginNano % cycleNano
    val t = remain.toDouble() / cycleNano
    val t4 = t * 4
    return when {
        t4 < 1 -> interpolator.progress(t4)
        t4 < 2 -> interpolator.progress(2 - t4)
        t4 < 3 -> - interpolator.progress(t4 - 2)
        else -> - interpolator.progress(4 - t4)
    }
}

context(ctx: DslFrameContext)
fun swing(cycle: Duration, range: ClosedFloatingPointRange<Double>, interpolator: Interpolator = Interpolator.default): Double {
    return (swing(cycle,interpolator) * range.run { endInclusive - start } + range.start + range.endInclusive) / 2
}
