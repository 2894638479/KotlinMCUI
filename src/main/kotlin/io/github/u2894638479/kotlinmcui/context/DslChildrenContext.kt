package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.scope.DslChild


interface DslChildrenContext {
    val children: DslChild.List
}