package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.translate
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.size
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.StableRO
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun ScrollBarVertical(
    modifier: Modifier = Modifier,
    scrollerProp: StableRO<Scroller>,
    id:Any?
) = ScrollBar(modifier,scrollerProp,Axis.Vertical,id)

context(ctx: DslContext)
fun ScrollBarHorizontal(
    modifier: Modifier = Modifier,
    scrollerProp: StableRO<Scroller>,
    id:Any?
) = ScrollBar(modifier,scrollerProp,Axis.Horizontal,id)

context(ctx: DslContext)
fun ScrollBar(
    modifier: Modifier = Modifier,
    scrollerProp: StableRO<Scroller>,
    axis: Axis,
    id:Any?
) = Button(modifier,id = id) {
    var lastDown by local<Position?> { null }
    val scroller by scrollerProp
    val before = scroller.spaceBefore()
    val mid = scroller.size.unscaled
    val after = scroller.spaceAfter()
    context(ctx: DslContext)
    fun Bar(function: DslFunction) = when(axis) {
        Axis.Horizontal -> Row(function = function)
        Axis.Vertical -> Column(function = function)
    }
    Bar {
        Spacer(Modifier.weight(before)) {}
        Box(Modifier.weight(mid)) {}.change { object: DslComponent by it {
            override val focusable get() = true
            context(eventModifier: EventModifier, mouse: Position)
            override fun mouseDown(mouseButton: MouseButton): Boolean {
                if(it.mouseDown(mouseButton)) return true
                if(mouse !in instance.rect) return false
                lastDown = mouse
                return true
            }
            override val narratable get() = true
            override val narration get() = "${it.run { narration ?: "" }} ${translate("kotlinmcui.narration.scroller")}"
            context(eventModifier: EventModifier, mouse: Position)
            override fun mouseUp(mouseButton: MouseButton): Boolean {
                if(it.mouseUp(mouseButton)) return true
                return lastDown?.let { lastDown = null } != null
            }
            context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                backend.renderButton(instance.rect, instance.isHighlighted,instance.highlightable)
                backend.flush()
                it.render()
            }
        } }
        Spacer(Modifier.weight(after)) {}
    }.change { object: DslComponent by it {
        context(mouse: Position)
        override fun mouseMove() {
            it.mouseMove()
            val rate = when(axis) {
                Axis.Horizontal -> (mouse.x - (lastDown ?: return).x) / instance.rect.width
                Axis.Vertical -> (mouse.y - (lastDown ?: return).y) / instance.rect.height
            }
            lastDown = mouse
            val scroll = rate * (after + mid + before)
            scroller.scroll(scroll)
        }
    } }
}