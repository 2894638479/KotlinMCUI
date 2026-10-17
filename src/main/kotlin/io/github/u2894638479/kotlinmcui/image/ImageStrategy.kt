package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.backend.fillRect
import io.github.u2894638479.kotlinmcui.backend.renderImage
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Align.*
import io.github.u2894638479.kotlinmcui.math.align.Alignment
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.*
import kotlin.math.min

/**
 * Defines how an image is rendered into a target rectangle.
 *
 * Pre-built strategies are available in the [companion][Companion]:
 * - [stretch] — fills the rect, ignoring aspect ratio
 * - [clip] — fills while preserving aspect ratio, cropping excess
 * - [repeat] — tiles the image (optionally scaled)
 * - [nineSlice] — classic 9-slice scaling for panels and borders
 * - [fitIn] — letterboxes the image inside the rect
 *
 * Custom strategies can implement [UVStrategy] or the raw [render] method.
 */
interface ImageStrategy {
    context(backend: DslRootRenderer<RP>, renderParam: RP)
    fun <RP> render(rect: Rect, image: ImageHolder, color: Color)

    fun interface UVStrategy: ImageStrategy {
        fun uvRect(rect: Rect, image: ImageHolder): Rect
        context(backend: DslRootRenderer<RP>, renderParam: RP)
        override fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
            backend.renderImage(image,rect,uvRect(rect,image),color)
        }
    }

    companion object {
        val stretch = UVStrategy { _,image -> Rect(right = image.width, bottom = image.height) }

        val clip = UVStrategy { rect,image ->
            val ratio = rect.width / rect.height
            val imageRatio = image.width / image.height
            if(!ratio.isFinite() || !imageRatio.isFinite() || imageRatio == ratio) return@UVStrategy stretch.uvRect(rect,image)
            if(ratio > imageRatio) {
                val uvHeight = image.width / ratio
                Rect(0.px, (image.height - uvHeight) / 2, image.width, (image.height + uvHeight) / 2)
            } else {
                val uvWidth = image.height * ratio
                Rect((image.width - uvWidth) / 2, 0.px, (image.width + uvWidth) / 2, image.height)
            }
        }

        fun repeat(align: Alignment = Alignment(), scale: Double = 1.0) = UVStrategy { rect, image ->
            val uWidth = rect.width / scale
            val u0 = when(align.horizontal) {
                LOW -> 0.px
                HIGH -> -uWidth
                else -> - uWidth / 2
            }
            val vHeight = rect.height / scale
            val v0 = when(align.vertical) {
                LOW -> 0.px
                HIGH -> -vHeight
                else -> -vHeight / 2
            }
            Rect(u0, v0, u0 + uWidth, v0 + vHeight)
        }

        /** Walk from [a] to [b] in [step]-sized pieces, reporting whether a piece is full. */
        private inline fun iter(a: Measure, b: Measure, step: Measure, action:(Measure, Measure,Boolean)->Unit) {
            require(step > 0.px)
            if(!a.isNumber || !b.isNumber) return
            if(b <= a) return
            var current = a
            while (true) {
                val new = current + step
                if(new == b) {
                    action(current,b,true)
                    break
                } else if(new > b) {
                    action(current,b,false)
                    break
                } else {
                    action(current,new,true)
                    current = new
                }
            }
        }

        /**
         * Tile [uv] (scaled by [scale]) across [rect], cropping the last row and column.
         *
         * [tile] and [source] are the caller's scratch views: a tile's geometry is written
         * into them and consumed by the draw call, so tiling allocates nothing per tile.
         */
        context(backend: DslRootRenderer<RP>, renderParam: RP)
        private fun <RP> drawTiled(
            rect: Rect, uv: Rect, scale: Double, image: ImageHolder, color: Color,
            tile: MutRect, source: MutRect,
        ) {
            val left = rect.left; val right = rect.right
            val top = rect.top; val bottom = rect.bottom
            val uvLeft = uv.left; val uvTop = uv.top
            val uvWidth = uv.width; val uvHeight = uv.height
            val renderW = uvWidth * scale
            val renderH = uvHeight * scale
            iter(left,right,renderW) { l, r, bl1 ->
                iter(top,bottom,renderH) { t, b, bl2 ->
                    val w = if(bl1) uvWidth else uvWidth * ((r-l) / renderW)
                    val h = if(bl2) uvHeight else uvHeight * ((b-t) / renderH)
                    tile.left = l; tile.top = t; tile.right = r; tile.bottom = b
                    source.left = uvLeft; source.top = uvTop
                    source.right = uvLeft + w; source.bottom = uvTop + h
                    backend.renderImage(image, tile, source, color)
                }
            }
        }

        fun repeatUV(uv: Rect,scale: Double) = object : ImageStrategy {
            private val tile = MutRect()
            private val source = MutRect()

            context(backend: DslRootRenderer<RP>, renderParam: RP)
            override fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
                drawTiled(rect, uv, scale, image, color, tile, source)
            }
        }

        fun nineSlice(uvOuter:Rect,uvInner:Rect,scale:Double) = object:ImageStrategy {
            private val inner = MutRect()
            private val patch = MutRect()
            private val tile = MutRect()
            private val source = MutRect()
            private val tileSource = MutRect()

            private inline fun getW(inner:Rect,outer:Rect,align: Align,action:(Measure, Measure)->Unit) = when(align) {
                LOW -> action(outer.left,inner.left)
                MID -> action(inner.left,inner.right)
                HIGH -> action(inner.right,outer.right)
            }
            private inline fun getH(inner:Rect,outer:Rect,align: Align,action:(Measure, Measure)->Unit) = when(align) {
                LOW -> action(outer.top,inner.top)
                MID -> action(inner.top,inner.bottom)
                HIGH -> action(inner.bottom,outer.bottom)
            }
            context(backend: DslRootRenderer<RP>, renderParam: RP)
            override fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
                rect.ifEmpty { return }
                if(rect.width < 1e-3.px || rect.height < 1e-3.px) return
                val scale = min(
                    min(scale,rect.width / (uvOuter.width - uvInner.width)),
                    rect.height / (uvOuter.height - uvInner.height)
                )
                val w1 = (uvInner.left - uvOuter.left) * scale
                val w2 = (uvOuter.right - uvInner.right) * scale
                val h1 = (uvInner.top - uvOuter.top) * scale
                val h2 = (uvOuter.bottom - uvInner.bottom) * scale
                inner.left = rect.left + w1; inner.top = rect.top + h1
                inner.right = rect.right - w2; inner.bottom = rect.bottom - h2
                for(aw in Align.entries) {
                    for(ah in Align.entries) {
                        getW(uvInner,uvOuter,aw) { u1,u2 ->
                            getH(uvInner,uvOuter,ah) { v1,v2 ->
                                source.left = u1; source.top = v1; source.right = u2; source.bottom = v2
                                getW(inner,rect,aw) { x1,x2 ->
                                    getH(inner,rect,ah) { y1,y2 ->
                                        patch.left = x1; patch.top = y1; patch.right = x2; patch.bottom = y2
                                        drawTiled(patch, source, scale, image, color, tile, tileSource)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun stretchUV(uv: Rect) = UVStrategy { _,_-> uv }

        fun fitIn(alignment: Alignment, fillColor: Color = Color.TRANSPARENT_WHITE) = object : ImageStrategy {
            context(backend: DslRootRenderer<RP>, renderParam: RP)
            override fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
                backend.fillRect(rect,fillColor)
                val ratio = rect.width / rect.height
                val imageRatio = image.width / image.height
                if(!ratio.isFinite() || !imageRatio.isFinite()) return
                val rect = if(ratio > imageRatio) {
                    val width = rect.height * imageRatio
                    val left = when(alignment.horizontal) {
                        LOW -> rect.left
                        HIGH -> rect.right - width
                        else -> (rect.right + rect.left - width) / 2
                    }
                    Rect(left, rect.top, left + width, rect.bottom)
                } else {
                    val height = rect.width / imageRatio
                    val top = when(alignment.vertical) {
                        LOW -> rect.top
                        HIGH -> rect.bottom - height
                        else -> (rect.top + rect.bottom - height) / 2
                    }
                    Rect(rect.left, top, rect.right, top + height)
                }
                backend.renderImage(image,rect, Rect(right = image.width, bottom = image.height),color)
            }
        }
    }
}