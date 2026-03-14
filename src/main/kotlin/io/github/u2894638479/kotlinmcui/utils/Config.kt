package io.github.u2894638479.kotlinmcui.utils

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.*
import io.github.u2894638479.kotlinmcui.scope.DslChild
import kotlin.reflect.KMutableProperty0

object Config {
    context(ctx: DslContext)
    private val defaultModifier get() = Modifier.height(20.scaled).padding(2.scaled)

    context(ctx: DslContext)
    fun BoolButton(prop: KMutableProperty0<Boolean>, name: String = prop.name, id: Any = prop) = BoolButton(prop.property,name,id)

    context(ctx: DslContext)
    fun BoolButton(prop: StableRW<Boolean>, name: String, id: Any) = Button(defaultModifier,id = id) {
        TextFlatten {
            name.emit()
            ": ".emit()
            val bl = prop.value
            bl.toString().emit(if(bl) Color.GREEN else Color.RED)
        }
    }.clickable { prop.value = !prop.value }

    context(ctx: DslContext)
    fun <T: Enum<T>> EnumButton(prop: KMutableProperty0<T>, name:String = prop.name, enumName:(T) -> String = { it.name }, id: Any = prop) =
        EnumButton(prop.property,name,enumName,id)

    context(ctx: DslContext)
    fun <T : Enum<T>> EnumButton(prop: StableRW<T>, name:String, enumName:(T) -> String = { it.name }, id: Any): DslChild {
        var value by prop
        return Button(defaultModifier,id = id) {
            TextFlatten {
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
        TextFlatten {
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
        TextFlatten {
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
        TextFlatten {
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
    fun ColorEdit(prop: KMutableProperty0<Color>,id:Any = prop) = ColorEdit(prop.property,id)

    context(ctx: DslContext)
    fun ColorEdit(
        prop: StableRW<Color>,
        id: Any
    ) = Column(id = id) {
        var color by prop
        context(ctx: DslContext)
        fun S(text: String, get:()-> Double, set:(Double)-> Unit) = Slider(
            Modifier.width(20.scaled).padding(1.scaled),Axis.Vertical,
            object:StableRW<Double> {
                override fun getValue() = 1 - get()
                override fun setValue(value: Double) = set(1 - value)
            },id = text
        ) { TextAutoFold { text.emit() } }

        context(ctx: DslContext)
        fun items() {
            ColorRect(Modifier.minWidth(50.scaled).padding(1.scaled),color) {}
            S("r",{ color.rDouble },{color = color.change(r = it)})
            S("g",{ color.gDouble },{color = color.change(g = it)})
            S("b",{ color.bDouble },{color = color.change(b = it)})
            S("a",{ color.aDouble },{color = color.change(a = it)})
            S("h",{ color.hDouble },{color = color.changeHSV(h = it)})
            S("s",{ color.sDouble },{color = color.changeHSV(s = it)})
            S("v",{ color.vDouble },{color = color.changeHSV(v = it)})
        }

        val scrollerProp by Scroller.empty.remember.property
        ScrollableRow(Modifier.minHeight(50.scaled),scrollerProp) { items() }
        if(scrollerProp.value.isScrollable()) ScrollBar(Modifier.height(10.scaled),scrollerProp,Axis.Horizontal) {}
    }

    context(ctx: DslContext)
    fun slider(range: IntProgression, name: String = "", defaultValue: Int = range.first, id: Any): StableRW<Int> {
        val prop by dataStore.remember(newChildId(id),defaultValue).property
        Slider(prop,range,name,id)
        return prop
    }

    @JvmName("SliderDouble")
    context(ctx: DslContext)
    fun slider(range: ClosedFloatingPointRange<Double>, name: String = "", defaultValue: Double = range.start, id: Any): StableRW<Double> {
        val prop by dataStore.remember(newChildId(id),defaultValue).property
        Slider(prop,range,name,id)
        return prop
    }

    @JvmName("SliderFloat")
    context(ctx: DslContext)
    fun slider(range: ClosedFloatingPointRange<Float>, name: String = "", defaultValue: Float = range.start, id: Any): StableRW<Float> {
        val prop by dataStore.remember(newChildId(id),defaultValue).property
        Slider(prop,range,name,id)
        return prop
    }
}