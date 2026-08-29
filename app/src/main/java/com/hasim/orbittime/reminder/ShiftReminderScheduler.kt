package com.hasim.orbittime.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules the "shift starts in 5 minutes" reminder with [AlarmManager] instead of a Compose
 * timer, so it still fires while the app is backgrounded or fully closed.
 *
 * Uses `setAndAllowWhileIdle` (inexact, Doze-aware) rather than the exact-alarm APIs on purpose:
 * exact alarms require the user to separately grant "Schedule exact alarms" in system settings
 * on Android 12+, which is real friction for a reminder that only needs to land within a couple
 * of minutes of the target time, not to the second.
 */
object ShiftReminderScheduler {
    private const val PREFS_NAME = "shift_reminder"
    private const val KEY_SHIFT_START = "shift_start" // "HH:mm", java.time.LocalTime's default toString/parse format
    private const val REMINDER_LEAD_MINUTES = 5L
    private const val REQUEST_CODE = 4201

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The last shift start we know about, persisted so a device reboot (or the reminder
     * receiver re-arming the next occurrence) can recompute the next trigger without needing
     * Firestore or the rest of the app process to be alive. */
    fun savedShiftStart(context: Context): LocalTime? =
        prefs(context).getString(KEY_SHIFT_START, null)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    /** Called whenever the user's real shift start is loaded or changes — persists it and
     * (re)arms the next reminder for it, replacing any previously scheduled one so a shift-time
     * edit is picked up immediately instead of only on the next app restart. */
    fun schedule(context: Context, shiftStart: LocalTime) {
        prefs(context).edit { putString(KEY_SHIFT_START, shiftStart.toString()) }
        scheduleNext(context, shiftStart)
    }

    fun cancel(context: Context) {
        alarmManager(context).cancel(pendingIntent(context))
    }

    /**
     * Arms a one-shot alarm for the next scheduled work day (Mon–Sat) whose
     * shiftStart-minus-5-minutes hasn't passed yet. [ShiftReminderReceiver] re-arms the following
     * occurrence itself once this one fires, rather than using a repeating alarm — AlarmManager's
     * repeating alarms are inexact and drift, which would slowly detach the reminder from the
     * user's real shift time.
     */
    fun scheduleNext(context: Context, shiftStart: LocalTime? = savedShiftStart(context)) {
        if (shiftStart == null) return
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)

        fun triggerFor(date: LocalDate) = LocalDateTime.of(date, shiftStart).minusMinutes(REMINDER_LEAD_MINUTES)

        var candidateDate = now.toLocalDate()
        while (candidateDate.dayOfWeek == DayOfWeek.SUNDAY || !triggerFor(candidateDate).isAfter(now)) {
            candidateDate = candidateDate.plusDays(1)
        }

        val triggerMillis = triggerFor(candidateDate).atZone(zone).toInstant().toEpochMilli()
        alarmManager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent(context))
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ShiftReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
