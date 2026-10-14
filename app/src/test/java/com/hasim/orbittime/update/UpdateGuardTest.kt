package com.hasim.orbittime.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The check on the downloaded APK itself. The motivating case is the first v.1.3 release, whose
 * APK was really a 1.2 build (versionCode 2): installing it over 1.2 changed nothing and the app
 * offered the same update again on the next launch.
 */
class UpdateGuardTest {

    private val installed12 = PackageVersion("com.hasim.orbittime", versionCode = 2, versionName = "1.2")

    @Test
    fun `a genuine 1_3 build installs over 1_2`() {
        val downloaded = PackageVersion("com.hasim.orbittime", versionCode = 3, versionName = "1.3")
        assertNull(UpdateGuard.problem(installed12, downloaded))
    }

    @Test
    fun `a 1_2 build published under a 1_3 tag is refused, naming both versions`() {
        val mislabelled = PackageVersion("com.hasim.orbittime", versionCode = 2, versionName = "1.2")
        assertEquals(
            "The downloaded update is v1.2, which isn't newer than the installed v1.2. Please try again later.",
            UpdateGuard.problem(installed12, mislabelled),
        )
    }

    @Test
    fun `an older build is refused`() {
        val older = PackageVersion("com.hasim.orbittime", versionCode = 1, versionName = "1.1")
        assertNotNull(UpdateGuard.problem(installed12, older))
    }

    @Test
    fun `versionCode decides, not versionName`() {
        // A higher name with a stale code is still not an installable upgrade.
        val staleCode = PackageVersion("com.hasim.orbittime", versionCode = 2, versionName = "1.3")
        assertNotNull(UpdateGuard.problem(installed12, staleCode))
    }

    @Test
    fun `another app's APK is refused`() {
        val other = PackageVersion("com.example.other", versionCode = 99, versionName = "9.9")
        assertEquals("The downloaded file isn't an Orbit Time update.", UpdateGuard.problem(installed12, other))
    }

    @Test
    fun `a file Android can't read as an APK is refused`() {
        assertEquals(
            "The downloaded update couldn't be read. Please try again.",
            UpdateGuard.problem(installed12, downloaded = null),
        )
    }

    @Test
    fun `a missing versionName falls back to the build number in the message`() {
        val unnamed = PackageVersion("com.hasim.orbittime", versionCode = 2, versionName = null)
        assertEquals(
            "The downloaded update is build 2, which isn't newer than the installed v1.2. Please try again later.",
            UpdateGuard.problem(installed12, unnamed),
        )
    }
}
