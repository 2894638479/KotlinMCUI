package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment

/**
 * One declaration from a `META-INF/kotlinmcui/entries` file: a class and the environments it runs in.
 */
internal data class EntryDeclaration(val className: String, val environments: Set<Environment>)

/**
 * Parse one registry file.
 *
 * The format is deliberately tiny - the environments have to be written out, no `*` and no aliases, so a
 * typo is an error instead of a silent "runs everywhere":
 *
 * ```
 * # <class> <env>[,<env>...]
 * com.example.MyPage        client,server,standalone
 * com.example.ClientPanel   client
 * ```
 *
 * Blank lines and lines starting with `#` are skipped. Every other line must be exactly two
 * whitespace-separated fields. Format errors throw: a broken registry is a build mistake, not something to
 * skip past. Every message names the line number and the offending text: "line : " is useless in a file the
 * author cannot see at the point of the failure.
 */
internal fun parseEntryRegistry(text: String): List<EntryDeclaration> {
    val result = mutableListOf<EntryDeclaration>()
    val seen = mutableSetOf<String>()
    text.lineSequence().forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if(line.isEmpty() || line.startsWith("#")) return@forEachIndexed
        val fields = line.split(Regex("""\s+"""))
        if(fields.size != 2) {
            throw IllegalStateException(
                "entry registry, line ${index + 1}: expected '<class> <environment>[,<environment>...]', " +
                    "was: '$line'"
            )
        }
        val className = fields[0]
        val environments = fields[1].split(',').map { name ->
            Environment.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: throw IllegalStateException(
                    "entry registry, line ${index + 1}: unknown environment '$name' for $className; " +
                        "allowed: " + Environment.entries.joinToString { it.name.lowercase() }
                )
        }.toSet()
        if(!seen.add(className)) {
            throw IllegalStateException(
                "entry registry, line ${index + 1}: '$className' is declared more than once"
            )
        }
        result += EntryDeclaration(className, environments)
    }
    return result
}
