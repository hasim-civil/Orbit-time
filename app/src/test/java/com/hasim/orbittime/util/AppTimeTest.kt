package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Profile → App Time setting: 12-hour conversion, the device/manual offset, and the way
 * [OrbitClock] hands that to the rest of the app. */
class AppTimeTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val date: LocalDate = LocalDate.of(2026, 9, 15)

    @After
    fun tearDown() {
        // Nothing in the app may be left running on a test clock.
        OrbitClock.useDeviceTime()
    }

    private fun at(hour: Int, minute: Int = 0, second: Int = 0): Instant =
        date.atTime(LocalTime.of(hour, minute, second)).atZone(zone).toInstant()

    // --- 12-hour clock, including the two that always get this wrong ---

    @Test
    fun `12 am is midnight and 12 pm is noon`() {
        assertEquals(0, AppTime.to24Hour(12, isAm = true))
        assertEquals(12, AppTime.to24Hour(12, isAm = false))
        assertEquals(LocalTime.MIDNIGHT, AppTime.localTimeOf(12, 0, 0, isAm = true))
        assertEquals(LocalTime.NOON, AppTime.localTimeOf(12, 0, 0, isAm = false))
    }

    @Test
    fun `morning and afternoon hours convert both ways`() {
        assertEquals(1, AppTime.to24Hour(1, isAm = true))
        assertEquals(11, AppTime.to24Hour(11, isAm = true))
        assertEquals(13, AppTime.to24Hour(1, isAm = false))
        assertEquals(23, AppTime.to24Hour(11, isAm = false))

        assertEquals(12, AppTime.hour12Of(LocalTime.of(0, 5)))
        assertEquals(12, AppTime.hour12Of(LocalTime.of(12, 5)))
        assertEquals(1, AppTime.hour12Of(LocalTime.of(13, 5)))
        assertEquals(11, AppTime.hour12Of(LocalTime.of(23, 5)))

        assertTrue(AppTime.isAm(LocalTime.of(0, 0)))
        assertTrue(AppTime.isAm(LocalTime.of(11, 59, 59)))
        assertFalse(AppTime.isAm(LocalTime.NOON))
        assertFalse(AppTime.isAm(LocalTime.of(23, 59, 59)))
    }

    @Test
    fun `a picked time keeps its seconds`() {
        assertEquals(LocalTime.of(21, 5, 47), AppTime.localTimeOf(9, 5, 47, isAm = false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an hour outside the 12-hour dial is rejected`() {
        AppTime.to24Hour(13, isAm = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an out-of-range minute is rejected`() {
        AppTime.localTimeOf(9, 60, 0, isAm = true)
    }

    // --- device vs manual ---

    @Test
    fun `device mode is exactly the device clock`() {
        val deviceNow = at(14, 30)

        assertEquals(deviceNow, AppTime.instant(AppTimeSettings(), deviceNow))
    }

    @Test
    fun `a manual time reads back as the time that was set`() {
        val deviceNow = at(14, 30, 12)
        val settings = AppTime.manualSettings(LocalTime.of(9, 0, 0), deviceNow, zone)

        assertEquals(AppTimeMode.MANUAL, settings.mode)
        assertEquals(LocalTime.of(9, 0), settings.customTime)
        assertEquals(LocalTime.of(9, 0), AppTime.instant(settings, deviceNow).atZone(zone).toLocalTime())
    }

    @Test
    fun `a manual clock keeps ticking with the device`() {
        val deviceNow = at(14, 0)
        val settings = AppTime.manualSettings(LocalTime.of(9, 0), deviceNow, zone)

        // Half an hour of real time later, the app clock has advanced by the same half hour.
        val later = AppTime.instant(settings, deviceNow.plus(Duration.ofMinutes(30)))
        assertEquals(LocalTime.of(9, 30), later.atZone(zone).toLocalTime())
    }

    @Test
    fun `a manual time set to 12 am and 12 pm lands on the right half of the day`() {
        val deviceNow = at(14, 30)

        val midnight = AppTime.manualSettings(AppTime.localTimeOf(12, 0, 0, isAm = true), deviceNow, zone)
        assertEquals(LocalTime.MIDNIGHT, AppTime.instant(midnight, deviceNow).atZone(zone).toLocalTime())
        // Midnight *today*, so the app's date does not jump forward a day.
        assertEquals(date, AppTime.instant(midnight, deviceNow).atZone(zone).toLocalDate())

        val noon = AppTime.manualSettings(AppTime.localTimeOf(12, 0, 0, isAm = false), deviceNow, zone)
        assertEquals(LocalTime.NOON, AppTime.instant(noon, deviceNow).atZone(zone).toLocalTime())
        assertEquals(date, AppTime.instant(noon, deviceNow).atZone(zone).toLocalDate())
    }

    @Test
    fun `setting a time earlier in the day moves the clock back, not forward a day`() {
        val deviceNow = at(23, 45)
        val settings = AppTime.manualSettings(LocalTime.of(1, 15), deviceNow, zone)

        val appNow = AppTime.instant(settings, deviceNow).atZone(zone)
        assertEquals(LocalTime.of(1, 15), appNow.toLocalTime())
        assertEquals(date, appNow.toLocalDate())
        assertTrue(settings.offsetSeconds < 0)
    }

    @Test
    fun `sub-second device precision is preserved so the clock never stalls`() {
        val deviceNow = at(14, 30).plusMillis(250)
        val settings = AppTime.manualSettings(LocalTime.of(9, 0), deviceNow, zone)

        val appNow = AppTime.instant(settings, deviceNow)
        assertEquals(LocalTime.of(9, 0, 0), appNow.atZone(zone).toLocalTime().withNano(0))
    }

    // --- what the rest of the app reads ---

    @Test
    fun `OrbitClock serves whatever source is installed`() {
        val fixed = FixedSource(at(21, 45, 30), zone)
        OrbitClock.install(fixed)

        assertEquals(fixed.instant, OrbitClock.now())
        assertEquals(LocalTime.of(21, 45, 30), OrbitClock.localTime())
        assertEquals(date, OrbitClock.today())
        assertEquals(date, AttendanceTimeFormat.today())
        // Only the numeric part is asserted: the am/pm marker itself is locale-dependent.
        assertTrue(AttendanceTimeFormat.clockTime(OrbitClock.now()).startsWith("9:45"))
        assertTrue(AttendanceTimeFormat.clockTimeWithSeconds(OrbitClock.now()).startsWith("9:45:30"))
        assertEquals("Good evening", AttendanceTimeFormat.greeting())
    }

    @Test
    fun `OrbitClock falls back to device time when nothing is installed`() {
        OrbitClock.useDeviceTime()

        assertEquals(AppTimeMode.DEVICE, OrbitClock.settings.mode)
        // Device time, so within a second of the real clock.
        val drift = Duration.between(Instant.now(), OrbitClock.now()).seconds
        assertTrue("drifted by $drift s", drift in -1L..1L)
    }

    @Test
    fun `a manual source drives the app's idea of today`() {
        val deviceNow = at(2, 0)
        // 11:30 pm the previous evening, applied on the device's own day.
        val settings = AppTime.manualSettings(LocalTime.of(23, 30), deviceNow, zone)
        OrbitClock.install(OffsetSource(settings, deviceNow, zone))

        assertEquals(LocalTime.of(23, 30), OrbitClock.localTime())
        assertEquals(date, OrbitClock.today())
    }

    private class FixedSource(val instant: Instant, override val zone: ZoneId) : AppTimeSource {
        override val settings: AppTimeSettings = AppTimeSettings()
        override fun now(): Instant = instant
    }

    private class OffsetSource(
        override val settings: AppTimeSettings,
        private val deviceNow: Instant,
        override val zone: ZoneId,
    ) : AppTimeSource {
        override fun now(): Instant = AppTime.instant(settings, deviceNow)
    }
}
