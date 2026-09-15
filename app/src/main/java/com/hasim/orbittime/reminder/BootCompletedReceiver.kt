package com.hasim.orbittime.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hasim.orbittime.data.settings.AppTimeSettingsStore

/** Re-arms the shift reminder after a device reboot, since AlarmManager's alarms don't survive
 * one. Reads the last-known shift start from local storage rather than needing Firestore or the
 * rest of the app process to already be running. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppTimeSettingsStore.ensureInitialised(context)
        ShiftReminderScheduler.savedShiftStart(context)?.let { shiftStart ->
            ShiftReminderScheduler.scheduleNext(context, shiftStart)
        }
    }
}
