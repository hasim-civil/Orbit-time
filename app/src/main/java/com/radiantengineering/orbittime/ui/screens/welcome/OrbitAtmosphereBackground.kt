package com.radiantengineering.orbittime.ui.screens.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.radiantengineering.orbittime.ui.theme.OrbitColors
import com.radiantengineering.orbittime.ui.theme.OrbitMotion
import kotlin.math.cos
import kotlin.math.sin

private data class AtmosphereGlow(
    val color: Color,
    val diameter: Dp,
    val anchorXFraction: Float,
    val anchorYFraction: Float,
    val driftAmplitude: Dp,
    val cyclePhaseDegrees: Float,
    val periodMs: Int,
    val alpha: Float,
)

private val glows = listOf(
    // Warm coral glow, upper-left.
    AtmosphereGlow(
        color = OrbitColors.coral500,
        diameter = 320.dp,
        anchorXFraction = 0.06f,
        anchorYFraction = 0.14f,
        driftAmplitude = 26.dp,
        cyclePhaseDegrees = 0f,
        periodMs = OrbitMotion.ATMOSPHERE_DRIFT,
        alpha = 0.60f,
    ),
    // Violet glow, upper-right / right edge.
    AtmosphereGlow(
        color = OrbitColors.violet600,
        diameter = 400.dp,
        anchorXFraction = 0.92f,
        anchorYFraction = 0.10f,
        driftAmplitude = 30.dp,
        cyclePhaseDegrees = 90f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 1.25f).toInt(),
        alpha = 0.55f,
    ),
    // Cool blue glow, left / mid.
    AtmosphereGlow(
        color = OrbitColors.blue500,
        diameter = 320.dp,
        anchorXFraction = -0.05f,
        anchorYFraction = 0.55f,
        driftAmplitude = 24.dp,
        cyclePhaseDegrees = 180f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 0.85f).toInt(),
        alpha = 0.45f,
    ),
    // Cyan glow, lower centre-right.
    AtmosphereGlow(
        color = OrbitColors.cyan400,
        diameter = 280.dp,
        anchorXFraction = 0.85f,
        anchorYFraction = 0.85f,
        driftAmplitude = 22.dp,
        cyclePhaseDegrees = 260f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 1.1f).toInt(),
        alpha = 0.48f,
    ),
)

/**
 * The living Orbit Time atmosphere: a soft cream base wash with drifting
 * violet, blue, cyan and coral glows behind the foreground content.
 */
@Composable
fun OrbitAtmosphereBackground(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        OrbitColors.cream100,
                        OrbitColors.sand50,
                        OrbitColors.lilacWhite,
                    ),
                ),
            ),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        glows.forEach { glow ->
            val infinite = rememberInfiniteTransition(label = "atmosphere")
            val angle by infinite.animateFloat(
                initialValue = glow.cyclePhaseDegrees,
                targetValue = glow.cyclePhaseDegrees + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(glow.periodMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "glowAngle",
            )

            val radians = Math.toRadians(angle.toDouble())
            val driftX = glow.driftAmplitude * cos(radians).toFloat()
            val driftY = glow.driftAmplitude * sin(radians).toFloat()

            val density = LocalDensity.current
            val anchorX = with(density) { (widthPx * glow.anchorXFraction).toDp() - glow.diameter / 2 }
            val anchorY = with(density) { (heightPx * glow.anchorYFraction).toDp() - glow.diameter / 2 }

            Box(
                modifier = Modifier
                    .offset(x = anchorX + driftX, y = anchorY + driftY)
                    .size(glow.diameter)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glow.color.copy(alpha = glow.alpha),
                                glow.color.copy(alpha = glow.alpha * 0.6f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }
    }
}
