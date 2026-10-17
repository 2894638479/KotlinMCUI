package io.github.u2894638479.kotlinmcui.backend

import kotlinx.coroutines.CoroutineDispatcher
import java.io.File

/**
 * The process-wide facts and helpers of the platform that published itself.
 *
 * Early form of the draft's `DslPlatform`; it grows in place from here - the draft's members appear at
 * their own names rather than in a second type that would have to be reconciled with it later.
 *
 * It is **derived** from the published backend rather than implemented by it, so a backend gains
 * nothing new to implement: what it already provides is what the platform answers with.
 */
interface DslPlatform {
    /** Metadata of the published platform. */
    val metadata: DslBackendMetadata

    /** The process-level helpers: translation, world state, images, exit. */
    val utils: DslPlatformUtils

    /** The process-lifetime serial dispatcher. */
    val dispatcher: CoroutineDispatcher
}

/**
 * The published platform.
 *
 * Derived on each read from whichever backend the runtime published, so a platform that is replaced
 * (only the first `platformReady` wins today, but the tests swap providers freely) is reflected
 * immediately. The backend is taken lazily: metadata alone must keep working for a platform that was
 * installed with a metadata provider and cannot build its backend yet.
 */
val dslPlatform: DslPlatform get() = platformOf { dslBackend }

/**
 * Metadata of the current platform.
 *
 * Falls back to the backend when the platform was installed without a separate metadata provider, in
 * which case reading metadata does build the backend.
 */
val dslMetadata: DslBackendMetadata get() = dslPlatform.metadata

private fun platformOf(backend: () -> DslBackend<*, *>) = object : DslPlatform {
    override val metadata get() = dslMetadataProvider?.invoke() ?: backend()
    override val utils: DslPlatformUtils get() = platformUtilsOf(backend())
    override val dispatcher get() = backend().mainDispatcher
}

/**
 * The transitional platform helpers: the aggregate backend's utils, seen as [DslPlatformUtils].
 *
 * An adapter rather than the backend itself, because the backend still implements the aggregate
 * [DslBackendUtils]. It goes away with the split, when a backend implements the two slices directly.
 */
private fun platformUtilsOf(backend: DslBackendUtils): DslPlatformUtils = object : DslPlatformUtils {
    override fun translate(key: String, vararg args: Any?) = backend.translate(key, *args)
    override val isInWorld get() = backend.isInWorld
    override fun loadLocalImage(file: File) = backend.loadLocalImage(file)
    override fun forceLoadLocalImage(file: File) = backend.forceLoadLocalImage(file)
    override fun quit(exitCode: Int) = backend.quit(exitCode)
}
