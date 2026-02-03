package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

data class ImageHolder(
    val id: String,
    val width: Measure,
    val height: Measure
) {
    val isEmpty get() = this === empty
    companion object {
        val empty = ImageHolder("",0.px,0.px)
    }
}