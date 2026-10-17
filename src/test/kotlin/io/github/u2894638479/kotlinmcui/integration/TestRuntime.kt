package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.backend.DslRuntime
import io.github.u2894638479.kotlinmcui.backend.dslPlatformProvider

/**
 * Installs and publishes the fake platform the way a real backend does.
 *
 * [DslRuntime.install] is deliberately single-shot, so this is a JVM-wide setup: the first case that
 * asks for it does the install and the publish, every later one only makes sure the platform and its
 * provider are there. `DslRuntimeTest` owns the sequence itself and must therefore run before
 * anything that ends up here.
 */
internal object TestRuntime {

    /** Install and publish the fake platform. Idempotent. */
    fun install() {
        if (!DslRuntime.isInstalled) DslRuntime.install { TestMetadata }
        if (dslPlatformProvider == null) DslRuntime.start { TestPlatform }
    }

    fun publish(): (() -> DslPlatform)? {
        if (!DslRuntime.isInstalled) DslRuntime.install { TestMetadata }
            val previous = dslPlatformProvider
        dslPlatformProvider = { TestPlatform }
        return previous
    }

    /** Put back whatever [publish] handed out. */
    fun restore(previous: (() -> DslPlatform)?) { dslPlatformProvider = previous }
}
