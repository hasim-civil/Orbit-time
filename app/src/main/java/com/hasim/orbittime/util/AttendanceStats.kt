package com.hasim.orbittime.util

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One day's check-in/check-out, in plain types — mirrors AttendanceRecord without the Firebase Timestamp. */
data class DailyAttendance(
    val date: LocalDate,
    val checkInAt: Instant? = null,
    val checkOutAt: Instant? = null,
)

enum class AttendanceRangeMode { WEEK, MONTH }

/**
 * WEEKEND covers every non-scheduled day (the work week is Mon–Sat, so: Sunday). It used to be
 * reported as "no status at all", which left those dates with nothing to render — and so with
 * nothing to list in Daily History.
 */
enum class AttendanceStatus { PRESENT, LATE, ABSENT, LEAVE, HOLIDAY, WEEKEND }

data class AttendanceSummary(
    val rangeLabel: String = "",
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val lateDays: Int = 0,
    val leaveDays: Int = 0,
    val worked: Duration = Duration.ZERO,
    val overtime: Duration = Duration.ZERO,
    val attendanceRatePercent: Int = 0,
)

/**
 * Pure attendance-rollup math — no Firebase or Android types — so the schedule
 * rules (Mon–Sat, late after each user's own shift start, overtime past 8h/day)
 * stay testable outside an Android runtime, matching every other calculation in
 * this app.
 */
object AttendanceStats {
    private val SCHEDULED_DAYS = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
    )

    /** Fallback only for a profile that hasn't loaded its own shift start/end yet — matches
     * [com.hasim.orbittime.data.user.UserProfile]'s own defaults. */
    val DEFAULT_LATE_AFTER: LocalTime = LocalTime.of(9, 0)
    val DEFAULT_SHIFT_END: LocalTime = LocalTime.of(17, 30)
    val OVERTIME_AFTER: Duration = Duration.ofHours(8)

    /** A shift's real scheduled length, handling the (rare) overnight case where end wraps past
     * midnight before start. Used as the denominator for shift-completion progress bars instead
     * of a hardcoded guess, so it always reflects each user's own configured shift. */
    fun shiftDuration(shiftStart: LocalTime, shiftEnd: LocalTime): Duration =
        Duration.between(shiftStart, shiftEnd).let { if (it.isNegative || it.isZero) it.plusHours(24) else it }

    fun summarize(
        records: Map<LocalDate, DailyAttendance>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        rangeLabel: String,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        lateAfter: LocalTime = DEFAULT_LATE_AFTER,
        leaveDates: Set<LocalDate> = emptySet(),
    ): AttendanceSummary {
        var present = 0
        var absent = 0
        var late = 0
        var leave = 0
        var worked = Duration.ZERO
        var overtime = Duration.ZERO

        var date = rangeStart
        while (!date.isAfter(rangeEnd)) {
            if (date.dayOfWeek in SCHEDULED_DAYS) {
                if (date in leaveDates) {
                    leave += 1
                } else {
                    val record = records[date]
                    val checkInAt = record?.checkInAt
                    when {
                        checkInAt != null -> {
                            present += 1
                            if (checkInAt.atZone(zone).toLocalTime().isAfter(lateAfter)) late += 1

                            val end = record.checkOutAt ?: if (date == today) now else null
                            if (end != null) {
                                val duration = Duration.between(checkInAt, end).let { if (it.isNegative) Duration.ZERO else it }
                                worked += duration
                                if (duration > OVERTIME_AFTER) overtime += duration - OVERTIME_AFTER
                            }
                        }
                        date.isBefore(today) -> absent += 1
                    }
                }
            }
            date = date.plusDays(1)
        }

        val counted = present + absent
        val rate = if (counted > 0) (present * 100) / counted else 0

        return AttendanceSummary(
            rangeLabel = rangeLabel,
            presentDays = present,
            absentDays = absent,
            lateDays = late,
            leaveDays = leave,
            worked = worked,
            overtime = overtime,
            attendanceRatePercent = rate,
        )
    }

    /**
     * Per-day status for calendar/history views. Returns null only for a day that genuinely has
     * nothing to say yet: today, or a future scheduled day, with no check-in.
     *
     * The order below is the rule, and it is deliberate:
     *  - a non-scheduled day is never "absent" — nobody was expected in;
     *  - a holiday outranks the punch record for labelling purposes, but (see [DailyHistory])
     *    the hours worked on it are still shown;
     *  - an actual check-in outranks a leave record, because the day was in fact worked;
     *  - only then does a past scheduled day with nothing on it count as absent.
     */
    fun classifyDay(
        checkInAt: Instant?,
        date: LocalDate,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        lateAfter: LocalTime = DEFAULT_LATE_AFTER,
        isOnLeave: Boolean = false,
        isHoliday: Boolean = false,
    ): AttendanceStatus? = when {
        date.dayOfWeek !in SCHEDULED_DAYS -> AttendanceStatus.WEEKEND
        isHoliday -> AttendanceStatus.HOLIDAY
        checkInAt != null ->
            if (checkInAt.atZone(zone).toLocalTime().isAfter(lateAfter)) AttendanceStatus.LATE else AttendanceStatus.PRESENT
        isOnLeave -> AttendanceStatus.LEAVE
        date.isBefore(today) -> AttendanceStatus.ABSENT
        else -> null
    }
}
