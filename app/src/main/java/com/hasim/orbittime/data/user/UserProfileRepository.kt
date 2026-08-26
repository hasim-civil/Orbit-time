package com.hasim.orbittime.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
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
}
