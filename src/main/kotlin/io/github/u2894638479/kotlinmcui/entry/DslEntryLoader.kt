package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import io.github.u2894638479.kotlinmcui.dsl.forEachWithId
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import java.io.BufferedReader
import java.io.InputStreamReader

object DslEntryLoader {
    private fun initClient() { load<DslEntryClient> { isClient = true }.forEach { it.initializeClient() } }
    private fun initGui() { load<DslEntryGui> { isGui = true } }
    private fun initOverlay() { load<DslEntryOverlay> { isOverlay = true } }
    private fun initServer() { load<DslEntryServer> { isServer = true }.forEach { it.initializeServer() } }
    private fun initCommon() { load<DslEntryCommon> { isCommon = true }.forEach { it.initialize() } }
    fun init(environment: Environment) {
        cache.clear()
        initCommon()
        when(environment) {
            Environment.CLIENT -> initClient()
            Environment.SERVER -> initServer()
            Environment.COMMON -> {}
        }
        initGui()
        initOverlay()
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
    private val cache = mutableMapOf<Class<*>,Pair<DslEntryService, Flags>>()
    val entries: Collection<Pair<DslEntryService,Flags>> get() = cache.values
    internal var enabledOverlays = mutableListOf<DslEntryOverlay>()
    context(ctx: DslContext)
    fun overlays() = enabledOverlays.forEachWithId {
        Box {
            it.overlay()
        }
    }
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
                                dslLogger.error("class load failed: $className")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            dslLogger.error("failed to load entries")
            return emptyList()
        }
        return result
    }

    private fun <T> getObject(clazz: Class<*>, service: Class<T>): T {
        try {
            val instance = clazz.getDeclaredConstructor().newInstance()
            try {
                return service.cast(instance).also {
                    dslLogger.info("loaded class: $clazz")
                }
            } catch (e: Exception) {
                dslLogger.error("failed to cast to DslEntryService.")
            }
        } catch (e: Exception) {
            dslLogger.warn("no constructor found, trying to load as Kotlin object.")
            dslLogger.warn("class: $clazz")
        }
        val instanceField = clazz.declaredFields.firstOrNull { it.name == "INSTANCE" }!!
        return instanceField.get(null)!!.let { service.cast(it) }.also {
            dslLogger.info("loaded Kotlin object: $clazz")
        }
    }
}