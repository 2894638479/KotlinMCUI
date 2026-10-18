package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.backend.dslPlatformProvider
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.forEachWithId
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Modifier

/**
 * Discovers the entry points declared by every mod and runs their two stages.
 *
 * The declaration lives in `META-INF/kotlinmcui/entries` - one line per entry, its class and the
 * environments it runs in - not in the JDK's `META-INF/services` files: the environment column is what
 * makes a declaration checkable before anything is loaded, and a wrong listing is a loud error rather than
 * a class that quietly does nothing.
 *
 * The load order is the safety property: parse and validate the text, **filter by environment**, and only
 * then load classes (`Class.forName(..., initialize = false)`). A class declared for another environment is
 * never touched - not loaded, not initialised.
 */
object DslEntryLoader {
    /** Every jar contributes this file; the classpath merges the lines. */
    private const val REGISTRY = "META-INF/kotlinmcui/entries"

    private var environment = Environment.STANDALONE
    private var launched = false

    private val cache = mutableMapOf<Class<*>, DslEntryService>()
    private var snapshot: List<Pair<DslEntryService, Flags>> = emptyList()

    private val registeredOverlays = mutableMapOf<String, DslEntryOverlay>()
    private val enabledIds = mutableListOf<String>()
    private var enabledSnapshot: List<DslEntryOverlay> = emptyList()

    /** Registered entries and their discovery flags. A snapshot, so callers may iterate it safely. */
    val entries: Collection<Pair<DslEntryService, Flags>> get() = snapshot

    /** Enabled overlays in stacking order: the first one is the topmost layer. */
    val enabledOverlays: List<DslEntryOverlay> get() = enabledSnapshot

    /** What one entry declared, plus what its type says about it. */
    class Flags(
        /** The environments the registry declares it for; shown as-is on the entry page. */
        val environments: Set<Environment>,
    ) {
        var isGui = false
            internal set
        var isOverlay = false
            internal set
    }

    /**
     * A listing that could not be turned into a service: the class is missing, does not implement
     * [DslEntryService], is neither a gui nor an overlay, or cannot be constructed.
     *
     * The log keeps the whole stack; this is what the config page shows as a placeholder, so [reason]
     * stays one short line.
     */
    class Failure internal constructor(
        val className: String,
        /** The kind the declaration expected. */
        val hookName: String,
        val reason: String,
    )

    private val failures = LinkedHashMap<String, Failure>()
    private var failureSnapshot: List<Failure> = emptyList()

    /** Listings that could not be loaded, in discovery order. A snapshot, like [entries]. */
    val brokenEntries: List<Failure> get() = failureSnapshot

    /**
     * Discover every entry on the classpath, keep the ones declared for [environment], and run their
     * `initialize` stage.
     *
     * Called by [io.github.u2894638479.kotlinmcui.backend.DslRuntime.install], which runs it exactly once
     * and guarantees that metadata (and therefore the environment) is already available.
     *
     * Format errors in a registry file throw - a broken declaration is a build mistake, not something to
     * skip. Unloadable classes do not throw: they go to [brokenEntries] and the entry page shows them.
     */
    internal fun discover(environment: Environment) {
        this.environment = environment
        launched = false
        failures.clear()
        cache.clear()

        val declarations = readDeclarations().filter { environment in it.environments }
        val loaded = mutableListOf<Pair<DslEntryService, Flags>>()
        declarations.forEach { declaration ->
            val cls = try {
                Class.forName(declaration.className, false, loader())
            } catch(e: ClassNotFoundException) {
                recordFailure(declaration.className, "DslEntryService", "class not found")
                return@forEach
            } catch(e: LinkageError) {
                recordFailure(declaration.className, "DslEntryService", "could not be loaded (missing dependency?)")
                return@forEach
            }
            if(!DslEntryService::class.java.isAssignableFrom(cls)) {
                recordFailure(declaration.className, "DslEntryService", "does not implement DslEntryService")
                return@forEach
            }
            val isGui = DslEntryGui::class.java.isAssignableFrom(cls)
            val isOverlay = DslEntryOverlay::class.java.isAssignableFrom(cls)
            // Instantiating is the last step: everything above is a decision about the class, not an
            // effect on it.
            val service = try {
                cache.getOrPut(cls) { getObject(cls) }
            } catch(e: Exception) {
                recordFailure(declaration.className, kindName(isGui, isOverlay), reasonOf(e))
                return@forEach
            } catch(e: LinkageError) {
                recordFailure(declaration.className, kindName(isGui, isOverlay), "could not be initialised (missing dependency?)")
                return@forEach
            }
            loaded += service to Flags(declaration.environments).also {
                it.isGui = isGui
                it.isOverlay = isOverlay
            }
        }

        snapshot = loaded
        failureSnapshot = failures.values.toList()
        collectOverlays()
        // The overlay pass can add failures (a duplicate id), so refresh the snapshot after it.
        failureSnapshot = failures.values.toList()
        enabledIds.clear()
        enabledIds += readOverlayIds()
        refreshEnabledOverlays()

        // Stage one: every entry that made it through the filter, in declaration order.
        loaded.forEach { (service, _) -> runStage(service, "initialize") { it.initialize() } }
    }

