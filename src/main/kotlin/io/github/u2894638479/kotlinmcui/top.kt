package io.github.u2894638479.kotlinmcui

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import org.slf4j.LoggerFactory


var dslBackendProvider: (()->DslBackend<*, *>)? = null

val dslBackend get() = dslBackendProvider?.invoke() ?: throw dslNoBackendError()

fun dslNoBackendError() = IllegalStateException("Dsl failed to find a backend. you should install a mod to provide it. or you are trying to use dsl before initialization.")

val dslLogger = LoggerFactory.getLogger("dsl")


@RequiresOptIn(
    message = "This backend is not stable, users may install other backends at runtime, causing a crash. \n use dslBackend instead",
    level = RequiresOptIn.Level.WARNING
)
annotation class InternalBackend
