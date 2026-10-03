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
    private var initialized = false

    private val registeredOverlays = mutableMapOf<String, DslEntryOverlay>()
    private val enabledIds = mutableListOf<String>()
    private var enabledSnapshot: List<DslEntryOverlay> = emptyList()
    private var overlaysLoaded = false

    /** Registered entries and their discovery flags. A snapshot, so callers may iterate it safely. */
    val entries: Collection<Pair<DslEntryService, Flags>> get() = snapshot

    /**
     * Enabled overlays in stacking order: the first one is the topmost layer.
     *
     * The stored list is read the first time it is needed rather than during [init]:
     * reading it needs the backend's config directory, and the backend is not usable
     * during mod initialisation.
     */
    val enabledOverlays: List<DslEntryOverlay>
        get() {
            ensureOverlaysLoaded()
            return enabledSnapshot
        }

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
     * Discover every entry on the classpath and initialise the ones the current
     * environment applies to.
     *
     * Call this **once**, from the backend's initialiser. A repeated call is ignored
     * apart from a warning: registering is idempotent, the enabled overlays are read
     * only once, and initialising a mod twice is never what anyone wants.
     */
    fun init(environment: Environment) {
        if (initialized) {
            dslLogger.warn("DslEntryLoader.init was already called, ignoring the $environment call")
        } else {
            initialized = true
            initCommon()
            when(environment) {
                Environment.CLIENT -> initClient()
                Environment.SERVER -> initServer()
                Environment.COMMON -> {}
            }
        }
        initGui()
        initOverlay()
        snapshot = cache.values.toList()
        collectOverlays()
        refreshEnabledOverlays()
    }

    /** Whether [overlay] is currently enabled. */
    fun isOverlayEnabled(overlay: DslEntryOverlay): Boolean {
        ensureOverlaysLoaded()
        return overlay.id in enabledIds
    }

    /** Enable or disable [overlay], storing the new list. */
    fun setOverlayEnabled(overlay: DslEntryOverlay, enabled: Boolean) {
        ensureOverlaysLoaded()
        if (enabled == (overlay.id in enabledIds)) return
        if (enabled) enabledIds += overlay.id else enabledIds -= overlay.id
        refreshEnabledOverlays()
        writeOverlayIds(enabledIds.toList())
    }

    /** Move [overlay] to the front of the stack, storing the new order. */
    fun bringOverlayToTop(overlay: DslEntryOverlay) {
        ensureOverlaysLoaded()
        if (enabledIds.firstOrNull() == overlay.id) return
        enabledIds.remove(overlay.id)
        enabledIds.add(0,overlay.id)
        refreshEnabledOverlays()
        writeOverlayIds(enabledIds.toList())
    }

    context(ctx: DslContext)
    fun overlays(): Unit {
        ensureOverlaysLoaded()
        enabledSnapshot.forEachWithId {
            Box {
                it.overlay()
            }
        }
    }

    private fun ensureOverlaysLoaded() {
        if (overlaysLoaded) return
        overlaysLoaded = true
        enabledIds.clear()
        enabledIds += readOverlayIds()
        refreshEnabledOverlays()
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
                                val item = cache.getOrPut(clazz) { getObject(clazz,serviceInterface) to Flags() }
                                item.second.flagAction()
                                result += item.first as T
                            } catch (e: Exception) {
                                dslLogger.error("failed to load entry $className", e)
                            } catch (e: LinkageError) {
                                // A missing dependency or a failed static initialiser takes
                                // down this entry only, never the rest of the load.
                                dslLogger.error("failed to load entry $className", e)
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

    private fun <T> getObject(clazz: Class<*>, service: Class<T>): T {
        // A Kotlin `object` has a private constructor, which is the recommended way to
        // declare an entry, so `INSTANCE` is the ordinary path rather than a fallback
        // worth warning about. A constructor that exists but throws is not silently
        // replaced by `INSTANCE` either: that would hide the failure behind a second
        // instance nobody asked for.
        val constructor = runCatching { clazz.getDeclaredConstructor() }.getOrNull()
        if (constructor != null && Modifier.isPublic(constructor.modifiers)) {
            return service.cast(constructor.newInstance()).also {
                dslLogger.info("loaded class: $clazz")
            }
        }
        val instance = clazz.declaredFields
            .firstOrNull { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
            ?.get(null)
            ?: error("$clazz has neither a public no-argument constructor nor an INSTANCE field")
        return service.cast(instance).also {
            dslLogger.info("loaded Kotlin object: $clazz")
        }
    }
}
