package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader

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
     * @throws IllegalStateException when [install] has not run, or when a backend was
     *   already published.
     */
    fun platformReady(backend: () -> DslBackend<*, *>) = synchronized(lock) {
        check(installed) { "DslRuntime.platformReady was called before DslRuntime.install" }
        check(dslBackendProvider == null) { "DslRuntime.platformReady was already called" }
        dslBackendProvider = backend
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