    /**
     * Run the second stage on every entry that passed the environment filter.
     *
     * Called by [io.github.u2894638479.kotlinmcui.backend.DslRuntime.launch] - explicitly, never as a side
     * effect of publishing a platform: "there is a platform" and "the game is ready" are two facts.
     *
     * The registry file's order is the order. Idempotent, and an error before the platform is published.
     */
    internal fun launch() {
        check(dslPlatformProvider != null) {
            "DslEntryLoader.launch was called before the platform was published; " +
                "game and standalone backends must call DslRuntime.start first"
        }
        if(launched) {
            dslLogger.warn("DslEntryLoader.launch already ran; this call is ignored")
            return
        }
        launched = true
        snapshot.forEach { (service, _) -> runStage(service, "launch") { it.launch() } }
    }

    private inline fun runStage(service: DslEntryService, stage: String, block: (DslEntryService) -> Unit) {
        try {
            block(service)
        } catch(e: Throwable) {
            // One broken entry must not take the others (or the game) down with it.
            dslLogger.error("entry ${service::class.java.name} threw in $stage", e)
        }
    }

    /** Whether [overlay] is currently enabled. */
    fun isOverlayEnabled(overlay: DslEntryOverlay): Boolean = overlay.id in enabledIds

    /** Enable or disable [overlay], storing the new list. */
    fun setOverlayEnabled(overlay: DslEntryOverlay, enabled: Boolean) {
        if (enabled == (overlay.id in enabledIds)) return
        if (enabled) enabledIds += overlay.id else enabledIds -= overlay.id
        refreshEnabledOverlays()
        writeOverlayIds(enabledIds.toList())
    }

    /** Move [overlay] to the front of the stack, storing the new order. */
    fun bringOverlayToTop(overlay: DslEntryOverlay) {
        if (enabledIds.firstOrNull() == overlay.id) return
        enabledIds.remove(overlay.id)
        enabledIds.add(0,overlay.id)
        refreshEnabledOverlays()
        writeOverlayIds(enabledIds.toList())
    }

    context(ctx: DslContext)
    internal fun overlays() {
        enabledSnapshot.forEachWithId {
            Box {
                it.overlay()
            }
        }
    }

    private fun readDeclarations(): List<EntryDeclaration> {
        val declarations = mutableListOf<EntryDeclaration>()
        val resources = loader().getResources(REGISTRY)
        var found = 0
        while(resources.hasMoreElements()) {
            found++
            resources.nextElement().openStream().use { stream ->
                BufferedReader(InputStreamReader(stream, "utf-8")).use { reader ->
                    declarations += parseEntryRegistry(reader.readText())
                }
            }
        }
        if(found == 0) {
            // Silence here is the bad case: a jar that bundles other jars and keeps one copy of each
            // resource drops every registry but its own, and the only symptom is an empty entry list
            // nobody is looking at.
            dslLogger.warn(
                "no $REGISTRY was found on the classpath, so no entry will load; a jar that bundles " +
                    "other jars has to merge this file, not keep one copy of it"
            )
        }
        return mergeDeclarations(declarations)
    }

