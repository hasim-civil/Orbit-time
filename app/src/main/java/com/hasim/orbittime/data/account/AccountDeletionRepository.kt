package com.hasim.orbittime.data.account

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class AccountDeletionException(message: String) : Exception(message)

/** Thrown when Firebase requires a fresher sign-in before the account itself can be deleted —
 * the caller should re-authenticate and call [AccountDeletionRepository.deleteAccount] again. */
class AccountDeletionRequiresReauthException :
    Exception("Please confirm your password to finish deleting your account.")

/**
 * Permanently erases everything this app ever wrote for one user: their attendance, holiday and
 * leave subcollections, their `users/{uid}` profile document (which already holds their photo
 * inline as base64 — this app has no Firebase Storage usage to separately clean up), and finally
 * their Firebase Authentication account itself.
 *
 * Firestore data is deleted BEFORE the Auth account both so a failed run is safely retryable
 * (re-deleting an already-empty collection is a no-op) and because the security rules require
 * `request.auth.uid == uid` — deleting the Auth account first would sign the user out and make
 * every subsequent Firestore delete fail permission checks.
 */
class AccountDeletionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun deleteAccount(uid: String): Result<Unit> = runCatching {
        val userDoc = firestore.collection("users").document(uid)
        deleteAllDocuments(userDoc.collection("attendance"))
        deleteAllDocuments(userDoc.collection("holidays"))
        deleteAllDocuments(userDoc.collection("leaves"))
        userDoc.delete().await()

        val user = auth.currentUser ?: error("You're not signed in.")
        user.delete().await()
        Unit
    }.recoverCatching { throwable ->
        if (throwable is FirebaseAuthRecentLoginRequiredException) {
            throw AccountDeletionRequiresReauthException()
        }
        throw AccountDeletionException(mapDeletionErrorMessage(throwable))
    }

    /** Firestore batches cap out at 500 writes, so a long-lived user's attendance history is
     * deleted in safely-sized chunks rather than one batch per collection. */
    private suspend fun deleteAllDocuments(collection: CollectionReference) {
        val snapshot = collection.get().await()
        snapshot.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
    }

    private fun mapDeletionErrorMessage(throwable: Throwable): String = when {
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You don't have permission to do that."
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "You're offline. Connect to the internet and try again."
        else -> "Couldn't delete your account. Please try again."
    }
}
