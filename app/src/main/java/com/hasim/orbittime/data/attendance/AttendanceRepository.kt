package com.hasim.orbittime.data.attendance

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Carries an already-friendly, user-facing message up to the ViewModel layer. */
class AttendanceException(message: String) : Exception(message)

/**
 * Reads and writes each user's daily attendance. Check-in/check-out both run
 * inside a Firestore transaction — read-then-write atomically on the server —
 * so a duplicate check-in or an out-of-order check-out can't slip through
 * even under a double-tap or a race between devices.
 */
class AttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun dayDoc(uid: String, date: String) =
        firestore.collection("users").document(uid).collection("attendance").document(date)

    /** Real-time view of one day's record; reflects Firestore's local cache immediately, even offline. */
    fun observeRecord(uid: String, date: String): Flow<AttendanceRecord?> = callbackFlow {
        val registration = dayDoc(uid, date).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapFirestoreError(error))
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject<AttendanceRecord>())
        }
        awaitClose { registration.remove() }
    }

    suspend fun checkIn(uid: String, date: String): Result<Unit> = runCatching {
        val docRef = dayDoc(uid, date)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(docRef).toObject<AttendanceRecord>()
            when {
                existing?.isCheckedIn == true ->
                    throw AttendanceException("You're already checked in.")
                existing?.isCompleted == true ->
                    throw AttendanceException("You've already completed today's attendance.")
                else ->
                    transaction.set(docRef, AttendanceRecord(date = date, checkInAt = Timestamp.now()))
            }
        }.await()
    }.recoverCatching { throwable ->
        throw if (throwable is AttendanceException) throwable else AttendanceException(mapFirestoreErrorMessage(throwable))
    }

    suspend fun checkOut(uid: String, date: String): Result<Unit> = runCatching {
        val docRef = dayDoc(uid, date)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(docRef).toObject<AttendanceRecord>()
            when {
                existing?.checkInAt == null ->
                    throw AttendanceException("You need to check in first.")
                existing.checkOutAt != null ->
                    throw AttendanceException("You've already checked out today.")
                else ->
                    transaction.update(docRef, "checkOutAt", Timestamp.now())
            }
        }.await()
    }.recoverCatching { throwable ->
        throw if (throwable is AttendanceException) throwable else AttendanceException(mapFirestoreErrorMessage(throwable))
    }

    private fun mapFirestoreError(error: FirebaseFirestoreException): AttendanceException =
        AttendanceException(mapFirestoreErrorMessage(error))

    /**
     * Firestore transactions (used for check-in/out, below) need a live round trip to the
     * server to guarantee atomicity, so they fail outright when offline rather than queueing —
     * unlike plain writes. The message reflects that: retry once back online, don't wait for it.
     */
    private fun mapFirestoreErrorMessage(throwable: Throwable): String = when {
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You don't have permission to do that."
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "You're offline. Connect to the internet to check in or out."
        else -> "Something went wrong. Please try again."
    }
}
