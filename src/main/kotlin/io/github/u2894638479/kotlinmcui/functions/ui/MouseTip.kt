package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun MouseTip(
    modifier: Modifier = Modifier,
    id: Any? = null,
    function: DslFunction
) = Overlay(modifier, OverlayAlign.ToPoint(dataStore.mouse), id, function)