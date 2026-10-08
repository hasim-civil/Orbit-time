package com.hasim.orbittime.data.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
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
                }.filterNot { it.dismissed }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    /** Every notification, dismissed ones included, straight from the server (fails offline) —
     * what the attendance rules compare against before changing anything. */
    suspend fun fetchAllFromServer(uid: String): List<UserNotification> =
        collection(uid).get(Source.SERVER).await().documents.mapNotNull { doc ->
            doc.toObject<UserNotification>()?.copy(id = doc.id)
        }

    /**
     * Applies one round of rule decisions atomically. Each create uses its deterministic id inside
     * a transaction that first checks the document is still absent, so two devices (or two quick
     * re-runs) can't both create it, and an existing — even dismissed — one is never overwritten.
     */
    suspend fun applyChanges(
        uid: String,
        creates: List<UserNotification>,
        retractions: Map<String, String>,
        restores: Set<String>,
    ) {
        creates.forEach { notification ->
            val doc = collection(uid).document(notification.id)
            firestore.runTransaction { tx ->
                if (!tx.get(doc).exists()) {
                    tx.set(doc, notification.copy(createdAt = appNow(), rulesVersion = CURRENT_NOTIFICATION_RULES_VERSION))
                }
                Unit
            }.await()
        }
        if (retractions.isEmpty() && restores.isEmpty()) return
        val batch = firestore.batch()
        retractions.forEach { (id, reason) ->
            // Read, too: a withdrawn alert shouldn't keep lighting the unread dot.
            batch.update(collection(uid).document(id), mapOf("retracted" to true, "retractedReason" to reason, "read" to true))
        }
        restores.forEach { id ->
            batch.update(collection(uid).document(id), mapOf("retracted" to false, "retractedReason" to ""))
        }
        batch.commit().await()
    }

    suspend fun markRead(uid: String, id: String): Result<Unit> = runCatching {
        collection(uid).document(id).update("read", true).await()
        Unit
    }

    /** Hides every notification instead of deleting it: a deleted one would simply be created
     * again the next time the attendance rules run, since its fact is still true. */
    suspend fun clearAll(uid: String): Result<Unit> = runCatching {
        val snapshot = collection(uid).get().await()
        snapshot.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.update(doc.reference, mapOf("dismissed" to true, "read" to true)) }
            batch.commit().await()
        }
    }
}
