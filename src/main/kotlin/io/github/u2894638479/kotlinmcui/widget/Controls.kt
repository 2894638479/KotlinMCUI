package io.github.u2894638479.kotlinmcui.widget

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.dsl.translate
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.*
import io.github.u2894638479.kotlinmcui.container.DslChild
import kotlin.reflect.KMutableProperty0

object Controls {
    context(ctx: DslContext)
    private val defaultModifier get() = Modifier.height(20.scaled).padding(2.scaled)

    context(ctx: DslContext)
    fun BoolButton(prop: KMutableProperty0<Boolean>, name: String = prop.name, id: Any = prop) = BoolButton(prop.property,name,id)

    context(ctx: DslContext)
    fun BoolButton(prop: StableRW<Boolean>, name: String, id: Any) = Button(defaultModifier,id = id) {
        TextFlatten(Modifier.padding(h = 5.scaled)) {
            name.emit()
            ": ".emit()
            val bl = prop.value
            val string = if(bl) translate("kotlinmcui.widget.true") else translate("kotlinmcui.widget.false")
            string.emit(if(bl) Color.GREEN else Color.RED)
        }
    }.clickable { prop.value = !prop.value }

    context(ctx: DslContext)
    fun <T: Enum<T>> EnumButton(prop: KMutableProperty0<T>, name:String = prop.name, enumName:(T) -> String = { it.name }, id: Any = prop) =
        EnumButton(prop.property,name,enumName,id)

    context(ctx: DslContext)
    fun <T : Enum<T>> EnumButton(prop: StableRW<T>, name:String, enumName:(T) -> String = { it.name }, id: Any): DslChild {
        var value by prop
        return Button(defaultModifier,id = id) {
            TextFlatten(Modifier.padding(h = 5.scaled)) {
                name.emit()
                ": ".emit()
                enumName(value).emit()
            }
        }.clickable {
            val constants = value::declaringJavaClass.get().enumConstants
            value = constants[(value.ordinal + 1) % constants.size]
        }
    }

    context(ctx: DslContext)
    fun Slider(prop: KMutableProperty0<Int>, range: IntProgression, name: String = prop.name, id: Any = prop) =
        Slider(prop.property,range,name,id)
    
    context(ctx: DslContext)
    fun Slider(prop: StableRW<Int>, range: IntProgression, name: String, id: Any) = Slider(
        defaultModifier,Axis.Horizontal,range,prop,id = id
    ) {
        TextFlatten(Modifier.padding(h = 5.scaled)) {
            name.emit()
            ": ".emit()
            prop.value.toString().emit()
        }
    }

    @JvmName("SliderDouble")
    context(ctx: DslContext)
    fun Slider(prop: KMutableProperty0<Double>, range: ClosedFloatingPointRange<Double>, name: String = prop.name, id: Any = prop) =
        Slider(prop.property,range,name,id)

    @JvmName("SliderDouble")
    context(ctx: DslContext)
    fun Slider(prop: StableRW<Double>, range: ClosedFloatingPointRange<Double>, name: String, id: Any) = Slider(
        defaultModifier,Axis.Horizontal,range,prop,id = id
    ) {
        TextFlatten(Modifier.padding(h = 5.scaled)) {
            name.emit()
            ": ".emit()
            prop.value.toString().emit()
        }
    }

    @JvmName("SliderFloat")
    context(ctx: DslContext)
    fun Slider(prop: KMutableProperty0<Float>, range: ClosedFloatingPointRange<Float>, name: String = prop.name, id: Any = prop) =
        Slider(prop.property,range,name,id)

    @JvmName("SliderFloat")
    context(ctx: DslContext)
    fun Slider(prop: StableRW<Float>, range: ClosedFloatingPointRange<Float>, name: String, id: Any) = Slider(
        defaultModifier,Axis.Horizontal,range.run { start.toDouble()..endInclusive.toDouble() },
        prop.remap(Float::toDouble,Double::toFloat),id = id
    ) {
        TextFlatten(Modifier.padding(h = 5.scaled)) {
            name.emit()
            ": ".emit()
            prop.value.toString().emit()
        }
    }

    context(ctx: DslContext)
    fun String(prop: KMutableProperty0<String>, name: String = prop.name, id: Any = prop) = String(prop.property,name,id)

    context(ctx: DslContext)
    fun String(prop: StableRW<String>, name: String, id: Any) = Row(defaultModifier,id = id) {
        TextFlatten(Modifier.width(Measure.AUTO_MIN)) {
            name.emit()
            ": ".emit()
        }
        EditableText(defaultModifier,prop) {}
    }.editBoxBackground()

    context(ctx: DslContext)
    fun Slider(range: IntProgression, name: String = "", defaultValue: Int = range.first, id: Any): StableRW<Int> {
        val prop = local(newChildId(id)) { defaultValue }
        Slider(prop,range,name,id)
        return prop
    }

    @JvmName("SliderDouble")
    context(ctx: DslContext)
    fun Slider(range: ClosedFloatingPointRange<Double>, name: String = "", defaultValue: Double = range.start, id: Any): StableRW<Double> {
        val prop = local(newChildId(id)) { defaultValue }
        Slider(prop,range,name,id)
        return prop
    }

    @JvmName("SliderFloat")
    context(ctx: DslContext)
    fun Slider(range: ClosedFloatingPointRange<Float>, name: String = "", defaultValue: Float = range.start, id: Any): StableRW<Float> {
        val prop = local(newChildId(id)) { defaultValue }
        Slider(prop,range,name,id)
        return prop
    }
}