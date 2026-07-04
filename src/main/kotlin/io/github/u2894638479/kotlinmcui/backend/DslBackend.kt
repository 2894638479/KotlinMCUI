package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.functions.DslFunction

interface DslBackend<RP,SC>:
    DslBackendRenderer<RP>,
    DslBackendScreenFactory<SC>,
    DslBackendUtils,
    DslBackendMetadata


fun <RP,SC> DslBackend<RP,SC>.createScreen(title: String = "DSL Screen", dslFunction: DslFunction) = create(title,dslFunction)
fun <RP,SC> DslBackend<RP,SC>.showScreen(title: String = "DSL Screen", dslFunction: DslFunction) = createScreen(title,dslFunction).show()