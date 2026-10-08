package com.hasim.orbittime.data.holiday

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.toObject
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HolidayException(message: String) : Exception(message)

/**
 * The organization's holiday calendar: one shared `holidays` collection that every employee
 * reads, so a holiday added once applies to everyone — nothing is copied per user.
 *
 * Only the Holiday Manager can write it; firestore.rules enforces that, and a write from anyone
 * else fails with PERMISSION_DENIED (surfaced as "You don't have permission to do that.").
 */
class HolidayRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val holidaysCollection get() = firestore.collection(GLOBAL_COLLECTION)

    /** Where holidays lived before they became organization-wide: `users/{uid}/holidays`. Read
     * only by [migrateLegacyHolidays]. */
    private fun legacyHolidaysCollection(uid: String) =
        firestore.collection("users").document(uid).collection(LEGACY_SUBCOLLECTION)

    fun observeHolidays(): Flow<List<HolidayRecord>> = callbackFlow {
        val registration = holidaysCollection.addSnapshotListener { snapshot, error ->
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

    /** One-shot read, served from the cache when offline — for the background shift reminder. */
    suspend fun fetchHolidays(): List<HolidayRecord> =
        holidaysCollection.get().await()
            .documents.mapNotNull { doc -> doc.toObject<HolidayRecord>()?.copy(id = doc.id) }

    /** One-shot read straight from the server (fails offline) — see
     * [com.hasim.orbittime.data.attendance.AttendanceRepository.fetchRangeFromServer]. */
    suspend fun fetchHolidaysFromServer(): List<HolidayRecord> =
        holidaysCollection.get(Source.SERVER).await()
            .documents.mapNotNull { doc -> doc.toObject<HolidayRecord>()?.copy(id = doc.id) }

    suspend fun saveHoliday(holiday: HolidayRecord): Result<Unit> = runCatching {
        val fields = holiday.toFields()
        if (holiday.id.isBlank()) {
            holidaysCollection.add(fields).await()
        } else {
            holidaysCollection.document(holiday.id).set(fields).await()
        }
        Unit
    }.recoverCatching { throwable -> throw HolidayException(mapFirestoreErrorMessage(throwable)) }

    suspend fun deleteHoliday(holidayId: String): Result<Unit> = runCatching {
        holidaysCollection.document(holidayId).delete().await()
        Unit
    }.recoverCatching { throwable -> throw HolidayException(mapFirestoreErrorMessage(throwable)) }

    /**
     * Carries the Holiday Manager's own pre-existing personal holidays into the shared
     * collection, so the holidays they had already been maintaining become everyone's without
     * being re-entered — and without copying anything into any other user's account.
     *
     * Non-destructive and one-time per holiday: each legacy document keeps its data and is only
     * stamped with `migratedToGlobal`, in the same batch that writes its global copy under the
     * same document id. A holiday the manager later deletes globally therefore never comes back
     * on a later run. Other users' legacy holidays are never read or touched here.
     */
    suspend fun migrateLegacyHolidays(uid: String): Result<Int> = runCatching {
        val pending = legacyHolidaysCollection(uid).get().await().documents
            .filter { it.getBoolean(MIGRATED_FIELD) != true }
            // A legacy entry without a real date can't be a holiday on any day (and the rules
            // would refuse it), so it stays where it is rather than failing the whole batch.
            .filter { doc -> doc.getString("date")?.let { runCatching { LocalDate.parse(it) }.isSuccess } == true }
        pending.chunked(200).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc ->
                val record = HolidayRecord(
                    name = doc.getString("name").orEmpty().take(100),
                    date = doc.getString("date").orEmpty(),
                    description = doc.getString("description").orEmpty().take(500),
                )
                batch.set(holidaysCollection.document(doc.id), record.toFields())
                batch.update(doc.reference, MIGRATED_FIELD, true)
            }
            batch.commit().await()
        }
        pending.size
    }.recoverCatching { throwable -> throw HolidayException(mapFirestoreErrorMessage(throwable)) }

    /** Exactly the fields firestore.rules accepts for a holiday — never the document id. */
    private fun HolidayRecord.toFields(): Map<String, Any> =
        mapOf("name" to name, "date" to date, "description" to description)

    private fun mapFirestoreErrorMessage(throwable: Throwable): String = when {
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You don't have permission to do that."
        throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "You're offline. Connect to the internet and try again."
        else -> "Something went wrong. Please try again."
    }

    private companion object {
        const val GLOBAL_COLLECTION = "holidays"
        const val LEGACY_SUBCOLLECTION = "holidays"
        const val MIGRATED_FIELD = "migratedToGlobal"
    }
}
