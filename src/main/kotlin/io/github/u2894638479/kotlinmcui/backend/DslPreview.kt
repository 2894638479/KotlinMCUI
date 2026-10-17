package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.translate
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.TextAutoFold
import io.github.u2894638479.kotlinmcui.dsl.ui.defaultBackground
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import io.github.u2894638479.kotlinmcui.modifier.Modifier as DslModifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration.Companion.milliseconds

/**
 * The in-game preview session: open the page a launch property names, and - only when a second
 * property asks for it - photograph the result and stop.
 *
 * All of this is policy and therefore lives here; the platform only supplies the two members
 * [DslRootUtils.capture] and [DslPlatformUtils.quit] plus the render loop that ticks
 * [root.state.frameIndex]. Keeping the timing rules in the framework means they are the same on
 * every platform, and testable without a game.
 *
 * `-Dkotlinmcui.preview=<reference>` opens the page. `-Dkotlinmcui.preview.capture=true` turns the
 * session into the one a machine can gate on: it photographs the page and quits, with a non-zero
 * code when the reference could not be resolved, the page threw, or no file was written.
 *
 * A page is a function, named by reference rather than registered, so the framework needs no
 * preview-only API:
 *
 * - the reference an IDE gives for a top-level function, which is the file name plus the function
 *   (`a.b.TestColorPage` means `a.b.TestColorPageKt.TestColorPage`);
 * - `a.b.Class#method` for a static function or for a member of an object, which is what an entry's
 *   page is (`a.b.Entry$Page#content`).
 *
 * A member function is reached through the object's own `INSTANCE`, through the outer class's static
 * field when the class is a nested or companion object, or through a no-argument constructor; an
 * abstract class is out of reach. When a class declares several functions under the same name, the
 * one that takes nothing but the DSL context wins, because reflection promises no order.
 *
 * A page takes the DSL context, and may take further parameters - but only defaulted ones, because
 * the preview passes no arguments. Whether a parameter has a default is invisible to Java reflection,
 * so a page that is not shaped that way is refused instead of being called with values nobody supplied.
 *
 * A reference that does not resolve, or a page shaped function whose parameters cannot be left out,
 * is shown as an error screen carrying the reason: the interactive session then stays in the game,
 * where the reason can be read, and the capture session photographs that screen instead and still
 * leaves the non-zero code and the log line.
 */
internal object DslPreview {
    /** The page to open: one launch property, and the only name this feature has. */
    private const val PREVIEW_PROPERTY = "kotlinmcui.preview"

    /** Whether [PREVIEW_PROPERTY] should be photographed and left: the one extra switch. */
    private const val CAPTURE_PROPERTY = "kotlinmcui.preview.capture"

    /** Frames the page is given before it is photographed: a frame clock, not a wall clock. */
    private const val FRAMES = 30L

    private const val FRAME_POLL_MS = 16L
    private const val READY_POLL_MS = 50L
    private const val READY_TIMEOUT_MS = 240_000L

    /**
     * Open the page the platform asked for on [root]; does nothing unless it asked for one.
     *
     * The root is the backend's own. The preview draws into the tree it is handed and reads no
     * global, so a second backend in the same process cannot end up photographing the first one's UI.
     */
    fun start(root: DslRoot) {
        val request = request() ?: return
        CoroutineScope(Dispatchers.Default).launch { run(root, request.first,request.second) }
    }

    /**
     * The page the platform asked for and whether it also wants a screenshot, or `null` when it
     * asked for no page at all. Reading both properties in one place keeps the command line and the
     * run configuration agreeing on what they mean.
     */
    internal fun request(): Pair<String,Boolean>? {
        val reference = System.getProperty(PREVIEW_PROPERTY)?.takeIf { it.isNotBlank() } ?: return null
        return reference to (System.getProperty(CAPTURE_PROPERTY)?.toBoolean() ?: false)
    }

