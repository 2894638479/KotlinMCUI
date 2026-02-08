package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.functions.DslTopFunction

fun interface DslBackendScreenFactory<SC> {
    fun create(title:String, dslFunction: DslTopFunction): DslBackendScreenHolder<SC>
}