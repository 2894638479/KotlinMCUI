package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.modifier.Modifier

interface DslComponentMetadata {
    val identity: DslId
    val modifier: Modifier
    var instance: DslComponent

    val focusable: Boolean get() = false
    val tooltip: DslFunction? get() = null
    val narration: String? get() = null
    val narratable: Boolean get() = false
    val highlightable: Boolean get() = instance.focusable
}