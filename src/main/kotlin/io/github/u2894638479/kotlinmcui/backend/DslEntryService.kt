package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import java.util.ServiceLoader

interface DslEntryService {
    val name: String
    val id: String
    val icon: ImageHolder
    fun initialize()
    fun createScreen(): DslBackendScreenHolder<*>? = null
    companion object {
        var services = listOf<DslEntryService>()
            private set
        fun loadServices() {
            try {
                services = ServiceLoader.load(
                    DslEntryService::class.java,
                    DslEntryService::class.java.classLoader
                ).toList()
            } catch (e: Throwable) {
                throw Throwable("load DslEntryService failed. must has a no-argument constructor",e)
            }
        }
    }
}