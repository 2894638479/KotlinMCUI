package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position

/**
 * The per-frame performance and state readings a screen's debug overlay shows.
 *
 * This is the contract only: how the readings are drawn is a demo concern and lives with the
 * demo overlay in the `test` package.
 */
interface DebugOverlay {
    val screenTitle: String
    val guiScale: Double
    val localProps: Int
    val stableProps: Int
    val staticProps: Int
    val frameRate: Double
    val frameTimeMs: Double
    val frameRateSmooth: Double
    val frameTimeMsSmooth: Double
    val frameRenderTimeMs: Double
    val dslRenderTimeMs: Double
    val backendRenderTimeMs: Double
    val components: Int
    val conflictId: DslId?
    val focusedId: DslId?
    val hoveredId: DslId?
    val focusedDepth: Int
    val hoveredDepth: Int
    val mouse: Position
}
