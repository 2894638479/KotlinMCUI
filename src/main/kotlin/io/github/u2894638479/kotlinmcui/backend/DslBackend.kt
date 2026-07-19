package io.github.u2894638479.kotlinmcui.backend

/**
 * The central backend interface that aggregates all backend capabilities.
 *
 * A backend implementation provides the bridge between the DSL framework and a
 * specific rendering platform (e.g. Minecraft GUI screens). Implement this
 * interface to create a new backend.
 *
 * @param RP The render parameter type — a platform-specific context object
 *           passed to all rendering operations (e.g. a MatrixStack or PoseStack).
 * @param SC The screen type — the platform-specific screen class that the
 *           backend creates and manages.
 */
interface DslBackend<RP, SC> :
    DslBackendRenderer<RP>,
    DslBackendScreenFactory<SC>,
    DslBackendUtils,
    DslBackendMetadata,
    DslBackendInput
