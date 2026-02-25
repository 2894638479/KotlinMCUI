package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.translate
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.scope.DslChild

context(ctx: DslContext)
fun DslChild.buttonBackground(color: Color = Color.WHITE,padding: Measure = 2.scaled) = change {
    object:DslComponent by it {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            backend.renderButton(instance.rect.expand(padding), instance.isHighlighted,instance.highlightable,color)
            it.render()
        }

        override val narratable get() = true
        override val narration get() = "${it.run { narration ?: "" }} ${translate("kotlinmcui.narration.button")}"
        override val modifier = it.modifier.padding(padding)
    }
}

context(ctx: DslContext)
fun Button(
    modifier: Modifier = Modifier,
    color: Color = Color.WHITE,
    id:Any? = null,
    function: DslFunction
) = Box(modifier,id,function).buttonBackground(color,0.px)