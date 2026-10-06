package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import io.github.u2894638479.kotlinmcui.dsl.forEachWithId
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Modifier

object DslEntryLoader {
    private val cache = mutableMapOf<Class<*>, Pair<DslEntryService, Flags>>()
    private var snapshot: List<Pair<DslEntryService, Flags>> = emptyList()

    private val registeredOverlays = mutableMapOf<String, DslEntryOverlay>()
    private val enabledIds = mutableListOf<String>()
    private var enabledSnapshot: List<DslEntryOverlay> = emptyList()

    /** Registered entries and their discovery flags. A snapshot, so callers may iterate it safely. */
    val entries: Collection<Pair<DslEntryService, Flags>> get() = snapshot

    /** Enabled overlays in stacking order: the first one is the topmost layer. */
    val enabledOverlays: List<DslEntryOverlay> get() = enabledSnapshot

    class Flags {
        var isClient = false
            internal set
        var isServer = false
            internal set
        var isCommon = false
            internal set
        var isGui = false
            internal set
        var isOverlay = false
            internal set
    }

    /**
     * A listing that could not be turned into a service: the class is missing, does not implement
     * the hook it is listed under, or cannot be constructed.
     *
     * The log keeps the whole stack; this is what the config page shows as a placeholder, so
     * [reason] stays one short line.
     */
    class Failure internal constructor(
        val className: String,
        /** The hook interface whose service file lists it. */
        val hookName: String,
        val reason: String,
    )

    private val failures = LinkedHashMap<String, Failure>()
    private var failureSnapshot: List<Failure> = emptyList()

    /** Listings that could not be loaded, in discovery order. A snapshot, like [entries]. */
    val brokenEntries: List<Failure> get() = failureSnapshot

    /**
     * Discover every entry on the classpath and initialise the ones the current
     * environment applies to, then read the stored overlay list.
     *
     * Called by [io.github.u2894638479.kotlinmcui.backend.DslRuntime.install], which
     * runs it exactly once and guarantees that metadata (and therefore the config
     * directory) is already available.
     */
    internal fun discover(environment: Environment) {
        failures.clear()
        initCommon()
        when(environment) {
            Environment.CLIENT -> initClient()
            Environment.SERVER -> initServer()
            Environment.COMMON -> {}
        }
        initGui()
        initOverlay()
        snapshot = cache.values.toList()
        failureSnapshot = failures.values.toList()
        collectOverlays()
        enabledIds.clear()
        enabledIds += readOverlayIds()
        refreshEnabledOverlays()
    }

    /** Whether [overlay] is currently enabled. */
    fun isOverlayEnabled(overlay: DslEntryOverlay): Boolean = overlay.id in enabledIds

    /** Enable or disable [overlay], storing the new list. */
    fun setOverlayEnabled(overlay: DslEntryOverlay, enabled: Boolean) {
        if (enabled == (overlay.id in enabledIds)) return
        if (enabled) enabledIds += overlay.id else enabledIds -= overlay.id
        refreshEnabledOverlays()
        writeOverlayIds(enabledIds.toList())
    }

    /** Move [overlay] to the front of the stack, storing the new order. */
    fun bringOverlayToTop(overlay: DslEntryOverlay) {
        if (enabledIds.firstOrNull() == overlay.id) return
        enabledIds.remove(overlay.id)
        enabledIds.add(0,overlay.id)
        refreshEnabledOverlays()
        writeOverlayIds(enabledIds.toList())
    }

    context(ctx: DslContext)
    internal fun overlays() {
        enabledSnapshot.forEachWithId {
            Box {
                it.overlay()
            }
        }
    }

    private fun collectOverlays() {
        registeredOverlays.clear()
        snapshot.forEach { (service, flags) ->
            if (!flags.isOverlay) return@forEach
            val overlay = service as DslEntryOverlay
            if (registeredOverlays.put(overlay.id,overlay) != null) {
                // The stored list holds ids, so two overlays sharing one cannot be
                // told apart once the config is written.
                dslLogger.warn("two overlays share the id '${overlay.id}'; only one of them can be enabled")
            }
        }
    }

    private fun refreshEnabledOverlays() {
        enabledSnapshot = enabledIds.mapNotNull(registeredOverlays::get)
    }

