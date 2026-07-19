package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.backend.dslBackend
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Metadata for a single translation key node.
 *
 * @param name The segment name at this level (e.g. `"button"`).
 * @param parents The names of all ancestor keys, from root to immediate parent.
 */
data class KeyInfo(
    val name: String,
    val parents: List<String>
) {
    /** The full dot-separated translation path (e.g. `"kotlinmcui.narration.button"`). */
    val path = parents.joinToString(".") + "." + name
}

// ── Interfaces ──────────────────────────────────────────────────────────────

/**
 * A node in the translation key tree.
 *
 * The tree mirrors the structure of your language JSON files (e.g. `en_us.json`).
 * Each node knows its own [KeyInfo], which includes the full dot-separated path
 * back to the root.
 */
interface TranslationKey {
    fun getInfo(): KeyInfo
}

/**
 * A translation key that can spawn child keys.
 *
 * Child keys extend the parent's path with their own segment:
 * `parent.path + "." + child.name`.
 *
 * ### Usage
 * Declare child keys with [KeyProperty] delegates (`by name`, `by type`, `by string`).
 * The delegate automatically initialises each child's [KeyInfo] from its parent
 * using Kotlin's `by` delegation mechanism.
 */
interface ParentKey : TranslationKey {
    /**
     * Create a [KeyInfo] for a direct child with the given [name].
     * Called automatically by [KeyProperty] during delegate initialisation.
     */
    fun spawn(name: String) = KeyInfo(name, getInfo().let { it.parents + it.name })
}

/**
 * A translation key that can produce translated strings at runtime.
 *
 * Calling the key as a function invokes [io.github.u2894638479.kotlinmcui.backend.dslBackend.translate]
 * and returns the translated string, falling back to the path itself if no
 * translation is found.
 */
interface TranslatableKey : TranslationKey {
    /** The full dot-separated path used as the translation lookup key. */
    fun getPath() = getInfo().path

    /**
     * Look up the translated string.
     * @param args Optional format arguments (for `String.format`-style placeholders).
     * @return The translated string, or [getPath] if no translation is registered.
     */
    operator fun invoke(vararg args: Any?) = dslBackend.translate(getPath(), *args) ?: getPath()
}

// ── Base implementations ─────────────────────────────────────────────────────

/**
 * Abstract base implementing [TranslationKey] with lazy initialisation.
 *
 * A key's [KeyInfo] is set exactly once by [init], which is called automatically
 * by [KeyProperty] when the key is first accessed via delegation.
 */
abstract class BaseKey : TranslationKey {
    private var keyInfo: KeyInfo? = null

    override fun getInfo() = keyInfo ?: error("TranslationKey not initialized — ensure it is accessed via a KeyProperty delegate")

    /** Set the [KeyInfo] for this key. Must only be called once. */
    fun init(info: KeyInfo) {
        if (keyInfo != null) error("TranslationKey init twice — each key instance must be used with exactly one delegate")
        keyInfo = info
    }
}

/** A concrete [ParentKey] backed by [BaseKey]. */
abstract class BaseParentKey : ParentKey, BaseKey()

/** A concrete [TranslatableKey] backed by [BaseKey]. */
abstract class BaseTranslatableKey : TranslatableKey, BaseKey()

/** A concrete key that is both a [ParentKey] and [TranslatableKey]. */
abstract class BaseTranslatableParentKey : ParentKey, TranslatableKey, BaseKey()

// ── Root ────────────────────────────────────────────────────────────────────

/**
 * The root of the translation key tree.
 *
 * All top-level translation key objects should use [top] as their parent.
 * [top] itself has no parent and produces [KeyInfo] with empty [KeyInfo.parents].
 */
val top: ParentKey = object : ParentKey, BaseKey() {
    override fun spawn(name: String) = KeyInfo(name, emptyList())
}

// ── Delegates ────────────────────────────────────────────────────────────────

