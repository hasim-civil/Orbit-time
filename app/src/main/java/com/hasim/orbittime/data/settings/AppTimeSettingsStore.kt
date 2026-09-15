package com.hasim.orbittime.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.hasim.orbittime.util.AppTime
import com.hasim.orbittime.util.AppTimeMode
import com.hasim.orbittime.util.AppTimeSettings
import com.hasim.orbittime.util.AppTimeSource
import com.hasim.orbittime.util.OrbitClock
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the Profile → App Time setting and serves it to [OrbitClock].
 *
 * SharedPreferences rather than Firestore on purpose: this is a local, per-device developer/user
 * override of *this install's* clock, not part of the user's account — and it has to be readable
 * synchronously, before any network call, since the very first thing the app does with it is ask
 * what "today" is. Same storage approach as
 * [com.hasim.orbittime.reminder.ShiftReminderScheduler]'s own flag.
 */
object AppTimeSettingsStore : AppTimeSource {

    private const val PREFS_NAME = "app_time"
    private const val KEY_MODE = "mode"
    private const val KEY_OFFSET_SECONDS = "offset_seconds"

    /** "HH:mm:ss" — [LocalTime]'s own parse/toString format. */
    private const val KEY_CUSTOM_TIME = "custom_time"

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(AppTimeSettings())

    /** Observable for the setting screen, so the App Time row reflects a change immediately. */
    val settingsFlow: StateFlow<AppTimeSettings> = _settings.asStateFlow()

    override val settings: AppTimeSettings get() = _settings.value

    override fun now(): Instant = AppTime.instant(_settings.value, Instant.now())

    /**
     * Loads the saved setting and makes it the app's clock. Idempotent, and safe to call from
     * every entry point that can start the process (the activity and the background receivers),
     * so a manual override still applies to work done with no UI on screen.
     */
    fun ensureInitialised(context: Context) {
        if (prefs != null) {
            OrbitClock.install(this)
            return
        }
        val loaded = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = loaded
        _settings.value = read(loaded)
        OrbitClock.install(this)
    }

    /** Switches the app to a user-set time, anchored to the device clock at this moment. */
    fun setManualTime(time: LocalTime) {
        update(AppTime.manualSettings(time, Instant.now(), zone))
    }

    /** Back to the device's own clock — the "switch back" the setting screen offers. */
    fun useDeviceTime() {
        update(AppTimeSettings())
    }

    private fun update(next: AppTimeSettings) {
        prefs?.edit {
            putString(KEY_MODE, next.mode.name)
            putLong(KEY_OFFSET_SECONDS, next.offsetSeconds)
            if (next.customTime == null) remove(KEY_CUSTOM_TIME) else putString(KEY_CUSTOM_TIME, next.customTime.toString())
        }
        _settings.value = next
    }

    /** A stored value that can't be read back (an unknown mode, an unparseable time) falls back
     * to device time rather than throwing — a broken preference must never stop the app starting. */
    private fun read(prefs: SharedPreferences): AppTimeSettings {
        val mode = prefs.getString(KEY_MODE, null)
            ?.let { name -> AppTimeMode.entries.firstOrNull { it.name == name } }
            ?: return AppTimeSettings()
        if (mode != AppTimeMode.MANUAL) return AppTimeSettings()
        return AppTimeSettings(
            mode = AppTimeMode.MANUAL,
            offsetSeconds = prefs.getLong(KEY_OFFSET_SECONDS, 0L),
            customTime = prefs.getString(KEY_CUSTOM_TIME, null)?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        )
    }
}
