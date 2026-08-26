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
 * rules (Mon–Sat, late after 11:00, overtime past 8h/day) stay testable outside
 * an Android runtime, matching every other calculation in this app.
 */
object AttendanceStats {
    private val SCHEDULED_DAYS = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
    )
    private val LATE_AFTER = LocalTime.of(11, 0)
    private val OVERTIME_AFTER = Duration.ofHours(8)

    fun summarize(
        records: Map<LocalDate, DailyAttendance>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        rangeLabel: String,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): AttendanceSummary {
        var present = 0
        var absent = 0
        var late = 0
        var worked = Duration.ZERO
        var overtime = Duration.ZERO

        var date = rangeStart
        while (!date.isAfter(rangeEnd)) {
            if (date.dayOfWeek in SCHEDULED_DAYS) {
                val record = records[date]
                val checkInAt = record?.checkInAt
                when {
                    checkInAt != null -> {
                        present += 1
                        if (checkInAt.atZone(zone).toLocalTime().isAfter(LATE_AFTER)) late += 1

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
            date = date.plusDays(1)
        }

        val counted = present + absent
        val rate = if (counted > 0) (present * 100) / counted else 0

        return AttendanceSummary(
            rangeLabel = rangeLabel,
            presentDays = present,
            absentDays = absent,
            lateDays = late,
            leaveDays = 0,
            worked = worked,
            overtime = overtime,
            attendanceRatePercent = rate,
        )
    }
}
