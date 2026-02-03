package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.backGround
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.weight

context(ctx: DslContext)
fun TestLayoutPage() = Row {
    Column {
        TextFlatten(Modifier.weight(0.0)) { "weight:0.0".emit() }.backGround(Color(100, 100, 100))
        TextFlatten(Modifier.weight(1.0)) { "weight:1.0".emit() }.backGround(Color.RED)
        TextFlatten(Modifier.weight(2.0)) { "weight:2.0".emit() }.backGround(Color.GREEN)
        TextFlatten(Modifier.weight(3.0)) { "weight:3.0".emit() }.backGround(Color.BLUE)
    }
    Column {
        Row {
            TextFlatten(Modifier.weight(0.0)) { "weight:0.0".emit() }.backGround(Color(100, 100, 100))
            TextFlatten(Modifier.weight(1.0)) { "weight:1.0".emit() }.backGround(Color.RED)
            TextFlatten(Modifier.weight(2.0)) { "weight:2.0".emit() }.backGround(Color.GREEN)
            TextFlatten(Modifier.weight(3.0)) { "weight:3.0".emit() }.backGround(Color.BLUE)
        }
        TextFlatten(Modifier.padding(10.scaled)) { "padding:10.scaled".emit() }.backGround(Color(100, 100, 100))
        TextFlatten(Modifier.padding(20.scaled)) { "padding:20.scaled".emit() }.backGround(Color(100, 100, 100))
        TextFlatten(Modifier.padding(10.px)) { "padding:10.px".emit() }.backGround(Color(100, 100, 100))
        TextFlatten(Modifier.padding(20.px)) { "padding:20.px".emit() }.backGround(Color(100, 100, 100))
    }
    Column {
        TextFlatten { "weight:1.0".emit() }.backGround(Color.RED)
        TextFlatten(Modifier.weight(Double.MAX_VALUE)) { "weight:Double.MAX_VALUE".emit() }.backGround(Color.GREEN)
    }
}