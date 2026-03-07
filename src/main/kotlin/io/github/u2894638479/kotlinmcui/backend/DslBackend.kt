package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.functions.DslTopFunction

interface DslBackend<RP,SC>:
    DslBackendRenderer<RP>,
    DslBackendScreenFactory<SC>,
    DslBackendUtils,
    DslBackendMetadata


fun <RP,SC> DslBackend<RP,SC>.createScreen(title: String = "DSL Screen", dslFunction: DslTopFunction) = create(title,dslFunction)
fun <RP,SC> DslBackend<RP,SC>.showScreen(title: String = "DSL Screen", dslFunction: DslTopFunction) = createScreen(title,dslFunction).show()