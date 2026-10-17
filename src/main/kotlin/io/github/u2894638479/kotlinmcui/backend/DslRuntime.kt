package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.container.mainRoot
import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader
import io.github.u2894638479.kotlinmcui.logger.dslLogger

/**
 * The framework's startup sequence.
 *
 * A backend calls [install] while the mod initialises — metadata is readable that
 * early, the backend itself is not — and [platformReady] once screens can be built.
 * Nothing else calls them: entry discovery and the config files that only need
 * metadata happen inside [install], and [dslBackend] starts working at [platformReady].
 *
 * Both calls take a lock: the whole point is that they happen exactly once, and
 * "exactly once" has to hold no matter which thread the loader happens to use.
 */
object DslRuntime {
    private val lock = Any()
    private var installed = false

    /** Whether [install] has run. */
    internal val isInstalled get() = synchronized(lock) { installed }

    /** Installed, but the backend has not been published yet. */
    internal val installedButNotReady get() = synchronized(lock) { installed && dslBackendProvider == null }

    /**
     * The process owner root: the one root that stands for this process.
     *
     * Today that is the main root, which is also what [io.github.u2894638479.kotlinmcui.container.TopComponent]
     * projects. When roots become multiple, this is the one that holds `isOwner` - the draft's
     * `DslRuntime.ownerRoot` - and the type stays the same.
     *
     * Reading it materialises the main root if it does not exist yet, so a backend can take the handle
     * before it has mounted anything. That makes it never `null`: the "queried before the root exists"
     * window does not arise, which removes a whole class of too-early failures from the backend side.
     */
    val ownerRoot: DslRoot get() = mainRoot

    /**
     * Register the platform and do everything that is safe this early.
     *
     * @param metadata platform facts, read immediately: it must be side-effect free,
     *   because the platform cannot build a backend yet.
     * @throws IllegalStateException when it is called a second time.
     */
    fun install(metadata: () -> DslBackendMetadata) = synchronized(lock) {
        check(!installed) { "DslRuntime.install was already called" }
        dslMetadataProvider = metadata
        DslEntryLoader.discover(metadata().environment)
        installed = true
    }

    /**
     * Publish [backend]: from here on [dslBackend] works.
     *
     * Call it when the platform can create screens — for Fabric, after
     * `Minecraft.<init>` returns. The factory arrives together with readiness, so there
     * is no "registered but not published" state to keep.
     *
     * Only the first backend wins. A second call is refused and logged rather than
     * thrown, because two backends in one instance is a user's installation mistake and
     * must not become a client crash — but the caller has to be able to see that it lost:
     * a platform that goes on to open its own screens would draw them with the winner's
     * renderer. **Check the return value before doing anything visible.**
     *
     * @return `true` when this call published the backend, `false` when another one was
     *   already published (the published one is left untouched).
     * @throws IllegalStateException when [install] has not run.
     */
    fun platformReady(backend: () -> DslBackend<*, *>): Boolean = synchronized(lock) {
        check(installed) { "DslRuntime.platformReady was called before DslRuntime.install" }
        if (dslBackendProvider != null) {
            dslLogger.warn(
                "DslRuntime.platformReady was already called; this backend is ignored. " +
                    "Install only one backend in a game instance.",
            )
            return@synchronized false
        }
        dslBackendProvider = backend
        // A platform that asked for a page to be shown gets it now; otherwise this does nothing.
        DslPreview.start()
        true
    }

    /**
     * Restore the state the process starts in: nothing installed, no providers published.
     *
     * Exists for the test suite. "Exactly once" is a process-wide promise, so a case can
     * only exercise the sequence from a defined starting point if something can put the
     * singleton back into it — otherwise the cases only work when they run before every
     * other class in the JVM. Tests only; nothing in the framework calls this.
     */
    internal fun resetForTest() = synchronized(lock) {
        installed = false
        dslMetadataProvider = null
        dslBackendProvider = null
    }
}