    /**
     * Opens [reference] and, with [capture] set, photographs it and stops the game, exiting with a
     * non-zero code when the page could not be resolved, threw while it was built or rendered, or
     * produced no file.
     */
    internal suspend fun run(root: DslRoot, reference: String, capture: Boolean) {
        val platform = dslPlatform
        val screens = root.screenFactory ?: error("preview needs a root that can create screens")
        val page = try {
            pageFor(reference)
        } catch(e: Throwable) {
            dslLogger.error("preview: '$reference' could not be resolved",e)
            showFailure(root, reference,errorText(reference,e),capture)
            return
        }
        if(page.usesDefaults) {
            dslLogger.info("preview: ${page.describe()} takes parameters; all of them have defaults, so they are passed as zero values")
        }
        try {
            if(!awaitFirstFrame(root)) {
                dslLogger.error("preview: the platform never rendered a frame, giving up on '$reference'")
                platform.utils.quit(1)
                return
            }
            val opened = root.state.frameIndex
            withContext(platform.dispatcher) { screens.create(reference) { page.draw() } }.show()
            dslLogger.info("preview: opened '$reference' as ${page.describe()}")
            while(root.state.frameIndex < opened + FRAMES) delay(FRAME_POLL_MS.milliseconds)
            if(!capture) return
            val file = withContext(platform.dispatcher) { root.rootUtils.capture(fileNameFor(reference)) }
            if(root.state.renderFailure != null) {
                dslLogger.error("preview: ${page.describe()} threw while rendering",root.state.renderFailure)
                platform.utils.quit(1)
                return
            }
            if(file == null) {
                dslLogger.error("preview: no screenshot was written for '$reference'")
                platform.utils.quit(1)
                return
            }
            dslLogger.info("preview: wrote ${file.absolutePath} for ${page.describe()}")
            platform.utils.quit(0)
        } catch(e: Throwable) {
            // A page that throws while being built, or a platform that throws while capturing, has
            // to end the run with a code as well - hanging is the one outcome nobody can gate on.
            dslLogger.error("preview: '$reference' failed",e)
            platform.utils.quit(1)
        }
    }

    /**
     * Puts [text] on screen instead of the page that could not be opened. In capture mode the error
     * screen is photographed under the name the page would have had and the run still ends with a
     * non-zero code, so a machine gate gets both the picture and the verdict; when nobody asked for a
     * screenshot the game is left running, which is where a human wants to read the reason. The log
     * line is written by the caller, because it carries the exception itself.
     */
    private suspend fun showFailure(root: DslRoot, reference: String,text: String,capture: Boolean) {
        val platform = dslPlatform
        val screens = root.screenFactory ?: error("preview needs a root that can create screens")
        // The error screen needs the same readiness as a real page: opened any earlier it would be
        // pushed aside by the platform's own loading screen, and the screenshot would show that
        // instead of the reason.
        if(!awaitFirstFrame(root)) {
            dslLogger.error("preview: the platform never rendered a frame, giving up on '$reference'")
            platform.utils.quit(1)
            return
        }
        try {
            withContext(platform.dispatcher) {
                screens.create("Preview error") { PreviewErrorPage(text) }
            }.show()
        } catch(e: Throwable) {
            // A failing error screen must not swallow the reason it was meant to show, so this path
            // stays a plain exit.
            dslLogger.error("preview: could not show the error screen for '$reference'",e)
            platform.utils.quit(1)
            return
        }
        if(!capture) return
        val opened = root.state.frameIndex
        while(root.state.frameIndex < opened + FRAMES) delay(FRAME_POLL_MS.milliseconds)
        val file = withContext(platform.dispatcher) { root.rootUtils.capture(fileNameFor(reference)) }
        if(file == null) dslLogger.error("preview: no screenshot was written for '$reference'")
        else dslLogger.info("preview: wrote ${file.absolutePath} for the error screen of '$reference'")
        platform.utils.quit(1)
    }

    /**
     * What the error screen shows: the reference that was asked for and why it could not be opened.
     * For a reference that did not resolve, that reason already lists every name that was tried and
     * what was wrong with each candidate. The reason is folded into one fact per line, because the
     * text component only folds where its box ends and these diagnostics are a single long sentence.
     */
    internal fun errorText(reference: String,error: Throwable) =
        "$reference\n\n" + (error.message ?: error.toString()).replace(", ","\n").replace(" - ","\n    ")

    /**
     * Waits until the platform has rendered one frame through the framework, which is the moment it
     * is past its own loading screen and a screen of ours can survive.
     */
    private suspend fun awaitFirstFrame(root: DslRoot): Boolean {
        val deadline = System.currentTimeMillis() + READY_TIMEOUT_MS
        while(root.state.frameIndex == 0L) {
            if(System.currentTimeMillis() > deadline) return false
            delay(READY_POLL_MS.milliseconds)
        }
        return true
    }

