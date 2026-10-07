package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.logger.dslLogger
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
 * [DslBackendUtils.capture] and [DslBackendUtils.quit] plus the render loop that ticks
 * [TopComponent.frameIndex]. Keeping the timing rules in the framework means they are the same on
 * every platform, and testable without a game.
 *
 * `-Dkotlinmcui.preview=<reference>` opens the page. `-Dkotlinmcui.preview.capture=true` turns the
 * session into the one a machine can gate on: it photographs the page and quits, with a non-zero
 * code when the reference could not be resolved, the page threw, or no file was written.
 *
 * A page is named by reference rather than registered, so the framework needs no preview-only API:
 *
 * - the reference an IDE gives for a top-level function, which is the file name plus the function
 *   (`a.b.TestColorPage` means `a.b.TestColorPageKt.TestColorPage`);
 * - `a.b.Class#method` for a static function;
 * - a class name, in which case its `content()` draws the page (an entry).
 *
 * A page takes the DSL context, and may take further parameters - but only defaulted ones, because
 * the preview passes no arguments. Whether a parameter has a default is invisible to Java reflection,
 * so a page that is not shaped that way is refused instead of being called with values nobody supplied.
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

    /** Called once the backend is published; does nothing unless the platform asked for a page. */
    fun start() {
        val request = request() ?: return
        CoroutineScope(Dispatchers.Default).launch { run(request.first,request.second) }
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
    internal suspend fun run(reference: String, capture: Boolean) {
        val backend = dslBackend
        val page = try {
            pageFor(reference)
        } catch(e: Throwable) {
            dslLogger.error("preview: '$reference' could not be resolved",e)
            backend.quit(1)
            return
        }
        if(page.usesDefaults) {
            dslLogger.info("preview: ${page.describe()} takes parameters; all of them have defaults, so they are passed as zero values")
        }
        try {
            if(!awaitFirstFrame()) {
                dslLogger.error("preview: the platform never rendered a frame, giving up on '$reference'")
                backend.quit(1)
                return
            }
            val opened = TopComponent.frameIndex
            withContext(backend.mainDispatcher) { backend.create(reference) { page.draw() } }.show()
            dslLogger.info("preview: opened '$reference' as ${page.describe()}")
            while(TopComponent.frameIndex < opened + FRAMES) delay(FRAME_POLL_MS.milliseconds)
            if(!capture) return
            val file = withContext(backend.mainDispatcher) { backend.capture(fileNameFor(reference)) }
            if(TopComponent.renderFailure != null) {
                dslLogger.error("preview: ${page.describe()} threw while rendering",TopComponent.renderFailure)
                backend.quit(1)
                return
            }
            if(file == null) {
                dslLogger.error("preview: no screenshot was written for '$reference'")
                backend.quit(1)
                return
            }
            dslLogger.info("preview: wrote ${file.absolutePath} for ${page.describe()}")
            backend.quit(0)
        } catch(e: Throwable) {
            // A page that throws while being built, or a platform that throws while capturing, has
            // to end the run with a code as well - hanging is the one outcome nobody can gate on.
            dslLogger.error("preview: '$reference' failed",e)
            backend.quit(1)
        }
    }

    /**
     * Waits until the platform has rendered one frame through the framework, which is the moment it
     * is past its own loading screen and a screen of ours can survive.
     */
    private suspend fun awaitFirstFrame(): Boolean {
        val deadline = System.currentTimeMillis() + READY_TIMEOUT_MS
        while(TopComponent.frameIndex == 0L) {
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
            tried += "$type#$method"
            staticPage(type,method,refused)?.let { return it }
        } else {
            // The JVM class of a top-level function is its file name with `Kt` appended.
            val simple = reference.substringAfterLast('.')
            val parent = reference.substringBeforeLast('.',"")
            val fileClass = if(parent.isEmpty()) "${simple}Kt" else "$parent.${simple}Kt"
            tried += "$fileClass#$simple"
            staticPage(fileClass,simple,refused)?.let { return it }
            tried += "$reference#content"
            entryPage(reference,refused)?.let { return it }
        }
        val found = if(refused.isEmpty()) "" else "; found ${refused.joinToString()}"
        val rule = if(refused.isEmpty()) "" else
            "; a preview page may only take the DSL context or parameters that all have default values"
        error("no preview page for '$reference' - tried ${tried.joinToString()}$found$rule")
    }

    private fun staticPage(className: String,methodName: String,refused: MutableList<String>): Page? {
        val type = forName(className) ?: return null
        for(method in type.declaredMethods) {
            if(method.name != methodName || !Modifier.isStatic(method.modifiers) || !pageShape(method)) continue
            val page = resolve(method,null,refused)
            if(page != null) return page
        }
        return null
    }

    private fun entryPage(className: String,refused: MutableList<String>): Page? {
        val type = forName(className) ?: return null
        val candidates = type.declaredMethods.filter { it.name == "content" && pageShape(it) }
        if(candidates.isEmpty()) return null
        val instance = runCatching { type.getField("INSTANCE").get(null) }.getOrNull()
            ?: runCatching { type.getDeclaredConstructor().newInstance() }.getOrNull()
            ?: return null
        // The interface's own `content()` is the parameterless one, so it wins over an object's
        // parameterised overload: the order `declaredMethods` reports is not specified.
        candidates.firstOrNull { it.parameterCount == 1 }?.let { return Page(it,instance,null) }
        for(method in candidates) {
            val page = resolve(method,instance,refused)
            if(page != null) return page
        }
        return null
    }

    /** The page for [method], or `null` after recording in [refused] why it cannot be previewed. */
    private fun resolve(method: Method,receiver: Any?,refused: MutableList<String>): Page? {
        val reason = refusalReason(method)
        if(reason == null) return Page(method,receiver,defaultsFor(method))
        refused += "${method.declaringClass.name}#${method.name}(${parameterTypes(method)}) - $reason"
        return null
    }

    private fun parameterTypes(method: Method) = method.parameterTypes.joinToString(",") { it.name }

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
