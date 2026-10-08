package com.hasim.orbittime.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.holiday.HolidayRecord
import com.hasim.orbittime.data.holiday.HolidayRepository
import com.hasim.orbittime.util.HolidayAccess
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
    val isSaving: Boolean = false,
    /** Whether to show Add/Edit/Delete — UX only; firestore.rules is what actually refuses a
     * write from anyone but the Holiday Manager. */
    val canManageHolidays: Boolean = false,
)

/** Backs the Holiday List screen — the organization's shared holiday calendar. Everyone can
 * view it; only the Holiday Manager ([HolidayAccess]) can change it. */
class HolidayViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val holidayRepository = HolidayRepository()

    private val _uiState = MutableStateFlow(HolidayUiState())
    val uiState: StateFlow<HolidayUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        if (user == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            val canManage = HolidayAccess.canManageHolidays(user.email, user.isEmailVerified)
            _uiState.update { it.copy(canManageHolidays = canManage) }
            viewModelScope.launch {
                holidayRepository.observeHolidays()
                    .catch { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
                    .collect { holidays ->
                        val sorted = holidays.sortedBy { runCatching { LocalDate.parse(it.date) }.getOrNull() ?: LocalDate.MAX }
                        _uiState.update { it.copy(isLoading = false, holidays = sorted) }
                    }
            }
        }
    }

    /** Guarded against a rapid double-tap: [holidayRepository.saveHoliday] creates a brand-new
     * document via `.add()` for a new holiday (blank id), which — unlike a `.set()` to a fixed
     * id — is not naturally idempotent, so firing it twice before the first write lands would
     * create two duplicate holidays. */
    fun saveHoliday(holiday: HolidayRecord) {
        if (!_uiState.value.canManageHolidays || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            holidayRepository.saveHoliday(holiday)
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun deleteHoliday(holidayId: String) {
        if (!_uiState.value.canManageHolidays || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            holidayRepository.deleteHoliday(holidayId)
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
