package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.withId
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun SliderVertical(
    modifier: Modifier = Modifier,
    progress: StableRWProperty<Double>? = null,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,progress,color,buttonSize,false,id,function)

context(ctx: DslContext)
fun SliderHorizontal(
    modifier: Modifier = Modifier,
    progress: StableRWProperty<Double>? = null,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,progress,color,buttonSize,true,id,function)

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    progress: StableRWProperty<Double>? = null,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    horizontal: Boolean = true,
    id:Any? = null,
    function: DslFunction
) = run {
    var progress by progress ?: withId(id ?: function::class) {
        val prop by 0.5.remember.property
        prop
    }
    val identity = newChildId(id ?: function::class)
    var alreadyDown by dataStore.remember<Unit?>(identity,null)
    val delegate = DslScopeImpl(identity, modifier, ctx, function)
    collect(object : DslComponent by delegate {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
        override fun <RP> render(mouse: Position) {
            val rect = instance.rect
            val progress = progress
            backend.renderButton(rect, false, false, color)
            val buttonRect = if (horizontal) Rect(
                rect.left + (rect.width - buttonSize) * progress,
                rect.top,
                rect.right - (rect.width - buttonSize) * (1 - progress),
                rect.bottom
            ) else Rect(
                rect.left,
                rect.top + (rect.height - buttonSize) * progress,
                rect.right,
                rect.bottom - (rect.height - buttonSize) * (1 - progress)
            )
            backend.renderButton(buttonRect, isHighlighted, true, color)
            delegate.render(mouse)
        }

        context(instance: DslComponent)
        fun setProgress(mouse: Position) {
            val rect = instance.rect
            val value = if (horizontal) (mouse.x - rect.left - buttonSize / 2) / (rect.width - buttonSize)
            else (mouse.y - rect.top - buttonSize / 2) / (rect.height - buttonSize)
            progress = value.coerceIn(0.0..1.0)
        }

        context(instance: DslComponent)
        override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
            if (delegate.mouseDown(mouse, mouseButton)) return true
            if (mouse in instance.rect) {
                ctx.dataStore.backend.playButtonSound()
                alreadyDown = Unit
                setProgress(mouse)
                return true
            }
            return false
        }

        context(instance: DslComponent)
        override fun mouseMove(mouse: Position) {
            delegate.mouseMove(mouse)
            alreadyDown ?: return
            setProgress(mouse)
        }

        context(instance: DslComponent)
        override fun mouseUp(mouse: Position, mouseButton: MouseButton): Boolean {
            alreadyDown = null
            return delegate.mouseUp(mouse, mouseButton)
        }

        context(instance: DslComponent)
        override val focusable get() = true
    })
}