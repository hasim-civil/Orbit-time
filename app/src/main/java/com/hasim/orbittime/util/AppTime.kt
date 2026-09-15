package com.hasim.orbittime.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Where Orbit Time reads "now" from. */
enum class AppTimeMode {
    /** The device's own clock — the default, and what every build before this setting existed used. */
    DEVICE,

    /** A user-set time, applied as a fixed offset from the device clock (see [AppTimeSettings.offsetSeconds]). */
    MANUAL,
}

/**
 * The App Time setting, as stored and as read by [OrbitClock].
 *
 * A manual override is kept as an *offset* from the device clock rather than as a frozen
 * instant, for two reasons: the app time still has to tick (a check-in followed by a check-out
 * must produce a real worked duration, and the Home clock must keep moving), and an offset
 * survives a restart without needing the app to have been running in between.
 *
 * [customTime] is only what the user picked, kept so the setting screen can say what was set;
 * nothing calculates from it. The clock itself is [offsetSeconds].
 */
data class AppTimeSettings(
    val mode: AppTimeMode = AppTimeMode.DEVICE,
    val offsetSeconds: Long = 0L,
    val customTime: LocalTime? = null,
) {
    val isManual: Boolean get() = mode == AppTimeMode.MANUAL
}

/**
 * Pure App Time math — no Android, no Firebase — so the 12-hour conversions and the offset
 * arithmetic stay testable outside an Android runtime, like every other calculation in this app.
 *
 * This is the only place 12-hour clock values are converted, so the awkward cases (12:00 am is
 * hour 0, 12:00 pm is hour 12) are decided once rather than per screen.
 */
object AppTime {

    /** Nothing here ever touches the device's own clock settings — this is an Orbit Time-internal
     * override only. */
    fun to24Hour(hour12: Int, isAm: Boolean): Int {
        require(hour12 in 1..12) { "hour12 must be 1..12, was $hour12" }
        return when {
            hour12 == 12 -> if (isAm) 0 else 12
            isAm -> hour12
            else -> hour12 + 12
        }
    }

    /** 0 -> 12 (am), 13 -> 1 (pm) — the hour as a 12-hour dial reads it. */
    fun hour12Of(time: LocalTime): Int = ((time.hour + 11) % 12) + 1

    fun isAm(time: LocalTime): Boolean = time.hour < 12

    /** Builds a [LocalTime] from what the picker collects: a 12-hour dial plus am/pm. */
    fun localTimeOf(hour12: Int, minute: Int, second: Int, isAm: Boolean): LocalTime {
        require(minute in 0..59) { "minute must be 0..59, was $minute" }
        require(second in 0..59) { "second must be 0..59, was $second" }
        return LocalTime.of(to24Hour(hour12, isAm), minute, second)
    }

    /**
     * How far the app clock has to be shifted so that "now" reads [target] — measured against
     * [deviceNow]'s own calendar day, so setting 9:00 am while the device says 2:00 pm moves the
     * app clock back five hours *today* rather than forward to tomorrow morning.
     *
     * Whole seconds, taken between the two epoch-second values rather than from the exact
     * elapsed duration: the device clock carries a sub-second fraction, and rounding that into
     * the offset would land the app clock just short of the chosen second (setting 9:00:00 read
     * back as 8:59:59). Comparing whole seconds leaves the fraction where it belongs — on the
     * ticking clock — so the time the user picked is the time the app reads.
     */
    fun offsetSecondsFor(target: LocalTime, deviceNow: Instant, zone: ZoneId): Long {
        val targetInstant = deviceNow.atZone(zone).toLocalDate().atTime(target).atZone(zone).toInstant()
        return targetInstant.epochSecond - deviceNow.epochSecond
    }

    fun manualSettings(target: LocalTime, deviceNow: Instant, zone: ZoneId): AppTimeSettings =
        AppTimeSettings(
            mode = AppTimeMode.MANUAL,
            offsetSeconds = offsetSecondsFor(target, deviceNow, zone),
            customTime = target,
        )

    /** The app's current instant: the device clock in [AppTimeMode.DEVICE], shifted by the saved
     * offset in [AppTimeMode.MANUAL]. Either way it keeps ticking with the device. */
    fun instant(settings: AppTimeSettings, deviceNow: Instant): Instant =
        if (settings.isManual) deviceNow.plusSeconds(settings.offsetSeconds) else deviceNow
}
