plugins {
    kotlin("jvm")
    `java-library`
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

// Pinned to the floor the supported game versions ship (Minecraft 1.20.1 has slf4j 2.0.1 and
// fastutil 8.5.9): compiling against anything newer risks using an API the runtime lacks.
val slf4j = "org.slf4j:slf4j-api:2.0.1"
val fastutil = "it.unimi.dsi:fastutil:8.5.9"
val serializationCore = "org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0"
val serializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0"
val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0"

dependencies {
    // The platform supplies every runtime dependency of this library: Minecraft ships slf4j and
    // fastutil, and a mod loader's Kotlin provider (Fabric Language Kotlin, and its equivalents)
    // bundles the Kotlin runtime, coroutines and serialization. The library must neither bundle them
    // (there is no fat jar) nor hand them to consumers: `compileOnlyApi` keeps a dependency on the
    // consumer's COMPILE classpath, where the public API needs it, and out of the published runtime
    // metadata, where a second copy would collide with the one the platform already put there.
    compileOnlyApi(slf4j)
    compileOnlyApi(serializationCore)
    compileOnlyApi(coroutines)
    compileOnly(fastutil)
    compileOnly(serializationJson)
    // Only the preview session reads Kotlin reflection, and only to tell which page parameters have
    // defaults. It is compile-time only: the published jar must not carry the 3 MB runtime, and the
    // platforms that can run a preview ship it already (Fabric's language loader bundles it).
    compileOnly(kotlin("reflect"))
    // compileOnly dependencies are absent from the test classpath, and the tests exercise all of them.
    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    testImplementation(slf4j)
    testImplementation(fastutil)
    testImplementation(serializationCore)
    testImplementation(serializationJson)
    testImplementation(coroutines)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        // Link against the JDK 8 API surface rather than the toolchain's. The library has
        // to load on old Minecraft versions, and a JDK 9+ call would compile happily here
        // only to fail with NoSuchMethodError on the game's older runtime.
        freeCompilerArgs.add("-Xjdk-release=1.8")
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
