package com.hasim.orbittime.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The v1.3 release as the updater meets it: tagged `v1.3` (releases up to 1.2 used `v.1.2`),
 * carrying the workflow's `OrbitTime-v1.3.apk`, checked from a phone on 1.2 and one on 1.3.
 */
class ReleaseV13UpdateTest {

    private val v13Release = """
        {
          "tag_name": "v1.3",
          "name": "Orbit Time v1.3",
          "body": "What's new",
          "assets": [{"name":"OrbitTime-v1.3.apk","content_type":"application/vnd.android.package-archive",
            "browser_download_url":"https://github.com/hasim-civil/Orbit-time/releases/download/v1.3/OrbitTime-v1.3.apk","size":1}]
        }
    """.trimIndent()

    private val checker = UpdateChecker(ReleaseSource { v13Release })

    @Test
    fun `a phone on 1_2 is offered v1_3 and downloads that release's own APK`() = runTest {
        val result = checker.check(installedVersion = "1.2")

        assertTrue(result is UpdateCheck.Available)
        val apk = (result as UpdateCheck.Available).apk
        assertEquals("OrbitTime-v1.3.apk", apk.name)
        assertEquals("https://github.com/hasim-civil/Orbit-time/releases/download/v1.3/OrbitTime-v1.3.apk", apk.downloadUrl)
        assertEquals("v1.3", displayVersion(result.release.tag))
    }

    @Test
    fun `once 1_3 is installed nothing is offered`() = runTest {
        assertEquals(UpdateCheck.None, checker.check(installedVersion = "1.3"))
    }

    @Test
    fun `old and new tag styles compare by number`() {
        assertTrue(AppVersion.isNewer(installed = "1.2", latest = "v1.3"))
        assertFalse(AppVersion.isNewer(installed = "1.3", latest = "v1.3"))
        // The mislabelled first v.1.3 release names the same version, so it never re-prompts 1.3.
        assertFalse(AppVersion.isNewer(installed = "1.3", latest = "v.1.3"))
        assertFalse(AppVersion.isNewer(installed = "1.3", latest = "v.1.2"))
    }
}
