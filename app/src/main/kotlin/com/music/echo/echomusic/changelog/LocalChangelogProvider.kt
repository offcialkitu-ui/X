package iad1tya.echo.music.echomusic.changelog

import iad1tya.echo.music.BuildConfig

/**
 * Provides local changelog data for the current app version.
 * Add changelog sections here for offline/instant display
 * without needing to fetch from GitHub.
 */
object LocalChangelogProvider {

    /**
     * Returns changelog sections for a given version tag.
     * Override this to provide local changelog content
     * without relying on GitHub release assets.
     */
    fun getChangelogForVersion(versionTag: String): List<ChangelogSection> {
        // Add version-specific changelog entries here.
        // Example:
        // if (versionTag == "v${BuildConfig.VERSION_NAME}") {
        //     return listOf(
        //         ChangelogSection("✨ New Features", listOf("Feature 1", "Feature 2")),
        //         ChangelogSection("🛠️ Improvements", listOf("Improvement 1")),
        //     )
        // }
        return emptyList()
    }

    /**
     * Whether local changelog is available for the current version.
     */
    fun hasChangelogForCurrentVersion(): Boolean =
        getChangelogForVersion("v${BuildConfig.VERSION_NAME}").isNotEmpty()
}
