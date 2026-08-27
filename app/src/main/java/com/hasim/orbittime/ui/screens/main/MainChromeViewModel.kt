package com.hasim.orbittime.ui.screens.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.notification.NotificationRepository
import com.hasim.orbittime.data.user.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainChromeUiState(
    val photoBase64: String = "",
    val hasUnreadNotifications: Boolean = false,
)

/** Feeds the one piece of state every tab's top bar needs in common — the signed-in user's real
 * profile photo and whether they have unread alerts — from a single pair of live Firestore
 * listeners shared across Home/Punch/Timesheet/Reports/Profile, so the avatar and bell dot update
 * everywhere the instant either changes, with no manual refresh wiring per screen. */
class MainChromeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val profileRepository = UserProfileRepository()
    private val notificationRepository = NotificationRepository()

    private val _uiState = MutableStateFlow(MainChromeUiState())
    val uiState: StateFlow<MainChromeUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                profileRepository.observeProfile(uid)
                    .catch { }
                    .collect { profile -> _uiState.update { it.copy(photoBase64 = profile?.photoBase64.orEmpty()) } }
            }
            viewModelScope.launch {
                notificationRepository.observeNotifications(uid)
                    .catch { }
                    .collect { list -> _uiState.update { it.copy(hasUnreadNotifications = list.any { n -> !n.read }) } }
            }
        }
    }
}
