package com.hasim.orbittime.update

import org.json.JSONException
import org.json.JSONObject

/** One downloadable file attached to a GitHub release. */
data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val contentType: String,
    val sizeBytes: Long,
)

/**
 * The published release this app would update to, reduced to the four things the update flow
 * actually needs: what it's called, what changed, and where the APK is.
 */
data class GithubRelease(
    val tag: String,
    val displayName: String,
    val notes: String,
    val apkAsset: ReleaseAsset?,
) {
    /** True once there is something installable — a release with no APK can't be offered. */
    val hasApk: Boolean get() = apkAsset != null
}

/**
 * Turns GitHub's `releases/latest` JSON into a [GithubRelease].
 *
 * Kept separate from the network call so the awkward half — a truncated body, a release with
 * no assets, a field that isn't the type it should be — is testable without a socket, and so a
 * malformed response can only ever produce `null` (no update offered) rather than an exception
 * reaching the UI.
 */
object ReleaseParser {

    fun parse(json: String?): GithubRelease? {
        if (json.isNullOrBlank()) return null

        val root = try {
            JSONObject(json)
        } catch (error: JSONException) {
            return null
        }

        // A release with no tag has no version to compare against, so there is nothing
        // meaningful to offer even if it carries an APK.
        val tag = root.optString("tag_name").trim()
        if (tag.isEmpty()) return null

        val assets = mutableListOf<ReleaseAsset>()
        val assetsJson = root.optJSONArray("assets")
        if (assetsJson != null) {
            for (index in 0 until assetsJson.length()) {
                val asset = assetsJson.optJSONObject(index) ?: continue
                val url = asset.optString("browser_download_url").trim()
                val name = asset.optString("name").trim()
                // An asset GitHub hasn't finished uploading has no usable URL; skipping it
                // lets a later asset in the same release still be found.
                if (url.isEmpty() || name.isEmpty()) continue
                if (!url.startsWith("https://")) continue
                assets += ReleaseAsset(
                    name = name,
                    downloadUrl = url,
                    contentType = asset.optString("content_type").trim(),
                    sizeBytes = asset.optLong("size", 0L),
                )
            }
        }

        val name = root.optString("name").trim()
        return GithubRelease(
            tag = tag,
            displayName = name.ifEmpty { tag },
            notes = root.optString("body").trim(),
            apkAsset = UpdateConfig.selectApkAsset(assets, tag),
        )
    }
}
