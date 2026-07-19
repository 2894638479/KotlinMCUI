package io.github.u2894638479.kotlinmcui.backend

import java.nio.file.Path

/**
 * Read-only metadata about the backend's runtime environment.
 */
interface DslBackendMetadata {
    /** The configuration directory (e.g. `.minecraft/config`). */
    val configDir: Path

    /** The game installation directory (e.g. `.minecraft`). */
    val gameDir: Path

    /** The game version string (e.g. `"1.21.4"`). */
    val gameVersion: String

    /** The mod loader identifier (e.g. `"forge"`, `"fabric"`, `"neoforge"`). */
    val gameLoader: String

    /** The current runtime environment (client, server, or common). */
    val environment: Environment
}
