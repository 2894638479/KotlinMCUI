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

    /** Registered entries and their discovery flags. A snapshot, so callers may iterate it safely. */
    val entries: Collection<Pair<DslEntryService, Flags>> get() = snapshot

    /** Overlays the user enabled, in stacking order (front of the list is on top). */
    internal var enabledOverlays = mutableListOf<DslEntryOverlay>()

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
     * apart from a warning: registering is idempotent, and initialising a mod twice
     * is never what anyone wants.
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
    }

    context(ctx: DslContext)
    fun overlays() = enabledOverlays.forEachWithId {
        Box {
            it.overlay()
        }
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
