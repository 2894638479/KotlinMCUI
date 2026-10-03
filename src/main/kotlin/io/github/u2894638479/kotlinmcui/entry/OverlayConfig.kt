package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.dslMetadata
import io.github.u2894638479.kotlinmcui.config.configFormat
import io.github.u2894638479.kotlinmcui.config.loadConfig
import io.github.u2894638479.kotlinmcui.config.saveConfig
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * The enabled overlays as they are stored on disk: an ordered list of
 * [DslEntryService.id]s, the one in front being the topmost layer.
 *
 * Ids of overlays whose mod is not installed right now are kept, so removing a mod
 * for a while does not forget that its overlay was switched on.
 */
@Serializable
data class OverlayConfig(val enabled: List<String> = emptyList())

/** Where the framework keeps the overlay configuration: `<configDir>/kotlinmcui/overlays.json`. */
internal fun overlayConfigPath(): Path = dslMetadata.configDir.resolve("kotlinmcui/overlays.json")

internal fun readOverlayIds(path: Path = overlayConfigPath()): List<String> =
    loadConfig(path,configFormat) { OverlayConfig() }.enabled

internal fun writeOverlayIds(ids: List<String>, path: Path = overlayConfigPath()) =
    saveConfig(path,OverlayConfig(ids),configFormat)
