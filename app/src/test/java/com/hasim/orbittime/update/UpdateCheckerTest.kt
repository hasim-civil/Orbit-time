package com.hasim.orbittime.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The update check end to end, minus the socket: a fake [ReleaseSource] stands in for GitHub so
 * every branch — including the ones that only happen on a bad day — is exercised deterministically.
 */
class UpdateCheckerTest {

    private fun release(
        tag: String = "v.1.2",
        name: String = "Orbit Time v1.2",
        body: String = "Faster timesheets.",
        assets: String = """[${apkAsset("OrbitTime-v.1.2.apk")}]""",
    ) = """
        {
          "tag_name": "$tag",
          "name": "$name",
          "body": "$body",
          "assets": $assets
        }
    """.trimIndent()

    private fun apkAsset(
        name: String,
        contentType: String = "application/vnd.android.package-archive",
        url: String = "https://github.com/hasim-civil/Orbit-time/releases/download/v.1.2/$name",
        size: Long = 17_340_087L,
    ) = """{"name":"$name","content_type":"$contentType","browser_download_url":"$url","size":$size}"""

    private fun checkerReturning(body: String) = UpdateChecker(ReleaseSource { body })

    // --- update available -------------------------------------------------------------

    @Test
    fun `newer release with an APK is offered, with its notes and direct asset URL`() = runTest {
        val result = checkerReturning(release()).check(installedVersion = "1.1")

        assertTrue(result is UpdateCheck.Available)
        val available = result as UpdateCheck.Available
        assertEquals("v.1.2", available.release.tag)
        assertEquals("Orbit Time v1.2", available.release.displayName)
        assertEquals("Faster timesheets.", available.release.notes)
        // The APK is downloaded straight from the release asset — never a repository or
        // release web page, and never a URL the user has to choose between.
        assertEquals(
            "https://github.com/hasim-civil/Orbit-time/releases/download/v.1.2/OrbitTime-v.1.2.apk",
            available.apk.downloadUrl,
        )
        assertTrue(available.apk.downloadUrl.startsWith("https://"))
        assertEquals(17_340_087L, available.apk.sizeBytes)
    }

    @Test
    fun `v1_9 installed is offered v1_10`() = runTest {
        val body = release(tag = "v.1.10", assets = "[${apkAsset("OrbitTime-v.1.10.apk")}]")
        val result = checkerReturning(body).check(installedVersion = "1.9")

        assertTrue(result is UpdateCheck.Available)
        assertEquals("v.1.10", (result as UpdateCheck.Available).release.tag)
    }

    @Test
    fun `release notes are optional`() = runTest {
        val body = release(body = "")
        val result = checkerReturning(body).check(installedVersion = "1.1")

        assertTrue(result is UpdateCheck.Available)
        assertEquals("", (result as UpdateCheck.Available).release.notes)
    }

    // --- no update --------------------------------------------------------------------

    @Test
    fun `same version shows nothing`() = runTest {
        val body = release(tag = "v.1.1", assets = "[${apkAsset("OrbitTime-v.1.1.apk")}]")
        assertEquals(UpdateCheck.None, checkerReturning(body).check(installedVersion = "1.1"))
    }

    @Test
    fun `older release shows nothing`() = runTest {
        val body = release(tag = "v.1.1", assets = "[${apkAsset("OrbitTime-v.1.1.apk")}]")
        assertEquals(UpdateCheck.None, checkerReturning(body).check(installedVersion = "1.2"))
    }

    // --- invalid release data ---------------------------------------------------------

    @Test
    fun `malformed JSON shows nothing instead of throwing`() = runTest {
        assertEquals(UpdateCheck.None, checkerReturning("{ not json").check(installedVersion = "1.1"))
        assertEquals(UpdateCheck.None, checkerReturning("").check(installedVersion = "1.1"))
        assertEquals(UpdateCheck.None, checkerReturning("[]").check(installedVersion = "1.1"))
    }

    @Test
    fun `release with no tag shows nothing`() = runTest {
        val body = """{"name":"Orbit Time","body":"","assets":[${apkAsset("OrbitTime.apk")}]}"""
        assertEquals(UpdateCheck.None, checkerReturning(body).check(installedVersion = "1.1"))
    }

    @Test
    fun `unparseable tag shows nothing`() = runTest {
        val body = release(tag = "nightly", assets = "[${apkAsset("OrbitTime-nightly.apk")}]")
        assertEquals(UpdateCheck.None, checkerReturning(body).check(installedVersion = "1.1"))
    }

