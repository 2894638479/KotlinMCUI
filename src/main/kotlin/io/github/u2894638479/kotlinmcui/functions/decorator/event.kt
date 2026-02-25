package io.github.u2894638479.kotlinmcui.functions.decorator

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.ctxBackend
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.scope.DslChild
import org.lwjgl.glfw.GLFW

context(ctx: DslContext)
fun DslChild.clickable(enabled:Boolean = true, block: context(DslExecuteContext, DslDataStoreContext) DslComponent.()->Unit)
= change { if(!enabled) it else object : DslComponent by it {

    fun click() {
        ctxBackend.playButtonSound()
        block(DslExecuteContext,ctx,instance)
    }

    override val focusable get() = true

    context(eventModifier: EventModifier, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton): Boolean {
        if(it.mouseDown(mouseButton)) return true
        if(mouse !in instance.rect) return false
        click()
        return true
    }

    context(eventModifier: EventModifier)
    override fun keyDown(key: Int, scanCode: Int): Boolean {
        if(it.keyDown(key, scanCode)) return true
        if(key == GLFW.GLFW_KEY_ENTER && instance.isFocused) {
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
    action: context(DslExecuteContext, DslDataStoreContext) (Boolean) -> Unit
) = change {
    object : DslComponent by it {
        override fun hoverChanged(newHover: DslId?) {
            it.hoverChanged(newHover)
            action(DslExecuteContext,ctx,instance.identity == newHover)
        }
    }
}

context(ctx: DslContext)
fun DslChild.narrate(string: String) = change {
    object : DslComponent by it {
        override val narration get() = string
        override val narratable get() = true
    }
}

context(ctx: DslContext)
fun DslChild.onFocused(
    action: context(DslExecuteContext, DslDataStoreContext) (Boolean) -> Unit
) = change {
    object: DslComponent by it {
        override fun focusChanged(newFocus: DslId?) {
            it.hoverChanged(newFocus)
            action(DslExecuteContext,ctx,instance.identity == newFocus)
        }
    }
}

context(ctx: DslContext)
fun DslChild.globalFocusChanged(
    action: context(DslExecuteContext, DslDataStoreContext) (DslId?) -> Unit
) = change {
    object: DslComponent by it {
        override fun focusChanged(newFocus: DslId?) {
            it.hoverChanged(newFocus)
            action(DslExecuteContext,ctx,newFocus)
        }
    }
}

context(ctx: DslContext)
fun DslChild.tooltip(function: DslFunction) = change {
    object : DslComponent by it {
        override val tooltip get() = function
    }
}