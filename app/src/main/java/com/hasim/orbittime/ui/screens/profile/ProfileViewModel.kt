package com.hasim.orbittime.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.util.UserDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val initials: String = "?",
    val role: String = "",
    val company: String = "",
    val shiftStart: String? = null,
    val shiftEnd: String? = null,
)

/** Loads the signed-in user's name/email (from Firebase Auth) and role/shift/company (from their
 * Firestore profile document) for the Profile screen — reuses the same repositories every other
 * screen already uses, no new Firebase logic. A live snapshot listener keeps this state (and so
 * the Profile screen) in sync the instant Edit Profile saves, with no manual refresh needed. */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val profileRepository = UserProfileRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        val fallbackName = user?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@").orEmpty()
        _uiState.update {
            it.copy(
                name = fallbackName,
                email = user?.email.orEmpty(),
                initials = UserDisplay.initials(user),
            )
        }

        val uid = user?.uid
        if (uid != null) {
            viewModelScope.launch {
                profileRepository.observeProfile(uid)
                    .catch { /* Auth-derived name/email above still render if this listener fails. */ }
                    .collect { profile ->
                        if (profile != null) {
                            _uiState.update {
                                it.copy(
                                    name = profile.name.takeIf { name -> name.isNotBlank() } ?: it.name,
                                    email = profile.email.takeIf { email -> email.isNotBlank() } ?: it.email,
                                    role = profile.role,
                                    company = profile.company,
                                    shiftStart = profile.shiftStart,
                                    shiftEnd = profile.shiftEnd,
                                )
                            }
                        }
                    }
            }
        }
    }
}
