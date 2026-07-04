package io.github.u2894638479.kotlinmcui.functions.decorator

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.backend
import io.github.u2894638479.kotlinmcui.functions.executeContext
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.scope.DslChild
import kotlinx.coroutines.CoroutineScope
import org.lwjgl.glfw.GLFW
import java.nio.file.Path

context(ctx: DslContext)
fun DslChild.clickable(enabled:Boolean = true, block: context(DslExecuteContext) CoroutineScope.()->Unit)
= change { if(!enabled) it else object : DslComponent by it {
    val launchable = local.launchableContext
    fun click() {
        backend.playButtonSound()
        block(DslExecuteContext(),launchable)
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
fun DslChild.onHovered(
    action: context(DslExecuteContext) CoroutineScope.(Boolean) -> Unit
) = change {
    object : DslComponent by it {
        val launchable = local.launchableContext
        override fun globalHoverChanged(newHover: DslId?) {
            it.globalHoverChanged(newHover)
            action(executeContext,launchable,instance.identity == newHover)
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
    action: context(DslExecuteContext) CoroutineScope.(Boolean) -> Unit
) = change {
    object: DslComponent by it {
        override fun globalFocusChanged(newFocus: DslId?) {
            it.globalHoverChanged(newFocus)
            action(executeContext,local.launchableContext,instance.identity == newFocus)
        }
    }
}

context(ctx: DslContext)
fun DslChild.onFocusChanged(
    action: context(DslExecuteContext) CoroutineScope.(DslId?) -> Unit
) = change {
    object: DslComponent by it {
        val launchable = local.launchableContext
        override fun globalFocusChanged(newFocus: DslId?) {
            it.globalHoverChanged(newFocus)
            action(executeContext,launchable,newFocus)
        }
    }
}

context(ctx: DslContext)
fun DslChild.onHoverChanged(
    action: context(DslExecuteContext) CoroutineScope.(DslId?) -> Unit
) = change {
    object: DslComponent by it {
        val launchable = local.launchableContext
        override fun globalHoverChanged(newHover: DslId?) {
            it.globalHoverChanged(newHover)
            action(executeContext,launchable,newHover)
        }
    }
}

context(ctx: DslContext)
fun DslChild.tooltip(function: DslFunction) = change {
    object : DslComponent by it {
        override val tooltip get() = function
    }
}

context(ctx: DslContext)
fun DslChild.onFilesDropped(action:context(DslExecuteContext) CoroutineScope.(List<Path>) -> Unit) = change {
    object : DslComponent by it {
        val launchable = local.launchableContext
        context(mouse: Position)
        override fun dropFiles(files: List<Path>): Boolean {
            if(mouse in instance.rect) {
                action(executeContext,launchable,files)
                return true
            }
            return it.dropFiles(files)
        }
    }
}

context(ctx: DslContext)
fun DslChild.scissor() = change {
    object:DslComponent by it {
        context(eventModifier: EventModifier, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton) = mouse in instance.rect && it.mouseDown(mouseButton)

        context(mouse: Position)
        override fun mouseScrollHorizontal(amount: Double): Double {
            if(mouse !in instance.rect) return amount
            return it.mouseScrollHorizontal(amount)
        }

        context(mouse: Position)
        override fun mouseScrollVertical(amount: Double): Double {
            if(mouse !in instance.rect) return amount
            return it.mouseScrollVertical(amount)
        }

        override fun <T> testHit(mouse: Position, get: (DslComponent) -> T?): T? {
            if(mouse !in instance.rect) return null
            return it.testHit(mouse,get)
        }

        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() = backend.withScissor(instance.rect) { it.render() }
    }
}