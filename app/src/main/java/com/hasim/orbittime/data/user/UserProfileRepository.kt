package com.hasim.orbittime.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"

/** Reads and writes the basic user profile document backing each account. */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun saveProfile(profile: UserProfile) {
        firestore.collection(USERS_COLLECTION)
            .document(profile.uid)
            .set(profile)
            .await()
    }

    suspend fun getProfile(uid: String): UserProfile? {
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        return snapshot.toObject<UserProfile>()
    }

    /** Live profile updates — lets every screen showing the user's photo/name/role stay in sync
     * the moment Edit Profile saves, with no manual refresh plumbing needed. */
    fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<UserProfile>())
            }
        awaitClose { registration.remove() }
    }
}
