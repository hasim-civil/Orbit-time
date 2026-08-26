package com.hasim.orbittime.ui.screens.punch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.observeIsOnline
import java.time.Duration
import java.time.Instant
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

    private val _uiState = MutableStateFlow(PunchUiState())
    val uiState: StateFlow<PunchUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            observeRecord(uid)
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

    private fun observeConnectivity() {
        viewModelScope.launch {
            observeIsOnline(getApplication()).collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    /** Keeps the elapsed counter moving while a session is running; a no-op once checked out. */
    private fun tickElapsedWhileRunning() {
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                val current = _uiState.value
                if (current.isCheckedIn) {
                    _uiState.update { it.copy(elapsed = computeElapsed(current.checkInAt, current.checkOutAt)) }
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
}
