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

    /** Install, publish the fake platform and register its provider. Idempotent. */
    fun install() {
        if (!DslRuntime.isInstalled) DslRuntime.install { TestMetadata }
        if (dslPlatformProvider == null) DslRuntime.platformReady { TestPlatform }
        if (DslRuntime.registeredProviders.isEmpty()) DslRuntime.registerProvider(TestRootProvider)
    }

    /**
     * Publish the fake platform for the duration of a case, and return whatever was published before
     * so [restore] can put it back.
     *
     * This is the only place the cases publish a platform, which is why the signature flip touched
     * this object and not the 26 cases.
     */
    fun publish(): (() -> DslPlatform)? {
        if (!DslRuntime.isInstalled) DslRuntime.install { TestMetadata }
        if (DslRuntime.registeredProviders.isEmpty()) DslRuntime.registerProvider(TestRootProvider)
        val previous = dslPlatformProvider
        dslPlatformProvider = { TestPlatform }
        return previous
    }

    /** Put back whatever [publish] handed out. */
    fun restore(previous: (() -> DslPlatform)?) { dslPlatformProvider = previous }
}
