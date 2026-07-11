package io.github.u2894638479.kotlinmcui.backend

interface DslBackend<RP,SC>:
    DslBackendRenderer<RP>,
    DslBackendScreenFactory<SC>,
    DslBackendUtils,
    DslBackendMetadata,
    DslBackendInput
