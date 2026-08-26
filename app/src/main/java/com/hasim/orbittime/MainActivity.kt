package com.hasim.orbittime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hasim.orbittime.ui.navigation.OrbitNavHost
import com.hasim.orbittime.ui.theme.OrbitTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OrbitTimeTheme {
                OrbitNavHost()
            }
        }
    }
}
