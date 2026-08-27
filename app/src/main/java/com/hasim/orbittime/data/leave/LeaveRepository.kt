package com.hasim.orbittime.data.leave

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LeaveException(message: String) : Exception(message)

/** Reads and writes a user's own personal leave records — no manager/admin workflow. */
class LeaveRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun leavesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("leaves")

    fun observeLeaves(uid: String): Flow<List<LeaveRecord>> = callbackFlow {
        val registration = leavesCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(LeaveException(mapFirestoreErrorMessage(error)))
                return@addSnapshotListener
            }
            val leaves = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<LeaveRecord>()?.copy(id = doc.id)
            } ?: emptyList()
            trySend(leaves)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveLeave(uid: String, leave: LeaveRecord): Result<Unit> = runCatching {
        val collection = leavesCollection(uid)
        if (leave.id.isBlank()) {
            collection.add(leave.copy(id = "")).await()
        } else {
            collection.document(leave.id).set(leave).await()
        }
        Unit
    }.recoverCatching { throwable -> throw LeaveException(mapFirestoreErrorMessage(throwable)) }

    suspend fun deleteLeave(uid: String, leaveId: String): Result<Unit> = runCatching {
        leavesCollection(uid).document(leaveId).delete().await()
        Unit
    }.recoverCatching { throwable -> throw LeaveException(mapFirestoreErrorMessage(throwable)) }

    private fun mapFirestoreErrorMessage(throwable: Throwable): String = when {
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You don't have permission to do that."
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "You're offline. Connect to the internet and try again."
        else -> "Something went wrong. Please try again."
    }
}
