package io.github.u2894638479.kotlinmcui.backend

/**
 * The runtime environment in which the framework is executing.
 *
 * It says which game side exists in this process - client classes, server classes, or neither - and it is
 * what the entry registry is filtered by. It does **not** mean "load no entries": a standalone process
 * still loads every entry that declared standalone.
 */
enum class Environment {
    /** A game client exists in this process. */
    CLIENT,

    /** A game server exists in this process. */
    SERVER,

    /**
     * No game side at all: neither client nor server classes are available. The standalone shell and the
     * tools driving the framework without a game run here.
     */
    STANDALONE
}
