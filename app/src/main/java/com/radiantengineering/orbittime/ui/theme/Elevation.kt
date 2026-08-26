package com.radiantengineering.orbittime.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shadow tokens. The reference design favours soft, diffuse shadows rather than
 * hard Material elevation — these are tuned for use with a large ambient blur radius.
 */
object OrbitElevation {
    val none: Dp = 0.dp
    val low: Dp = 4.dp
    val medium: Dp = 12.dp
    val high: Dp = 24.dp

    /** Extra-soft glow used behind the hero orbit symbol and gradient CTAs. */
    val glow: Dp = 40.dp
}
