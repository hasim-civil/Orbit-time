package com.hasim.orbittime.ui.screens.timesheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.hasim.orbittime.data.attendance.AttendanceLocation
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.holiday.HolidayRecord
import com.hasim.orbittime.data.holiday.HolidayRepository
import com.hasim.orbittime.data.leave.LeaveRecord
import com.hasim.orbittime.data.leave.LeaveRepository
import com.hasim.orbittime.data.leave.LeaveType
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.DailyHistory
import com.hasim.orbittime.util.DayPunch
import com.hasim.orbittime.util.HistoryDay
import com.hasim.orbittime.util.observeIsOnline
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One calendar day within the displayed month — resolved from attendance, leaves and holidays. */
typealias TimesheetDay = HistoryDay

data class TimesheetUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
    val displayedMonth: LocalDate = AttendanceTimeFormat.today().withDayOfMonth(1),
    val monthLabel: String = "",
    val days: List<TimesheetDay> = emptyList(),
    /** Every date of the displayed history period, newest first and with no gaps — see
     * [DailyHistory.period]. Never filtered down to "days with a check-in". */
    val history: List<TimesheetDay> = emptyList(),
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

    /**
     * The user's shift start — the late-arrival cutoff — as its own flow, so a late-arriving
     * profile re-derives the month through the same single pipeline as everything else instead
     * of patching state on the side. The shift *end* isn't needed here: "late" comes from the
     * start, and both overtime and the history progress bar are measured against the required
     * 8-hour working day rather than the scheduled span.
     */
    private val shiftStart = MutableStateFlow(AttendanceStats.DEFAULT_LATE_AFTER)

    private var monthJob: Job? = null

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            observeConnectivity()
            observeMonth(uid, _uiState.value.displayedMonth)
            loadShiftStart(uid)
        }
    }

    private fun loadShiftStart(uid: String) {
        viewModelScope.launch {
            val profile = runCatching { profileRepository.getProfile(uid) }.getOrNull() ?: return@launch
            runCatching { LocalTime.parse(profile.shiftStart) }.getOrNull()?.let { shiftStart.value = it }
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

    /**
     * The Timesheet's one and only data pipeline: the displayed month is derived from every
     * source that can describe a date — attendance, leaves, holidays and the user's shift —
     * combined so that a change in *any* of them re-derives the month immediately.
     *
     * This replaces a set of separate listeners that each wrote into a mutable field and then
     * asked for a rebuild. That had two ways of going stale, and both showed up as "I added a
     * holiday for a past date and the Timesheet didn't change":
     *
     *  - every listener was ended by `catch {}` on its first error, and the repositories close
     *    their flow on *any* snapshot error (a moment offline is enough). After that the screen
     *    kept rendering the last data it happened to hold, and no later leave or holiday write
     *    could reach it. Each source now retries instead of dying;
     *  - the month being rendered was whatever the attendance listener had last reported, so a
     *    leave/holiday snapshot arriving mid-month-change rebuilt the previous month under the
     *    new month's heading. The month is now an input to the pipeline, not a leftover field.
     */
    private fun observeMonth(uid: String, monthStart: LocalDate) {
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())

        monthJob?.cancel()
        monthJob = viewModelScope.launch {
            combine(
                attendanceRepository
                    .observeRange(uid, AttendanceTimeFormat.dateKey(monthStart), AttendanceTimeFormat.dateKey(monthEnd))
                    .retryForever { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } },
                // Leaves and holidays start empty so the month renders as soon as attendance
                // arrives instead of waiting on all three. Both are whole-collection listeners,
                // so a leave or holiday saved for *any* date — past months included — lands here.
                leaveRepository.observeLeaves(uid).retryForever().onStart { emit(emptyList()) },
                holidayRepository.observeHolidays(uid).retryForever().onStart { emit(emptyList()) },
                shiftStart,
            ) { records, leaves, holidays, lateAfter ->
                val punches = records.mapNotNull { record ->
                    runCatching { LocalDate.parse(record.date) }.getOrNull()?.let { date ->
                        date to DayPunch(
                            checkInAt = record.checkInAt?.toDate()?.toInstant(),
                            checkOutAt = record.checkOutAt?.toDate()?.toInstant(),
                            location = record.location,
                        )
                    }
                }.toMap()
                DailyHistory.buildMonth(
                    monthStart = monthStart,
                    today = AttendanceTimeFormat.today(),
                    punches = punches,
                    leaveLabels = leaves.leaveLabelsByDate(),
                    holidayNames = holidays.holidayNamesByDate(),
                    lateAfter = lateAfter,
                )
            }.collect { month ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        monthLabel = AttendanceTimeFormat.monthLabel(monthStart),
                        days = month.days,
                        history = month.history,
                    )
                }
            }
        }
    }

    /**
     * Keeps a live listener alive across transient failures. The repositories close their flow on
     * any Firestore snapshot error — including routine ones, like a moment offline — so without
     * this a single blip would silently stop the screen updating for good.
     */
    private fun <T> Flow<T>.retryForever(onError: (Throwable) -> Unit = {}): Flow<T> =
        retryWhen { cause, attempt ->
            onError(cause)
            delay(RETRY_BACKOFF_MS * (attempt + 1).coerceAtMost(MAX_BACKOFF_STEPS))
            true
        }

    /** Each covered date mapped to that leave's type label, so a leave row can name its type. */
    private fun List<LeaveRecord>.leaveLabelsByDate(): Map<LocalDate, String> =
        flatMap { leave -> leave.dateRange().map { date -> date to leave.typeLabel() } }.toMap()

    /** Each holiday date mapped to that holiday's own name (which may be blank). */
    private fun List<HolidayRecord>.holidayNamesByDate(): Map<LocalDate, String> =
        mapNotNull { holiday ->
            runCatching { LocalDate.parse(holiday.date) }.getOrNull()?.let { date -> date to holiday.name }
        }.toMap()

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

    private companion object {
        const val RETRY_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_STEPS = 30L
    }
}
