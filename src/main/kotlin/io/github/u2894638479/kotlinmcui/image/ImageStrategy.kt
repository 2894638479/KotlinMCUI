package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Align.*
import io.github.u2894638479.kotlinmcui.math.align.Alignment
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.math.min

interface ImageStrategy {
    context(backend: DslBackendRenderer<RP>, renderParam: RP)
    fun <RP> render(rect: Rect, image: ImageHolder, color: Color)

    fun interface UVStrategy: ImageStrategy {
        fun uvRect(rect: Rect, image: ImageHolder): Rect
        context(backend: DslBackendRenderer<RP>, renderParam: RP)
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

        fun repeatUV(uv: Rect,scale: Double) = object : ImageStrategy {
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
            context(backend: DslBackendRenderer<RP>, renderParam: RP)
            override fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
                val renderW = uv.width * scale
                val renderH = uv.height * scale
                iter(rect.left,rect.right,renderW) { l, r, bl1 ->
                    iter(rect.top,rect.bottom,renderH) { t, b, bl2 ->
                        val w = if(bl1) uv.width else uv.width * ((r-l) / renderW)
                        val h = if(bl2) uv.height else uv.height * ((b-t) / renderH)
                        backend.renderImage(image,Rect(l,t,r,b),Rect(uv.left,uv.top,uv.left + w,uv.top + h),color)
                    }
                }
            }
        }

        fun nineSlice(uvOuter:Rect,uvInner:Rect,scale:Double) = object:ImageStrategy {
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
            context(backend: DslBackendRenderer<RP>, renderParam: RP)
            override fun <RP> render(rect: Rect, image: ImageHolder, color: Color) {
                if(rect.isEmpty) return
                val scale = min(
                    min(scale,rect.width / (uvOuter.width - uvInner.width)),
                    rect.height / (uvOuter.height - uvInner.height)
                )
                val w1 = (uvInner.left - uvOuter.left) * scale
                val w2 = (uvOuter.right - uvInner.right) * scale
                val h1 = (uvInner.top - uvOuter.top) * scale
                val h2 = (uvOuter.bottom - uvInner.bottom) * scale
                val rectInner = Rect(rect.left + w1,rect.top + h1,rect.right - w2,rect.bottom - h2)
                for(aw in Align.entries) {
                    for(ah in Align.entries) {
                        getW(uvInner,uvOuter,aw) { u1,u2 ->
                            getH(uvInner,uvOuter,ah) { v1,v2 ->
                                val uv = Rect(u1,v1,u2,v2)
                                getW(rectInner,rect,aw) { x1,x2 ->
                                    getH(rectInner,rect,ah) { y1,y2 ->
                                        val r = Rect(x1,y1,x2,y2)
                                        repeatUV(uv,scale).render(r,image,color)
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
            context(backend: DslBackendRenderer<RP>, renderParam: RP)
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