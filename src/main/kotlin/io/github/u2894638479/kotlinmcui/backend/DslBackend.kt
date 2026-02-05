package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.functions.DslFunction

interface DslBackend<RP,SC>:
    DslBackendRenderer<RP>,
    DslBackendScreenFactory<SC>,
    DslBackendUtils


fun <RP,SC> DslBackend<RP,SC>.createScreen(dslFunction: DslFunction) = create(dslFunction)
fun <RP,SC> DslBackend<RP,SC>.showScreen(dslFunction: DslFunction) = createScreen(dslFunction).show()