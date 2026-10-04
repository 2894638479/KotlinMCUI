package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.fillRectGradient
import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.mixed
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.translateGlobal
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.*
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.math.set
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import kotlin.math.roundToInt

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    axis: Axis,
    range: ClosedFloatingPointRange<Double>,
    progress: StableRW<Double> = local { (range.start + range.endInclusive) / 2 },
    step: Double = 1.0/16, // for keyboard
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,axis,progress.remap(
    { (it - range.start) / (range.endInclusive - range.start) },
    { range.start + it * (range.endInclusive - range.start) }
),step,buttonSize,id,function)

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    axis: Axis,
    range: IntProgression,
    progress: StableRW<Int> = local { range.first },
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Slider(modifier,axis,progress.remap(
    { (it - range.first).toDouble() / (range.last - range.first) },
    { range.first() + range.step * (it * (range.last - range.first) / range.step).roundToInt() }
), range.step.toDouble() / (range.last - range.first),buttonSize,id,function)

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    axis: Axis,
    prop: ColorProp<Double>,
    color: StableRW<Color> = local { Color.WHITE },
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Box(modifier,id,function).change { delegate ->
    val status by context(delegate as DslIdContext) {
        local { SliderImpl.Status() }
    }
    val progress = color.remap({ prop.get(it) }, { prop.set(color.value, it) })
    val backgroundColor = Color.WHITE.mixed
    object : SliderImpl(delegate, axis, progress, 0.0 /* arrows owned by stepUp/stepDown below */, color.value, buttonSize, status) {
        override val knobColor get() = color.value

        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> renderBackground() {
            val rect = instance.rect
            backend.renderButton(rect, keyFocused, false, backgroundColor)
            drawGradient(axis, rect.expand(-(3.px * instance.scale)), gradientStops(color.value, prop))
            backend.flush()
        }

        context(eventModifier: KeyModifiers)
        override fun keyDown(key: Key, scanCode: Int): Boolean {
            if(!instance.isFocused || !keyFocused) return super.keyDown(key, scanCode)
            val direction = when (key) {
                Key.LEFT -> if (axis == Axis.Horizontal) -1 else return super.keyDown(key, scanCode)
                Key.RIGHT -> if (axis == Axis.Horizontal) 1 else return super.keyDown(key, scanCode)
                Key.UP -> if (axis == Axis.Vertical) -1 else return super.keyDown(key, scanCode)
                Key.DOWN -> if (axis == Axis.Vertical) 1 else return super.keyDown(key, scanCode)
                else -> return super.keyDown(key, scanCode)
            }
            color.value = if (direction > 0) prop.stepUp(color.value) else prop.stepDown(color.value)
            return true
        }
    }
}

context(ctx: DslContext)
fun Slider(
    modifier: Modifier = Modifier,
    axis: Axis,
    progress: StableRW<Double> = local { 0.5 },
    step: Double = 1.0/16, // for keyboard
    buttonSize: Measure = 8.scaled,
    id:Any? = null,
    function: DslFunction
) = Box(modifier,id,function).change { delegate ->
    val status by context(delegate as DslIdContext) {
        local { SliderImpl.Status() }
    }
    SliderImpl(delegate, axis, progress, step, Color.WHITE.mixed, buttonSize, status)
}

