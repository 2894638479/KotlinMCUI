package io.github.u2894638479.kotlinmcui.backend

/**
 * The runtime environment in which the framework is executing.
 *
 * Determines which entry points are safe to load without causing
 * [ClassNotFoundException] or similar linkage errors.
 */
enum class Environment {
    /**
     * A game client is available.
     * [io.github.u2894638479.kotlinmcui.entry.DslEntryClient.initializeClient] will be called.
     */
    CLIENT,

    /**
     * A game server is available.
     * [io.github.u2894638479.kotlinmcui.entry.DslEntryServer.initializeServer] will be called.
     */
    SERVER,

    /**
     * No game side at all: neither the client nor the server entry points are initialised - only
     * common, gui and overlay run. This is the standalone shell and the tools that drive the framework
     * without a game.
     *
     * The matching entry point is
     * [io.github.u2894638479.kotlinmcui.entry.DslEntryCommon] - "common" says the code depends on no
     * particular side, which is exactly what this environment is. There is deliberately no
     * DslEntryNone.
     */
    NONE
}