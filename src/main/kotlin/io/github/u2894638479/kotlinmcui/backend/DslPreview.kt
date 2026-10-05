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

/**
 * The in-game preview session: open the page a launch property names, let it render, photograph it
 * and stop - or, for a human, just open it and stay.
 *
 * All of this is policy and therefore lives here; the platform only supplies the two members
 * [DslBackendUtils.capture] and [DslBackendUtils.quit] plus the render loop that ticks
 * [TopComponent.frameIndex]. Keeping the timing rules in the framework means they are the same on
 * every platform, and testable without a game.
 *
 * A page is named by reference rather than registered, so the framework needs no preview-only API:
 *
 * - the reference an IDE gives for a top-level function, which is the file name plus the function
 *   (`a.b.TestColorPage` means `a.b.TestColorPageKt.TestColorPage`);
 * - `a.b.Class#method` for a static function;
 * - a class name, in which case its `content()` draws the page (an entry).
 */
internal object DslPreview {
    /** Opens the referenced page, photographs it and stops: the mode a machine can gate on. */
    private const val VERIFY_PROPERTY = "kotlinmcui.verify"

    /** Opens the referenced page and stays: the mode a human looks at. */
    private const val PREVIEW_PROPERTY = "kotlinmcui.preview"

    /** Frames the page is given before it is photographed: a frame clock, not a wall clock. */
    private const val FRAMES = 30L

    private const val FRAME_POLL_MS = 16L
    private const val READY_POLL_MS = 50L
    private const val READY_TIMEOUT_MS = 240_000L

    /** Called once the backend is published; does nothing unless the platform asked for a page. */
    fun start() {
        val verify = System.getProperty(VERIFY_PROPERTY)?.takeIf { it.isNotBlank() }
        val reference = verify ?: System.getProperty(PREVIEW_PROPERTY)?.takeIf { it.isNotBlank() } ?: return
        CoroutineScope(Dispatchers.Default).launch { run(reference,verify != null) }
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
        try {
            if(!awaitFirstFrame()) {
                dslLogger.error("preview: the platform never rendered a frame, giving up on '$reference'")
                backend.quit(1)
                return
            }
            val opened = TopComponent.frameIndex
            withContext(backend.mainDispatcher) { backend.create(reference) { page.draw() } }.show()
            while(TopComponent.frameIndex < opened + FRAMES) delay(FRAME_POLL_MS)
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
            delay(READY_POLL_MS)
        }
        return true
    }

    /** The screenshot name for [reference]: stable and readable, derived from the page's own name. */
    internal fun fileNameFor(reference: String): String {
        val simple = reference.substringAfterLast('.').substringAfterLast('$')
        val safe = simple.map { if(it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }.joinToString("")
        return "kotlinmcui-${safe.ifEmpty { "page" }}.png"
    }

    /** Resolves [reference] to a page, or fails with every name that was tried. */
    internal fun pageFor(reference: String): Page {
        val tried = mutableListOf<String>()
        val hash = reference.indexOf('#')
        if(hash > 0) {
            val type = reference.substring(0,hash)
            val method = reference.substring(hash + 1)
            tried += "$type#$method"
            staticPage(type,method)?.let { return it }
        } else {
            // The JVM class of a top-level function is its file name with `Kt` appended.
            val simple = reference.substringAfterLast('.')
            val parent = reference.substringBeforeLast('.',"")
            val fileClass = if(parent.isEmpty()) "${simple}Kt" else "$parent.${simple}Kt"
            tried += "$fileClass#$simple"
            staticPage(fileClass,simple)?.let { return it }
            tried += "$reference#content"
            entryPage(reference)?.let { return it }
        }
        error("no preview page for '$reference' - tried ${tried.joinToString()}")
    }

    private fun staticPage(className: String, methodName: String): Page? =
        forName(className)?.declaredMethods
            ?.firstOrNull { it.name == methodName && Modifier.isStatic(it.modifiers) && isPage(it) }
            ?.let { Page(it,null) }

    private fun entryPage(className: String): Page? = forName(className)?.let { type ->
        val instance = runCatching { type.getField("INSTANCE").get(null) }.getOrNull()
            ?: runCatching { type.getDeclaredConstructor().newInstance() }.getOrNull()
        val method = type.declaredMethods.firstOrNull { it.name == "content" && isPage(it) }
        if(instance != null && method != null) Page(method,instance) else null
    }

    private fun forName(className: String): Class<*>? = runCatching { Class.forName(className) }.getOrNull()

    /** A page draws into the DSL context, so it takes exactly that one parameter. */
    private fun isPage(method: Method) =
        method.parameterTypes.size == 1 &&
            method.parameterTypes[0] == DslContext::class.java &&
            !Modifier.isAbstract(method.modifiers)

    /** A resolved page: the method to call and, for an entry member, the object to call it on. */
    internal class Page internal constructor(private val method: Method,private val receiver: Any?) {
        context(ctx: DslContext)
        fun draw() {
            method.invoke(receiver,ctx)
        }

        fun describe() = "${method.declaringClass.name}#${method.name}"
    }
}
