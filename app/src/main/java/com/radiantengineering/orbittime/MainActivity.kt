package com.radiantengineering.orbittime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.radiantengineering.orbittime.ui.screens.welcome.WelcomeScreen
import com.radiantengineering.orbittime.ui.screens.welcome.rememberWelcomeScreenStrings
import com.radiantengineering.orbittime.ui.theme.OrbitTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OrbitTimeTheme {
                WelcomeScreen(
                    strings = rememberWelcomeScreenStrings(),
                    onSignInClick = { /* wired up in Phase 2 */ },
                    onCreateAccountClick = { /* wired up in Phase 2 */ },
                )
            }
        }
    }
}
