package io.github.u2894638479.kotlinmcui.config

import io.github.u2894638479.kotlinmcui.backend.dslMetadata
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * The format the framework writes its own configuration files with: readable for
 * someone editing them by hand, and tolerant of keys written by a newer version.
 */
val configFormat: StringFormat = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

/**
 * Save [obj] as a configuration file at [path].
 *
 * The file is written next to the target and then moved into place, so a crash
 * halfway through cannot leave a truncated configuration behind. A failure is
 * logged rather than thrown: saving a setting should never take the game down.
 */
fun <T> saveConfig(path: Path, obj: T, format: StringFormat, serializer: SerializationStrategy<T>) {
    try {
        path.parent?.let { Files.createDirectories(it) }
        val temp = path.resolveSibling("${path.fileName}.tmp")
        Files.write(temp, format.encodeToString(serializer, obj).toByteArray(Charsets.UTF_8))
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            // Some file systems refuse an atomic move even within one directory.
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (e: IOException) {
        dslLogger.error("failed to save config $path", e)
    }
}

/**
 * Load the configuration file at [path].
 *
 * A missing, unreadable or malformed file yields [default]: the first launch has no
 * file at all, and a hand-edited mistake should cost the user their settings, not
 * their ability to start the game.
 */
fun <T> loadConfig(
    path: Path,
    format: StringFormat,
    default: () -> T,
    deserializer: DeserializationStrategy<T>
): T {
    if (!Files.exists(path)) return default()
    return try {
        format.decodeFromString(deserializer, String(Files.readAllBytes(path), Charsets.UTF_8))
    } catch (e: Exception) {
        dslLogger.error("failed to read config $path, using defaults", e)
        default()
    }
}

inline fun <reified T> saveConfig(path: Path, obj: T, format: StringFormat) =
    saveConfig(path,obj,format,serializer<T>())

inline fun <reified T> loadConfig(path: Path, format: StringFormat, noinline default: () -> T): T =
    loadConfig(path,format,default,serializer<T>())

/** Save [obj] as [name] inside the backend's configuration directory. */
inline fun <reified T> saveConfig(name: String, obj: T, format: StringFormat) =
    saveConfig(dslMetadata.configDir.resolve(name),obj,format,serializer<T>())

/** Load [name] from the backend's configuration directory. */
inline fun <reified T> loadConfig(name: String, format: StringFormat, noinline default: () -> T): T =
    loadConfig(dslMetadata.configDir.resolve(name),format,default,serializer<T>())
