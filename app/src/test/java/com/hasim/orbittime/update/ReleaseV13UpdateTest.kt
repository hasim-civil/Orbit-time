package com.hasim.orbittime.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The v1.3 release as the updater will meet it: tagged `v1.3` (the old releases used `v.1.2`),
 * with the workflow's `OrbitTime-v1.3.apk` asset, against phones on 1.2 and on 1.3.
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
    fun `a phone on 1_2 is offered v1_3 with its APK`() = runTest {
        val result = checker.check(installedVersion = "1.2")
        assertTrue(result is UpdateCheck.Available)
        assertEquals("OrbitTime-v1.3.apk", (result as UpdateCheck.Available).apk.name)
    }

    @Test
    fun `a phone already on 1_3 is not offered anything`() = runTest {
        assertEquals(UpdateCheck.None, checker.check(installedVersion = "1.3"))
    }

    @Test
    fun `old and new tag styles compare by number`() {
        assertTrue(AppVersion.isNewer(installed = "1.2", latest = "v1.3"))
        assertFalse(AppVersion.isNewer(installed = "1.3", latest = "v1.3"))
        assertFalse(AppVersion.isNewer(installed = "1.3", latest = "v.1.2"))
        assertEquals("v1.3", displayVersion("1.3"))
    }

    @Test
    fun `Later mutes only the release it was tapped on`() {
        assertTrue(shouldPromptForRelease("v1.3", dismissedTag = null))
        assertFalse(shouldPromptForRelease("v1.3", dismissedTag = "v1.3"))
        assertFalse(shouldPromptForRelease("v1.3", dismissedTag = "v.1.3"))
        assertTrue(shouldPromptForRelease("v1.4", dismissedTag = "v1.3"))
    }
}
