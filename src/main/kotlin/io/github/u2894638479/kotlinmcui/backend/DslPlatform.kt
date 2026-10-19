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
 * The published platform. Fails loudly when nothing has published one yet.
 */
val dslPlatform: DslPlatform get() = publishedPlatform()

/**
 * Metadata of the current platform.
 *
 * An installed metadata provider answers before the platform is published, so mod initialisation can
 * read directories without building anything; afterwards the platform's own metadata is the answer.
 */
val dslMetadata: DslPlatformMetadata
    get() = dslMetadataProvider?.invoke() ?: dslPlatform.metadata
