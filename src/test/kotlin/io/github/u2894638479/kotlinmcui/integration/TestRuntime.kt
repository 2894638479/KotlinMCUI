package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatformUtils
import io.github.u2894638479.kotlinmcui.backend.DslRuntime
import io.github.u2894638479.kotlinmcui.backend.dslMetadataProvider
import io.github.u2894638479.kotlinmcui.backend.dslUtilsProvider

/**
 * Installs and publishes the fake platform the way a real backend does.
 *
 * [DslRuntime.install] is deliberately single-shot, so this is a JVM-wide setup: the first case that
 * asks for it does the install and the publish, every later one only makes sure the metadata and the
 * utils provider are there. `DslRuntimeTest` owns the sequence itself and must therefore run before
 * anything that ends up here.
 */
internal object TestRuntime {

    /** Install the fake metadata and publish the fake utils. Idempotent. */
    fun install() {
        if (!DslRuntime.isInstalled) DslRuntime.install { TestMetadata }
        // A case may hide the metadata provider without being able to install again; put it back.
        else if (dslMetadataProvider == null) dslMetadataProvider = { TestMetadata }
        if (dslUtilsProvider == null) DslRuntime.start { TestPlatform }
    }

    /**
     * Publish the fake utils and return whatever was published before, so the caller can restore
     * it: the singleton is process-wide and the cases share one JVM.
     */
    fun publish(): (() -> DslPlatformUtils)? {
        if (!DslRuntime.isInstalled) DslRuntime.install { TestMetadata }
        val previous = dslUtilsProvider
        dslUtilsProvider = { TestPlatform }
        return previous
    }

    /** Put back whatever [publish] handed out. */
    fun restore(previous: (() -> DslPlatformUtils)?) { dslUtilsProvider = previous }
}
