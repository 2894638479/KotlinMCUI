package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.translate
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.RectImpl
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.setValue
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

context(ctx: DslContext)
fun SliderVertical(
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Double>,
    progress: StableRWProperty<Double>? = null,
    step: Double = 1.0/16, // for keyboard
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,false,range,progress,step,color,buttonSize,id,function)

context(ctx: DslContext)
fun SliderHorizontal(
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Double>,
    progress: StableRWProperty<Double>? = null,
    step: Double = 1.0/16, // for keyboard
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,true,range,progress,step,color,buttonSize,id,function)

context(ctx: DslContext)
fun SliderVertical(
    modifier: Modifier = Modifier,
    range: IntProgression,
    progress: StableRWProperty<Int>? = null,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,false,range,progress,color,buttonSize,id,function)

context(ctx: DslContext)
fun SliderHorizontal(
    modifier: Modifier = Modifier,
    range: IntProgression,
    progress: StableRWProperty<Int>? = null,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,true,range,progress,color,buttonSize,id,function)

context(ctx: DslContext)
fun SliderVertical(
    modifier: Modifier = Modifier,
    progress: StableRWProperty<Double>? = null,
    step: Double = 1.0/16,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,false,progress,step,color,buttonSize,id,function)

context(ctx: DslContext)
fun SliderHorizontal(
    modifier: Modifier = Modifier,
    progress: StableRWProperty<Double>? = null,
    step: Double = 1.0/16,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,true,progress,step,color,buttonSize,id,function)

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    horizontal:Boolean,
    range: ClosedFloatingPointRange<Double>,
    progress: StableRWProperty<Double>? = null,
    step: Double = 1.0/16, // for keyboard
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,horizontal,progress?.remap(
    { (it - range.start) / (range.endInclusive - range.start) },
    { range.start + it * (range.endInclusive - range.start) }
),step,color,buttonSize,id,function)

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    horizontal:Boolean,
    range: IntProgression,
    progress: StableRWProperty<Int>? = null,
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,horizontal,progress?.remap(
    { (it - range.first).toDouble() / (range.last - range.first) },
    { range.first() + range.step * (it * (range.last - range.first) / range.step).roundToInt() }
), range.step.toDouble() / (range.last - range.first),color,buttonSize,id,function)

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    horizontal: Boolean,
    progress: StableRWProperty<Double>? = null,
    step: Double = 1.0/16, // for keyboard
    color: Color = Color.WHITE,
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Box(modifier,id,function).change { delegate ->
    object : DslComponent by delegate {
        context(instance: DslComponent)
        override val narratable get() = true
        context(instance: DslComponent)
        override val narration get() = "${delegate.run { narration }} ${translate("kotlinmcui.narration.slider")}"

        var progress by progress ?: run {
            val prop by 0.5.remember.property
            prop
        }
        var alreadyDown by remember<Unit?>(null)
        var keyFocused by remember<Unit?>(null)
        context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
        override fun <RP> render(mouse: Position) {
            val rect = instance.rect
            val progress = this.progress
            backend.renderButton(rect, keyFocused != null, false, color)
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
            this.progress = value.coerceIn(0.0..1.0)
        }

        context(instance: DslComponent, eventModifier: EventModifier)
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

        context(instance: DslComponent, eventModifier: EventModifier)
        override fun mouseUp(mouse: Position, mouseButton: MouseButton): Boolean {
            alreadyDown = null
            return delegate.mouseUp(mouse, mouseButton)
        }

        context(instance: DslComponent)
        override val focusable get() = true

        context(instance: DslComponent)
        override fun focusChanged(newFocus: DslId?) {
            super.focusChanged(newFocus)
            if(keyFocused != null && newFocus != instance.identity) {
                keyFocused = null
            }
        }

        context(instance: DslComponent, eventModifier: EventModifier)
        override fun keyDown(key: Int, scanCode: Int): Boolean {
            if(!isFocused) return super.keyDown(key, scanCode)
            when(key) {
                GLFW.GLFW_KEY_LEFT -> if(horizontal && keyFocused != null) {
                    this.progress = (this.progress - step).coerceIn(0.0..1.0)
                    return true
                }
                GLFW.GLFW_KEY_RIGHT -> if(horizontal && keyFocused != null) {
                    this.progress = (this.progress + step).coerceIn(0.0..1.0)
                    return true
                }
                GLFW.GLFW_KEY_UP -> if(!horizontal && keyFocused != null) {
                    this.progress = (this.progress - step).coerceIn(0.0..1.0)
                    return true
                }
                GLFW.GLFW_KEY_DOWN -> if(!horizontal && keyFocused != null) {
                    this.progress = (this.progress + step).coerceIn(0.0..1.0)
                    return true
                }
                GLFW.GLFW_KEY_ENTER -> {
                    keyFocused = if(keyFocused == Unit) null else Unit
                    return true
                }
                GLFW.GLFW_KEY_HOME -> {
                    this.progress = 0.0
                    return true
                }
                GLFW.GLFW_KEY_END -> {
                    this.progress = 1.0
                    return true
                }
            }
            return super.keyDown(key, scanCode)
        }
    }
}