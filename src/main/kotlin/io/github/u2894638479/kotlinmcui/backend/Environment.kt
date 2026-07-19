package io.github.u2894638479.kotlinmcui.backend

/**
 * The runtime environment in which the framework is executing.
 *
 * Determines which entry points are safe to load without causing
 * [ClassNotFoundException] or similar linkage errors.
 */
enum class Environment {
    /**
     * Client-specific code is available.
     * [io.github.u2894638479.kotlinmcui.entry.DslEntryClient.initializeClient] will be called.
     */
    CLIENT,

    /**
     * Server-specific code is available.
     * [io.github.u2894638479.kotlinmcui.entry.DslEntryServer.initializeServer] will be called.
     */
    SERVER,

    /**
     * Only common code is guaranteed to be available.
     * Only [io.github.u2894638479.kotlinmcui.entry.DslEntryCommon.initialize] will be called.
     */
    COMMON
}
