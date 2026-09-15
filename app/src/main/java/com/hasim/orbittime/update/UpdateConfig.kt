package com.hasim.orbittime.update

/**
 * The single place that knows where Orbit Time's updates come from.
 *
 * Everything about the distribution channel — which public GitHub repository holds the
 * releases, and how this project names its APK assets — lives here, so changing the
 * repository or the asset naming never means touching the checker, the downloader or the UI.
 *
 * The naming convention is the one the release workflow already uses
 * (`.github/workflows/build-debug-apk.yml`): a release tagged `v.1.1` publishes
 * `OrbitTime-v.1.1.apk`. Nothing here is a secret — the repository is public and the
 * endpoints below are read without any token or sign-in, which is deliberate: an APK is
 * decompilable, so a credential shipped inside one is a published credential.
 */
object UpdateConfig {

    const val OWNER: String = "hasim-civil"
    const val REPO: String = "Orbit-time"

    /** Prefix every published APK asset carries, e.g. `OrbitTime-v.1.1.apk`. */
    const val APK_ASSET_PREFIX: String = "OrbitTime-"
    const val APK_EXTENSION: String = ".apk"
    const val APK_CONTENT_TYPE: String = "application/vnd.android.package-archive"

    /**
     * GitHub's "latest release" endpoint. It deliberately excludes drafts and prereleases,
     * so the `beta-NN` builds this project publishes on every branch push are never offered
     * as an update — only the real `v.x.y` releases are. HTTPS only.
     */
    val latestReleaseUrl: String
        get() = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    /** Sent on every request; GitHub asks unauthenticated clients to identify themselves. */
    const val USER_AGENT: String = "OrbitTime-Android"
    const val ACCEPT_HEADER: String = "application/vnd.github+json"

    /** File name the downloaded APK is saved under before the installer is handed the file. */
    fun downloadFileName(versionTag: String): String =
        APK_ASSET_PREFIX + versionTag.ifBlank { "latest" } + APK_EXTENSION

    /**
     * Picks the installable asset out of a release.
     *
     * Prefers the name this project's workflow produces for that exact tag, then any APK by
     * content type or extension — so a release whose asset was renamed still updates rather
     * than silently offering nothing.
     */
    fun selectApkAsset(assets: List<ReleaseAsset>, tag: String): ReleaseAsset? {
        val expected = APK_ASSET_PREFIX + tag + APK_EXTENSION
        return assets.firstOrNull { it.name.equals(expected, ignoreCase = true) }
            ?: assets.firstOrNull { it.contentType.equals(APK_CONTENT_TYPE, ignoreCase = true) }
            ?: assets.firstOrNull { it.name.endsWith(APK_EXTENSION, ignoreCase = true) }
    }
}
