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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val initials: String = "?",
    val role: String = "",
    val photoBase64: String = "",
    val shiftStart: String? = null,
    val shiftEnd: String? = null,
)

/** Loads the signed-in user's name/email (from Firebase Auth) and role/shift/photo (from their
 * Firestore profile document) for the Profile screen — reuses the same repositories
 * every other screen already uses, no new Firebase logic. */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val profileRepository = UserProfileRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Re-reads the current user + Firestore profile — called after Edit Profile saves changes,
     * so the Profile screen reflects the update immediately without needing to reopen the tab. */
    fun refresh() = load()

    private fun load() {
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
                val profile = runCatching { profileRepository.getProfile(uid) }.getOrNull()
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            name = profile.name.takeIf { name -> name.isNotBlank() } ?: it.name,
                            email = profile.email.takeIf { email -> email.isNotBlank() } ?: it.email,
                            role = profile.role,
                            photoBase64 = profile.photoBase64,
                            shiftStart = profile.shiftStart,
                            shiftEnd = profile.shiftEnd,
                        )
                    }
                }
            }
        }
    }
}
