package io.github.u2894638479.kotlinmcui.integration

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
        if (!DslRuntime.isInstalled) DslRuntime.install({ TestMetadata })
        if (dslBackendProvider == null) DslRuntime.platformReady({ TestBackend })
    }
}
