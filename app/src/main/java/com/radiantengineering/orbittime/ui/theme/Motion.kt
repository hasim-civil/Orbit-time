package com.radiantengineering.orbittime.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

/** Animation durations (ms) and easings shared across the app. */
object OrbitMotion {
    const val FAST = 150
    const val NORMAL = 300
    const val SLOW = 500
    const val SCREEN_ENTER = 600

    /** One full slow rotation of the orbit ring / drifting atmosphere. */
    const val ORBIT_ROTATION = 24_000
    const val ORBIT_MOON = 12_000
    const val ATMOSPHERE_DRIFT = 18_000

    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val linear: Easing = LinearEasing
}
