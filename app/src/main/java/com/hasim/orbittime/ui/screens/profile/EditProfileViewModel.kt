package com.hasim.orbittime.ui.screens.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.storage.StorageRepository
import com.hasim.orbittime.data.user.UserProfile
import com.hasim.orbittime.data.user.UserProfileRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val ShiftTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val photoUrl: String = "",
    val localPhotoPreview: Uri? = null,
    val shiftStart: LocalTime = LocalTime.of(9, 0),
    val shiftEnd: LocalTime = LocalTime.of(17, 30),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

/** Backs the Edit Profile screen: loads the real signed-in user's data, then saves any changes
 * back through the same [AuthRepository] (name/email/password) and [UserProfileRepository]
 * (role/shift/photo) every other screen already uses, plus [StorageRepository] for the photo. */
class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val profileRepository = UserProfileRepository()
    private val storageRepository = StorageRepository()

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var pendingPhotoUri: Uri? = null

    init {
        val user = authRepository.currentUser
        _uiState.update { it.copy(email = user?.email.orEmpty(), name = user?.displayName.orEmpty()) }

        val uid = user?.uid
        if (uid != null) {
            viewModelScope.launch {
                val profile = runCatching { profileRepository.getProfile(uid) }.getOrNull()
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        name = profile?.name?.takeIf { it.isNotBlank() } ?: current.name,
                        role = profile?.role.orEmpty(),
                        photoUrl = profile?.photoUrl.orEmpty(),
                        shiftStart = profile?.shiftStart?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: current.shiftStart,
                        shiftEnd = profile?.shiftEnd?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: current.shiftEnd,
                    )
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        }
    }

    fun onPhotoPicked(uri: Uri) {
        pendingPhotoUri = uri
        _uiState.update { it.copy(localPhotoPreview = uri) }
    }

    fun save(
        name: String,
        email: String,
        role: String,
        shiftStart: LocalTime,
        shiftEnd: LocalTime,
        newPassword: String,
    ) {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(errorMessage = "You're not signed in.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            var firstError: String? = null

            var uploadedPhotoUrl: String? = null
            pendingPhotoUri?.let { uri ->
                storageRepository.uploadProfilePhoto(uid, uri)
                    .onSuccess { url -> uploadedPhotoUrl = url }
                    .onFailure { error -> firstError = firstError ?: error.message }
            }

            val trimmedName = name.trim()
            if (trimmedName.isNotBlank() && trimmedName != authRepository.currentUser?.displayName) {
                authRepository.updateDisplayName(trimmedName)
                    .onFailure { error -> firstError = firstError ?: error.message }
            }

            val trimmedEmail = email.trim()
            if (trimmedEmail.isNotBlank() && trimmedEmail != authRepository.currentUser?.email) {
                authRepository.updateEmail(trimmedEmail)
                    .onFailure { error -> firstError = firstError ?: error.message }
            }

            if (newPassword.isNotBlank()) {
                authRepository.updatePassword(newPassword)
                    .onFailure { error -> firstError = firstError ?: error.message }
            }

            val existing = runCatching { profileRepository.getProfile(uid) }.getOrNull()
            val profile = UserProfile(
                uid = uid,
                name = trimmedName,
                email = authRepository.currentUser?.email ?: trimmedEmail,
                role = role.trim(),
                photoUrl = uploadedPhotoUrl ?: existing?.photoUrl.orEmpty(),
                shiftStart = ShiftTimeFormatter.format(shiftStart),
                shiftEnd = ShiftTimeFormatter.format(shiftEnd),
            )
            runCatching { profileRepository.saveProfile(profile) }
                .onFailure { error -> firstError = firstError ?: (error.message ?: "Couldn't save your profile.") }

            _uiState.update { it.copy(isSaving = false, errorMessage = firstError, saved = firstError == null) }
        }
    }
}
