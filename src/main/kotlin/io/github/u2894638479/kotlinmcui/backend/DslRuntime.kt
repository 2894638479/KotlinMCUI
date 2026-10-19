package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader
import io.github.u2894638479.kotlinmcui.logger.dslLogger

/**
 * Process-wide startup: install the platform's metadata once, publish its utils when screens can be built.
 *
 * This object deliberately knows nothing about roots. A root belongs to the backend that built it,
 * which is the whole shape of the framework now: the runtime hands out no roots, so "who owns the
 * process root" is not a question it can answer - and therefore not a question anyone can get wrong.
 *
 * The sequence a backend follows is: [install] while the game initialises (metadata only, no DSL
 * evaluation), build its own root, then [start] once the platform can create screens.
 */
object DslRuntime {
    private val lock = Any()
    private var installed = false

    /** Whether [install] has run. */
    internal val isInstalled get() = synchronized(lock) { installed }

    /**
     * Install the platform's metadata and do everything that is safe this early.
     *
     * @param metadata platform facts, read immediately: it must be side-effect free,
     *   because the platform cannot build anything yet.
     * @throws IllegalStateException when it is called a second time.
     */
    fun install(metadata: () -> DslPlatformMetadata) = synchronized(lock) {
        check(!installed) { "DslRuntime.install was already called" }
        dslMetadataProvider = metadata
        DslEntryLoader.discover(metadata().environment)
        installed = true
    }

    /**
     * Publish [utils]: from here on [dslUtils] works.
     *
     * Call it when the platform can create screens.
     *
     * Only the first backend wins. A second call is refused and logged rather than thrown, because
     * two backends in one instance is a user's installation mistake and must not become a client
     * crash - but the caller has to be able to see that it lost. **Check the return value before
     * doing anything visible.**
     *
     * @return `true` when this call published the utils, `false` when another one was already
     *   published (the published one is left untouched).
     * @throws IllegalStateException when [install] has not run.
     */
    fun start(utils: () -> DslPlatformUtils): Boolean = synchronized(lock) {
        check(installed) { "DslRuntime.start was called before DslRuntime.install" }
        if (dslUtilsProvider != null) {
            dslLogger.warn(
                "DslRuntime.start was already called; these utils are ignored. " +
                    "Install only one platform in a game instance.",
            )
            return@synchronized false
        }
        dslUtilsProvider = utils
        true
    }

    /**
     * The platform's owner declares readiness: entries may now open windows, start services and build
     * roots.
     *
     * Deliberately **not** a side effect of [start]. "There is a platform" and "the game is ready" are two
     * facts: a standalone tool or a test can publish a platform without a game being ready, and running the
     * entries' launch hooks then would be wrong. In a game the two calls sit next to each other; the game
     * backend (headless) is what calls this after `start`.
     *
     * Idempotent, and an error before the platform is published - the entries cannot do anything useful
     * without one.
     */
    fun launch() = DslEntryLoader.launch()

    /**
     * Restore the state the process starts in: nothing installed, no platform published.
     *
     * Exists for the test suite. "Exactly once" is a process-wide promise, so a case can only
     * exercise the sequence from a defined starting point if something can put the singleton back
     * into it - otherwise the cases only work when they run before every other class in the JVM.
     * Tests only; nothing in the framework calls this.
     */
    internal fun resetForTest() = synchronized(lock) {
        installed = false
        dslMetadataProvider = null
        dslUtilsProvider = null
    }
}
