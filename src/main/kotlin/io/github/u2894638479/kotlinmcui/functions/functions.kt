package io.github.u2894638479.kotlinmcui.functions

import io.github.u2894638479.kotlinmcui.backend.showScreen
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslPreventContext
import io.github.u2894638479.kotlinmcui.identity.DslProperty
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import io.github.u2894638479.kotlinmcui.math.animate.Interpolator
import io.github.u2894638479.kotlinmcui.math.animate.toInterpolatable
import io.github.u2894638479.kotlinmcui.prop.LocalROProperty
import io.github.u2894638479.kotlinmcui.prop.LocalRWProperty
import io.github.u2894638479.kotlinmcui.prop.StableROProperty
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.scope.DslChild
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias DslFunction = context(DslContext) ()->Unit

context(ctx: DslContext)
fun showScreen(function: DslFunction) = dataStore.backend.showScreen(function)

context(ctx: DslContext)
fun newChildId(id:Any?) = ctx.identity + id

context(ctx: DslContext)
inline fun <T> withId(obj:Any?, block: context(DslContext) ()->T) =
    context(ctx.change(dslIdentity = identity + obj),block)

context(ctx: DslContext)
inline fun <T> Iterable<T>.forEachWithId(block:context(DslContext) (T) -> Unit) = forEach { withId(it) { block(it) } }


context(_: DslDataStoreContext, _: DslIdContext)
val <T> LocalRWProperty<T>.property get() = property(this)

context(_: DslDataStoreContext, ctx: DslIdContext)
fun <T> property(prop: LocalRWProperty<T>) = object: LocalROProperty<StableRWProperty<T>> {
    override val identity = ctx.identity
    override fun getValue(property: DslProperty<*>) = object: StableRWProperty<T> {
        override fun getValue() = prop.getValue(DslProperty(property.kProp, identity))
        override fun setValue(value: T) = prop.setValue(DslProperty(property.kProp, identity),value)
    }
}

context(_: DslDataStoreContext, _: DslIdContext)
val <T> LocalROProperty<T>.property get() = property(this)

context(_: DslDataStoreContext, ctx: DslIdContext)
fun <T> property(prop: LocalROProperty<T>) = object: LocalROProperty<StableROProperty<T>> {
    override val identity = ctx.identity
    override fun getValue(property: DslProperty<*>) = StableROProperty {
        prop.getValue(DslProperty(property.kProp, identity))
    }
}

context(_: DslDataStoreContext, _: DslIdContext)
fun <T> remember(defaultValue:T) = dataStore.remember(identity,defaultValue)

context(_: DslDataStoreContext, _: DslIdContext)
val <T> T.remember get() = remember(this)

context(_: DslDataStoreContext, _: DslIdContext)
fun <T> remember(defaultValue:()->T) = dataStore.remember(identity,defaultValue)

context(_: DslDataStoreContext, _: DslIdContext)
fun <T: Interpolatable<T>> animatable(beginValue: T, duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default) =
    dataStore.animatable(identity,beginValue,duration,interpolator)

context(_: DslDataStoreContext, _: DslIdContext)
fun <T: Interpolatable<T>> autoAnimate(value: T, duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default) =
    dataStore.autoAnimate(identity,value,duration,interpolator)

context(_: DslDataStoreContext, _: DslIdContext)
fun animatable(beginValue: Double, duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default) =
    dataStore.animatable(identity,beginValue.toInterpolatable(),duration,interpolator).remap({it.toDouble()},{it.toInterpolatable()})

context(_: DslDataStoreContext, _: DslIdContext)
fun autoAnimate(value: Double, duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default) =
    dataStore.autoAnimate(identity,value.toInterpolatable(),duration,interpolator).remap { it.toDouble() }

context(_: DslDataStoreContext, _: DslIdContext)
fun imageFile(file: File?): ImageHolder {
    return ctxBackend.loadLocalImage(file ?: return ImageHolder.empty)
}

context(_: DslDataStoreContext, _: DslIdContext)
fun <K,V> cached(key: K, value:(K)->V) = dataStore.cached(identity,key,value)

context(_: DslDataStoreContext, _: DslIdContext)
fun imageResource(location: String, width: Measure, height: Measure) = ImageHolder(location, width, height)


context(ctx: DslContext)
fun collect(component: DslComponent) = ctx.children.collect(component)

context(ctx: DslContext)
fun remove(child: DslChild) = ctx.children.remove(child)

context(ctx: DslContext)
inline fun init(block: context(DslPreventContext) () -> Unit){
    var frameIndex by remember<ULong?>(ULong.MAX_VALUE)
    when(frameIndex) {
        ULong.MAX_VALUE -> {
            frameIndex = dataStore.frameIndex
            context(DslPreventContext) { block() }
        }
        null -> {}
        dataStore.frameIndex -> context(DslPreventContext) { block() }
        else -> frameIndex = null
    }
}

context(ctx: DslContext)
inline fun launchedEffect(block: context(DslPreventContext) () -> Unit){
    var frameIndex by remember(0uL)
    val currentFrame = dataStore.frameIndex
    when(frameIndex) {
        currentFrame -> context(DslPreventContext,block)
        currentFrame - 1uL -> frameIndex = currentFrame
        else -> {
            context(DslPreventContext,block)
            frameIndex = currentFrame
        }
    }
}


