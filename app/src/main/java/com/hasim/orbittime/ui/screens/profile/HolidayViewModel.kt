package com.hasim.orbittime.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.holiday.HolidayRecord
import com.hasim.orbittime.data.holiday.HolidayRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HolidayUiState(
    val isLoading: Boolean = true,
    val holidays: List<HolidayRecord> = emptyList(),
    val errorMessage: String? = null,
)

/** Backs the Holiday List screen — a purely personal, self-managed calendar. */
class HolidayViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val holidayRepository = HolidayRepository()

    private val _uiState = MutableStateFlow(HolidayUiState())
    val uiState: StateFlow<HolidayUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            viewModelScope.launch {
                holidayRepository.observeHolidays(uid)
                    .catch { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
                    .collect { holidays ->
                        val sorted = holidays.sortedBy { runCatching { LocalDate.parse(it.date) }.getOrNull() ?: LocalDate.MAX }
                        _uiState.update { it.copy(isLoading = false, holidays = sorted) }
                    }
            }
        }
    }

    fun saveHoliday(holiday: HolidayRecord) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            holidayRepository.saveHoliday(uid, holiday)
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun deleteHoliday(holidayId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            holidayRepository.deleteHoliday(uid, holidayId)
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }
}
