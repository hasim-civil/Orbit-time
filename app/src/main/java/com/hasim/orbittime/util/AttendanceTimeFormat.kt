package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** Pure date/time formatting for the attendance screens — no Android or Firebase types. */
object AttendanceTimeFormat {

    /** A real minus sign (U+2212), not a hyphen: it matches the "+" in width and reads as a
     * sign rather than as punctuation at the small sizes these labels are shown at. */
    private const val MINUS_SIGN = "\u2212"

    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    private val shortDayLabelFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val clockWithSecondsFormatter = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault())
    private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val weekStartFormatter = DateTimeFormatter.ofPattern("d", Locale.getDefault())
    private val weekEndFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    /** Today according to *the app's* clock ([OrbitClock]), so a manual App Time override moves
     * every screen's idea of "today" together instead of only some of them. */
    fun today(zone: ZoneId = OrbitClock.zone): LocalDate = OrbitClock.today(zone)

    /** Firestore document id / stable per-day key, e.g. "2026-08-26". */
    fun dateKey(date: LocalDate): String = date.toString()

    /** "MONDAY 24 AUGUST" style label for the punch hero card. */
    fun dayLabel(date: LocalDate): String = dayLabelFormatter.format(date).uppercase(Locale.getDefault())

    /** "Fri 21 Aug" style label, used for notification copy. */
    fun shortDayLabel(date: LocalDate): String = shortDayLabelFormatter.format(date)

    /** "9:02 am" style clock format. */
    fun clockTime(instant: Instant, zone: ZoneId = OrbitClock.zone): String =
        clockFormatter.format(instant.atZone(zone)).lowercase(Locale.getDefault())

    /** "9:02:47 am" — used where seconds matter, i.e. the App Time setting's own live clock. */
    fun clockTimeWithSeconds(instant: Instant, zone: ZoneId = OrbitClock.zone): String =
        clockWithSecondsFormatter.format(instant.atZone(zone)).lowercase(Locale.getDefault())

    /** "9:02:47 am" for a bare time-of-day — the custom time the user picked. */
    fun clockTimeWithSeconds(time: LocalTime): String =
        clockWithSecondsFormatter.format(time).lowercase(Locale.getDefault())

    /** "5h 43m" style duration, always showing both units for readability. */
    fun elapsedLabel(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    fun greeting(time: LocalTime = OrbitClock.localTime()): String = when {
        time.isBefore(LocalTime.NOON) -> "Good morning"
        time.isBefore(LocalTime.of(17, 0)) -> "Good afternoon"
        else -> "Good evening"
    }

    fun dayOfWeekAndDate(date: LocalDate): String =
        "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
            "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"

    /** "August 2026" style label for the Monthly Attendance card. */
    fun monthLabel(date: LocalDate): String = monthLabelFormatter.format(date)

    /** "18–24 Aug" style label for the Weekly Attendance card. */
    fun weekRangeLabel(start: LocalDate, end: LocalDate): String =
        "${weekStartFormatter.format(start)}–${weekEndFormatter.format(end)}"

    /**
     * A signed hours-and-minutes balance: "+2h 30m", "−45m", "0h", "+8h 15m".
     *
     * Hours alone were never enough for a balance — a 30-minute deficit rendered as "0h", which
     * is how a short day used to look identical to an exact one. Sub-minute remainders are
     * dropped (never rounded up), so a balance only ever reads as time actually worked.
     */
    fun signedDurationLabel(duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        if (totalMinutes == 0L) return "0h"
        val sign = if (totalMinutes > 0L) "+" else MINUS_SIGN
        val hours = abs(totalMinutes) / 60
        val minutes = abs(totalMinutes) % 60
        return when {
            hours > 0L && minutes > 0L -> "$sign${hours}h ${minutes}m"
            hours > 0L -> "$sign${hours}h"
            else -> "$sign${minutes}m"
        }
    }

    /** [signedDurationLabel] from a plain minute count — for the Home cell, whose count-up
     * animation runs over whole minutes. */
    fun signedMinutesLabel(totalMinutes: Int): String = signedDurationLabel(Duration.ofMinutes(totalMinutes.toLong()))
}
