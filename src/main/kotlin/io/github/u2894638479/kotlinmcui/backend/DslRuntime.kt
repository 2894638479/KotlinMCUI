package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.container.DslRootGeometry
import io.github.u2894638479.kotlinmcui.container.DslRootState
import io.github.u2894638479.kotlinmcui.container.ViewerLifetime
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

    /** Providers registered by entry points, in registration order. */
    private val providers = mutableListOf<DslRootProvider<*, *>>()

    /** The single refusal ledger; log lines are derived from it. */
    private val refusalLedger = mutableListOf<DslRejection>()

    /** Whether an owner root has been created; the second [createOwner] must be refused. */
    private var ownerHeld = false

    /** How many owner roots were actually built. One, for the life of the process. */
    internal var ownerCreations = 0
        private set

    /** Whether [install] has run. */
    internal val isInstalled get() = synchronized(lock) { installed }

    /** Installed, but the backend has not been published yet. */
    internal val installedButNotReady get() = synchronized(lock) { installed && dslBackendProvider == null }

    /**
     * The process owner root: the one root that stands for this process.
     *
     * Today that is the main root. When roots become multiple, this is the one that holds `isOwner` -
     * the draft's `DslRuntime.ownerRoot` - and the type stays the same.
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
    fun platformReady(platform: () -> DslPlatform): Boolean = synchronized(lock) {
        check(installed) { "DslRuntime.platformReady was called before DslRuntime.install" }
        if (dslPlatformProvider != null) {
            dslLogger.warn(
                "DslRuntime.platformReady was already called; this platform is ignored. " +
                    "Install only one platform in a game instance.",
            )
            return@synchronized false
        }
        dslPlatformProvider = platform
        // A platform that asked for a page to be shown gets it now; otherwise this does nothing.
        DslPreview.start()
        true
    }

    /**
     * Register a [provider] an entry point brought with it. It may be shared by several roots and
     * registered more than once; no root exists at this point.
     */
    fun registerProvider(provider: DslRootProvider<*, *>) = synchronized(lock) {
        providers += provider
    }

    /** The providers registered so far, in registration order. */
    val registeredProviders: List<DslRootProvider<*, *>>
        get() = synchronized(lock) { providers.toList() }

    /**
     * Every refusal the runtime handed out. The ledger is the only record; log lines are derived from
     * it, never written next to it.
     */
    val rejections: List<DslRejection>
        get() = synchronized(lock) { refusalLedger.toList() }

    data class DslRejection(val what: String, val reason: DslRejectionReason, val holder: String?)

    enum class DslRejectionReason {
        /** Nothing published a platform yet, so a root could not be given one. */
        PLATFORM_NOT_PUBLISHED,

        /** Another root already owns the process; `isOwner` must stay a structural guarantee. */
        PLATFORM_ALREADY_HELD
    }

    /** Whether this root was built by the runtime; a refused root answers `false`. */
    val DslRoot.isRegistered: Boolean get() = refusal == null

    /** Why this root was refused, or `null` for a working one. The reason travels with the object. */
    val DslRoot.refusalReason: DslRejectionReason? get() = refusal

    /**
     * Create a root for [provider].
     *
     * Never returns `null`: when the platform is not published yet the caller gets a *refused* root,
     * which fails loudly with [refusalReason] as soon as it is used. Its screen's store is stamped
     * with this root's state when the screen is mounted, which happens before the screen builds.
     *
     * @throws IllegalStateException when [install] has not run.
     */
    fun createRoot(provider: DslRootProvider<*, *>, viewerLifetime: ViewerLifetime): DslRoot =
        synchronized(lock) {
            check(installed) { "DslRuntime.createRoot was called before DslRuntime.install" }
            if (dslPlatformProvider == null) {
                refuse("root for ${provider.describe()}", DslRejectionReason.PLATFORM_NOT_PUBLISHED)
            } else {
                buildRoot(provider, viewerLifetime, isOwner = false, refusal = null)
            }
        }

    /**
     * The one entry point that may create the process owner.
     *
     * A second call is refused with [DslRejectionReason.PLATFORM_ALREADY_HELD] and recorded in
     * [rejections]: limiting *who* may set `isOwner` without limiting *how often* would stop being a
     * structural guarantee.
     */
    fun createOwner(provider: DslRootProvider<*, *>): DslRoot = synchronized(lock) {
        check(installed) { "DslRuntime.createOwner was called before DslRuntime.install" }
        when {
            dslPlatformProvider == null ->
                refuse("owner root for ${provider.describe()}", DslRejectionReason.PLATFORM_NOT_PUBLISHED)
            ownerHeld ->
                refuse("owner root for ${provider.describe()}", DslRejectionReason.PLATFORM_ALREADY_HELD)
            else -> {
                ownerHeld = true
                ownerCreations++
                buildRoot(provider, ViewerLifetime.PROCESS, isOwner = true, refusal = null)
            }
        }
    }

    private fun refuse(what: String, reason: DslRejectionReason): DslRoot {
        refusalLedger += DslRejection(what, reason, holder = null)
        return buildRoot(RefusedProvider, ViewerLifetime.NONE, isOwner = false, refusal = reason)
    }

    /**
     * A refused root still needs a provider object; nothing reaches it, because [DslRoot.children]
     * throws on the refusal first.
     */
    private object RefusedProvider : DslRootProvider<Nothing, Nothing> {
        override val renderer get() = null
        override val input: DslBackendInput get() = error("this root was refused")
        override val screenFactory get() = null
        override val rootUtils: DslRootUtils get() = error("this root was refused")
        override fun frame(root: DslRoot, renderParam: Nothing) = error("this root was refused")
    }

    private fun DslRootProvider<*, *>.describe(): String = this::class.simpleName ?: "root provider"

    private fun buildRoot(
        provider: DslRootProvider<*, *>,
        viewerLifetime: ViewerLifetime,
        isOwner: Boolean,
        refusal: DslRejectionReason?
    ): DslRoot {
        lateinit var root: DslRoot
        val state = DslRootState(
            onFocusChanged = { newFocus, source -> root.component.globalFocusChanged(newFocus, source) },
            onHoverChanged = { newHover -> root.component.globalHoverChanged(newHover) },
            onNarrate = { provider.rootUtils.narrate(it) }
        )
        root = DslRoot(state, DslRootGeometry(), provider, viewerLifetime, isOwner, refusal)
        return root
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
        dslPlatformProvider = null
        providers.clear()
        refusalLedger.clear()
        ownerHeld = false
        // `ownerCreations` is deliberately not reset: it counts the process's owner roots, and the
        // main root survives a reset (it is a lazily built singleton).
    }
}
