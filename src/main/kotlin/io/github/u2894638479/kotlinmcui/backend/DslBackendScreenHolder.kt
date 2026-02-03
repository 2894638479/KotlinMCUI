package io.github.u2894638479.kotlinmcui.backend

interface DslBackendScreenHolder<SC> {
    fun show()
    val screen: SC
}