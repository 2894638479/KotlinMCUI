pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version extra["kotlin_version"] as String
        id("org.jetbrains.kotlin.plugin.serialization") version extra["kotlin_version"] as String
    }
}
rootProject.name = "kotlinmcui"