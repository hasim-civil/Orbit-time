package com.hasim.orbittime.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What a completed update check found. Every failure mode collapses to [None]: the app is
 * fully usable without GitHub, so an unreachable network, a rate-limited API, a malformed
 * response or a release with no APK all mean the same thing to the caller — show nothing.
 */
sealed interface UpdateCheck {
    /** Nothing to offer: already current, unreachable, or unreadable release data. */
    data object None : UpdateCheck

    /** A strictly newer release exists and carries an installable APK. */
    data class Available(val release: GithubRelease, val apk: ReleaseAsset) : UpdateCheck
}

/** Fetches the raw `releases/latest` body. Separated so tests can supply one without a socket. */
fun interface ReleaseSource {
    /** Returns the response body, or throws — a throw is treated as "no update". */
    suspend fun fetchLatestRelease(): String
}

/**
 * Decides whether to offer an update.
 *
 * Deliberately total: [check] never throws, so a caller can run it on startup without a
 * try/catch and without any chance of a failed check affecting the rest of the app.
 */
class UpdateChecker(
    private val source: ReleaseSource = HttpReleaseSource(),
) {

    suspend fun check(installedVersion: String?): UpdateCheck = withContext(Dispatchers.IO) {
        val body = try {
            source.fetchLatestRelease()
        } catch (cancellation: CancellationException) {
            // The check is tied to the screen's lifecycle; a cancelled check is not a failure
            // to report, and swallowing it here would break structured concurrency.
            throw cancellation
        } catch (error: Exception) {
            return@withContext UpdateCheck.None
        }

        val release = ReleaseParser.parse(body) ?: return@withContext UpdateCheck.None
        if (!AppVersion.isNewer(installed = installedVersion, latest = release.tag)) {
            return@withContext UpdateCheck.None
        }

        // A newer tag with no APK attached (an upload that failed, or a notes-only release)
        // can't be installed, so it is not offered — the alternative would be an "Update now"
        // button with nothing to download behind it.
        val apk = release.apkAsset ?: return@withContext UpdateCheck.None
        UpdateCheck.Available(release = release, apk = apk)
    }
}
