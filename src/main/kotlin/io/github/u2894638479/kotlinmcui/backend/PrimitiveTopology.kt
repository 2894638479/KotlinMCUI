package io.github.u2894638479.kotlinmcui.backend

/**
 * Vertex connection topology for [DslBackendRenderer.renderVertices].
 */
enum class PrimitiveTopology {
    /** Every 3 vertices form one independent triangle. */
    TRIANGLES,
    /** vertices[0] is the hub; each consecutive pair (i, i+1) forms a triangle. */
    TRIANGLE_FAN,
    /** Each new vertex combined with the previous 2 forms a triangle. */
    TRIANGLE_STRIP,
}
