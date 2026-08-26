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

    /** Subtle idle "alive" motion on the orbit sphere and background glows. */
    const val ORBIT_FLOAT = 7_000
    const val ORBIT_BREATHE = 5_200
    const val ORBIT_HIGHLIGHT_PULSE = 6_400
    const val ATMOSPHERE_MORPH = 9_000

    /** Staggered Welcome-screen entrance: background, then logo, then copy, then buttons. */
    const val ENTRANCE_BACKGROUND_DELAY = 0
    const val ENTRANCE_BACKGROUND_DURATION = 500
    const val ENTRANCE_LOGO_DELAY = 150
    const val ENTRANCE_LOGO_DURATION = 550
    const val ENTRANCE_TITLE_DELAY = 350
    const val ENTRANCE_TITLE_DURATION = 500
    const val ENTRANCE_BUTTONS_DELAY = 500
    const val ENTRANCE_BUTTONS_DURATION = 500

    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val linear: Easing = LinearEasing

    /** Symmetric ease-in-out used for continuous idle loops (float, breathe, morph) — no snap. */
    val gentle: Easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
}
