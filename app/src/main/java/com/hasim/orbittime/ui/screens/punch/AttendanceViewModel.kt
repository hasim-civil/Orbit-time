package com.hasim.orbittime.ui.screens.punch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.attendance.AttendanceLocation
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.holiday.HolidayRepository
import com.hasim.orbittime.data.leave.LeaveRepository
import com.hasim.orbittime.data.notification.NotificationKind
import com.hasim.orbittime.data.notification.NotificationRepository
import com.hasim.orbittime.data.notification.UserNotification
import com.hasim.orbittime.data.settings.AppTimeSettingsStore
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.reminder.ShiftReminderScheduler
import com.hasim.orbittime.util.AttendanceRangeMode
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceStatus
import com.hasim.orbittime.util.AttendanceSummary
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.DailyAttendance
import com.hasim.orbittime.util.OrbitClock
import com.hasim.orbittime.util.observeIsOnline
import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plain java.time types only — no Firebase Timestamp here — so the Punch
 * screen's UI has zero Firebase dependency and can be rendered/tested outside
 * an Android runtime, matching every other screen in this app.
 */
data class PunchUiState(
    val isLoading: Boolean = true,
    val checkInAt: Instant? = null,
    val checkOutAt: Instant? = null,
    val elapsed: Duration = Duration.ZERO,
    val isOnline: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSummaryLoading: Boolean = true,
    val rangeMode: AttendanceRangeMode = AttendanceRangeMode.MONTH,
    val summary: AttendanceSummary = AttendanceSummary(),
    val summaryErrorMessage: String? = null,
    val successMessage: PunchSuccessKind? = null,
    val shiftStart: LocalTime = AttendanceStats.DEFAULT_LATE_AFTER,
    val shiftEnd: LocalTime = AttendanceStats.DEFAULT_SHIFT_END,
) {
    val isCheckedIn: Boolean get() = checkInAt != null && checkOutAt == null
    val isCompleted: Boolean get() = checkInAt != null && checkOutAt != null
}

/** Which punch just succeeded — drives the ~1s custom success overlay, never shown until the
 * Firestore write it reports on has actually completed. */
enum class PunchSuccessKind { CHECK_IN, CHECK_OUT, PAST_ATTENDANCE_ADDED }

private const val TICK_INTERVAL_MS = 30_000L

/** Company policy: this many late arrivals are pre-approved each calendar month. */
private const val LATE_ALLOWANCE_PER_MONTH = 2

class AttendanceViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()
    private val profileRepository = UserProfileRepository()
    private val leaveRepository = LeaveRepository()
    private val holidayRepository = HolidayRepository()
    private val notificationRepository = NotificationRepository()
    private val todayDate = AttendanceTimeFormat.today()
    private val todayKey = AttendanceTimeFormat.dateKey(todayDate)

    private val monthStart = todayDate.withDayOfMonth(1)
    private val monthEnd = todayDate.withDayOfMonth(todayDate.lengthOfMonth())
    private val weekStart = todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val weekEnd = todayDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    private var rangeRecords: Map<LocalDate, DailyAttendance> = emptyMap()
    private var leaveDates: Set<LocalDate> = emptySet()

    /** A holiday is not a missed day, so these dates are excluded from the absence check below —
     * and nothing is required of the user on one, so a holiday that *was* worked counts as
     * overtime in full rather than as a day short of the required hours. */
    private var holidayDates: Set<LocalDate> = emptySet()

    /** Each user's own late-arrival cutoff — their shift start, loaded from their profile. */
    private var lateAfter: LocalTime = AttendanceStats.DEFAULT_LATE_AFTER

    /** Each user's own shift end, loaded from their profile — paired with [lateAfter] as the
     * real denominator for shift-completion progress bars (was previously a hardcoded 8.5h). */
    private var shiftEnd: LocalTime = AttendanceStats.DEFAULT_SHIFT_END

    /** Notification ids already confirmed-or-created this session, so repeated recomputes (e.g.
     * every time the month-range listener ticks) don't re-check Firestore for facts we already
     * know about — the real source of truth is still Firestore's own existence check in
     * [NotificationRepository.createIfMissing], this is just a fast-path skip. */
    private val notifiedThisSession = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(PunchUiState())
    val uiState: StateFlow<PunchUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, isSummaryLoading = false, errorMessage = "You're not signed in.") }
        } else {
            observeRecord(uid)
            observeMonthRange(uid)
            observeLeaves(uid)
            observeHolidays(uid)
            observeConnectivity()
            tickElapsedWhileRunning()
            observeAppTime()
            loadShiftStart(uid)
        }
    }

    /**
     * The Profile → App Time setting feeds [OrbitClock], which every figure below is measured
     * against — so switching to (or away from) a manual time has to recompute them straight
     * away rather than waiting for the next 30-second tick or attendance snapshot.
     */
    private fun observeAppTime() {
        viewModelScope.launch {
            AppTimeSettingsStore.settingsFlow.collect {
                val current = _uiState.value
                _uiState.update { it.copy(elapsed = computeElapsed(current.checkInAt, current.checkOutAt)) }
                recomputeSummary()
            }
        }
    }

    private fun observeLeaves(uid: String) {
        viewModelScope.launch {
            leaveRepository.observeLeaves(uid)
                .catch { /* Leave dates are an enhancement to the summary; a failure here shouldn't block attendance. */ }
                .collect { leaves ->
                    leaveDates = leaves.flatMap { it.dateRange() }.toSet()
                    recomputeSummary()
                }
        }
    }

    private fun observeHolidays(uid: String) {
        viewModelScope.launch {
            holidayRepository.observeHolidays(uid)
                .catch { /* Holidays only suppress a notification; a failure here shouldn't block attendance. */ }
                .collect { holidays ->
                    holidayDates = holidays.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
                    recomputeSummary()
                }
        }
    }

    /** Live rather than one-shot, so an Edit Profile shift-time change is picked up immediately —
     * both for the progress bars here and, importantly, for [ShiftReminderScheduler], which must
     * re-arm against the new time right away rather than waiting for the next app restart. */
    private fun loadShiftStart(uid: String) {
        viewModelScope.launch {
            profileRepository.observeProfile(uid)
                .catch { /* Falls back to the already-loaded (or default) shift times. */ }
                .collect { profile ->
                    val parsedStart = profile?.shiftStart?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    val parsedEnd = profile?.shiftEnd?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    if (parsedStart != null) {
                        lateAfter = parsedStart
                        ShiftReminderScheduler.schedule(getApplication(), parsedStart)
                    }
                    if (parsedEnd != null) shiftEnd = parsedEnd
                    if (parsedStart != null || parsedEnd != null) {
                        _uiState.update { it.copy(shiftStart = lateAfter, shiftEnd = shiftEnd) }
                        recomputeSummary()
                    }
                }
        }
    }

    private fun observeRecord(uid: String) {
        viewModelScope.launch {
            attendanceRepository.observeRecord(uid, todayKey)
                .catch { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
                .collect { record ->
                    val checkInAt = record?.checkInAt?.toDate()?.toInstant()
                    val checkOutAt = record?.checkOutAt?.toDate()?.toInstant()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            checkInAt = checkInAt,
                            checkOutAt = checkOutAt,
                            elapsed = computeElapsed(checkInAt, checkOutAt),
                        )
                    }
                }
        }
    }

    /** Covers both the current week and current month in one listener — a week can spill into the previous month. */
    private fun observeMonthRange(uid: String) {
        val queryStart = if (weekStart.isBefore(monthStart)) weekStart else monthStart
        val queryEnd = if (weekEnd.isAfter(monthEnd)) weekEnd else monthEnd

        viewModelScope.launch {
            attendanceRepository.observeRange(uid, AttendanceTimeFormat.dateKey(queryStart), AttendanceTimeFormat.dateKey(queryEnd))
                .catch { error -> _uiState.update { it.copy(isSummaryLoading = false, summaryErrorMessage = error.message) } }
                .collect { records ->
                    rangeRecords = records.mapNotNull { record ->
                        val date = runCatching { LocalDate.parse(record.date) }.getOrNull() ?: return@mapNotNull null
                        date to DailyAttendance(
                            date = date,
                            checkInAt = record.checkInAt?.toDate()?.toInstant(),
                            checkOutAt = record.checkOutAt?.toDate()?.toInstant(),
                        )
                    }.toMap()
                    recomputeSummary()
                }
        }
    }

    fun setRangeMode(mode: AttendanceRangeMode) {
        _uiState.update { it.copy(rangeMode = mode) }
        recomputeSummary()
    }

    private fun recomputeSummary() {
        val mode = _uiState.value.rangeMode
        val summary = when (mode) {
            AttendanceRangeMode.MONTH -> AttendanceStats.summarize(
                records = rangeRecords,
                rangeStart = monthStart,
                rangeEnd = monthEnd,
                today = todayDate,
                rangeLabel = AttendanceTimeFormat.monthLabel(todayDate),
                lateAfter = lateAfter,
                leaveDates = leaveDates,
                holidayDates = holidayDates,
            )
            AttendanceRangeMode.WEEK -> AttendanceStats.summarize(
                records = rangeRecords,
                rangeStart = weekStart,
                rangeEnd = weekEnd,
                today = todayDate,
                rangeLabel = AttendanceTimeFormat.weekRangeLabel(weekStart, weekEnd),
                lateAfter = lateAfter,
                leaveDates = leaveDates,
                holidayDates = holidayDates,
            )
        }
        _uiState.update { it.copy(isSummaryLoading = false, summary = summary) }
        checkAttendanceNotifications()
    }

    /**
     * Evaluates the two data-driven notification rules against the current month's real
     * attendance and creates a notification for each newly-true fact:
     *  - the user has reached this month's [LATE_ALLOWANCE_PER_MONTH] approved late arrivals;
     *  - a past scheduled day this month has no check-in at all (a missed day).
     * Both use a deterministic notification id (a specific month, a specific date) so re-running
     * this on every attendance-data refresh never creates a duplicate for the same fact.
     */
    private fun checkAttendanceNotifications() {
        val uid = authRepository.currentUser?.uid ?: return

        val monthSummary = AttendanceStats.summarize(
            records = rangeRecords,
            rangeStart = monthStart,
            rangeEnd = monthEnd,
            today = todayDate,
            rangeLabel = "",
            lateAfter = lateAfter,
            leaveDates = leaveDates,
            holidayDates = holidayDates,
        )
        if (monthSummary.lateDays >= LATE_ALLOWANCE_PER_MONTH) {
            val monthKey = AttendanceTimeFormat.dateKey(monthStart).take(7) // "yyyy-MM"
            maybeNotify(uid, "late-allowance-$monthKey") {
                UserNotification(
                    kind = NotificationKind.ATTENDANCE_INFO,
                    title = "Late allowance used",
                    body = "You've reached your $LATE_ALLOWANCE_PER_MONTH approved late arrivals for " +
                        "${AttendanceTimeFormat.monthLabel(monthStart)}.",
                )
            }
        }

        var date = monthStart
        while (!date.isAfter(monthEnd)) {
            val status = AttendanceStats.classifyDay(
                checkInAt = rangeRecords[date]?.checkInAt,
                date = date,
                today = todayDate,
                lateAfter = lateAfter,
                isOnLeave = date in leaveDates,
                isHoliday = date in holidayDates,
            )
            if (status == AttendanceStatus.ABSENT) {
                val missedDate = date
                maybeNotify(uid, "missed-${AttendanceTimeFormat.dateKey(missedDate)}") {
                    UserNotification(
                        kind = NotificationKind.LATE_ARRIVAL,
                        title = "Missed attendance",
                        body = "You didn't check in on ${AttendanceTimeFormat.shortDayLabel(missedDate)}.",
                    )
                }
            }
            date = date.plusDays(1)
        }
    }

    private fun maybeNotify(uid: String, id: String, notification: () -> UserNotification) {
        if (!notifiedThisSession.add(id)) return
        viewModelScope.launch {
            notificationRepository.createIfMissing(uid, id, notification())
                .onFailure { notifiedThisSession.remove(id) }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            observeIsOnline(getApplication()).collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    /** Keeps the elapsed counter (and the monthly worked/overtime totals) moving while a session is running. */
    private fun tickElapsedWhileRunning() {
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                val current = _uiState.value
                if (current.isCheckedIn) {
                    _uiState.update { it.copy(elapsed = computeElapsed(current.checkInAt, current.checkOutAt)) }
                    recomputeSummary()
                }
            }
        }
    }

    /** Today's elapsed time, from the same rule the summaries use — and from the app's own
     * clock, so a manual App Time override moves the counter with it. */
    private fun computeElapsed(checkInAt: Instant?, checkOutAt: Instant?): Duration =
        AttendanceStats.workedDuration(
            checkInAt = checkInAt,
            checkOutAt = checkOutAt,
            date = todayDate,
            today = todayDate,
            now = OrbitClock.now(),
        ) ?: Duration.ZERO

    fun checkIn() {
        val uid = authRepository.currentUser?.uid ?: return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            attendanceRepository.checkIn(uid, todayKey)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, successMessage = PunchSuccessKind.CHECK_IN) }
                    maybeLogLateArrival(uid)
                }
                .onFailure { error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message) } }
        }
    }

    /** Logs a real "Late arrival" notification the moment a check-in lands after the user's own
     * shift start — never before the Firestore check-in write above has already succeeded. */
    private suspend fun maybeLogLateArrival(uid: String) {
        val now = OrbitClock.localTime()
        val lateMinutes = Duration.between(lateAfter, now).toMinutes()
        if (lateMinutes <= 0) return
        val body = "${AttendanceTimeFormat.shortDayLabel(todayDate)} — clocked in at ${AttendanceTimeFormat.clockTime(OrbitClock.now())}, " +
            "$lateMinutes minute${if (lateMinutes == 1L) "" else "s"} after shift start."
        notificationRepository.addLateArrival(uid, title = "Late arrival logged", body = body)
    }

    fun checkOut() {
        val uid = authRepository.currentUser?.uid ?: return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            attendanceRepository.checkOut(uid, todayKey)
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, successMessage = PunchSuccessKind.CHECK_OUT) } }
                .onFailure { error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message) } }
        }
    }

    /** Clears the success overlay once the UI has shown it for its ~1s window. */
    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /** Corrects today's punch times — e.g. a forgotten check-in or a check-out that ran a few minutes late. */
    fun editTodayTimes(checkInTime: LocalTime, checkOutTime: LocalTime?, location: AttendanceLocation?) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            attendanceRepository.setManualTimes(
                uid = uid,
                date = todayKey,
                checkInAt = checkInTime.toTimestamp(todayDate),
                checkOutAt = checkOutTime?.toTimestamp(todayDate),
                location = location,
            ).onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    /** Backfills a day that was never punched — a full check-in/check-out pair for a past date. */
    fun addPastAttendance(date: LocalDate, checkInTime: LocalTime, checkOutTime: LocalTime, location: AttendanceLocation?) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            attendanceRepository.setManualTimes(
                uid = uid,
                date = AttendanceTimeFormat.dateKey(date),
                checkInAt = checkInTime.toTimestamp(date),
                checkOutAt = checkOutTime.toTimestamp(date),
                location = location,
            )
                .onSuccess { _uiState.update { it.copy(successMessage = PunchSuccessKind.PAST_ATTENDANCE_ADDED) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    private fun LocalTime.toTimestamp(date: LocalDate): Timestamp =
        Timestamp(Date.from(date.atTime(this).atZone(OrbitClock.zone).toInstant()))
}
