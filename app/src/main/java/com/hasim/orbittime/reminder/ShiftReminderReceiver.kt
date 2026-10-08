package com.hasim.orbittime.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hasim.orbittime.data.attendance.AttendanceRepository
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.holiday.HolidayRepository
import com.hasim.orbittime.data.leave.LeaveRepository
import com.hasim.orbittime.data.settings.AppTimeSettingsStore
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.OrbitClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires ~5 minutes before the user's saved shift start, even with the app closed or backgrounded.
 * Reads today's attendance, the user's leave and the organization's holidays once and shows the
 * reminder only when today still resolves to "nothing yet" — never on a leave day, a holiday, a
 * week off, or after a check-in — then always re-arms the next occurrence before finishing, since
 * AlarmManager's one-shot alarms don't repeat on their own.
 */
class ShiftReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The process can be cold-started straight into this receiver, with no activity having
        // run — so the App Time setting has to be loaded here too, or "today" below would be
        // resolved on device time while the rest of the app is on an override.
        AppTimeSettingsStore.ensureInitialised(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = AuthRepository().currentUser?.uid
                if (uid != null) {
                    val todayDate = AttendanceTimeFormat.today()
                    val today = AttendanceTimeFormat.dateKey(todayDate)
                    val record = runCatching { AttendanceRepository().getRecord(uid, today) }.getOrNull()
                    // Any failed read counts as "nothing known" — erring on the side of still
                    // showing the reminder rather than silently skipping a real one.
                    val onLeave = runCatching { LeaveRepository().fetchLeaves(uid) }.getOrNull()
                        .orEmpty().any { todayDate in it.dateRange() }
                    val isHoliday = runCatching { HolidayRepository().fetchHolidays() }.getOrNull()
                        .orEmpty().any { it.date == today }
                    // The same day status every screen shows: no reminder to check in on a leave
                    // day, a holiday or a week off, nor once a check-in exists.
                    val status = AttendanceStats.classifyDay(
                        checkInAt = record?.checkInAt?.toDate()?.toInstant(),
                        date = todayDate,
                        today = todayDate,
                        zone = OrbitClock.zone,
                        isOnLeave = onLeave,
                        isHoliday = isHoliday,
                    )
                    if (status == null) {
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
