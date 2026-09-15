package com.hasim.orbittime.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Where [OrbitClock] gets its time from. The app installs the persisted
 * [com.hasim.orbittime.data.settings.AppTimeSettingsStore]; tests install their own.
 */
interface AppTimeSource {
    val settings: AppTimeSettings
    val zone: ZoneId get() = ZoneId.systemDefault()
    fun now(): Instant
}

/** Plain device time — the default, used until (and unless) a source is installed. */
internal object DeviceTimeSource : AppTimeSource {
    override val settings: AppTimeSettings = AppTimeSettings()
    override fun now(): Instant = Instant.now()
}

/**
 * The single source of "now" for the whole app: attendance, check-in/check-out, elapsed time,
 * the summaries, the overtime balance and every screen's clock all read it, so the Profile →
 * App Time setting applies everywhere at once instead of each screen calling `Instant.now()`
 * for itself.
 *
 * Deliberately free of Android types: the persistence lives behind [AppTimeSource], which keeps
 * this (and everything calculating from it) testable on a plain JVM. With nothing installed it
 * is exactly the device clock, so code that runs before the app is initialised — and unit tests
 * — behave as they always did.
 */
object OrbitClock {

    @Volatile
    private var source: AppTimeSource = DeviceTimeSource

    fun install(source: AppTimeSource) {
        this.source = source
    }

    /** Drops back to the plain device clock. Used by tests; also the honest fallback if the
     * persisted source ever fails to load. */
    fun useDeviceTime() {
        source = DeviceTimeSource
    }

    val settings: AppTimeSettings get() = source.settings

    val zone: ZoneId get() = source.zone

    fun now(): Instant = source.now()

    fun today(zone: ZoneId = this.zone): LocalDate = now().atZone(zone).toLocalDate()

    fun localTime(zone: ZoneId = this.zone): LocalTime = now().atZone(zone).toLocalTime()
}
