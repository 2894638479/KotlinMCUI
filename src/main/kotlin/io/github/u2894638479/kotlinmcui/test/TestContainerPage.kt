package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.containerBackground
import io.github.u2894638479.kotlinmcui.functions.decorator.hoverMask
import io.github.u2894638479.kotlinmcui.functions.decorator.slotBackground
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value

context(ctx: DslContext)
fun TestContainerPage() = ScrollableColumn {
    class ItemInfo(val id:String,val count: Int,val damage: Double? = null,val enchanted:Boolean = false)
    var mouseHold by remember<ItemInfo?>(null)
    MouseTip {
        mouseHold?.run {
            Item(Modifier.size(16.scaled,16.scaled),id,count,damage,enchanted) {}
        }
    }
    Row {
        val size by 10.remember.property
        SliderHorizontal(Modifier.height(20.scaled),0..100,size) {
            TextFlatten { "slot size:${size.value}".emit() }
        }
        Spacer(Modifier.size(size.value.scaled,size.value.scaled)) {}.slotBackground()
    }
    Row {
        val width by 80.remember.property
        val height by 50.remember.property
        Column {
            SliderHorizontal(Modifier.height(20.scaled),0..200,width) {
                TextFlatten { "container width:${width.value}".emit() }
            }
            SliderHorizontal(Modifier.height(20.scaled),0..100,height) {
                TextFlatten { "container height:${height.value}".emit() }
            }
        }
        Spacer(Modifier.size(width.value.scaled,height.value.scaled)) {}.containerBackground()
    }
    val items by remember {
        Array<ItemInfo?>(4 * 9) { null }.also {
            it[3] = ItemInfo("placeholder",3,0.5,true)
            it[13] = ItemInfo("minecraft:grass_block",35,0.4,true)
            it[20] = ItemInfo("minecraft:redstone",8,0.4,true)
            it[22] = ItemInfo("minecraft:diamond_axe",1,0.2,true)
        }
    }

    Column(Modifier.width(Measure.AUTO_MIN)) {
        val text by "text".remember.property
        EditableText(Modifier.padding(4.scaled),text) {}.slotBackground(2.scaled)
        (0..<4).forEachWithId { row ->
            Row {
                (0..<9).forEachWithId { col ->
                    val index = row*9+col
                    items[index].run {
                        if(this == null) Spacer(Modifier.size(18.scaled,18.scaled)) {}.slotBackground()
                        else Item(Modifier.size(16.scaled,16.scaled),id,count,damage,enchanted) {}.slotBackground(1.scaled)
                    }.clickable {
                        val item = mouseHold
                        mouseHold = items[index]
                        items[index] = item
                    }.hoverMask()
                }
            }
            if(row == 2) Spacer(Modifier.height(4.scaled)) {}
        }
    }.containerBackground(7.scaled)
}