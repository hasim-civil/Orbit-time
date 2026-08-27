package com.hasim.orbittime.data.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val NOTIFICATIONS_SUBCOLLECTION = "notifications"

/** Reads and writes a user's personal notification feed — one Firestore subcollection per user,
 * same pattern as their attendance/holidays/leaves subcollections. */
class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun collection(uid: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(uid).collection(NOTIFICATIONS_SUBCOLLECTION)

    fun observeNotifications(uid: String): Flow<List<UserNotification>> = callbackFlow {
        val registration = collection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject<UserNotification>()?.copy(id = doc.id)
                }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addLateArrival(uid: String, title: String, body: String): Result<Unit> = runCatching {
        collection(uid).add(
            UserNotification(kind = NotificationKind.LATE_ARRIVAL, title = title, body = body, createdAt = Timestamp.now()),
        ).await()
        Unit
    }

    suspend fun markRead(uid: String, id: String): Result<Unit> = runCatching {
        collection(uid).document(id).update("read", true).await()
        Unit
    }

    suspend fun clearAll(uid: String): Result<Unit> = runCatching {
        deleteAllDocuments(collection(uid))
    }

    internal suspend fun deleteAllDocuments(collection: CollectionReference) {
        val snapshot = collection.get().await()
        snapshot.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
    }
}
