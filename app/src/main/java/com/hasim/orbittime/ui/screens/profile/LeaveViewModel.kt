package com.hasim.orbittime.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.leave.LeaveRecord
import com.hasim.orbittime.data.leave.LeaveRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaveUiState(
    val isLoading: Boolean = true,
    val leaves: List<LeaveRecord> = emptyList(),
    val errorMessage: String? = null,
)

/** Backs the Add Leave screen — purely personal attendance tracking, no approval workflow. */
class LeaveViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val leaveRepository = LeaveRepository()

    private val _uiState = MutableStateFlow(LeaveUiState())
    val uiState: StateFlow<LeaveUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        } else {
            viewModelScope.launch {
                leaveRepository.observeLeaves(uid)
                    .catch { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
                    .collect { leaves ->
                        val sorted = leaves.sortedByDescending { runCatching { LocalDate.parse(it.startDate) }.getOrNull() ?: LocalDate.MIN }
                        _uiState.update { it.copy(isLoading = false, leaves = sorted) }
                    }
            }
        }
    }

    fun saveLeave(leave: LeaveRecord) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            leaveRepository.saveLeave(uid, leave)
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun deleteLeave(leaveId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            leaveRepository.deleteLeave(uid, leaveId)
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }
}
