package com.hasim.orbittime.ui.screens.punch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.util.AttendanceRangeMode
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceSummary
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.DailyAttendance
import com.hasim.orbittime.util.observeIsOnline
import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
) {
    val isCheckedIn: Boolean get() = checkInAt != null && checkOutAt == null
    val isCompleted: Boolean get() = checkInAt != null && checkOutAt != null
}

private const val TICK_INTERVAL_MS = 30_000L

class AttendanceViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()
    private val todayDate = AttendanceTimeFormat.today()
    private val todayKey = AttendanceTimeFormat.dateKey(todayDate)

    private val monthStart = todayDate.withDayOfMonth(1)
    private val monthEnd = todayDate.withDayOfMonth(todayDate.lengthOfMonth())
    private val weekStart = todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val weekEnd = todayDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    private var rangeRecords: Map<LocalDate, DailyAttendance> = emptyMap()

    private val _uiState = MutableStateFlow(PunchUiState())
    val uiState: StateFlow<PunchUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, isSummaryLoading = false, errorMessage = "You're not signed in.") }
        } else {
            observeRecord(uid)
            observeMonthRange(uid)
            observeConnectivity()
            tickElapsedWhileRunning()
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
            )
            AttendanceRangeMode.WEEK -> AttendanceStats.summarize(
                records = rangeRecords,
                rangeStart = weekStart,
                rangeEnd = weekEnd,
                today = todayDate,
                rangeLabel = AttendanceTimeFormat.weekRangeLabel(weekStart, weekEnd),
            )
        }
        _uiState.update { it.copy(isSummaryLoading = false, summary = summary) }
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

    private fun computeElapsed(checkInAt: Instant?, checkOutAt: Instant?): Duration {
        val start = checkInAt ?: return Duration.ZERO
        val end = checkOutAt ?: Instant.now()
        return Duration.between(start, end).let { if (it.isNegative) Duration.ZERO else it }
    }

    fun checkIn() {
        val uid = authRepository.currentUser?.uid ?: return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            attendanceRepository.checkIn(uid, todayKey)
                .onSuccess { _uiState.update { it.copy(isSubmitting = false) } }
                .onFailure { error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message) } }
        }
    }

    fun checkOut() {
        val uid = authRepository.currentUser?.uid ?: return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            attendanceRepository.checkOut(uid, todayKey)
                .onSuccess { _uiState.update { it.copy(isSubmitting = false) } }
                .onFailure { error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message) } }
        }
    }

    /** Corrects today's punch times — e.g. a forgotten check-in or a check-out that ran a few minutes late. */
    fun editTodayTimes(checkInTime: LocalTime, checkOutTime: LocalTime?) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            attendanceRepository.setManualTimes(
                uid = uid,
                date = todayKey,
                checkInAt = checkInTime.toTimestamp(todayDate),
                checkOutAt = checkOutTime?.toTimestamp(todayDate),
            ).onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    /** Backfills a day that was never punched — a full check-in/check-out pair for a past date. */
    fun addPastAttendance(date: LocalDate, checkInTime: LocalTime, checkOutTime: LocalTime) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            attendanceRepository.setManualTimes(
                uid = uid,
                date = AttendanceTimeFormat.dateKey(date),
                checkInAt = checkInTime.toTimestamp(date),
                checkOutAt = checkOutTime.toTimestamp(date),
            ).onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    private fun LocalTime.toTimestamp(date: LocalDate): Timestamp =
        Timestamp(Date.from(date.atTime(this).atZone(ZoneId.systemDefault()).toInstant()))
}
