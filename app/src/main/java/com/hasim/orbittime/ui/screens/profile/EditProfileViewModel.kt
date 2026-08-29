package com.hasim.orbittime.ui.screens.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.account.AccountDeletionRepository
import com.hasim.orbittime.data.account.AccountDeletionRequiresReauthException
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.user.UserProfile
import com.hasim.orbittime.data.user.UserProfileRepository
import com.hasim.orbittime.reminder.ShiftReminderScheduler
import com.hasim.orbittime.util.ImageCodec
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ShiftTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val company: String = "",
    val photoBase64: String = "",
    val localPhotoPreview: Uri? = null,
    val shiftStart: LocalTime = LocalTime.of(9, 0),
    val shiftEnd: LocalTime = LocalTime.of(17, 30),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isDeletingAccount: Boolean = false,
    val deleteError: String? = null,
    val needsReauthToDelete: Boolean = false,
    val canReauthWithPassword: Boolean = true,
)

/** Backs the Edit Profile screen: loads the real signed-in user's data, then saves any changes
 * back through the same [AuthRepository] (name/email/password) and [UserProfileRepository]
 * (role/shift/photo) every other screen already uses — the photo is compressed and stored
 * inline via [ImageCodec] rather than Firebase Storage, which needs the paid Blaze plan. */
class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val profileRepository = UserProfileRepository()
    private val accountDeletionRepository = AccountDeletionRepository()

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
                        company = profile?.company.orEmpty(),
                        photoBase64 = profile?.photoBase64.orEmpty(),
                        shiftStart = profile?.shiftStart?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: current.shiftStart,
                        shiftEnd = profile?.shiftEnd?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: current.shiftEnd,
                    )
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "You're not signed in.") }
        }
    }

    /** Clears a leftover error banner from a previous visit — this ViewModel is scoped to the
     * whole signed-in session, not just one visit to this screen, so it outlives a single "in
     * and back out" round trip. */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null, deleteError = null) }
    }

    fun onPhotoPicked(uri: Uri) {
        pendingPhotoUri = uri
        _uiState.update { it.copy(localPhotoPreview = uri) }
    }

    fun save(
        name: String,
        email: String,
        role: String,
        company: String,
        shiftStart: LocalTime,
        shiftEnd: LocalTime,
        newPassword: String,
        onSaved: () -> Unit,
    ) {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(errorMessage = "You're not signed in.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            var firstError: String? = null

            var encodedPhoto: String? = null
            pendingPhotoUri?.let { uri ->
                val encoded = withContext(Dispatchers.IO) {
                    runCatching { ImageCodec.compressToBase64(getApplication(), uri) }.getOrNull()
                }
                if (encoded != null) {
                    encodedPhoto = encoded
                } else {
                    firstError = firstError ?: "Couldn't process that photo. Please pick another."
                }
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

            // The current photo is already known — it was loaded into this screen's own state
            // when it opened (or just replaced above via a freshly-picked one) — so there's no
            // need for an extra Firestore read here just to re-fetch a value already in hand.
            val profile = UserProfile(
                uid = uid,
                name = trimmedName,
                email = authRepository.currentUser?.email ?: trimmedEmail,
                role = role.trim(),
                company = company.trim(),
                photoBase64 = encodedPhoto ?: _uiState.value.photoBase64,
                shiftStart = ShiftTimeFormatter.format(shiftStart),
                shiftEnd = ShiftTimeFormatter.format(shiftEnd),
            )
            runCatching { profileRepository.saveProfile(profile) }
                .onFailure { error -> firstError = firstError ?: (error.message ?: "Couldn't save your profile.") }

            val succeeded = firstError == null
            _uiState.update {
                if (!succeeded) {
                    it.copy(isSaving = false, errorMessage = firstError)
                } else {
                    it.copy(
                        isSaving = false,
                        errorMessage = null,
                        name = profile.name,
                        email = profile.email,
                        role = profile.role,
                        company = profile.company,
                        photoBase64 = profile.photoBase64,
                        localPhotoPreview = null,
                        shiftStart = shiftStart,
                        shiftEnd = shiftEnd,
                    )
                }
            }
            if (succeeded) {
                pendingPhotoUri = null
                onSaved()
            }
        }
    }

    /** Cancels a pending re-authentication prompt — called when the user backs out of the
     * delete-account dialog instead of confirming their password. */
    fun dismissReauth() {
        _uiState.update { it.copy(needsReauthToDelete = false, deleteError = null) }
    }

    /**
     * Permanently deletes the signed-in user's account and everything this app ever stored for
     * them. [password] is only needed on a retry after [EditProfileUiState.needsReauthToDelete]
     * comes back true — Firebase requires a fresh sign-in before it will delete an Auth account,
     * and a stale session (the common case, since this ViewModel — and the session it belongs
     * to — can live for hours) won't count as fresh.
     */
    fun deleteAccount(password: String?, onDeleted: () -> Unit) {
        if (_uiState.value.isDeletingAccount) return
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(deleteError = "You're not signed in.") }
            return
        }
        _uiState.update { it.copy(isDeletingAccount = true, deleteError = null, needsReauthToDelete = false) }
        viewModelScope.launch {
            var reauthError: String? = null
            if (password != null) {
                authRepository.reauthenticateWithPassword(password)
                    .onFailure { error -> reauthError = error.message }
            }

            if (reauthError != null) {
                _uiState.update { it.copy(isDeletingAccount = false, deleteError = reauthError) }
                return@launch
            }

            accountDeletionRepository.deleteAccount(uid)
                .onSuccess {
                    ShiftReminderScheduler.cancel(getApplication())
                    _uiState.update { it.copy(isDeletingAccount = false) }
                    onDeleted()
                }
                .onFailure { error ->
                    if (error is AccountDeletionRequiresReauthException) {
                        _uiState.update {
                            it.copy(
                                isDeletingAccount = false,
                                needsReauthToDelete = true,
                                canReauthWithPassword = authRepository.currentUserHasPasswordCredential,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isDeletingAccount = false, deleteError = error.message) }
                    }
                }
        }
    }
}
