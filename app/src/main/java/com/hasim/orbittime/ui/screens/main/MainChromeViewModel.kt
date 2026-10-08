package com.hasim.orbittime.ui.screens.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.holiday.HolidayRepository
import com.hasim.orbittime.data.notification.NotificationRepository
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.util.HolidayAccess
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
    private val holidayRepository = HolidayRepository()

    private val _uiState = MutableStateFlow(MainChromeUiState())
    val uiState: StateFlow<MainChromeUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        val uid = user?.uid
        if (user != null && HolidayAccess.canManageHolidays(user.email, user.isEmailVerified)) {
            // Holidays used to be per-user. The Holiday Manager's own are the organization's, so
            // they're published to the shared collection once, as soon as the manager is in the
            // app — nobody else's are copied anywhere. Idempotent; a failure retries next launch.
            viewModelScope.launch { holidayRepository.migrateLegacyHolidays(user.uid) }
        }
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
