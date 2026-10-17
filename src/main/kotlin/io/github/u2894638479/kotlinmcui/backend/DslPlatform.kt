package io.github.u2894638479.kotlinmcui.backend

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The process-wide facts and helpers of the platform that published itself.
 *
 * Early form of the draft's `DslPlatform`; it grows in place from here - the draft's members appear at
 * their own names rather than in a second type that would have to be reconciled with it later.
 *
 * A platform is a first-class object the backend builds and publishes; it is not derived from the
 * aggregate backend any more, which is what lets the core stop depending on that aggregate.
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
