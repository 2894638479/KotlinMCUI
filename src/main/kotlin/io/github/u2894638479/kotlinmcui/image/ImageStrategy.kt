package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Alignment
import io.github.u2894638479.kotlinmcui.math.px

fun interface ImageStrategy {
    fun uvRect(rect: Rect, image: ImageHolder): Rect

    context(backend: DslBackendRenderer<RP>, renderParam: RP)
    fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
        backend.renderImage(image,rect,uvRect(rect,image),color)
    }

    companion object {
        val stretch = ImageStrategy { _,image -> Rect(right = image.width, bottom = image.height) }

        val clip = ImageStrategy { rect,image ->
            val ratio = rect.width / rect.height
            val imageRatio = image.width / image.height
            if(!ratio.isFinite() || !imageRatio.isFinite() || imageRatio == ratio) return@ImageStrategy stretch.uvRect(rect,image)
            if(ratio > imageRatio) {
                val uvHeight = image.width / ratio
                Rect(0.px, (image.height - uvHeight) / 2, image.width, (image.height + uvHeight) / 2)
            } else {
                val uvWidth = image.height * ratio
                Rect((image.width - uvWidth) / 2, 0.px, (image.width + uvWidth) / 2, image.height)
            }
        }

        fun repeat(align: Alignment = Alignment(), scale: Double = 1.0) = ImageStrategy { rect, image ->
            val uWidth = rect.width / scale
            val u0 = when(align.horizontal) {
                Align.LOW -> 0.px
                Align.HIGH -> -uWidth
                else -> - uWidth / 2
            }
            val vHeight = rect.height / scale
            val v0 = when(align.vertical) {
                Align.LOW -> 0.px
                Align.HIGH -> -vHeight
                else -> -vHeight / 2
            }
            Rect(u0, v0, u0 + uWidth, v0 + vHeight)
        }

        fun fitIn(alignment: Alignment, fillColor: Color = Color.TRANSPARENT_WHITE) = object : ImageStrategy {
            override fun uvRect(rect: Rect, image: ImageHolder) = Rect()
            context(backend: DslBackendRenderer<RP>, renderParam: RP)
            override fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
                backend.fillRect(rect,fillColor)
                val ratio = rect.width / rect.height
                val imageRatio = image.width / image.height
                if(!ratio.isFinite() || !imageRatio.isFinite()) return
                val rect = if(ratio > imageRatio) {
                    val width = rect.height * imageRatio
                    val left = when(alignment.horizontal) {
                        Align.LOW -> rect.left
                        Align.HIGH -> rect.right - width
                        else -> (rect.right + rect.left - width) / 2
                    }
                    Rect(left, rect.top, left + width, rect.bottom)
                } else {
                    val height = rect.width / imageRatio
                    val top = when(alignment.vertical) {
                        Align.LOW -> rect.top
                        Align.HIGH -> rect.bottom - height
                        else -> (rect.top + rect.bottom - height) / 2
                    }
                    Rect(rect.left, top, rect.right, top + height)
                }
                backend.renderImage(image,rect, Rect(right = image.width, bottom = image.height),color)
            }
        }
    }
}