package com.hasim.orbittime.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Version ordering. The case that motivates all of this is `v1.9` vs `v1.10`: string ordering
 * puts them the wrong way round, which would strand every user on the ninth release.
 */
class AppVersionTest {

    @Test
    fun `ten is newer than nine despite sorting lower as text`() {
        // Text ordering really does put them the wrong way round — this is the bug being guarded against.
        assertTrue("v1.10" < "v1.9")
        assertTrue(AppVersion.isNewer(installed = "1.9", latest = "v1.10"))
        assertFalse(AppVersion.isNewer(installed = "1.10", latest = "v1.9"))
    }

    @Test
    fun `accepts every tag shape this project uses`() {
        // The release workflow tags v.1.1; versionName is the bare 1.1; v1.1 is also read.
        assertTrue(AppVersion.isNewer(installed = "1.1", latest = "v.1.2"))
        assertTrue(AppVersion.isNewer(installed = "1.1", latest = "v1.2"))
        assertTrue(AppVersion.isNewer(installed = "1.1", latest = "1.2"))
        assertFalse(AppVersion.isNewer(installed = "1.2", latest = "v.1.2"))
    }

    @Test
    fun `identical versions are not an update`() {
        assertFalse(AppVersion.isNewer(installed = "1.1", latest = "v.1.1"))
        assertFalse(AppVersion.isNewer(installed = "v.1.1", latest = "v.1.1"))
    }

    @Test
    fun `missing trailing components count as zero`() {
        assertEquals(AppVersion.parse("1.2"), AppVersion.parse("1.2.0"))
        assertFalse(AppVersion.isNewer(installed = "1.2", latest = "1.2.0"))
        assertTrue(AppVersion.isNewer(installed = "1.2", latest = "1.2.1"))
    }

    @Test
    fun `compares major before minor`() {
        assertTrue(AppVersion.isNewer(installed = "1.99", latest = "v.2.0"))
        assertFalse(AppVersion.isNewer(installed = "2.0", latest = "v.1.99"))
        assertTrue(AppVersion.isNewer(installed = "1.9.9", latest = "v.1.10.0"))
    }

    @Test
    fun `double digit minors order numerically among themselves`() {
        assertTrue(AppVersion.isNewer(installed = "1.10", latest = "v.1.11"))
        assertTrue(AppVersion.isNewer(installed = "1.11", latest = "v.1.100"))
        assertFalse(AppVersion.isNewer(installed = "1.100", latest = "v.1.11"))
    }

    @Test
    fun `suffixes are ignored rather than mis-parsed`() {
        assertEquals(AppVersion.parse("1.2"), AppVersion.parse("v1.2-beta3"))
        assertFalse(AppVersion.isNewer(installed = "1.2", latest = "v1.2-beta3"))
    }

    @Test
    fun `unparseable versions never trigger an update`() {
        assertFalse(AppVersion.parse("").isValid)
        assertFalse(AppVersion.parse(null).isValid)
        assertFalse(AppVersion.parse("nightly").isValid)
        assertFalse(AppVersion.isNewer(installed = "1.1", latest = "nightly"))
        assertFalse(AppVersion.isNewer(installed = "1.1", latest = null))
        assertFalse(AppVersion.isNewer(installed = null, latest = "v.1.9"))
        assertFalse(AppVersion.isNewer(installed = "", latest = "v.1.9"))
    }

    @Test
    fun `renders the bare numeric version`() {
        assertEquals("1.10", AppVersion.parse("v.1.10").toString())
        assertEquals("v1.10", displayVersion("v.1.10"))
        assertEquals("v1.1", displayVersion("1.1"))
        assertEquals("nightly", displayVersion("nightly"))
    }
}
