package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Pure date/time formatting for the attendance screens — no Android or Firebase types. */
object AttendanceTimeFormat {
    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

    /** Firestore document id / stable per-day key, e.g. "2026-08-26". */
    fun dateKey(date: LocalDate): String = date.toString()

    /** "MONDAY 24 AUGUST" style label for the punch hero card. */
    fun dayLabel(date: LocalDate): String = dayLabelFormatter.format(date).uppercase(Locale.getDefault())

    /** "9:02 am" style clock format. */
    fun clockTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        clockFormatter.format(instant.atZone(zone)).lowercase(Locale.getDefault())

    /** "5h 43m" style duration, always showing both units for readability. */
    fun elapsedLabel(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    fun greeting(time: LocalTime = LocalTime.now()): String = when {
        time.isBefore(LocalTime.NOON) -> "Good morning"
        time.isBefore(LocalTime.of(17, 0)) -> "Good afternoon"
        else -> "Good evening"
    }

    fun dayOfWeekAndDate(date: LocalDate): String =
        "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
            "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"
}
