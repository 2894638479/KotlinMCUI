package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.attachInstance
import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.*
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl

context(ctx: DslContext)
fun DslChild.tooltipBackground(padding: Measure = 3.px) = change {
    object : DslComponent by it {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            backend.renderTooltip(instance.rect.expand(padding))
            it.render()
        }

        override val modifier = it.modifier.padding(3.px)
    }
}

context(ctx: DslContext)
fun Tooltip(id: Any) {
    val modifier = Modifier.width(Measure.AUTO_MIN).height(Measure.AUTO_MIN)
    val delegate = DslScopeImpl(newChildId(id),modifier,ctx,{})
    ctx.dataStore.dslScreen.children.collect(
        object : DslComponent by delegate, MouseTipComponent {
            override fun build() {}
            override fun layoutHorizontal() {}
            override fun layoutVertical() {}

            context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                val focused = dataStore.focused?.let { id ->
                    dataStore.dslScreen.run {
                        testHit { it.takeIf { it.identity == id } }?.takeIf { it.focusable }
                    }
                }
                val hovered = dataStore.dslScreen.run {
                    testHit { it.takeIf { mouse in it.rect }?.tooltip }
                }
                val func = focused?.tooltip ?: hovered ?: return

                instance.children.buildThis(ctx,func)
                instance.children.forEach { it.attachInstance();it.build() }

                val useFocus = focused?.tooltip != null && focused.tooltip != hovered

                val boundH = if(useFocus) layoutFocusH(focused.rect,dataStore.dslScreen.rect,instance.childrenMaxWidth)
                    else layoutMouseH(mouse,dataStore.dslScreen.rect,instance.childrenMaxWidth)

                instance.rect.bound(Axis.Horizontal) copyFrom boundH
                delegate.layoutHorizontal()

                val boundV = if(useFocus) layoutFocusV(focused.rect,dataStore.dslScreen.rect,instance.childrenMaxHeight)
                    else layoutMouseV(mouse,dataStore.dslScreen.rect,instance.childrenMaxHeight)

                instance.rect.bound(Axis.Vertical) copyFrom boundV
                delegate.layoutVertical()

                delegate.render()
            }

            fun layoutMouseH(mouse: Position, screen: Rect, width: Measure) = MutBound(0.px,0.px).also {
                it.low = if(screen.right - mouse.x >= width) mouse.x else screen.right - width
                it.high = it.low + width
            }
            fun layoutMouseV(mouse: Position, screen: Rect, height: Measure) = MutBound(0.px,0.px).also {
                it.low = if(mouse.y - screen.top >= height) mouse.y - height else screen.top
                it.high = it.low + height
            }

            fun layoutFocusH(focused: Rect, screen: Rect, width: Measure) = MutBound(0.px,0.px).also {
                it.low = if(screen.right - focused.left >= width) focused.left else max(screen.left,focused.right - width)
                it.high = it.low + width
            }
            fun layoutFocusV(focused: Rect, screen: Rect, height: Measure) = MutBound(0.px,0.px).also {
                it.low = if(screen.bottom - focused.bottom >= height) focused.bottom else max(screen.top,focused.top - height)
                it.high = it.low + height
            }
        }
    )
}