package com.hasim.orbittime.ui.screens.timesheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.hasim.orbittime.data.attendance.AttendanceLocation
import com.hasim.orbittime.data.attendance.AttendanceRecord
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.holiday.HolidayRepository
import com.hasim.orbittime.data.leave.LeaveRecord
import com.hasim.orbittime.data.leave.LeaveRepository
import com.hasim.orbittime.data.leave.LeaveType
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceStatus
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.DailyHistory
import com.hasim.orbittime.util.observeIsOnline
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One calendar day within the displayed month — plain types, no Firebase dependency. */
data class TimesheetDay(
    val date: LocalDate,
    val checkInAt: Instant? = null,
    val checkOutAt: Instant? = null,
    val status: AttendanceStatus? = null,
    /** One of [com.hasim.orbittime.data.attendance.AttendanceLocation]'s names, set only via a
     * manual edit or backfill — mirrors [com.hasim.orbittime.data.attendance.AttendanceRecord.location]. */
    val location: String? = null,
    /** The covering leave's type label ("Sick leave", …), so a leave day can name itself. */
    val leaveLabel: String? = null,
    /** The covering holiday's own name, so a holiday can name itself rather than just "Holiday".
     * Blank for a holiday saved without a name. */
    val holidayName: String? = null,
)

data class TimesheetUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
    val displayedMonth: LocalDate = AttendanceTimeFormat.today().withDayOfMonth(1),
    val monthLabel: String = "",
    val days: List<TimesheetDay> = emptyList(),
    /** Every date of the displayed history period, oldest first and with no gaps — see
     * [DailyHistory.period]. Never filtered down to "days with a check-in". */
    val history: List<TimesheetDay> = emptyList(),
    val shiftDuration: Duration = AttendanceStats.shiftDuration(AttendanceStats.DEFAULT_LATE_AFTER, AttendanceStats.DEFAULT_SHIFT_END),
)

class TimesheetViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()
    private val profileRepository = UserProfileRepository()
    private val leaveRepository = LeaveRepository()
    private val holidayRepository = HolidayRepository()

    private val _uiState = MutableStateFlow(TimesheetUiState())
    val uiState: StateFlow<TimesheetUiState> = _uiState.asStateFlow()

    private var rangeJob: Job? = null
    private var lastMonthStart: LocalDate? = null
    private var lastRecordsByDate: Map<LocalDate, AttendanceRecord> = emptyMap()

    /** Each covered date mapped to that leave's type label, so the row can show which leave it is. */
    private var leaveLabelsByDate: Map<LocalDate, String> = emptyMap()

    /** Each holiday date mapped to that holiday's own name. */
    private var holidayNamesByDate: Map<LocalDate, String> = emptyMap()

    /** Each user's own late-arrival cutoff — their shift start, loaded from their profile. */
    private var lateAfter: LocalTime = AttendanceStats.DEFAULT_LATE_AFTER

    /** Each user's own shift end, loaded from their profile — the real denominator for each
     * day's history progress bar (was previously a hardcoded 8.5h). */
    private var shiftEnd: LocalTime = AttendanceStats.DEFAULT_SHIFT_END

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            observeConnectivity()
            observeMonth(uid, _uiState.value.displayedMonth)
            observeLeaves(uid)
            observeHolidays(uid)
            loadShiftStart(uid)
        }
    }

    private fun observeLeaves(uid: String) {
        viewModelScope.launch {
            leaveRepository.observeLeaves(uid)
                .catch { /* Leave dates are an enhancement to the calendar; a failure here shouldn't block attendance. */ }
                .collect { leaves ->
                    leaveLabelsByDate = leaves
                        .flatMap { leave -> leave.dateRange().map { date -> date to leave.typeLabel() } }
                        .toMap()
                    rebuildDays()
                }
        }
    }

    private fun observeHolidays(uid: String) {
        viewModelScope.launch {
            holidayRepository.observeHolidays(uid)
                .catch { /* Holidays only annotate the calendar; a failure here shouldn't block attendance. */ }
                .collect { holidays ->
                    holidayNamesByDate = holidays.mapNotNull { holiday ->
                        runCatching { LocalDate.parse(holiday.date) }.getOrNull()?.let { date ->
                            date to holiday.name
                        }
                    }.toMap()
                    rebuildDays()
                }
        }
    }

    private fun loadShiftStart(uid: String) {
        viewModelScope.launch {
            val profile = runCatching { profileRepository.getProfile(uid) }.getOrNull()
            val parsedStart = profile?.shiftStart?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            val parsedEnd = profile?.shiftEnd?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            if (parsedStart != null) lateAfter = parsedStart
            if (parsedEnd != null) shiftEnd = parsedEnd
            if (parsedStart != null || parsedEnd != null) {
                _uiState.update { it.copy(shiftDuration = AttendanceStats.shiftDuration(lateAfter, shiftEnd)) }
                // Re-derive the already-fetched month's days with the corrected shift times
                // instead of re-subscribing to observeMonth() — that re-fetched the exact same
                // month's attendance range a second time on every single screen open, since
                // init() had already started that listener moments earlier.
                rebuildDays()
            }
        }
    }

    fun showPreviousMonth() = changeMonth(-1L)
    fun showNextMonth() = changeMonth(1L)

    private fun changeMonth(deltaMonths: Long) {
        val uid = authRepository.currentUser?.uid ?: return
        val newMonth = _uiState.value.displayedMonth.plusMonths(deltaMonths).withDayOfMonth(1)
        _uiState.update { it.copy(displayedMonth = newMonth, isLoading = true, errorMessage = null) }
        observeMonth(uid, newMonth)
    }

    private fun observeMonth(uid: String, monthStart: LocalDate) {
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())

        rangeJob?.cancel()
        rangeJob = viewModelScope.launch {
            attendanceRepository.observeRange(uid, AttendanceTimeFormat.dateKey(monthStart), AttendanceTimeFormat.dateKey(monthEnd))
                .catch { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
                .collect { records ->
                    lastMonthStart = monthStart
                    lastRecordsByDate = records.mapNotNull { record ->
                        runCatching { LocalDate.parse(record.date) }.getOrNull()?.let { it to record }
                    }.toMap()
                    rebuildDays()
                }
        }
    }

    private fun rebuildDays() {
        val monthStart = lastMonthStart ?: return
        val today = AttendanceTimeFormat.today()
        val byDate = lastRecordsByDate

        val days = (1..monthStart.lengthOfMonth()).map { dayOfMonth ->
            val date = monthStart.withDayOfMonth(dayOfMonth)
            val record = byDate[date]
            val checkInAt = record?.checkInAt?.toDate()?.toInstant()
            val checkOutAt = record?.checkOutAt?.toDate()?.toInstant()
            TimesheetDay(
                date = date,
                checkInAt = checkInAt,
                checkOutAt = checkOutAt,
                status = AttendanceStats.classifyDay(
                    checkInAt = checkInAt,
                    date = date,
                    today = today,
                    lateAfter = lateAfter,
                    isOnLeave = date in leaveLabelsByDate,
                    isHoliday = date in holidayNamesByDate,
                ),
                location = record?.location,
                leaveLabel = leaveLabelsByDate[date],
                holidayName = holidayNamesByDate[date],
            )
        }

        // Daily History lists the period's dates themselves, not the records — every date is
        // present, in order, and days with no attendance carry their own status (Absent,
        // Leave, Holiday, Weekend) instead of vanishing from the list.
        val daysByDate = days.associateBy { day -> day.date }
        val history = DailyHistory.period(monthStart, today).map { date -> daysByDate.getValue(date) }

        _uiState.update {
            it.copy(
                isLoading = false,
                monthLabel = AttendanceTimeFormat.monthLabel(monthStart),
                days = days,
                history = history,
            )
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            observeIsOnline(getApplication()).collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    /**
     * Corrects one specific Daily History day's punch times/location. [observeMonth]'s live
     * listener picks up the write automatically, so the list and every derived stat refresh on
     * their own — no local state patch needed here.
     */
    fun editDay(date: LocalDate, checkInTime: LocalTime, checkOutTime: LocalTime?, location: AttendanceLocation?) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            attendanceRepository.setManualTimes(
                uid = uid,
                date = AttendanceTimeFormat.dateKey(date),
                checkInAt = checkInTime.toTimestamp(date),
                checkOutAt = checkOutTime?.toTimestamp(date),
                location = location,
            ).onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    /** Permanently removes one day's record — the document ID is that exact date, so this can
     * never touch any other day. The live [observeMonth] listener removes it from the list and
     * recomputes every stat as soon as Firestore confirms the delete. */
    fun deleteDay(date: LocalDate) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            attendanceRepository.deleteRecord(uid, AttendanceTimeFormat.dateKey(date))
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    /** Falls back to the catch-all label rather than dropping the leave, so an unknown or
     * hand-edited type still reads as leave instead of silently showing as absent. */
    private fun LeaveRecord.typeLabel(): String =
        runCatching { LeaveType.valueOf(type) }.getOrNull()?.label ?: LeaveType.OTHER.label

    private fun LocalTime.toTimestamp(date: LocalDate): Timestamp =
        Timestamp(Date.from(date.atTime(this).atZone(ZoneId.systemDefault()).toInstant()))
}
