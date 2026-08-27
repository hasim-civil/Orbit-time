package com.hasim.orbittime.data.holiday

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HolidayException(message: String) : Exception(message)

/** Reads and writes a user's own personal holiday calendar — self-managed, no company data source. */
class HolidayRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun holidaysCollection(uid: String) =
        firestore.collection("users").document(uid).collection("holidays")

    fun observeHolidays(uid: String): Flow<List<HolidayRecord>> = callbackFlow {
        val registration = holidaysCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(HolidayException(mapFirestoreErrorMessage(error)))
                return@addSnapshotListener
            }
            val holidays = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<HolidayRecord>()?.copy(id = doc.id)
            } ?: emptyList()
            trySend(holidays)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveHoliday(uid: String, holiday: HolidayRecord): Result<Unit> = runCatching {
        val collection = holidaysCollection(uid)
        if (holiday.id.isBlank()) {
            collection.add(holiday.copy(id = "")).await()
        } else {
            collection.document(holiday.id).set(holiday).await()
        }
        Unit
    }.recoverCatching { throwable -> throw HolidayException(mapFirestoreErrorMessage(throwable)) }

    suspend fun deleteHoliday(uid: String, holidayId: String): Result<Unit> = runCatching {
        holidaysCollection(uid).document(holidayId).delete().await()
        Unit
    }.recoverCatching { throwable -> throw HolidayException(mapFirestoreErrorMessage(throwable)) }

    private fun mapFirestoreErrorMessage(throwable: Throwable): String = when {
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You don't have permission to do that."
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "You're offline. Connect to the internet and try again."
        else -> "Something went wrong. Please try again."
    }
}
