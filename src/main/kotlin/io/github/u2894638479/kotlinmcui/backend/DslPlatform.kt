package io.github.u2894638479.kotlinmcui.backend

/**
 * The process-wide facts and helpers of the platform that published itself.
 *
 * Early form of the draft's `DslPlatform`: it starts with the two things the framework asks for today
 * - the metadata and translation - and grows in place (`utils`, `dispatcher`, ...) rather than in a
 * second type that would have to be reconciled with it later.
 *
 * It is **derived** from the published backend rather than implemented by it, so a backend gains
 * nothing new to implement: what it already provides is what the platform answers with.
 */
interface DslPlatform {
    /** Metadata of the published platform. */
    val metadata: DslBackendMetadata

    /** Translate a key for the current language, or `null` when the key is unknown. */
    fun translate(key: String, vararg args: Any): String?
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
    override fun translate(key: String, vararg args: Any) = backend().translate(key, *args)
}
