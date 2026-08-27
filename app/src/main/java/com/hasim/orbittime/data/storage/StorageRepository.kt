package com.hasim.orbittime.data.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageException(message: String) : Exception(message)

/** Uploads user-owned files (currently just profile photos) to Firebase Storage. */
class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {
    /** Uploads [localUri] as this user's profile photo and returns its public download URL. */
    suspend fun uploadProfilePhoto(uid: String, localUri: Uri): Result<String> = runCatching {
        val photoRef = storage.reference.child("profile_photos/$uid.jpg")
        photoRef.putFile(localUri).await()
        photoRef.downloadUrl.await().toString()
    }.recoverCatching { throwable ->
        throw StorageException(mapStorageErrorMessage(throwable))
    }

    private fun mapStorageErrorMessage(throwable: Throwable): String = when {
        throwable.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
            "You don't have permission to upload a photo."
        throwable.message?.contains("Object does not exist", ignoreCase = true) == true ->
            "That photo couldn't be found. Please pick it again."
        else -> "Couldn't upload your photo. Please try again."
    }
}