    @Test
    fun `asset without a download URL is ignored`() = runTest {
        val body = release(assets = """[{"name":"OrbitTime-v.1.2.apk","content_type":"application/vnd.android.package-archive"}]""")
        assertEquals(UpdateCheck.None, checkerReturning(body).check(installedVersion = "1.1"))
    }

    @Test
    fun `non-HTTPS asset URL is refused`() = runTest {
        val insecure = apkAsset("OrbitTime-v.1.2.apk", url = "http://github.com/hasim-civil/Orbit-time/x.apk")
        assertEquals(UpdateCheck.None, checkerReturning(release(assets = "[$insecure]")).check(installedVersion = "1.1"))
    }

    // --- missing APK ------------------------------------------------------------------

    @Test
    fun `newer release with no assets at all shows nothing`() = runTest {
        assertEquals(UpdateCheck.None, checkerReturning(release(assets = "[]")).check(installedVersion = "1.1"))
    }

    @Test
    fun `newer release with only non-APK assets shows nothing`() = runTest {
        val notes = """{"name":"release-notes.md","content_type":"text/markdown","browser_download_url":"https://example.com/n.md","size":12}"""
        assertEquals(UpdateCheck.None, checkerReturning(release(assets = "[$notes]")).check(installedVersion = "1.1"))
    }

    @Test
    fun `a renamed APK asset is still found`() = runTest {
        // Falls back from the exact OrbitTime-<tag>.apk name to content type, then extension.
        val renamed = apkAsset("orbit-time-release.apk", contentType = "application/octet-stream")
        val result = checkerReturning(release(assets = "[$renamed]")).check(installedVersion = "1.1")

        assertTrue(result is UpdateCheck.Available)
        assertEquals("orbit-time-release.apk", (result as UpdateCheck.Available).apk.name)
    }

    @Test
    fun `the tagged APK wins when a release carries several files`() = runTest {
        val body = release(
            assets = "[" + listOf(
                """{"name":"mapping.txt","content_type":"text/plain","browser_download_url":"https://example.com/mapping.txt","size":5}""",
                apkAsset("OrbitTime-v.1.2.apk"),
                apkAsset("OrbitTime-Beta-99.apk"),
            ).joinToString(",") + "]",
        )
        val result = checkerReturning(body).check(installedVersion = "1.1")

        assertEquals("OrbitTime-v.1.2.apk", (result as UpdateCheck.Available).apk.name)
    }

    // --- network failure --------------------------------------------------------------

    @Test
    fun `network failure shows nothing and never escapes as an exception`() = runTest {
        val offline = UpdateChecker(ReleaseSource { throw IOException("No route to host") })
        assertEquals(UpdateCheck.None, offline.check(installedVersion = "1.1"))
    }

    @Test
    fun `an HTTP error from GitHub shows nothing`() = runTest {
        val rateLimited = UpdateChecker(ReleaseSource { throw IOException("GitHub returned HTTP 403") })
        assertEquals(UpdateCheck.None, rateLimited.check(installedVersion = "1.1"))
    }

    @Test
    fun `an unknown installed version never triggers an update`() = runTest {
        assertEquals(UpdateCheck.None, checkerReturning(release()).check(installedVersion = null))
    }

    // --- asset selection and parsing directly -----------------------------------------

    @Test
    fun `parser returns null rather than throwing for unusable bodies`() {
        assertNull(ReleaseParser.parse(null))
        assertNull(ReleaseParser.parse(""))
        assertNull(ReleaseParser.parse("<html>rate limited</html>"))
        assertNotNull(ReleaseParser.parse(release()))
    }

    @Test
    fun `display name falls back to the tag`() {
        val parsed = ReleaseParser.parse(release(name = ""))
        assertEquals("v.1.2", parsed?.displayName)
    }

    @Test
    fun `selectApkAsset prefers this project's naming convention`() {
        val expected = ReleaseAsset("OrbitTime-v.1.2.apk", "https://e/1.apk", "application/octet-stream", 1)
        val other = ReleaseAsset("something-else.apk", "https://e/2.apk", UpdateConfig.APK_CONTENT_TYPE, 2)

        assertEquals(expected, UpdateConfig.selectApkAsset(listOf(other, expected), "v.1.2"))
        assertNull(UpdateConfig.selectApkAsset(emptyList(), "v.1.2"))
    }

    @Test
    fun `the latest release endpoint is HTTPS and points at the configured repository`() {
        assertEquals(
            "https://api.github.com/repos/hasim-civil/Orbit-time/releases/latest",
            UpdateConfig.latestReleaseUrl,
        )
        assertTrue(UpdateConfig.latestReleaseUrl.startsWith("https://"))
        assertEquals("OrbitTime-v.1.2.apk", UpdateConfig.downloadFileName("v.1.2"))
    }
}
