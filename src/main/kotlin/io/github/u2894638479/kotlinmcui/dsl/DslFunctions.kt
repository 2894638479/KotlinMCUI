package io.github.u2894638479.kotlinmcui.dsl

import io.github.u2894638479.kotlinmcui.backend.dslUtils
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.image.ImageSource
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.container.DslChild
import java.io.File

/** A DSL function that builds UI content within a [DslContext]. */
typealias DslFunction = context(DslContext) ()->Unit

context(ctx: DslDataStoreContext)
fun translate(string: String,vararg args: Any) = dslUtils.translate(string,*args) ?: string

/**
 * [translate] for code that runs outside a DSL context, such as entry metadata.
 * Falls back to [key] when the backend has no translation for it.
 */
fun translateGlobal(key: String,vararg args: Any) = dslUtils.translate(key,*args) ?: key

context(_: DslExecuteContext)
fun showScreen(title:String = "DSL Screen",function: DslFunction) =
    (dataStore.root.screenFactory
        ?: error("this root has no screen factory: screens can only be created for a root built with one"))
        .create(title,function).show()

context(ctx: DslContext)
fun newChildId(id:Any?) = ctx.identity + id

context(ctx: DslContext)
inline fun <T> withId(obj:Any?, block: () -> T) = ctx.withIdentity(identity + obj,block)

context(ctx: DslContext)
inline fun <T> withScale(scale: Double, block: () -> T) = ctx.withScale(scale,block)

/**
 * Enter a colour scope for the enclosed UI: every component in [block]
 * resolves its appearance colour against [color] (via `Color.mixed`).
 * Do the blending at the call site, e.g. `withColor(Color.RED.mixed) { }`
 * or `withColor(Color.WHITE.mixed.change(a = 128)) { }` for translucency.
 */
context(ctx: DslContext)
inline fun <T> withColor(color: Color, block: () -> T) = ctx.withColor(color, block)

context(ctx: DslContext)
inline fun <T> Iterable<T>.forEachWithId(block: (T) -> Unit) = forEach { withId(it) { block(it) } }

context(_: DslDataStoreContext, _: DslIdContext)
fun localImage(file: File?): ImageHolder = dataStore.utils.localImage(file ?: return ImageHolder.empty)

/**
 * Drops the cached copy of [file] and reads it again.
 *
 * Taking [DslExecuteContext] is the guard, not a formality: this belongs in an event handler such as
 * `clickable { }`, and the render context does not provide that receiver, so writing it inside a page
 * body does not compile.
 */
context(ctx: DslExecuteContext)
fun reloadLocalImage(file: File) = ctx.dataStore.utils.reloadLocalImage(file)

/** [reloadLocalImage] for a url: drop the cached copy and download it again. */
context(ctx: DslExecuteContext)
fun reloadRemoteImage(url: String) = ctx.dataStore.utils.reloadRemoteImage(url)

context(_: DslDataStoreContext, _: DslIdContext)
fun imageResource(location: String, width: Measure, height: Measure) = ImageHolder(ImageSource.Resource(location), width, height)

context(_: DslDataStoreContext, _: DslIdContext)
fun remoteImage(url: String): ImageHolder = dataStore.utils.remoteImage(url)

context(ctx: DslContext)
fun collect(component: DslComponent) = ctx.children.collect(component)

context(ctx: DslContext)
fun remove(child: DslChild) = ctx.children.remove(child)
