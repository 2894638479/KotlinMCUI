package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslScreen

interface DslBackendScreenHolder<SC> {
    fun show()
    val screen: SC
    val dslScreen: DslScreen
}