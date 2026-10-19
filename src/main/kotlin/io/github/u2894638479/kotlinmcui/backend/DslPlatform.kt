package io.github.u2894638479.kotlinmcui.backend

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The process-wide facts and helpers of the platform a backend publishes.
 *
 * A platform is a first-class object the backend builds and publishes: the process-level facts and
 * helpers, plus the dispatcher. The core depends on this type rather than on a backend aggregate.
 */
interface DslPlatform {
    /** Metadata of the published platform. */
    val metadata: DslPlatformMetadata

    /** The process-level helpers: translation, world state, images, exit. */
    val utils: DslPlatformUtils

    /** The process-lifetime serial dispatcher. */
    val dispatcher: CoroutineDispatcher
}

/**
 * The platform in force: the published one, or the placeholder that answers while none is published.
 *
 * The placeholder makes the benign queries usable during entry initialisation; only [DslPlatform.dispatcher]
 * fails there, because a session thread cannot be faked.
 */
val dslPlatform: DslPlatform get() = currentPlatform()

/**
 * Metadata of the current platform.
 *
 * An installed metadata provider answers before the platform is published, so mod initialisation can
 * read directories without building anything.
 */
val dslMetadata: DslPlatformMetadata
    get() = dslMetadataProvider?.invoke() ?: dslPlatform.metadata
