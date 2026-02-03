package io.github.u2894638479.kotlinmcui.backend

interface DslBackendLanguages {
    fun translate(key: String,vararg args: Any): String?
}