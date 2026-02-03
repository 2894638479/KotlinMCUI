package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.functions.DslFunction

fun interface DslBackendScreenFactory<SC> {
    fun create(dslFunction: DslFunction): DslBackendScreenHolder<SC>
}