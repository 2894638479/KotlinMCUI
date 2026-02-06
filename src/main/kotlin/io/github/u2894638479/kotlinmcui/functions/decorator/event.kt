package io.github.u2894638479.kotlinmcui.functions.decorator

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslPreventContext
import io.github.u2894638479.kotlinmcui.functions.ctxBackend
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.scope.DslChild
import org.lwjgl.glfw.GLFW

context(ctx: DslContext)
fun DslChild.clickable(enabled:Boolean = true, block: context(DslPreventContext, DslDataStoreContext) DslComponent.()->Unit)
= change { if(!enabled) it else object : DslComponent by it {

    context(instance: DslComponent)
    fun click() {
        ctxBackend.playButtonSound()
        block(DslPreventContext,ctx,instance)
    }

    context(instance: DslComponent)
    override val focusable get() = true

    context(instance: DslComponent)
    override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
        if(it.mouseDown(mouse, mouseButton)) return true
        if(mouse !in instance.rect) return false
        click()
        return true
    }

    context(instance: DslComponent)
    override fun keyDown(key: Int, scanCode: Int, eventModifier: EventModifier): Boolean {
        if(it.keyDown(key, scanCode, eventModifier)) return true
        if(key == GLFW.GLFW_KEY_ENTER && isFocused) {
            click()
            return true
        }
        return false
    }
}}

context(ctx: DslContext)
fun DslChild.forceId(id: DslId) = change { object: DslComponent by it {
    override val identity = id
}}

context(ctx: DslContext)
fun DslChild.onHovered(
    action: context(DslPreventContext) (Boolean) -> Unit
) = change {
    object : DslComponent by it {
        context(instance: DslComponent)
        override fun hoverChanged(newHover: DslId?) {
            super.hoverChanged(newHover)
            action(DslPreventContext,instance.identity == newHover)
        }
    }
}