/**
 * A property delegate that auto-initialises a [BaseKey]'s [KeyInfo] from its
 * parent context.
 *
 * When a key is declared as `val myKey by name`, the delegate:
 * 1. Reads the parent key from [thisRef] (the containing object, which must be a [ParentKey])
 * 2. Uses [property.name] (or the explicit [string]) as the key's segment name
 * 3. Calls [BaseKey.init] with the parent's [ParentKey.spawn] result
 *
 * @param key A new instance of the key to initialise (e.g. `SimpleKey()`).
 * @param string An explicit segment name, or `null` to use the Kotlin property name.
 */
class KeyProperty<T : BaseKey>(val key: T, val string: String? = null) : ReadOnlyProperty<Any?, T> {
    var initialized = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!initialized) {
            initialized = true
            val parent = (thisRef as? ParentKey) ?: top
            key.init(parent.spawn(string ?: property.name))
        }
        return key
    }

    /** Set an explicit key type while keeping the same segment name. */
    infix fun <T : BaseKey> type(type: T) = KeyProperty(type, string)
}

// ── Convenience builders ─────────────────────────────────────────────────────

/** A minimal concrete key that can produce translations (has no children). */
class SimpleKey : BaseTranslatableKey()

/**
 * Create a [KeyProperty] for a [SimpleKey] using the Kotlin property name as
 * the translation key segment.
 *
 * ### Example
 * ```kotlin
 * object MyKeys : BaseParentKey() {
 *     val button by name        // path: "MyKeys.button"
 *     val title by name         // path: "MyKeys.title"
 * }
 * ```
 */
val name get() = KeyProperty(SimpleKey())

/**
 * Create a [KeyProperty] for a [SimpleKey] with an explicit segment name.
 *
 * ### Example
 * ```kotlin
 * val showDetails by string("show_details")  // path: "...show_details"
 * ```
 */
fun string(string: String) = KeyProperty(SimpleKey(), string)

/**
 * Create a [KeyProperty] for a custom key type.
 *
 * ### Example
 * ```kotlin
 * val mySubtree by type(MySubtreeKey())
 * ```
 */
fun <K : BaseKey> type(key: K) = KeyProperty(key)

// ── Framework translation keys ───────────────────────────────────────────────

/**
 * Root of the framework's own translation key tree (path prefix `"kotlinmcui"`).
 *
 * This object mirrors the structure of `en_us.json` (or equivalent) shipped
 * with the framework. Each child key corresponds to a top-level JSON key under
 * `kotlinmcui.*`.
 */
val kotlinmcui by type(KotlinMCUIKey())

class KotlinMCUIKey : BaseParentKey() {
    val testpage by string("testpage")
    val testentry by name type SimpleKey()
    val test_translate by name
    val layout by name
    val text by name
    val image by name
    val scroll by name
    val slider by name
    val translation by name
    val id by name
    val container by name
    val mousetip by name
    val screen by name
    val metadata by name
    val rotation by name
    val animation by name
    val narration by name type NarrationKey()

    /** `kotlinmcui.narration.*` — accessibility narration strings. */
    class NarrationKey : BaseParentKey() {
        val button by name
        val row by name
        val column by name
        val scrollablecolumn by name
        val slider by name
        val scroller by name
        val text by name
        val editabletext by name
    }

    val entrypage by name type EntryPageKey()

    /** `kotlinmcui.entrypage.*` — entry list UI strings. */
    class EntryPageKey : BaseParentKey() {
        val overlay by name
        val show_details by name
        val show_code_entry by name
        val tooltip by name type TooltipKey()

        class TooltipKey : BaseParentKey() {
            val client by name
            val server by name
            val common by name
            val gui by name
            val overlay by name
        }
    }

    val utils by name type UtilsKey()

    /** `kotlinmcui.utils.*` — utility UI strings (e.g. true/false labels). */
    class UtilsKey : BaseParentKey() {
        val `true` by name
        val `false` by name
    }

    val debugoverlay by name
}