    /**
     * Fold the declarations of every registry on the classpath into one list.
     *
     * The same file is legitimately seen twice when a jar and its sources jar share a classpath, so a
     * repeat that agrees is ignored instead of rejected: a false alarm at startup teaches people to
     * ignore the check, which is a worse outcome than the duplicate itself. A repeat that disagrees
     * about the environments is the real mistake - one class cannot load in two different sets - and
     * still throws.
     */
    internal fun mergeDeclarations(declarations: List<EntryDeclaration>): List<EntryDeclaration> {
        val merged = LinkedHashMap<String, EntryDeclaration>()
        declarations.forEach { declaration ->
            val previous = merged[declaration.className]
            when {
                previous == null -> merged[declaration.className] = declaration
                previous.environments == declaration.environments -> dslLogger.warn(
                    "'${declaration.className}' is declared more than once with the same environments; " +
                        "the repeat is ignored"
                )
                else -> throw IllegalStateException(
                    "'${declaration.className}' is declared with different environments: " +
                        "${previous.environments} and ${declaration.environments}"
                )
            }
        }
        return merged.values.toList()
    }

    private fun collectOverlays() {
        registeredOverlays.clear()
        snapshot.forEach { (service, flags) ->
            if (!flags.isOverlay) return@forEach
            val overlay = service as DslEntryOverlay
            val first = registeredOverlays[overlay.id]
            if (first != null) {
                // Two mods may pick the same id ("settings" is the obvious one). Declaring first wins,
                // because the file order is deterministic; the later one is not silently dropped - it is
                // reported on the entry page, where its author can see it without anything crashing.
                recordFailure(
                    service::class.java.name, "DslEntryOverlay",
                    "overlay id '${overlay.id}' is already used by ${first::class.java.name}",
                )
                return@forEach
            }
            registeredOverlays[overlay.id] = overlay
        }
    }

    private fun refreshEnabledOverlays() {
        enabledSnapshot = enabledIds.mapNotNull(registeredOverlays::get)
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader

    private fun kindName(isGui: Boolean, isOverlay: Boolean): String = when {
        isGui && isOverlay -> "DslEntryGui+DslEntryOverlay"
        isGui -> "DslEntryGui"
        isOverlay -> "DslEntryOverlay"
        else -> "DslEntryService"
    }

    private fun recordFailure(className: String, hook: String, reason: String) {
        failures.putIfAbsent("$className|$hook", Failure(className, hook, reason))
    }

    private fun reasonOf(e: Exception): String = when {
        e is java.lang.reflect.InvocationTargetException ->
            "constructor failed: ${e.cause?.let { cause -> cause::class.simpleName } ?: "unknown"}"
        else -> e.message?.lineSequence()?.firstOrNull()?.trim()?.take(120)?.takeIf { it.isNotEmpty() }
            ?: e::class.simpleName
            ?: "failed to load"
    }

    private fun getObject(clazz: Class<*>): DslEntryService {
        // A Kotlin `object` has a private constructor, which is the recommended way to
        // declare an entry, so `INSTANCE` is the ordinary path rather than a fallback
        // worth warning about. A constructor that exists but throws is not silently
        // replaced by `INSTANCE` either: that would hide the failure behind a second
        // instance nobody asked for.
        val constructor = runCatching { clazz.getDeclaredConstructor() }.getOrNull()
        val instance: Any
        if (constructor != null && Modifier.isPublic(constructor.modifiers)) {
            instance = constructor.newInstance()
            dslLogger.info("loaded class: $clazz")
        } else {
            instance = clazz.declaredFields
                .firstOrNull { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
                ?.get(null)
                ?: error("$clazz has neither a public no-argument constructor nor an INSTANCE field")
            dslLogger.info("loaded Kotlin object: $clazz")
        }
        return instance as? DslEntryService
            ?: error("$clazz does not implement DslEntryService")
    }
}
