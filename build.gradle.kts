plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

/** Reads a value from `gradle.properties`, failing the build when it is missing. */
fun gradleProperty(name: String) = providers.gradleProperty(name).get()

val maven_group = gradleProperty("maven_group")
val archives_base_name = gradleProperty("archives_base_name")
val license = gradleProperty("license")
val mod_version = gradleProperty("mod_version")
val mod_id = gradleProperty("mod_id")
val mod_name = gradleProperty("mod_name")
val mod_description = gradleProperty("mod_description")
val mod_authors = gradleProperty("mod_authors")
val mod_github = gradleProperty("mod_github")
val mod_issues = gradleProperty("mod_issues")
val mod_mcmod = gradleProperty("mod_mcmod")
val mod_modrinth = gradleProperty("mod_modrinth")
val mod_cuseforge = gradleProperty("mod_cuseforge")
val icon = gradleProperty("icon")

group = maven_group
version = mod_version


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.20")
    implementation("it.unimi.dsi:fastutil:8.5.19")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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

val sourcesJar = tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").get()
sourcesJar.exclude("fabric.mod.json")
sourcesJar.exclude("META-INF/mods.toml")
sourcesJar.exclude("META-INF/neoforge.mods.toml")
sourcesJar.from("LICENSE")

tasks.jar {
    from("LICENSE")
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
    filesMatching("META-INF/mods.toml") { expand(map) }
    filesMatching("META-INF/neoforge.mods.toml") { expand(map) }
    filesMatching("pack.mcmeta") { expand(map) }
}


val tag = "v$mod_version"

val shouldPublish by lazy {
    providers.exec {
        commandLine("git", "ls-remote", "--tags", "origin", "refs/tags/$tag")
    }.standardOutput.asText.get().isBlank()
}
tasks.configureEach {
    if(group == "publishing") enabled = shouldPublish
}

publishMods {
    file = tasks.jar.get().archiveFile
    additionalFiles.from(sourcesJar.archiveFile)
    changelog = "no changelog."
    type = when {
        mod_version.contains("SNAPSHOT",true) -> ALPHA
        mod_version.contains("alpha",true) -> ALPHA
        mod_version.contains("beta",true) -> BETA
        else -> STABLE
    }
    displayName = "KotlinMCUI $mod_version"
    modLoaders.addAll("fabric","forge","neoforge")

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "q8Q5ZFT7"
        optional("fabric-language-kotlin","kotlin-for-forge")
        requires("kotlinmcui-backend")
        minecraftVersionRange {
            start = "1.14"
            end = "latest"
        }
    }
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = "1460598"
        optional("fabric-language-kotlin","kotlin-for-forge")
        requires("kotlinmcui-backend")
        client = true
        server = false
        minecraftVersionRange {
            start = "1.14"
            end = "latest"
        }
    }
    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = "2894638479/KotlinMCUI"
        commitish = "master"
        tagName = tag
    }
}
