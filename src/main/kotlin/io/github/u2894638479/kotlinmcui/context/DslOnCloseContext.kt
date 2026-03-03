package io.github.u2894638479.kotlinmcui.context

@DslContextMarker
interface DslOnCloseContext: DslExecuteContext {
    fun defaultOnClose()
}

context(ctx: DslOnCloseContext)
fun defaultOnClose() = ctx.defaultOnClose()