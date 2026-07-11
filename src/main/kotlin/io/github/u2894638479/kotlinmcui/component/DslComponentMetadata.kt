package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.modifier.Modifier

interface DslComponentMetadata {
    val modifier: Modifier
    var instance: DslComponent
    val transform: Transform get() = Transform.empty

    val focusable: Boolean get() = false
    val tooltip: DslFunction? get() = null
    val narration: String? get() = null
    val narratable: Boolean get() = false
    val highlightable: Boolean get() = instance.focusable
}