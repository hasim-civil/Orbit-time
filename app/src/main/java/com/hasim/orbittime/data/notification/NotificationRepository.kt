package com.hasim.orbittime.data.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.hasim.orbittime.util.OrbitClock
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val NOTIFICATIONS_SUBCOLLECTION = "notifications"

/** Created-at, on the app's own clock ([OrbitClock]) rather than `Timestamp.now()`, so a
 * notification's "5 minutes ago" is measured against the same clock the rest of the app runs on. */
private fun appNow(): Timestamp = Timestamp(Date.from(OrbitClock.now()))

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
            UserNotification(kind = NotificationKind.LATE_ARRIVAL, title = title, body = body, createdAt = appNow()),
        ).await()
        Unit
    }

    /**
     * Creates a notification under a caller-chosen, deterministic [id] — a no-op if one with that
     * id already exists. Used for facts that get re-evaluated repeatedly (a specific missed date,
     * a given calendar month's late-arrival allowance) so the same fact is never reported twice,
     * and re-checking never resets an already-read notification back to unread.
     */
    suspend fun createIfMissing(uid: String, id: String, notification: UserNotification): Result<Unit> = runCatching {
        val doc = collection(uid).document(id)
        val exists = doc.get().await().exists()
        if (!exists) {
            doc.set(notification.copy(id = id, createdAt = appNow())).await()
        }
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
