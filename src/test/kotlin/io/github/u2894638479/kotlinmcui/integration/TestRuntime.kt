package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.DslRuntime
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider

/**
 * Installs the fake platform the way a real backend does.
 *
 * [DslRuntime.install] is deliberately single-shot, so this is a JVM-wide setup: the
 * first case that asks for it does the install, every later one only makes sure the
 * backend is published. `DslRuntimeTest` owns the sequence itself and must therefore
 * run before anything that ends up here.
 */
internal object TestRuntime {

    fun install() {
        if (!DslRuntime.isInstalled) DslRuntime.install { TestMetadata }
        if (dslBackendProvider == null) DslRuntime.platformReady { TestBackend }
    }

    /**
     * Publish the fixture backend for the duration of a case, and return whatever was published before
     * so [restore] can put it back.
     *
     * This is the only place the cases publish a backend. When `platformReady` starts taking a
     * platform instead of the aggregate backend, this object is what changes - not the 26 cases.
     */
    fun publish(): (() -> DslBackend<*, *>)? {
        val previous = dslBackendProvider
        dslBackendProvider = { TestBackend }
        return previous
    }

    /** Put back whatever [publish] handed out. */
    fun restore(previous: (() -> DslBackend<*, *>)?) { dslBackendProvider = previous }
}
