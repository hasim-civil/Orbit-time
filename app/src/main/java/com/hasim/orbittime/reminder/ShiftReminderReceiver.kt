package com.hasim.orbittime.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.util.AttendanceTimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires ~5 minutes before the user's saved shift start, even with the app closed or backgrounded.
 * Does a single one-shot Firestore read to check whether they've already checked in today (never
 * shows the reminder if so), then always re-arms the next occurrence before finishing, since
 * AlarmManager's one-shot alarms don't repeat on their own.
 */
class ShiftReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = AuthRepository().currentUser?.uid
                if (uid != null) {
                    val today = AttendanceTimeFormat.dateKey(AttendanceTimeFormat.today())
                    val record = runCatching { AttendanceRepository().getRecord(uid, today) }.getOrNull()
                    // If the read failed (e.g. offline) record is null, same as "no record yet" —
                    // erring on the side of still showing the reminder rather than silently
                    // skipping a real one.
                    if (record?.checkInAt == null) {
                        ShiftReminderNotifier.show(context)
                    }
                }
            } finally {
                ShiftReminderScheduler.scheduleNext(context)
                pendingResult.finish()
            }
        }
    }
}
