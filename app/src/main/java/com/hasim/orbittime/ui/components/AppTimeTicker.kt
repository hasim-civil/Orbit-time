package com.hasim.orbittime.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.hasim.orbittime.data.settings.AppTimeSettingsStore
import com.hasim.orbittime.util.OrbitClock
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * The app's current instant, as a Compose state that actually advances — a plain
 * `OrbitClock.now()` call in a composable is only as fresh as the last recomposition, which is
 * why the Home clock used to sit at whatever time the screen happened to be drawn at.
 *
 * Re-keyed on the App Time setting, so switching between device and manual time re-reads the
 * clock immediately instead of at the end of the current interval.
 */
@Composable
fun rememberAppTimeNow(intervalMillis: Long = 1_000L): State<Instant> {
    val settings by AppTimeSettingsStore.settingsFlow.collectAsState()
    return produceState(initialValue = OrbitClock.now(), settings, intervalMillis) {
        while (true) {
            value = OrbitClock.now()
            delay(intervalMillis)
        }
    }
}
