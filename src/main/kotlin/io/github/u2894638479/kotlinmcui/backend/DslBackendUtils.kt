package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File

/**
 * Miscellaneous platform utilities that the backend must provide.
 */
interface DslBackendUtils {
    /** The system clipboard content. Read and write for copy/paste support. */
    var clipboard: String

    /**
     * Speak the given string via accessibility / screen reader narration.
     * Called automatically when the focused or hovered component changes.
     */
    fun narrate(string: String)

    /**
     * Look up a translation key and return the localized string.
     * @param key The translation key (e.g. `"kotlinmcui.narration.button"`).
     * @param args Optional format arguments for the translation string.
     * @return The translated string, or `null` if no translation is found.
     */
    fun translate(key: String, vararg args: Any?): String?

    /** Whether the player is currently in a game world (as opposed to a menu). */
    val isInWorld: Boolean

    /**
     * Load an image from a local file asynchronously.
     * @return An [ImageHolder] that may be [ImageHolder.empty] if loading fails.
     */
    fun loadLocalImage(file: File): ImageHolder

    /**
     * Load an image from a local file synchronously (blocking).
     * Use sparingly; prefer [loadLocalImage] for most cases.
     * @return An [ImageHolder] that may be [ImageHolder.empty] if loading fails.
     */
    fun forceLoadLocalImage(file: File): ImageHolder

    /** Play the default UI button click sound. */
    fun playButtonSound()

    /** Open the given URI in the platform's default browser or handler. */
    fun openUri(uri: String)

    /** The main/UI thread dispatcher for launching coroutines. */
    val mainDispatcher: CoroutineDispatcher
}