    /** The screenshot name for [reference]: stable and readable, derived from the page's own name. */
    internal fun fileNameFor(reference: String): String {
        val simple = reference.substringAfterLast('.').substringAfterLast('$')
        val safe = simple.map { if(it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }.joinToString("")
        return "kotlinmcui-${safe.ifEmpty { "page" }}.png"
    }

    /**
     * Resolves [reference] to a page, or fails with every name that was tried and, for every method
     * that had a page's shape but could not be called, the reason.
     */
    internal fun pageFor(reference: String): Page {
        val tried = mutableListOf<String>()
        val refused = mutableListOf<String>()
        val hash = reference.indexOf('#')
        if(hash > 0) {
            val type = reference.substring(0,hash)
            val method = reference.substring(hash + 1)
            tried += describeCandidate(type,method)
            findPage(type,method,refused)?.let { return it }
        } else {
            // A page is a function, and the reference an IDE gives for a top-level one is the function
            // itself: its JVM class is the file name with `Kt` appended.
            val simple = reference.substringAfterLast('.')
            val parent = reference.substringBeforeLast('.',"")
            val fileClass = if(parent.isEmpty()) "${simple}Kt" else "$parent.${simple}Kt"
            tried += describeCandidate(fileClass,simple)
            findPage(fileClass,simple,refused)?.let { return it }
        }
        val found = if(refused.isEmpty()) "" else "; found ${refused.joinToString()}"
        val rule = if(refused.isEmpty()) "" else
            "; a preview page may only take the DSL context or parameters that all have default values"
        error("no preview page for '$reference' - tried ${tried.joinToString()}$found$rule")
    }

    /**
     * The page named by [className] and [methodName], or `null` when there is no such function. A
     * static one is called directly; a member one is called on the object's `INSTANCE`, or on an
     * instance built by a no-argument constructor when it is not a Kotlin object.
     */
    private fun findPage(className: String,methodName: String,refused: MutableList<String>): Page? {
        val type = forName(className) ?: return null
        val candidates = type.declaredMethods.filter { it.name == methodName && pageShape(it) }
        if(candidates.isEmpty()) return null
        for(method in candidates.filter { Modifier.isStatic(it.modifiers) }) {
            resolve(method,null,refused)?.let { return it }
        }
        val members = candidates.filterNot { Modifier.isStatic(it.modifiers) }
        if(members.isEmpty() || Modifier.isAbstract(type.modifiers)) return null
        val instance = instanceOf(type) ?: return null
        // The parameterless overload is the page when a class offers both: the order `declaredMethods`
        // reports is not specified.
        members.firstOrNull { it.parameterCount == 1 }?.let { return Page(it,instance,null) }
        for(method in members) {
            resolve(method,instance,refused)?.let { return it }
        }
        return null
    }

    /**
     * The receiver a member function is called on: the object's own `INSTANCE`, the outer class's
     * static field when the class is a companion object, or a no-argument constructor.
     */
    private fun instanceOf(type: Class<*>): Any? =
        runCatching { type.getField("INSTANCE").get(null) }.getOrNull()
            ?: outerInstance(type)
            ?: runCatching { type.getDeclaredConstructor().newInstance() }.getOrNull()

    /**
     * The instance a nested object or a companion object is reachable through: its outer class holds
     * it in a static field. The field is found by type rather than by name, because an unnamed
     * companion's field is called `Companion` and it is the object's own name otherwise.
     */
    private fun outerInstance(type: Class<*>): Any? {
        val outerName = type.name.substringBeforeLast('$',"")
        if(outerName.isEmpty()) return null
        val outer = forName(outerName) ?: return null
        return outer.declaredFields
            .firstOrNull { Modifier.isStatic(it.modifiers) && it.type == type }
            ?.let { runCatching { it.get(null) }.getOrNull() }
    }

    /** The page for [method], or `null` after recording in [refused] why it cannot be previewed. */
    private fun resolve(method: Method,receiver: Any?,refused: MutableList<String>): Page? {
        val reason = refusalReason(method)
        if(reason == null) return Page(method,receiver,defaultsFor(method))
        refused += "${method.declaringClass.name}#${method.name}(${parameterTypes(method)}) - $reason"
        return null
    }

    private fun parameterTypes(method: Method) = method.parameterTypes.joinToString(",") { it.name }

    /**
     * One candidate name as the failure message shows it. A candidate whose class is not on the
     * classpath is marked as such, because otherwise the list reads as if that member had been
     * looked for and rejected on its shape.
     */
    private fun describeCandidate(className: String,methodName: String) =
        if(forName(className) == null) "$className#$methodName (class not found)"
        else "$className#$methodName"

    private fun forName(className: String): Class<*>? = runCatching { Class.forName(className) }.getOrNull()

    /**
     * Whether Kotlin reflection is on the classpath. It is what tells the preview which parameters
     * have defaults, and the library needs it nowhere else, so it is a compile-time dependency whose
     * absence is answered once here instead of failing somewhere inside the lookup.
     */
    private val hasKotlinReflection by lazy {
        runCatching { Class.forName("kotlin.reflect.jvm.ReflectJvmMapping") }.isSuccess
    }

    /** A page draws into the DSL context and is a real function: shape only, no parameter rules. */
    private fun pageShape(method: Method) =
        method.parameterCount >= 1 &&
            method.parameterTypes[0] == DslContext::class.java &&
            !Modifier.isAbstract(method.modifiers)

    /**
     * Why [method] cannot be called with no arguments, or `null` when it can. Every parameter after
     * the DSL context has to have a default value, and only Kotlin reflection can say which ones do:
     * the synthetic `name$default` method exists as soon as one parameter is defaulted, so its
     * presence says nothing about the others.
     */
    private fun refusalReason(method: Method): String? {
        val extras = method.parameterCount - 1
        if(extras == 0) return null
        if(!hasKotlinReflection) {
            return "it takes $extras parameter(s), and telling whether they all have defaults needs kotlin-reflect, which is not on the classpath"
        }
        val function = runCatching { method.kotlinFunction }.getOrNull()
            ?: return "kotlin-reflect could not read its parameters"
        // Context parameters have their own kind, so what is left is exactly the page's own list.
        val values = function.parameters.filter { it.kind == KParameter.Kind.VALUE }
        if(values.size != extras) {
            return "kotlin-reflect reported ${values.size} value parameter(s) for $extras JVM parameter(s)"
        }
        val vararg = values.firstOrNull { it.isVararg }
        if(vararg != null) return "parameter '${vararg.name}' is a vararg"
        val required = values.filterNot { it.isOptional }
        if(required.isNotEmpty()) {
            return required.joinToString { "parameter '${it.name}' (${it.type}) has no default value" }
        }
        // A member function can be optional without declaring a default of its own - the default
        // comes from the super declaration - and then this class has no `name$default` to call.
        if(defaultsFor(method) == null) return "its default values come from a super declaration"
        return null
    }

    /** The `name$default` method of [method], or `null` when it takes no defaulted parameter. */
    private fun defaultsFor(method: Method): Method? = runCatching {
        method.declaringClass.getDeclaredMethod(
            "${method.name}\$default",
            *method.parameterTypes,
            Int::class.java,
            Any::class.java,
        )
    }.getOrNull()

    /** The argument passed for a parameter whose default the `name$default` mask tells Kotlin to use. */
    private fun unusedArgument(type: Class<*>): Any? = when(type) {
        Boolean::class.java -> false
        Byte::class.java -> 0.toByte()
        Short::class.java -> 0.toShort()
        Char::class.java -> '\u0000'
        Int::class.java -> 0
        Long::class.java -> 0L
        Float::class.java -> 0.0f
        Double::class.java -> 0.0
        else -> null
    }

    /**
     * A resolved page: the method that draws it, the object it is a member of, and - when it takes
     * parameters - the `name$default` method used to call it with every one of them defaulted.
     */
    internal class Page internal constructor(
        private val method: Method,
        private val receiver: Any?,
        private val defaultMethod: Method?
    ) {
        /**
         * Whether the page is called through its synthetic `name$default` method, which is what
         * happens when it declares parameters that all have defaults; a page that takes only the DSL
         * context is called directly.
         */
        internal val usesDefaults get() = defaultMethod != null

        context(ctx: DslContext)
        fun draw() {
            val target = defaultMethod ?: method
            if(defaultMethod == null) {
                target.invoke(receiver,ctx)
                return
            }
            // `name$default` takes the page's parameters, then one bit per parameter saying which
            // of them to leave alone, then the marker Kotlin passes when it has no value to pass.
            // Those bits cover the context parameter as well, so the page's own parameters start
            // at the second bit. Resolution has already proved that every one of them has a default,
            // so the zero arguments below are never read.
            val extras = method.parameterCount - 1
            val arguments = arrayOfNulls<Any?>(method.parameterCount + 2)
            arguments[0] = ctx
            for(index in 1..extras) arguments[index] = unusedArgument(method.parameterTypes[index])
            arguments[extras + 1] = ((1 shl extras) - 1) shl 1
            arguments[extras + 2] = null
            target.invoke(receiver,*arguments)
        }

        fun describe() = "${method.declaringClass.name}#${method.name}"
    }
}


/**
 * The screen a preview shows when the page it was asked for cannot be opened: the reference, why it
 * could not be opened, and nothing else, so that a screenshot of it is a usable report.
 */
context(ctx: DslContext)
private fun PreviewErrorPage(text: String) = Column(DslModifier.padding(6.scaled)) {
    TextAutoFold { translate("kotlinmcui.preview.error").emit() }
    TextAutoFold { "\n$text".emit() }
}.defaultBackground()
