package com.hasim.orbittime.util

import java.time.Duration
import java.time.LocalDate

/**
 * The single status shown on one Daily History row. Exactly one of these applies to any given
 * day — [DailyHistory.statusOf] is the only place the precedence is decided — so a day can
 * never end up labelled twice (the old row printed "· overtime" on the time line *and* an
 * "Overtime" status on the right).
 */
enum class DailyHistoryStatus(val label: String) {
    /** A date in the user's own holiday calendar, worked or not. */
    HOLIDAY("Holiday"),

    /** A non-scheduled day (the work week is Mon–Sat, so this is Sunday). */
    WEEKEND("Weekend"),

    /** A scheduled day covered by a leave record, with no attendance for it. */
    LEAVE("Leave"),

    /** A past scheduled day with no check-in and no leave/holiday covering it. */
    ABSENT("Absent"),

    /** Checked in on a day that's already over, but never checked out — no reliable total. */
    INCOMPLETE("Incomplete"),

    /** Checked in after the user's own shift start. */
    LATE("Late"),

    /** Worked meaningfully longer than the user's own scheduled shift. */
    OVERTIME("Overtime"),

    WORK_FROM_HOME("WFH"),
    OUTSTATION("Outstation"),

    /** Checked in on time, from the office (or with no location recorded). */
    ON_TIME("On time"),

    /** Today, before the first punch of the day — not absent, it just hasn't happened yet. */
    PENDING("Pending"),
}

/** Pure Daily History list-building rules — no Android or Firebase types, so they stay testable. */
object DailyHistory {

    /** Mirrors [com.hasim.orbittime.data.attendance.AttendanceLocation]'s names, which is how a
     * location is stored on a record and carried through the UI state. */
    private const val LOCATION_WORK_FROM_HOME = "WORK_FROM_HOME"
    private const val LOCATION_OUTSTATION = "OUTSTATION"

    /**
     * How far past the scheduled shift a day has to run before it counts as overtime. Without it
     * a few minutes of overrun — or simply arriving early — would flag an ordinary day.
     */
    val OVERTIME_GRACE: Duration = Duration.ofMinutes(15)

    /**
     * Every calendar date the list must show for [monthStart]'s month, oldest first and with no
     * gaps — weekends, holidays, leave and absent days included. The list used to be built from
     * the attendance records alone, which silently dropped every date the user hadn't checked
     * into.
     *
     * The current month stops at [today]: later dates haven't happened yet, so they're not
     * history. A fully past month runs to its last day; a future month has no history at all.
     */
    fun period(monthStart: LocalDate, today: LocalDate): List<LocalDate> {
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        val last = if (monthEnd.isAfter(today)) today else monthEnd
        if (last.isBefore(monthStart)) return emptyList()
        return generateSequence(monthStart) { it.plusDays(1) }
            .takeWhile { !it.isAfter(last) }
            .toList()
    }

    /**
     * The one status for a day, from that day's own data. [dayStatus] is
     * [AttendanceStats.classifyDay]'s calendar-level verdict; everything else describes the
     * punch record itself.
     *
     * Order matters: a day with no punch at all can only be whatever the calendar says it is,
     * while a day that *was* worked is judged on its times — so working a holiday still shows
     * the hours, and a leave day that was worked anyway isn't reported as time off.
     *
     * Overtime is measured against [shiftDuration], the user's own shift start→end, rather than
     * a fixed 8 hours: the default shift is already 8h30m long, so a flat 8-hour rule labelled
     * every ordinary full day "Overtime".
     */
    fun statusOf(
        dayStatus: AttendanceStatus?,
        hasCheckIn: Boolean,
        hasCheckOut: Boolean,
        isOngoingToday: Boolean,
        worked: Duration?,
        shiftDuration: Duration,
        locationName: String?,
    ): DailyHistoryStatus = when {
        !hasCheckIn -> when (dayStatus) {
            AttendanceStatus.HOLIDAY -> DailyHistoryStatus.HOLIDAY
            AttendanceStatus.WEEKEND -> DailyHistoryStatus.WEEKEND
            AttendanceStatus.LEAVE -> DailyHistoryStatus.LEAVE
            AttendanceStatus.ABSENT -> DailyHistoryStatus.ABSENT
            else -> DailyHistoryStatus.PENDING
        }
        !hasCheckOut && !isOngoingToday -> DailyHistoryStatus.INCOMPLETE
        dayStatus == AttendanceStatus.LATE -> DailyHistoryStatus.LATE
        worked != null && worked > shiftDuration + OVERTIME_GRACE -> DailyHistoryStatus.OVERTIME
        locationName == LOCATION_WORK_FROM_HOME -> DailyHistoryStatus.WORK_FROM_HOME
        locationName == LOCATION_OUTSTATION -> DailyHistoryStatus.OUTSTATION
        else -> DailyHistoryStatus.ON_TIME
    }
}
