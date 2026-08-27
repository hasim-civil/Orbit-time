package com.hasim.orbittime.ui.screens.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.notification.NotificationRepository
import com.hasim.orbittime.data.notification.UserNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<UserNotification> = emptyList(),
    val errorMessage: String? = null,
)

/** Backs the Notifications screen with a live Firestore feed — real, persisted alerts (today
 * that means late-arrival check-ins, logged by [com.hasim.orbittime.ui.screens.punch.AttendanceViewModel]
 * the moment a check-in actually lands late), not a static mock list. */
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val notificationRepository = NotificationRepository()

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val uid = authRepository.currentUser?.uid

    init {
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                notificationRepository.observeNotifications(uid)
                    .catch { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
                    .collect { list -> _uiState.update { it.copy(isLoading = false, notifications = list) } }
            }
        }
    }

    fun markRead(id: String) {
        val uid = uid ?: return
        viewModelScope.launch { notificationRepository.markRead(uid, id) }
    }

    fun clearAll() {
        val uid = uid ?: return
        viewModelScope.launch { notificationRepository.clearAll(uid) }
    }
}
