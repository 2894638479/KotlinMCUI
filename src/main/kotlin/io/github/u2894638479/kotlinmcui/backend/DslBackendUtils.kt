package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File

interface DslBackendUtils {
    var clipBoard: String
    fun narrate(string: String)
    fun translate(key: String,vararg args: Any?): String?
    val isInWorld: Boolean
    fun loadLocalImage(file: File): ImageHolder
    fun forceLoadLocalImage(file: File): ImageHolder
    fun playButtonSound()
    fun openUri(uri: String)
    val mainDispatcher: CoroutineDispatcher
}