    private fun initClient() { load<DslEntryClient> { isClient = true }.forEach { it.initializeClient() } }
    private fun initGui() { load<DslEntryGui> { isGui = true } }
    private fun initOverlay() { load<DslEntryOverlay> { isOverlay = true } }
    private fun initServer() { load<DslEntryServer> { isServer = true }.forEach { it.initializeServer() } }
    private fun initCommon() { load<DslEntryCommon> { isCommon = true }.forEach { it.initialize() } }

    private inline fun <reified T: DslEntryService> load(flagAction: Flags.() -> Unit) = load(T::class.java) { flagAction() }

    private inline fun <T: DslEntryService> load(
        serviceInterface: Class<T>,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
        flagAction: Flags.() -> Unit
    ): List<T> {
        val result = mutableListOf<T>()
        try {
            val configs = classLoader.getResources("META-INF/services/${serviceInterface.name}")
            while (configs.hasMoreElements()) {
                configs.nextElement().openStream().use { stream ->
                    BufferedReader(InputStreamReader(stream, "utf-8")).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val className = line!!.trim()
                            if (className.isEmpty() || className.startsWith("#")) continue
                            try {
                                val clazz = Class.forName(className, false, classLoader)
                                val item = cache.getOrPut(clazz) { getObject(clazz) to Flags() }
                                // The cache is keyed by class alone, and `as T` would be erased
                                // to the type parameter's bound - the JVM cannot check a type
                                // variable - so the interface is checked here, by name. A wrong
                                // listing has to fail inside this catch: the caller's loop over
                                // the result has no catch, and would take the startup down.
                                result += serviceInterface.cast(item.first)
                                item.second.flagAction()
                            } catch (e: Exception) {
                                dslLogger.error("failed to load entry $className", e)
                                recordFailure(className, serviceInterface, e)
                            } catch (e: LinkageError) {
                                // A missing dependency or a failed static initialiser takes
                                // down this entry only, never the rest of the load.
                                dslLogger.error("failed to load entry $className", e)
                                recordFailure(className, serviceInterface, e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            dslLogger.error("failed to read the service files of ${serviceInterface.name}", e)
            return emptyList()
        }
        return result
    }

    /**
     * Keep a short, page-readable record of a listing that could not be turned into a service.
     * The same class listed wrongly under two hooks is two records: which file is wrong is
     * exactly what has to be fixed.
     */
    private fun recordFailure(className: String, serviceInterface: Class<*>, e: Throwable) {
        val hook = serviceInterface.simpleName
        val reason = when {
            e is ClassCastException -> "does not implement $hook"
            e is ClassNotFoundException -> "class not found"
            e is LinkageError -> "could not be initialised (missing dependency?)"
            e is java.lang.reflect.InvocationTargetException ->
                "constructor failed: ${e.cause?.let { cause -> cause::class.simpleName } ?: "unknown"}"
            else -> e.message?.lineSequence()?.firstOrNull()?.trim()?.take(120)?.takeIf { it.isNotEmpty() }
                ?: e::class.simpleName
                ?: "failed to load"
        }
        failures.putIfAbsent("$className|$hook",Failure(className,hook,reason))
    }

    private fun getObject(clazz: Class<*>): DslEntryService {
        // A Kotlin `object` has a private constructor, which is the recommended way to
        // declare an entry, so `INSTANCE` is the ordinary path rather than a fallback
        // worth warning about. A constructor that exists but throws is not silently
        // replaced by `INSTANCE` either: that would hide the failure behind a second
        // instance nobody asked for.
        val constructor = runCatching { clazz.getDeclaredConstructor() }.getOrNull()
        val instance: Any
        if (constructor != null && Modifier.isPublic(constructor.modifiers)) {
            instance = constructor.newInstance()
            dslLogger.info("loaded class: $clazz")
        } else {
            instance = clazz.declaredFields
                .firstOrNull { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
                ?.get(null)
                ?: error("$clazz has neither a public no-argument constructor nor an INSTANCE field")
            dslLogger.info("loaded Kotlin object: $clazz")
        }
        return instance as? DslEntryService
            ?: error("$clazz does not implement DslEntryService")
    }
}
