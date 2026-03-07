package io.github.u2894638479.kotlinmcui.backend

import java.nio.file.Path

interface DslBackendMetadata {
    val configDir: Path
    val gameDir: Path
    val gemaVersion: String
    val gameLoader: String
}