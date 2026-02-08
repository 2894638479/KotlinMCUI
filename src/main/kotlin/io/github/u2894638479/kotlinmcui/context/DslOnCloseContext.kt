package io.github.u2894638479.kotlinmcui.context

@DslContextMarker
fun interface DslOnCloseContext: DslExecuteContext {
    fun defaultOnClose()
}