package com.hasim.orbittime.ui.screens.timesheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceStatus
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.observeIsOnline
import java.time.Instant
import java.time.LocalDate
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
)

data class TimesheetUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
    val displayedMonth: LocalDate = AttendanceTimeFormat.today().withDayOfMonth(1),
    val monthLabel: String = "",
    val days: List<TimesheetDay> = emptyList(),
    val history: List<TimesheetDay> = emptyList(),
)

class TimesheetViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()

    private val _uiState = MutableStateFlow(TimesheetUiState())
    val uiState: StateFlow<TimesheetUiState> = _uiState.asStateFlow()

    private var rangeJob: Job? = null

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            observeConnectivity()
            observeMonth(uid, _uiState.value.displayedMonth)
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
                    val today = AttendanceTimeFormat.today()
                    val byDate = records.mapNotNull { record ->
                        runCatching { LocalDate.parse(record.date) }.getOrNull()?.let { it to record }
                    }.toMap()

                    val days = (1..monthStart.lengthOfMonth()).map { dayOfMonth ->
                        val date = monthStart.withDayOfMonth(dayOfMonth)
                        val record = byDate[date]
                        val checkInAt = record?.checkInAt?.toDate()?.toInstant()
                        val checkOutAt = record?.checkOutAt?.toDate()?.toInstant()
                        TimesheetDay(
                            date = date,
                            checkInAt = checkInAt,
                            checkOutAt = checkOutAt,
                            status = AttendanceStats.classifyDay(checkInAt, date, today),
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            monthLabel = AttendanceTimeFormat.monthLabel(monthStart),
                            days = days,
                            history = days.filter { day -> day.checkInAt != null }.sortedByDescending { day -> day.date },
                        )
                    }
                }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            observeIsOnline(getApplication()).collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }
}
