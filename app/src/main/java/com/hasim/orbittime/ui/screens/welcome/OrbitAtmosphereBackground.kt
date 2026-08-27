package com.hasim.orbittime.ui.screens.welcome

import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.hasim.orbittime.ui.theme.OrbitColors
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One drifting glow blob, ported directly from the reference design's four `driftA`..`driftD`
 * / `cloudPulse` CSS keyframe animations rather than an invented motion curve. Each blob:
 * - anchors at a fixed screen-relative point ([anchorXFraction], [anchorYFraction]);
 * - wanders through the same four-point path its reference counterpart does — [driftXStops] /
 *   [driftYStops] are fractions of the blob's OWN diameter (matching CSS's percentage-of-own-box
 *   semantics), not the screen's, and [scaleStops] breathes its size in and out at those same
 *   four points — on its own [driftPeriodMs];
 * - independently pulses opacity between the reference's fixed 0.5 / 0.95 / 0.72 stops on its
 *   own, shorter [pulsePeriodMs] — decoupled from the drift period, exactly as the reference
 *   pairs e.g. `driftA 11s` with `cloudPulse 7s` on the same element.
 */
private data class AtmosphereGlow(
    val color: Color,
    val baseAlpha: Float,
    val anchorXFraction: Float,
    val anchorYFraction: Float,
    val radiusFraction: Float,
    val driftXStops: FloatArray,
    val driftYStops: FloatArray,
    val scaleStops: FloatArray,
    val driftPeriodMs: Int,
    val pulsePeriodMs: Int,
)

private class GlowAnimState(
    val driftX: State<Float>,
    val driftY: State<Float>,
    val scale: State<Float>,
    val pulse: State<Float>,
)

/** Gradient stop positions never change frame to frame, only the colors at each stop do. */
private val GlowGradientStops = floatArrayOf(0f, 0.45f, 0.75f, 1f)

// Anchors, sizes and colors measured from the reference's own four blobs (in-frame version):
// top:-110px left:-90px 340px orange; top:180px right:-110px 320px purple; bottom:-90px
// left:-40px 300px blue; bottom:190px right:20px 220px orange-red — all against its 402x874
// frame, converted here to screen-fraction anchors and short-side-fraction radii.
private val glows = listOf(
    AtmosphereGlow(
        color = Color(0xFFFF8C42),
        baseAlpha = 0.55f,
        anchorXFraction = 0.20f,
        anchorYFraction = 0.08f,
        radiusFraction = 0.42f,
        driftXStops = floatArrayOf(0f, 0.34f, 0.12f, -0.26f),
        driftYStops = floatArrayOf(0f, 0.18f, 0.38f, 0.16f),
        scaleStops = floatArrayOf(1f, 1.22f, 0.9f, 1.14f),
        driftPeriodMs = 11_000,
        pulsePeriodMs = 7_000,
    ),
    AtmosphereGlow(
        color = Color(0xFF8430FF),
        baseAlpha = 0.5f,
        anchorXFraction = 0.88f,
        anchorYFraction = 0.39f,
        radiusFraction = 0.40f,
        driftXStops = floatArrayOf(0f, -0.32f, -0.08f, 0.28f),
        driftYStops = floatArrayOf(0f, -0.20f, -0.38f, -0.12f),
        scaleStops = floatArrayOf(1.05f, 0.88f, 1.2f, 0.96f),
        driftPeriodMs = 13_000,
        pulsePeriodMs = 9_000,
    ),
    AtmosphereGlow(
        color = Color(0xFF3068FF),
        baseAlpha = 0.42f,
        anchorXFraction = 0.27f,
        anchorYFraction = 0.93f,
        radiusFraction = 0.37f,
        driftXStops = floatArrayOf(0f, -0.30f, 0.20f, 0.34f),
        driftYStops = floatArrayOf(0f, 0.22f, -0.26f, 0.10f),
        scaleStops = floatArrayOf(1f, 1.2f, 0.88f, 1.1f),
        driftPeriodMs = 15_000,
        pulsePeriodMs = 11_000,
    ),
    AtmosphereGlow(
        color = Color(0xFFFF6642),
        baseAlpha = 0.38f,
        anchorXFraction = 0.68f,
        anchorYFraction = 0.66f,
        radiusFraction = 0.27f,
        driftXStops = floatArrayOf(0f, 0.30f, -0.16f, -0.32f),
        driftYStops = floatArrayOf(0f, -0.24f, -0.34f, 0.14f),
        scaleStops = floatArrayOf(1.08f, 0.9f, 1.24f, 0.98f),
        driftPeriodMs = 12_000,
        pulsePeriodMs = 8_000,
    ),
)

/** Reproduces one `driftA`..`driftD`-style keyframe curve: four stops at 0/25/50/75% of
 * [periodMs], looping back to the first stop at 100% so consecutive cycles never jump. */
private fun driftKeyframes(periodMs: Int, stops: FloatArray): DurationBasedAnimationSpec<Float> = keyframes {
    durationMillis = periodMs
    stops[0] at 0
    stops[1] at (periodMs * 0.25f).roundToInt()
    stops[2] at (periodMs * 0.5f).roundToInt()
    stops[3] at (periodMs * 0.75f).roundToInt()
    stops[0] at periodMs
}

