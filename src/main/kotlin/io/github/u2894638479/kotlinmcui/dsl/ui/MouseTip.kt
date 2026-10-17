package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.root
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun MouseTip(
    modifier: Modifier = Modifier,
    id: Any? = null,
    function: DslFunction
) = Overlay(modifier, OverlayAlign.ToPoint(root.input.mouse), id, function)