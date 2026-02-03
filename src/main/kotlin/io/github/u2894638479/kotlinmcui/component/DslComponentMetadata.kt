package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.modifier.Modifier

interface DslComponentMetadata {
    val identity: DslId
    val modifier: Modifier

    context(instance: DslComponent)
    val focusable: Boolean get() = false
    context(instance: DslComponent)
    val tooltip: DslComponent? get() = null
    context(instance: DslComponent)
    val narration: String? get() = null
    context(instance: DslComponent)
    val highlightable: Boolean get() = instance.focusable || instance.tooltip != null || instance.narration != null
}