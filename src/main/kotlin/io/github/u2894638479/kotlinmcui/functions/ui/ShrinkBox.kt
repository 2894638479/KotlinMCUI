package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.decorator.scissor
import io.github.u2894638479.kotlinmcui.functions.decorator.shrink
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun ShrinkBox(
    modifier: Modifier = Modifier,
    id:Any? = null,
    function: DslFunction
) = Box(modifier,id,function).shrink().scissor()