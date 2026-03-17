package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.identity.DslId

interface DslIdContext {
    val identity: DslId
    companion object {
        operator fun invoke(identity: DslId) = object : DslIdContext {
            override val identity = identity
        }
    }
}