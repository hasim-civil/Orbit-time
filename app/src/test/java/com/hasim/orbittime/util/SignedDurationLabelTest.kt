package com.hasim.orbittime.util

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

/** How the Home overtime card reads: hours *and* minutes, with the sign carried. */
class SignedDurationLabelTest {

    @Test
    fun `positive balances show hours and minutes`() {
        assertEquals("+2h 30m", AttendanceTimeFormat.signedDurationLabel(Duration.ofMinutes(150)))
        assertEquals("+8h 15m", AttendanceTimeFormat.signedDurationLabel(Duration.ofMinutes(495)))
        assertEquals("+1h", AttendanceTimeFormat.signedDurationLabel(Duration.ofHours(1)))
        assertEquals("+45m", AttendanceTimeFormat.signedDurationLabel(Duration.ofMinutes(45)))
    }

    @Test
    fun `negative balances show the deficit, with a real minus sign`() {
        assertEquals("−45m", AttendanceTimeFormat.signedDurationLabel(Duration.ofMinutes(-45)))
        assertEquals("−2h 30m", AttendanceTimeFormat.signedDurationLabel(Duration.ofMinutes(-150)))
        assertEquals("−8h", AttendanceTimeFormat.signedDurationLabel(Duration.ofHours(-8)))
    }

    @Test
    fun `zero is plain`() {
        assertEquals("0h", AttendanceTimeFormat.signedDurationLabel(Duration.ZERO))
        // Under a minute either way is not worth a sign.
        assertEquals("0h", AttendanceTimeFormat.signedDurationLabel(Duration.ofSeconds(30)))
        assertEquals("0h", AttendanceTimeFormat.signedDurationLabel(Duration.ofSeconds(-30)))
    }

    @Test
    fun `seconds are truncated towards zero, never rounded up`() {
        assertEquals("+1h 30m", AttendanceTimeFormat.signedDurationLabel(Duration.ofMinutes(90).plusSeconds(59)))
        assertEquals("−1h 30m", AttendanceTimeFormat.signedDurationLabel(Duration.ofMinutes(-90).minusSeconds(59)))
    }

    @Test
    fun `the minute-count overload matches the duration one`() {
        assertEquals("+2h 30m", AttendanceTimeFormat.signedMinutesLabel(150))
        assertEquals("−45m", AttendanceTimeFormat.signedMinutesLabel(-45))
        assertEquals("0h", AttendanceTimeFormat.signedMinutesLabel(0))
    }
}
