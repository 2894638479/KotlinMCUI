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
 * Declare child keys with [name], [string], or [type] delegates.
 * Each delegate automatically initialises the child's [KeyInfo] from its
 * parent using Kotlin's `by` delegation mechanism.
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

// ── Base implementation ──────────────────────────────────────────────────────

/**
 * Abstract base implementing [TranslationKey] with lazy initialisation.
 *
 * The [KeyInfo] is set internally by [KeyProperty] when the key is first
 * accessed via delegation. Direct use without a delegate will produce a
 * descriptive error.
 */
abstract class BaseKey : TranslationKey {
    internal var keyInfo: KeyInfo? = null

    override fun getInfo() = keyInfo ?: error(
        "TranslationKey was not initialized. " +
        "Keys must be declared with a delegate inside a ParentKey context.\n" +
        "  ✅ val myKey by name\n" +
        "  ✅ val myKey by string(\"custom_name\")\n" +
        "  ❌ val myKey = SimpleKey()  // missing delegate"
    )
}

// ── Concrete key types ───────────────────────────────────────────────────────

/**
 * A key that can both produce translations and have child keys.
 *
 * This is the most common base type for defining a translation key subtree.
 * Extend this class to group related translation keys:
 * ```
 * class MyPageKeys : KeyNode() {
 *     val title by name
 *     val subtitle by name
 *     val button by name type NarrationKey()
 *     class NarrationKey : KeyNode() {
 *         val confirm by name
 *         val cancel by name
 *     }
 * }
 * ```
 *
 * Each `by name` child automatically derives its dot-separated path from
 * the parent's position in the tree.
 */
open class KeyNode : BaseKey(), ParentKey, TranslatableKey

/**
 * A key that can produce translations but has no children.
 *
 * Use this (or [SimpleKey]) for leaf-level translation keys that only need to
 * look up translated strings. If you need a subtree, use [KeyNode] instead.
 */
open class LeafKey : BaseKey(), TranslatableKey

/**
 * The simplest concrete translation key — a [LeafKey] with a friendlier name.
 *
 * ```kotlin
 * val myText by name             // SimpleKey with property name as segment
 * val myText by string("my_text") // SimpleKey with explicit segment name
 * ```
 */
class SimpleKey : LeafKey()

// ── Root ────────────────────────────────────────────────────────────────────

/**
 * The root of the translation key tree.
 *
 * All top-level translation key objects should use [top] as their implicit
 * parent. [KeyProperty] automatically detects when a key is declared at the
 * top level and uses [top] as the fallback parent.
 *
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
 * 1. Gets the parent from [thisRef] (the containing object, which should be a [ParentKey])
 * 2. Uses [property.name] (or the explicit [string]) as the key's segment name
 * 3. Sets the key's [KeyInfo] via [BaseKey.keyInfo]
 *
 * @param key A new instance of the key to initialise (e.g. `SimpleKey()`).
 * @param string An explicit segment name, or `null` to use the Kotlin property name.
 */
class KeyProperty<T : BaseKey>(val key: T, val string: String? = null) : ReadOnlyProperty<Any?, T> {
    private var initialized = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!initialized) {
            initialized = true
            val parent = (thisRef as? ParentKey) ?: top
            key.keyInfo = parent.spawn(string ?: property.name)
        }
        return key
    }

    /** Set an explicit key type while keeping the same segment name. */
    infix fun <T : BaseKey> type(type: T) = KeyProperty(type, string)
}

// ── Convenience builders ─────────────────────────────────────────────────────

/**
 * Create a [KeyProperty] for a [SimpleKey] using the Kotlin property name as
 * the translation key segment.
 *
 * ### Example
 * ```kotlin
 * object MyKeys : KeyNode() {
 *     val button by name        // path: "...MyKeys.button"
 *     val title by name         // path: "...MyKeys.title"
 * }
 * ```
 */
val name get() = KeyProperty(SimpleKey())

/**
 * Create a [KeyProperty] for a [SimpleKey] with an explicit segment name.
 *
 * Use this when the translation key segment differs from the Kotlin property
 * name (e.g. snake_case in JSON vs camelCase in Kotlin).
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
 * Use this to declare a subtree with its own children:
 * ```kotlin
 * val narration by type(NarrationKey())
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

class KotlinMCUIKey : KeyNode() {
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
    class NarrationKey : KeyNode() {
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
    class EntryPageKey : KeyNode() {
        val overlay by name
        val show_details by name
        val show_code_entry by name
        val tooltip by name type TooltipKey()

        class TooltipKey : KeyNode() {
            val client by name
            val server by name
            val common by name
            val gui by name
            val overlay by name
        }
    }

    val utils by name type UtilsKey()

    /** `kotlinmcui.utils.*` — utility UI strings (e.g. true/false labels). */
    class UtilsKey : KeyNode() {
        val `true` by name
        val `false` by name
    }

    val debugoverlay by name
}
