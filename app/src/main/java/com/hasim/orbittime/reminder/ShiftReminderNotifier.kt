package com.hasim.orbittime.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hasim.orbittime.MainActivity
import com.hasim.orbittime.R

/** Builds and shows the real, system-level "shift starts in 5 minutes" notification — not an
 * in-app banner — so it's visible even while Orbit Time itself isn't open. */
object ShiftReminderNotifier {
    private const val CHANNEL_ID = "shift_reminder"
    private const val NOTIFICATION_ID = 4202

    fun show(context: Context) {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Shift starts in 5 minutes")
            .setContentText("Time to punch in.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Shift reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminds you 5 minutes before your shift starts."
        }
        manager.createNotificationChannel(channel)
    }
}
