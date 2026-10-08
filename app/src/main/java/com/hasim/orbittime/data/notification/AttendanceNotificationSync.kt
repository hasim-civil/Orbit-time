package com.hasim.orbittime.data.notification

import com.hasim.orbittime.data.attendance.AttendanceRecord
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.holiday.HolidayRecord
import com.hasim.orbittime.data.holiday.HolidayRepository
import com.hasim.orbittime.data.leave.LeaveRecord
import com.hasim.orbittime.data.leave.LeaveRepository
import com.hasim.orbittime.data.user.UserProfile
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.util.AttendanceNotifications
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.DailyAttendance
import com.hasim.orbittime.util.ExistingNotification
import com.hasim.orbittime.util.NotificationChange
import com.hasim.orbittime.util.OrbitClock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Brings the attendance notifications in line with [AttendanceNotifications]' rules.
 *
 * The old rules ran on whatever the screen's live listeners happened to hold — including the
 * empty sets every listener starts with — so a leave day, a holiday or even an attended day
 * could be written up as "Missed attendance" before its data had arrived, and nothing ever
 * looked at it again. Here every input is read from the server first; if any read fails
 * (offline, say) nothing is decided at all, and the next run tries again.
 */
private class SyncInputs(
    val records: List<AttendanceRecord>,
    val leaves: List<LeaveRecord>,
    val holidays: List<HolidayRecord>,
    val profile: UserProfile?,
    val existing: List<UserNotification>,
)

class AttendanceNotificationSync(
    private val attendanceRepository: AttendanceRepository = AttendanceRepository(),
    private val leaveRepository: LeaveRepository = LeaveRepository(),
    private val holidayRepository: HolidayRepository = HolidayRepository(),
    private val profileRepository: UserProfileRepository = UserProfileRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository(),
) {
    private val mutex = Mutex()

    /** Judges the previous month and this month up to today, so the last days of a month are
     * still settled after the month turns over. */
    suspend fun sync(uid: String): Result<Unit> = runCatching {
        mutex.withLock {
            val today = AttendanceTimeFormat.today()
            val now = OrbitClock.now()
            val zone = OrbitClock.zone
            val rangeStart = today.minusMonths(1).withDayOfMonth(1)

            val inputs = coroutineScope {
                val records = async {
                    attendanceRepository.fetchRangeFromServer(uid, AttendanceTimeFormat.dateKey(rangeStart), AttendanceTimeFormat.dateKey(today))
                }
                val leaves = async { leaveRepository.fetchLeavesFromServer(uid) }
                val holidays = async { holidayRepository.fetchHolidaysFromServer() }
                val profile = async { profileRepository.getProfileFromServer(uid) }
                val existing = async { notificationRepository.fetchAllFromServer(uid) }
                SyncInputs(records.await(), leaves.await(), holidays.await(), profile.await(), existing.await())
            }

            val attendance = inputs.records.mapNotNull { record ->
                val date = runCatching { LocalDate.parse(record.date) }.getOrNull() ?: return@mapNotNull null
                date to DailyAttendance(date, record.checkInAt?.toDate()?.toInstant(), record.checkOutAt?.toDate()?.toInstant())
            }.toMap()
            val leaveDates = inputs.leaves.flatMap { it.dateRange() }.toSet()
            val holidayDates = inputs.holidays.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
            val profile = inputs.profile
            val existing = inputs.existing

            val changes = AttendanceNotifications.plan(
                rangeStart = rangeStart,
                rangeEnd = today,
                today = today,
                now = now,
                records = attendance,
                leaveDates = leaveDates,
                holidayDates = holidayDates,
                shiftStart = profile?.shiftStart?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: AttendanceStats.DEFAULT_LATE_AFTER,
                shiftEnd = profile?.shiftEnd?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: AttendanceStats.DEFAULT_SHIFT_END,
                existing = existing.map { notification ->
                    ExistingNotification(
                        id = notification.id,
                        title = notification.title,
                        createdOn = notification.createdAt?.toDate()?.toInstant()?.atZone(zone)?.toLocalDate(),
                        retracted = notification.retracted,
                    )
                },
                zone = zone,
            )
            if (changes.isEmpty()) return@withLock

            notificationRepository.applyChanges(
                uid = uid,
                creates = changes.filterIsInstance<NotificationChange.Create>().map { create ->
                    UserNotification(
                        id = create.id,
                        kind = NotificationKind.valueOf(create.kind.name),
                        title = create.title,
                        body = create.body,
                        date = create.date?.let(AttendanceTimeFormat::dateKey).orEmpty(),
                    )
                },
                retractions = changes.filterIsInstance<NotificationChange.Retract>().associate { it.id to it.reason },
                restores = changes.filterIsInstance<NotificationChange.Restore>().map { it.id }.toSet(),
            )
        }
    }
}
