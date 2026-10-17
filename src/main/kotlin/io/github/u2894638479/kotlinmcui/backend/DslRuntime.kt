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
 * metadata happen inside [install], and [dslPlatform] starts working at [platformReady].
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
    internal val installedButNotReady get() = synchronized(lock) { installed && dslPlatformProvider == null }

    /**
     * The process owner root, owned by the first provider registered.
     *
     * Internal on purpose: a backend cannot name it, so "render the process root" is not something a
     * later backend can even express. Backends ask [rootFor] instead, which answers with this root for
     * the first registrant and with a fresh root for everybody else - the mistake stops being a
     * discipline question and becomes unrepresentable.
     *
     * Reading it materialises the main root if it does not exist yet.
     */
    internal val ownerRoot: DslRoot get() = mainRoot

    /**
     * The root that belongs to [provider] - ask this instead of deciding which root to use yourself.
     *
     * The first provider registered owns the process root, so its callers get exactly that root (the
     * one `isOwner` marks). Any later provider gets its own root from [createRoot] with
     * [viewerLifetime]: it renders its own tree, which is the whole point - a second backend that took
     * the process root would silently draw the first one's UI.
     *
     * The ordering test lives inside this call, so a caller cannot get it wrong by checking
     * [registeredProviders] at the wrong moment.
     *
     * **Only ask for a root after the platform is published.** An earlier call still returns an object
     * (never `null`, never a throw), but it is a *refused* root: nothing works, and the first thing
     * that touches its provider names the reason. The entry order is therefore "register, publish,
     * then take roots".
     */
    fun rootFor(provider: DslRootProvider<*, *>, viewerLifetime: ViewerLifetime): DslRoot =
        synchronized(lock) {
            if (providers.firstOrNull() === provider) ownerRoot else createRoot(provider, viewerLifetime)
        }

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
     * Publish [platform]: from here on [dslPlatform] works.
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
     *
     * Registration order decides one thing only: whether [rootFor] hands this provider the process
     * root. The first one registered owns it; every later one gets a root of its own. Callers never
     * need to know which case they are - that is what [rootFor] is for.
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
        return buildRoot(refusedProvider(reason), ViewerLifetime.NONE, isOwner = false, refusal = reason)
    }

    /**
     * The provider of a refused root: every member fails with the refusal reason.
     *
     * A refused root cannot report its reason from one place - the first thing a caller touches may be
     * the root's children, its component, or the screen factory it passes on to a screen's store. So
     * the reason travels with the object and is named by whichever path fails first, instead of each
     * path reporting its own local symptom ("cannot create screens") and hiding the real cause.
     */
    private fun refusedProvider(reason: DslRejectionReason): DslRootProvider<Nothing, Nothing> =
        object : DslRootProvider<Nothing, Nothing> {
            private fun fail(): Nothing = error(
                "this root was never usable: it was refused with $reason. " + when(reason) {
                    DslRejectionReason.PLATFORM_NOT_PUBLISHED ->
                        "No platform has been published yet: publish it before taking a root, and " +
                            "register the provider first."
                    DslRejectionReason.PLATFORM_ALREADY_HELD ->
                        "Another root already owns the process: every backend but the first must ask " +
                            "rootFor for its own root."
                }
            )

            override val renderer get() = fail()
            override val input: DslBackendInput get() = fail()
            override val screenFactory get() = fail()
            override val rootUtils: DslRootUtils get() = fail()
            override fun frame(root: DslRoot, renderParam: Nothing) = fail()
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
        dslPlatformProvider = null
        providers.clear()
        refusalLedger.clear()
        ownerHeld = false
        // `ownerCreations` is deliberately not reset: it counts the process's owner roots, and the
        // main root survives a reset (it is a lazily built singleton).
    }
}
