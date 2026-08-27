package com.hasim.orbittime.ui.screens.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitMotion
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
    val morphPeriodMs: Int,
    val alpha: Float,
)

/** The X and Y drift phases run at different, non-integer-ratio periods, so each blob traces
 * its own slow Lissajous-like wander rather than a simple repeating circle — no two blobs
 * ever look like they're on the same path or in sync. */
private class GlowAnimState(val phaseX: State<Float>, val phaseY: State<Float>, val morph: State<Float>)

private val glows = listOf(
    // Warm coral/pink glow, upper-left.
    AtmosphereGlow(
        color = OrbitColors.coral500,
        diameter = 340.dp,
        anchorXFraction = 0.08f,
        anchorYFraction = 0.16f,
        driftAmplitude = 46.dp,
        cyclePhaseDegrees = 0f,
        periodMs = OrbitMotion.ATMOSPHERE_DRIFT,
        morphPeriodMs = OrbitMotion.ATMOSPHERE_MORPH,
        alpha = 0.40f,
    ),
    // Violet glow, upper-right / right edge.
    AtmosphereGlow(
        color = OrbitColors.violet600,
        diameter = 420.dp,
        anchorXFraction = 0.9f,
        anchorYFraction = 0.12f,
        driftAmplitude = 52.dp,
        cyclePhaseDegrees = 90f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 1.25f).toInt(),
        morphPeriodMs = (OrbitMotion.ATMOSPHERE_MORPH * 1.35f).toInt(),
        alpha = 0.36f,
    ),
    // Cool blue glow, left / mid.
    AtmosphereGlow(
        color = OrbitColors.blue500,
        diameter = 340.dp,
        anchorXFraction = 0.02f,
        anchorYFraction = 0.55f,
        driftAmplitude = 44.dp,
        cyclePhaseDegrees = 180f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 0.85f).toInt(),
        morphPeriodMs = (OrbitMotion.ATMOSPHERE_MORPH * 0.8f).toInt(),
        alpha = 0.30f,
    ),
    // Cyan glow, lower centre-right.
    AtmosphereGlow(
        color = OrbitColors.cyan400,
        diameter = 300.dp,
        anchorXFraction = 0.82f,
        anchorYFraction = 0.86f,
        driftAmplitude = 40.dp,
        cyclePhaseDegrees = 260f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 1.1f).toInt(),
        morphPeriodMs = (OrbitMotion.ATMOSPHERE_MORPH * 1.15f).toInt(),
        alpha = 0.32f,
    ),
)

/**
 * The living Orbit Time atmosphere: a soft cream base wash with slow-drifting, gently
 * breathing violet, blue, cyan and coral/pink glows behind the foreground content — the
 * "light behind frosted glass" look.
 *
 * Deliberately does NOT use [androidx.compose.ui.draw.blur]: that modifier is backed by
 * `RenderEffect`, which only exists from API 31 onward — on this app's minSdk 26 devices it
 * silently does nothing, which is almost certainly why the glow could look static or invisible
 * on a real phone despite animating correctly in a newer-API preview. Instead, the "blur" is
 * simulated with a many-stop radial gradient that fades to transparent well before the drawn
 * circle's edge, which renders identically on every supported API level.
 *
 * All motion is read from `State.value` inside the [Canvas] draw lambda (not via `by` in the
 * composable body), so every frame updates the draw phase only — no recomposition, no layout
 * pass, cheap enough to run continuously on a real device. This composable's own size/position
 * in the layout tree is unchanged from before; only how the glow itself is rendered changed.
 */
@Composable
fun OrbitAtmosphereBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "atmosphere")
    val density = LocalDensity.current

    val motions = glows.map { glow ->
        val phaseX = infinite.animateFloat(
            initialValue = glow.cyclePhaseDegrees,
            targetValue = glow.cyclePhaseDegrees + 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(glow.periodMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowPhaseX",
        )
        val phaseY = infinite.animateFloat(
            initialValue = glow.cyclePhaseDegrees,
            targetValue = glow.cyclePhaseDegrees + 360f,
            animationSpec = infiniteRepeatable(
                // A non-integer multiple of periodMs decouples the Y drift from the X drift,
                // so the combined path is a slow Lissajous wander, not a simple circle.
                animation = tween((glow.periodMs * 1.63f).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowPhaseY",
        )
        val morph = infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(glow.morphPeriodMs, easing = OrbitMotion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowMorph",
        )
        GlowAnimState(phaseX, phaseY, morph)
    }

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

        Canvas(modifier = Modifier.fillMaxSize()) {
            glows.forEachIndexed { index, glow ->
                val motion = motions[index]
                val anchorXPx = widthPx * glow.anchorXFraction
                val anchorYPx = heightPx * glow.anchorYFraction
                val driftAmplitudePx = with(density) { glow.driftAmplitude.toPx() }
                val baseRadiusPx = with(density) { (glow.diameter / 2).toPx() }

                val radiansX = Math.toRadians(motion.phaseX.value.toDouble())
                val radiansY = Math.toRadians(motion.phaseY.value.toDouble())
                val centerX = anchorXPx + driftAmplitudePx * cos(radiansX).toFloat()
                val centerY = anchorYPx + driftAmplitudePx * sin(radiansY).toFloat()
                val radius = baseRadiusPx * (0.88f + motion.morph.value * 0.24f)
                val alpha = glow.alpha * (0.82f + motion.morph.value * 0.18f)
                val center = Offset(centerX, centerY)

                drawCircle(
                    brush = Brush.radialGradient(
                        0f to glow.color.copy(alpha = alpha),
                        0.45f to glow.color.copy(alpha = alpha * 0.45f),
                        0.75f to glow.color.copy(alpha = alpha * 0.12f),
                        1f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }
    }
}