private open class SliderImpl(
    val delegate: DslComponent,
    val axis: Axis,
    progress: StableRW<Double>,
    val step: Double,
    val buttonColor: Color,
    val buttonSize: Measure,
    val status: Status
): DslComponent by delegate {
    class Status {
        var alreadyDown = false
        var keyFocused = false
    }
    override val narratable get() = true
    override val narration get() = "${delegate.run { narration }} ${translateGlobal("kotlinmcui.narration.slider")}"

    /** Colour tinting the draggable knob. Overridable for reactive colours. */
    protected open val knobColor get() = buttonColor

    var progress by progress
    var alreadyDown by status::alreadyDown
    var keyFocused by status::keyFocused

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    fun <RP> renderButton() {
        val progress = this.progress
        val buttonRect = when(axis) {
            Axis.Horizontal -> Rect(
                rect.left + (rect.width - buttonSize) * progress,
                rect.top,
                rect.right - (rect.width - buttonSize) * (1 - progress),
                rect.bottom
            )
            Axis.Vertical -> Rect(
                rect.left,
                rect.top + (rect.height - buttonSize) * progress,
                rect.right,
                rect.bottom - (rect.height - buttonSize) * (1 - progress)
            )
        }
        backend.renderButton(buttonRect, instance.isHighlighted, true, knobColor)
        backend.flush()
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    open fun <RP> renderBackground() {
        val rect = instance.rect
        backend.renderButton(rect, keyFocused, false, buttonColor)
        backend.flush()
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        renderBackground()
        renderButton()
        delegate.render()
    }

    fun setProgress(mouse: Position) {
        val rect = instance.rect
        val value = when (axis) {
            Axis.Horizontal -> (mouse.x - rect.left - buttonSize / 2) / (rect.width - buttonSize)
            Axis.Vertical -> (mouse.y - rect.top - buttonSize / 2) / (rect.height - buttonSize)
        }
        this.progress = value.coerceIn(0.0..1.0)
    }

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton): Boolean {
        if (delegate.mouseDown(mouseButton)) return true
        if (mouse in instance.rect) {
            dslBackend.playButtonSound()
            alreadyDown = true
            setProgress(mouse)
            return true
        }
        return false
    }

    context(mouse: Position)
    override fun mouseMove() {
        delegate.mouseMove()
        if(!alreadyDown) return
        setProgress(mouse)
    }

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseUp(mouseButton: MouseButton): Boolean {
        return delegate.mouseUp(mouseButton) || alreadyDown.also { alreadyDown = false }
    }

    override val focusable get() = true

    override fun globalFocusChanged(newFocus: DslId?, source: FocusSource) {
        super.globalFocusChanged(newFocus, source)
        if(keyFocused && newFocus != instance.identity) {
            keyFocused = false
        }
    }

    context(eventModifier: KeyModifiers)
    override fun keyDown(key: Key, scanCode: Int): Boolean {
        if(!instance.isFocused) return super.keyDown(key, scanCode)
        when(key) {
            Key.LEFT -> if(axis == Axis.Horizontal && keyFocused) {
                this.progress = (this.progress - step).coerceIn(0.0..1.0)
                return true
            }
            Key.RIGHT -> if(axis == Axis.Horizontal && keyFocused) {
                this.progress = (this.progress + step).coerceIn(0.0..1.0)
                return true
            }
            Key.UP -> if(axis == Axis.Vertical && keyFocused) {
                this.progress = (this.progress - step).coerceIn(0.0..1.0)
                return true
            }
            Key.DOWN -> if(axis == Axis.Vertical && keyFocused) {
                this.progress = (this.progress + step).coerceIn(0.0..1.0)
                return true
            }
            Key.ENTER -> {
                keyFocused = !keyFocused
                return true
            }
            Key.HOME -> {
                this.progress = 0.0
                return true
            }
            Key.END -> {
                this.progress = 1.0
                return true
            }
        }
        return super.keyDown(key, scanCode)
    }
}

/** Segments used to approximate a smooth hue rainbow. */
private const val HUE_SEGMENTS = 12

/**
 * Colours reachable by dragging the slider, sampled along the 0..1 track.
 * Uses [ColorProp.set] so the gradient preview always matches the current colour.
 */
private fun gradientStops(current: Color, prop: ColorProp<Double>): List<Color> = when (prop) {
    HDouble -> List(HUE_SEGMENTS + 1) { i -> prop.set(current, i.toDouble() / HUE_SEGMENTS) }
    else -> listOf(prop.set(current, 0.0), prop.set(current, 1.0))
}

context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
private fun <RP> drawGradient(axis: Axis, rect: Rect, stops: List<Color>) {
    val segments = stops.size - 1
    if (segments <= 0) return
    repeat(segments) { i ->
        val c0 = stops[i]
        val c1 = stops[i + 1]
        val seg = when (axis) {
            Axis.Horizontal -> Rect(
                rect.left + rect.width * i / segments,
                rect.top,
                rect.left + rect.width * (i + 1) / segments,
                rect.bottom
            )
            Axis.Vertical -> Rect(
                rect.left,
                rect.top + rect.height * i / segments,
                rect.right,
                rect.top + rect.height * (i + 1) / segments
            )
        }
        if (axis == Axis.Horizontal) backend.fillRectGradient(seg, c0, c1, c0, c1)
        else backend.fillRectGradient(seg, c0, c0, c1, c1)
    }
}