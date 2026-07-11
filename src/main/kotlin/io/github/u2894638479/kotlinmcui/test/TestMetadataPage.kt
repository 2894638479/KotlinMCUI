package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.ui.Row
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.TextAutoFold
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size

context(ctx: DslContext)
fun TestMetadataPage() = ScrollableColumn {
    context(ctx:DslContext)
    fun item(pair: Pair<String,String>) = Row(Modifier.padding(5.scaled)) {
        TextFlatten(Modifier.size(Measure.AUTO_MIN,Measure.AUTO_MIN).padding(10.scaled)) {
            pair.first.emit(Color.GREEN)
        }
        TextAutoFold {
            pair.second.emit()
        }
    }
    val backend = ctx.dataStore.backend
    item("gameVersion" to backend.gameVersion)
    item("gameLoader" to backend.gameLoader)
    item("gameDir" to backend.gameDir.toString())
    item("configDir" to backend.configDir.toString())
}