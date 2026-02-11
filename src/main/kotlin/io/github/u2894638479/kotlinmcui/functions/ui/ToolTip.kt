package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.scope.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.scope.childrenMaxWidth
import kotlin.run

context(ctx: DslContext)
fun ToolTip(id: Any) {
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
                    val focused = dataStore.focused?.let { dataStore.dslScreen.child(it) }
                    val focusedTooltip = focused?.tooltip?.takeIf { focused.focusable }
                    val rect: Rect
                    val func: DslFunction
                    if(focusedTooltip != null) {
                        func = focusedTooltip
                        context(ctx.change(dslIdentity = instance.identity, dslChildren = delegate.children),func)
                        delegate.children.forEach { it.run { build() } }
                        rect = layoutFocus(focused.rect,dataStore.dslScreen.rect,delegate.childrenMaxWidth,delegate.childrenMaxHeight)
                    } else {
                        func = dataStore.dslScreen.testHit { it.takeIf { mouse in it.rect }?.tooltip } ?: return
                        context(ctx.change(dslIdentity = instance.identity, dslChildren = delegate.children),func)
                        delegate.children.forEach { it.run { build() } }
                        rect = layoutMouse(mouse,dataStore.dslScreen.rect,delegate.childrenMaxWidth,delegate.childrenMaxHeight)
                    }
                    delegate.children.forEach { it.run { build() } }
                    this.rect.copyFrom(rect)

                    delegate.alignerHorizontal.align(rect.left,rect.right,delegate.children.map { it.run { alignableHorizontal } })
                    delegate.children.forEach { it.run { layoutHorizontal() } }

                    delegate.alignerVertical.align(rect.top,rect.bottom,delegate.children.map { it.run { alignableVertical } })
                    delegate.children.forEach { it.run { layoutVertical() } }
                    delegate.render(mouse)
                }

                fun layoutMouse(mouse: Position, screen: Rect, width: Measure, height: Measure) = Rect().also {
                    it.left = if(screen.right - mouse.x >= width) mouse.x else screen.right - width
                    it.width = width
                    it.top = if(mouse.y - screen.top >= height) mouse.y - height else screen.top
                    it.height = height
                }
                fun layoutFocus(focused: Rect, screen: Rect, width: Measure, height: Measure) = Rect().also {
                    it.top = if(screen.bottom - focused.bottom >= height) focused.bottom else max(screen.top,focused.top - height)
                    it.height = height
                    it.left = if(screen.right - focused.left >= width) focused.left else max(screen.left,focused.right - width)
                    it.width = width
                }
            }
        )
    }
}