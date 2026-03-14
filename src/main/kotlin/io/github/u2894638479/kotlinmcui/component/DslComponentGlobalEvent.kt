package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.identity.DslId

interface DslComponentGlobalEvent {
    fun globalFocusChanged(newFocus: DslId?) {}
    fun globalHoverChanged(newHover: DslId?) {}
}