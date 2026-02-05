package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.size
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.minHeight
import io.github.u2894638479.kotlinmcui.modifier.minWidth
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.StableROProperty
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun ScrollBarVertical(
    modifier: Modifier = Modifier,
    scrollerProp: StableROProperty<Scroller>,
    id:Any?
) = ScrollBar(modifier,scrollerProp,false,id)

context(ctx: DslContext)
fun ScrollBarHorizontal(
    modifier: Modifier = Modifier,
    scrollerProp: StableROProperty<Scroller>,
    id:Any?
) = ScrollBar(modifier,scrollerProp,true,id)

context(ctx: DslContext)
fun ScrollBar(
    modifier: Modifier = Modifier,
    scrollerProp: StableROProperty<Scroller>,
    horizontal:Boolean = false,
    id:Any?
) = Button(modifier,id = id) {
    var lastDown by remember<Position?>(null)
    val scroller by scrollerProp
    val before = scroller.spaceBefore()
    val mid = scroller.size
    val after = scroller.spaceAfter()
    context(ctx: DslContext)
    fun Bar(function: DslFunction) = if(horizontal) Row(function = function) else Column(function = function)
    Bar {
        Spacer(Modifier.weight(before)) {}
        Button(Modifier.weight(mid).run { if(horizontal) minHeight(10.scaled) else minWidth(10.scaled) }) {}.change { object: DslComponent by it {
            context(instance: DslComponent)
            override val focusable get() = true
            context(instance: DslComponent)
            override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
                if(it.mouseDown(mouse, mouseButton)) return true
                if(mouse !in instance.rect) return false
                lastDown = mouse
                return true
            }
            context(instance: DslComponent)
            override fun mouseUp(mouse: Position, mouseButton: MouseButton): Boolean {
                if(it.mouseUp(mouse, mouseButton)) return true
                return lastDown?.let { lastDown = null } != null
            }
        } }
        Spacer(Modifier.weight(after)) {}
    }.change { object: DslComponent by it {
        context(instance: DslComponent)
        override fun mouseMove(mouse: Position) {
            it.mouseMove(mouse)
            val rate = if(horizontal) (mouse.x - (lastDown ?: return).x) / instance.rect.width
                else (mouse.y - (lastDown ?: return).y) / instance.rect.height
            lastDown = mouse
            val scroll = rate * (after + mid + before)
            scroller.scroll(scroll)
        }
    } }
}