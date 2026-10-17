package io.github.u2894638479.kotlinmcui.container

/**
 * How long a root is expected to live. It is a diagnostic label, not an ownership rule: the one root
 * that owns the process is decided by [DslRoot.isOwner], not by this.
 */
enum class ViewerLifetime { NONE, PROCESS, TRANSIENT }
