package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.align
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.MutBound
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.copyFrom
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.scope.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.scope.childrenMaxWidth

context(ctx: DslContext)
fun DslChild.tooltipBackground(padding: Measure = 3.px) = change {
    object : DslComponent by it {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
        override fun <RP> render(mouse: Position) {
            backend.renderTooltip(instance.rect.expand(padding))
            it.render(mouse)
        }

        override val modifier = it.modifier.padding(3.px)
    }
}

context(ctx: DslContext)
fun Tooltip(id: Any) {
    val ctx = ctx.change(dslChildren = dataStore.dslScreen.children)
    val modifier = Modifier.width(Measure.AUTO_MIN).height(Measure.AUTO_MIN)
    context(ctx) {
        val delegate = DslScopeImpl(newChildId(id),modifier,ctx,{})
        collect(
            object : DslComponent by delegate, MouseTipComponent {
                context(instance: DslComponent)
                override fun build() {}
                context(instance: DslComponent)
                override fun layoutHorizontal() {}
                context(instance: DslComponent)
                override fun layoutVertical() {}

                context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
                override fun <RP> render(mouse: Position) {
                    val focused = dataStore.focused?.let { id ->
                        dataStore.dslScreen.testHit { it.takeIf { it.identity == id } }?.takeIf { it.focusable }
                    }
                    val hovered = dataStore.dslScreen.testHit { it.takeIf { mouse in it.rect }?.tooltip }
                    val func = focused?.tooltip ?: hovered ?: return

                    context(ctx.change(dslIdentity = instance.identity, dslChildren = delegate.children),func)
                    delegate.children.forEach { it.run { build() } }

                    val useFocus = focused?.tooltip != null && focused.tooltip != hovered

                    val boundH = if(useFocus) layoutFocusH(focused.rect,dataStore.dslScreen.rect,delegate.childrenMaxWidth)
                        else layoutMouseH(mouse,dataStore.dslScreen.rect,delegate.childrenMaxWidth)

                    delegate.alignerHorizontal.align(boundH,delegate.children.map { it.run { alignableHorizontal } })
                    delegate.children.forEach { it.run { layoutHorizontal() } }

                    val boundV = if(useFocus) layoutFocusV(focused.rect,dataStore.dslScreen.rect,delegate.childrenMaxHeight)
                        else layoutMouseV(mouse,dataStore.dslScreen.rect,delegate.childrenMaxHeight)

                    delegate.alignerVertical.align(boundV,delegate.children.map { it.run { alignableVertical } })
                    delegate.children.forEach { it.run { layoutVertical() } }

                    instance.rect.copyFrom(Rect(boundH,boundV))
                    delegate.render(mouse)
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
}