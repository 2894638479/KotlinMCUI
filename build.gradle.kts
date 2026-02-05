import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0"
    id("me.modmuss50.mod-publish-plugin") version "0.8.1"
}

val maven_group: String by project
val archives_base_name: String by project
val license: String by project
val mod_version: String by project
val mod_id: String by project
val mod_name: String by project
val mod_description: String by project
val mod_authors: String by project
val mod_github: String by project
val mod_issues: String by project
val mod_mcmod: String by project
val mod_modrinth: String by project
val mod_cuseforge: String by project
val icon: String by project

group = maven_group
version = mod_version


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.1")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.lwjgl:lwjgl-glfw:3.3.1")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val sourcesJar: org.gradle.jvm.tasks.Jar by tasks
sourcesJar.exclude("fabric.mod.json")
sourcesJar.from("LICENSE")

tasks.jar {
    from("LICENSE")
}


val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}

tasks.processResources {
    val map = mapOf(
        "license" to license,
        "mod_version" to mod_version,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_description" to mod_description,
        "mod_authors" to mod_authors,
        "mod_github" to mod_github,
        "mod_issues" to mod_issues,
        "mod_mcmod" to mod_mcmod,
        "mod_modrinth" to mod_modrinth,
        "mod_cuseforge" to mod_cuseforge,
        "icon" to icon,
    )
    inputs.properties(map)
    filesMatching("fabric.mod.json") { expand(map) }
}


publishMods {
    file = tasks.jar.get().archiveFile
    additionalFiles = files(sourcesJar.archiveFile)
    changelog = ""
    type = ALPHA
    displayName = "KotlinMCUI ${project.version}"
    modLoaders.add("fabric")

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "q8Q5ZFT7"
        minecraftVersionRange {
            start = "1.14"
            end = "latest"
        }
    }
}
