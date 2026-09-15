package com.hasim.orbittime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hasim.orbittime.data.settings.AppTimeSettingsStore
import com.hasim.orbittime.ui.navigation.OrbitNavHost
import com.hasim.orbittime.ui.theme.OrbitTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before anything asks what "now" is: loads the saved Profile -> App Time setting and
        // makes it the clock every screen reads (OrbitClock). Device time until the user says
        // otherwise, so a fresh install behaves exactly as before this setting existed.
        AppTimeSettingsStore.ensureInitialised(applicationContext)
        enableEdgeToEdge()

        setContent {
            OrbitTimeTheme {
                OrbitNavHost()
            }
        }
    }
}
