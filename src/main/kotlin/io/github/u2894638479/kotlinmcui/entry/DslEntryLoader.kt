package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dslLogger
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.ui.Box
import java.io.BufferedReader
import java.io.InputStreamReader

object DslEntryLoader {
    fun initClient() { load<DslEntryClient>().forEach { it.initializeClient() } }
    fun initGui() { load<DslEntryGui>() }
    fun initOverlay() { enabledOverlays += load<DslEntryOverlay>() }
    fun initServer() { load<DslEntryServer>().forEach { it.initializeServer() } }
    fun initCommon() { load<DslEntryCommon>().forEach { it.initialize() } }
    private val cache = mutableMapOf<Class<*>,DslEntryService>()
    val entries: Collection<DslEntryService> get() = cache.values
    internal var enabledOverlays = mutableListOf<DslEntryOverlay>()
    context(ctx: DslContext)
    fun overlays() = enabledOverlays.forEachWithId {
        Box {
            it.overlay()
        }
    }
    private inline fun <reified T: DslEntryService> load() = load(T::class.java)
    private fun <T: DslEntryService> load(serviceInterface: Class<T>, classLoader: ClassLoader = Thread.currentThread().contextClassLoader): List<T> {
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
                                result += cache.getOrPut(clazz) { getObject(clazz,serviceInterface) } as T
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