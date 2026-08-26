package com.radiantengineering.orbittime.ui.screens.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.radiantengineering.orbittime.ui.theme.OrbitColors
import com.radiantengineering.orbittime.ui.theme.OrbitMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * The premium 3D Orbit Time mark: a glossy dark sphere wrapped in a fixed
 * gradient ring, with a small coral moon travelling the ring path.
 * Matches the hero art on the Welcome reference screen.
 */
@Composable
fun OrbitSymbol(modifier: Modifier = Modifier, diameter: Dp = 220.dp) {
    val infinite = rememberInfiniteTransition(label = "orbitSymbol")

    // The orbit path itself stays fixed at its tilt — only the satellite travels it.
    val ringTilt = -28f

    val moonPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(OrbitMotion.ORBIT_MOON, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moonPhase",
    )

    // Very subtle idle "alive" motion on the sphere only — never the whole symbol —
    // so it reads as a gently drifting 3D object rather than a spinner.
    val floatT by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(OrbitMotion.ORBIT_FLOAT, easing = OrbitMotion.gentle),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sphereFloat",
    )
    val breatheT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(OrbitMotion.ORBIT_BREATHE, easing = OrbitMotion.gentle),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sphereBreathe",
    )
    val highlightT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(OrbitMotion.ORBIT_HIGHLIGHT_PULSE, easing = OrbitMotion.gentle),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sphereHighlight",
    )

    Canvas(modifier = modifier.size(diameter)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val sphereRadius = size.minDimension * 0.235f
        val ringRx = size.minDimension * 0.40f
        val ringRy = size.minDimension * 0.245f

        // The sphere floats a couple of percent of its own size and breathes very
        // slightly in scale — subtle enough to read as depth, not motion.
        val sphereCenter = Offset(center.x, center.y + floatT * sphereRadius * 0.09f)
        val liveSphereRadius = sphereRadius * (1f + breatheT * 0.02f)

        // Ambient glow behind everything, pulsing gently with the same breathing life.
        // Kept well inside the canvas bounds (< 0.5 * diameter) so it never gets
        // clipped against the square canvas edge — that clip is invisible at full
        // opacity but shows as a hard-edged halo while the entrance fade is animating.
        val glowRadius = liveSphereRadius * (1.7f + breatheT * 0.15f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    OrbitColors.violet600.copy(alpha = 0.18f + breatheT * 0.08f),
                    Color.Transparent,
                ),
                center = sphereCenter,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = sphereCenter,
        )

        // Gradient orbit ring — fixed in place, tilted; only the moon travels it.
        rotate(degrees = ringTilt, pivot = center) {
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        OrbitColors.cyan400,
                        OrbitColors.blue500,
                        OrbitColors.purple500,
                        OrbitColors.coral500,
                    ),
                    start = Offset(center.x - ringRx, center.y + ringRy),
                    end = Offset(center.x + ringRx, center.y - ringRy),
                ),
                topLeft = Offset(center.x - ringRx, center.y - ringRy),
                size = Size(ringRx * 2f, ringRy * 2f),
                style = Stroke(width = size.minDimension * 0.028f),
            )
        }

        // The sphere itself — dark, glossy, subtly lit from the upper-left.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    OrbitColors.void300,
                    OrbitColors.void600,
                    OrbitColors.void900,
                ),
                center = Offset(sphereCenter.x - liveSphereRadius * 0.35f, sphereCenter.y - liveSphereRadius * 0.4f),
                radius = liveSphereRadius * 2.1f,
            ),
            radius = liveSphereRadius,
            center = sphereCenter,
        )

        // Gloss highlight — alpha drifts gently, like light slowly catching the surface.
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    OrbitColors.lavenderWhite.copy(alpha = 0.46f + highlightT * 0.16f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(
                sphereCenter.x - liveSphereRadius * 0.62f,
                sphereCenter.y - liveSphereRadius * 0.75f,
            ),
            size = Size(liveSphereRadius * 0.85f, liveSphereRadius * 0.55f),
        )

        // Coral moon, orbiting the ring path.
        val phiRad = Math.toRadians(moonPhase.toDouble())
        val tiltRad = Math.toRadians(ringTilt.toDouble())
        val ex = (ringRx * cos(phiRad)).toFloat()
        val ey = (ringRy * sin(phiRad)).toFloat()
        val moonX = center.x + (ex * cos(tiltRad) - ey * sin(tiltRad)).toFloat()
        val moonY = center.y + (ex * sin(tiltRad) + ey * cos(tiltRad)).toFloat()
        val moonRadius = size.minDimension * 0.048f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbitColors.coral400.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(moonX, moonY),
                radius = moonRadius * 3f,
            ),
            radius = moonRadius * 3f,
            center = Offset(moonX, moonY),
        )
        drawCircle(
            color = OrbitColors.coral500,
            radius = moonRadius,
            center = Offset(moonX, moonY),
        )
    }
}
