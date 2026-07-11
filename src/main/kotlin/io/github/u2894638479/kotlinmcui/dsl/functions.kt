package io.github.u2894638479.kotlinmcui.dsl

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.container.DslChild
import java.io.File

typealias DslFunction = context(DslContext) ()->Unit

context(ctx: DslDataStoreContext)
fun translate(string: String,vararg args: Any) = backend.translate(string,*args) ?: string

context(_: DslExecuteContext)
fun showScreen(title:String = "DSL Screen",function: DslFunction) = dataStore.backend.create(title,function).show()

context(ctx: DslContext)
fun newChildId(id:Any?) = ctx.identity + id

context(ctx: DslContext)
inline fun <T> withId(obj:Any?, block: () -> T) = ctx.withIdentity(identity + obj,block)

context(ctx: DslContext)
inline fun <T> withScale(scale: Double, block: () -> T) = ctx.withScale(scale,block)

context(ctx: DslContext)
inline fun <T> Iterable<T>.forEachWithId(block: (T) -> Unit) = forEach { withId(it) { block(it) } }

context(_: DslDataStoreContext, _: DslIdContext)
fun imageFile(file: File?): ImageHolder {
    return backend.loadLocalImage(file ?: return ImageHolder.empty)
}

context(_: DslDataStoreContext, _: DslIdContext)
fun imageResource(location: String, width: Measure, height: Measure) = ImageHolder(location, width, height)

context(ctx: DslContext)
fun collect(component: DslComponent) = ctx.children.collect(component)

context(ctx: DslContext)
fun remove(child: DslChild) = ctx.children.remove(child)
