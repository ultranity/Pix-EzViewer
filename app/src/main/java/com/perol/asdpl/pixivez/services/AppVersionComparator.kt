package com.perol.asdpl.pixivez.services

/**
 * Compares the version name shipped by the app with a GitHub release tag.
 *
 * Release tags use a leading `v`, while Android version names do not. The
 * comparison follows semantic-version precedence for the parts that the
 * project uses: numeric core components, dot-separated pre-release
 * identifiers, and ignored build metadata. A local debug build's exact
 * `-debug` suffix is treated as a local marker rather than a pre-release;
 * other pre-release suffixes retain their normal ordering.
 */
internal object AppVersionComparator {
    /**
     * Returns true only when [remoteVersion] is a valid version newer than
     * [currentVersion]. Invalid input is treated as no update so a malformed
     * release tag cannot trigger an update dialog.
     */
    fun isRemoteNewer(currentVersion: String, remoteVersion: String): Boolean {
        val current = parse(currentVersion, isLocalVersion = true) ?: return false
        val remote = parse(remoteVersion, isLocalVersion = false) ?: return false
        return compare(current, remote) < 0
    }

    private fun compare(left: ParsedVersion, right: ParsedVersion): Int {
        val coreSize = maxOf(left.core.size, right.core.size)
        for (index in 0 until coreSize) {
            val leftComponent = left.core.getOrNull(index) ?: NumericComponent.ZERO
            val rightComponent = right.core.getOrNull(index) ?: NumericComponent.ZERO
            val componentComparison = leftComponent.compareTo(rightComponent)
            if (componentComparison != 0) {
                return componentComparison
            }
        }

        if (left.preRelease.isEmpty() && right.preRelease.isNotEmpty()) {
            return 1
        }
        if (left.preRelease.isNotEmpty() && right.preRelease.isEmpty()) {
            return -1
        }

        val preReleaseSize = maxOf(left.preRelease.size, right.preRelease.size)
        for (index in 0 until preReleaseSize) {
            val leftIdentifier = left.preRelease.getOrNull(index)
            val rightIdentifier = right.preRelease.getOrNull(index)
            if (leftIdentifier == null) return -1
            if (rightIdentifier == null) return 1
            val identifierComparison = leftIdentifier.compareTo(rightIdentifier)
            if (identifierComparison != 0) {
                return identifierComparison
            }
        }
        // Build metadata does not affect semantic-version precedence.
        return 0
    }

    private fun parse(raw: String, isLocalVersion: Boolean): ParsedVersion? {
        var value = raw
        if (value.isEmpty()) return null

        if (value.first() == 'v') {
            value = value.substring(1)
        }
        if (value.isEmpty()) return null

        val buildSeparator = value.indexOf('+')
        val versionAndPreRelease = if (buildSeparator >= 0) {
            val buildMetadata = value.substring(buildSeparator + 1)
            if (!isValidMetadata(buildMetadata)) return null
            value.substring(0, buildSeparator)
        } else {
            value
        }

        val preReleaseSeparator = versionAndPreRelease.indexOf('-')
        val coreText: String
        var preReleaseText: String? = null
        if (preReleaseSeparator >= 0) {
            coreText = versionAndPreRelease.substring(0, preReleaseSeparator)
            preReleaseText = versionAndPreRelease.substring(preReleaseSeparator + 1)
            if (!isValidPreRelease(preReleaseText)) return null
        } else {
            coreText = versionAndPreRelease
        }

        val core = coreText.split('.').map { component ->
            parseNumericComponent(component) ?: return null
        }

        val preRelease = if (
            isLocalVersion && preReleaseText == DEBUG_SUFFIX
        ) {
            emptyList()
        } else {
            preReleaseText?.split('.')?.map(::parsePreReleaseIdentifier).orEmpty()
        }
        return ParsedVersion(core, preRelease)
    }

    private fun parseNumericComponent(component: String): NumericComponent? {
        if (component.isEmpty() || component.any { it !in '0'..'9' }) return null
        // Keep the accepted syntax unambiguous and semver-compatible.
        if (component.length > 1 && component.first() == '0') return null
        return NumericComponent(component)
    }

    private fun isValidMetadata(metadata: String): Boolean =
        metadata.isNotEmpty() && metadata.split('.').all(::isValidIdentifier)

    private fun isValidPreRelease(preRelease: String): Boolean =
        preRelease.isNotEmpty() && preRelease.split('.').all { identifier ->
            isValidIdentifier(identifier) &&
                !(identifier.all { it in '0'..'9' } &&
                    identifier.length > 1 && identifier.first() == '0')
        }

    private fun isValidIdentifier(identifier: String): Boolean =
        identifier.isNotEmpty() && identifier.all {
            it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' || it == '-'
        }

    private fun parsePreReleaseIdentifier(identifier: String): PreReleaseIdentifier =
        if (identifier.all { it in '0'..'9' }) {
            PreReleaseIdentifier.Numeric(NumericComponent(identifier))
        } else {
            PreReleaseIdentifier.Text(identifier)
        }

    private data class ParsedVersion(
        val core: List<NumericComponent>,
        val preRelease: List<PreReleaseIdentifier>
    )

    private data class NumericComponent(private val raw: String) : Comparable<NumericComponent> {
        private val normalized = raw.trimStart('0').ifEmpty { "0" }

        override fun compareTo(other: NumericComponent): Int =
            normalized.length.compareTo(other.normalized.length).takeIf { it != 0 }
                ?: normalized.compareTo(other.normalized)

        companion object {
            val ZERO = NumericComponent("0")
        }
    }

    private sealed class PreReleaseIdentifier : Comparable<PreReleaseIdentifier> {
        data class Numeric(val value: NumericComponent) : PreReleaseIdentifier()
        data class Text(val value: String) : PreReleaseIdentifier()

        override fun compareTo(other: PreReleaseIdentifier): Int = when {
            this is Numeric && other is Numeric -> value.compareTo(other.value)
            this is Numeric -> -1
            this is Text && other is Numeric -> 1
            else -> (this as Text).value.compareTo((other as Text).value)
        }
    }

    private const val DEBUG_SUFFIX = "debug"
}
