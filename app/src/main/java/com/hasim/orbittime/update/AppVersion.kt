package com.hasim.orbittime.update

/**
 * Orbit Time's release versions, compared by number rather than by text.
 *
 * A plain string comparison gets this wrong in exactly the case that matters:
 * `"1.10" < "1.9"` alphabetically, so the tenth release would never be offered to anyone on
 * the ninth. Each dot-separated component is therefore parsed and compared as an integer.
 *
 * Accepts every shape this project's tags and version names take — `v.1.1` (the tag format the
 * release workflow publishes), `v1.1`, and the bare `1.1` that `versionName` holds — so the
 * installed version and the release tag can be compared directly without either side being
 * normalised by its caller first.
 */
class AppVersion private constructor(val parts: List<Int>) : Comparable<AppVersion> {

    /** True for a version string that carried no usable numbers at all. */
    val isValid: Boolean get() = parts.isNotEmpty()

    /**
     * Compares component by component, treating a missing trailing component as zero so
     * `1.2` and `1.2.0` are the same version rather than different ones.
     */
    override fun compareTo(other: AppVersion): Int {
        val width = maxOf(parts.size, other.parts.size)
        for (index in 0 until width) {
            val mine = parts.getOrElse(index) { 0 }
            val theirs = other.parts.getOrElse(index) { 0 }
            if (mine != theirs) return mine.compareTo(theirs)
        }
        return 0
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0

    override fun hashCode(): Int = parts.dropLastWhile { it == 0 }.hashCode()

    override fun toString(): String = parts.joinToString(".")

    companion object {

        /** The empty version — every real version is newer than this. */
        val NONE: AppVersion = AppVersion(emptyList())

        /**
         * Parses a tag or version name into comparable numbers, ignoring a leading `v`/`v.`
         * and any build/prerelease suffix (`1.2-beta3` parses as `1.2`). Returns [NONE] for a
         * string with no digits, which callers treat as "don't offer an update" rather than
         * risking a bogus comparison against unrecognisable release data.
         */
        fun parse(raw: String?): AppVersion {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return NONE

            val withoutPrefix = trimmed
                .removePrefix("v")
                .removePrefix("V")
                .removePrefix(".")

            // A suffix such as "-beta3" or "+build7" is not part of the ordering here: this
            // project only ever compares released v.x.y tags against each other.
            val numeric = withoutPrefix.takeWhile { it.isDigit() || it == '.' }
            val parts = numeric.split('.')
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }

            return if (parts.isEmpty()) NONE else AppVersion(parts)
        }

        /**
         * True when [latest] is a strictly newer release than [installed].
         *
         * Unparseable input on either side means no update is offered — a release whose tag
         * can't be read is not a reason to push an install prompt at the user.
         */
        fun isNewer(installed: String?, latest: String?): Boolean {
            val current = parse(installed)
            val candidate = parse(latest)
            if (!current.isValid || !candidate.isValid) return false
            return candidate > current
        }
    }
}

/**
 * Normalises the several shapes a version is written in — the `v.1.1` release tag and the bare
 * `1.1` version name — to the single `v1.1` form the app shows users. A version that can't be
 * parsed is shown exactly as the release published it rather than being hidden.
 */
fun displayVersion(raw: String): String {
    val parsed = AppVersion.parse(raw)
    return if (parsed.isValid) "v$parsed" else raw
}
