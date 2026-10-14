package com.hasim.orbittime.update

/** An app package's identity and version, as Android itself reads them from the manifest. */
data class PackageVersion(
    val packageName: String,
    val versionCode: Long,
    val versionName: String?,
)

/**
 * Last check before a downloaded APK is handed to the system installer: is it really a newer
 * build of this app?
 *
 * The release tag only says which version a release *claims* to carry; the APK's manifest says
 * what it *is*. The first v.1.3 release shows why both matter: its APK was a 1.2 build, so the
 * system installer "updated" 1.2 to 1.2, the app still reported 1.2, and the same update was
 * offered again on the next launch, indefinitely. Comparing the downloaded file's own package
 * and versionCode with the installed app turns that loop into one clear message instead.
 *
 * Kept free of Android types so it is unit-testable; [ApkInstaller] reads both sides.
 */
object UpdateGuard {

    /**
     * @param downloaded what the downloaded APK's manifest says, or null when Android couldn't
     *   read it as an APK at all.
     * @return null when [downloaded] is a genuine upgrade of [installed], otherwise a message
     *   to show the user.
     */
    fun problem(installed: PackageVersion, downloaded: PackageVersion?): String? = when {
        downloaded == null ->
            "The downloaded update couldn't be read. Please try again."
        downloaded.packageName != installed.packageName ->
            "The downloaded file isn't an Orbit Time update."
        // Equal counts too: Android would reinstall the same build and change nothing.
        downloaded.versionCode <= installed.versionCode ->
            "The downloaded update is ${label(downloaded)}, which isn't newer than the installed " +
                "${label(installed)}. Please try again later."
        else -> null
    }

    private fun label(version: PackageVersion): String =
        version.versionName?.takeIf { it.isNotBlank() }?.let { displayVersion(it) }
            ?: "build ${version.versionCode}"
}