/** Reproduces the reference's single shared `cloudPulse` opacity curve (0%/35%/65%/100% ->
 * 0.5/0.95/0.72/0.5), just run on each blob's own [periodMs]. */
private fun pulseKeyframes(periodMs: Int): DurationBasedAnimationSpec<Float> = keyframes {
    durationMillis = periodMs
    0.5f at 0
    0.95f at (periodMs * 0.35f).roundToInt()
    0.72f at (periodMs * 0.65f).roundToInt()
    0.5f at periodMs
}

/**
 * The living Orbit Time atmosphere behind every screen: a soft cream wash with four large,
 * independently-drifting glow blobs (orange, purple, blue, orange-red) ported directly from the
 * reference design's `driftA`..`driftD` + `cloudPulse` keyframe animations — same colors, same
 * anchors, same four-point wander-and-breathe path and independent opacity pulse, each on its
 * own reference-matched period, rather than an invented motion curve.
 *
 * Rendering stays on the same proven approach as before this pass (this implementation was
 * already confirmed visibly animating on a real device, so it's refined here, not replaced):
 * each blob draws through [drawIntoCanvas] onto the platform [android.graphics.Canvas] with a
 * native [android.graphics.Paint] whose shader is a real [RadialGradient] fading to fully
 * transparent. [androidx.compose.ui.draw.blur] is never used — it's backed by `RenderEffect`,
 * only available from API 31, and this app's minSdk is 26; `BlurMaskFilter` is skipped for the
 * same reason (unsupported on Compose's default hardware-accelerated canvas). A soft-edged
 * radial gradient reads as a blurred glow with neither API involved, so nothing here can
 * silently no-op on any supported device.
 *
 * All motion is read from `State.value` inside the [Canvas] draw lambda (not via `by` in the
 * composable body), so every frame updates the draw phase only — no recomposition, no layout
 * pass, no extra battery cost beyond the draw itself.
 */
@Composable
fun OrbitAtmosphereBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "atmosphere")

    val motions = glows.map { glow ->
        val driftX = infinite.animateFloat(
            initialValue = glow.driftXStops[0],
            targetValue = glow.driftXStops[0],
            animationSpec = infiniteRepeatable(
                animation = driftKeyframes(glow.driftPeriodMs, glow.driftXStops),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowDriftX",
        )
        val driftY = infinite.animateFloat(
            initialValue = glow.driftYStops[0],
            targetValue = glow.driftYStops[0],
            animationSpec = infiniteRepeatable(
                animation = driftKeyframes(glow.driftPeriodMs, glow.driftYStops),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowDriftY",
        )
        val scale = infinite.animateFloat(
            initialValue = glow.scaleStops[0],
            targetValue = glow.scaleStops[0],
            animationSpec = infiniteRepeatable(
                animation = driftKeyframes(glow.driftPeriodMs, glow.scaleStops),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowScale",
        )
        val pulse = infinite.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = pulseKeyframes(glow.pulsePeriodMs),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowPulse",
        )
        GlowAnimState(driftX, driftY, scale, pulse)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(OrbitColors.cream100, OrbitColors.sand50, OrbitColors.lilacWhite),
                ),
            ),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val shortSidePx = min(widthPx, heightPx)

        // One reusable native Paint per blob, recreated only if the glow list itself changes
        // (never, at runtime) — its shader is reassigned fresh every frame since the gradient's
        // center/radius/colors all depend on that frame's animated position and scale.
        val paints = remember { glows.map { NativePaint(NativePaint.ANTI_ALIAS_FLAG) } }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                glows.forEachIndexed { index, glow ->
                    val motion = motions[index]
                    val anchorXPx = widthPx * glow.anchorXFraction
                    val anchorYPx = heightPx * glow.anchorYFraction
                    val baseRadiusPx = shortSidePx * glow.radiusFraction
                    val diameterPx = baseRadiusPx * 2f

                    val centerX = anchorXPx + diameterPx * motion.driftX.value
                    val centerY = anchorYPx + diameterPx * motion.driftY.value
                    val radius = (baseRadiusPx * motion.scale.value).coerceAtLeast(1f)
                    val alpha = (glow.baseAlpha * motion.pulse.value).coerceIn(0f, 1f)

                    val opaque = glow.color.copy(alpha = alpha).toArgb()
                    val mid = glow.color.copy(alpha = alpha * 0.45f).toArgb()
                    val fade = glow.color.copy(alpha = alpha * 0.12f).toArgb()
                    val clear = glow.color.copy(alpha = 0f).toArgb()

                    val paint = paints[index].apply {
                        shader = RadialGradient(
                            centerX,
                            centerY,
                            radius,
                            intArrayOf(opaque, mid, fade, clear),
                            GlowGradientStops,
                            Shader.TileMode.CLAMP,
                        )
                    }
                    nativeCanvas.drawCircle(centerX, centerY, radius, paint)
                }
            }
        }
    }
}
