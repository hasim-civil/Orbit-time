package com.hasim.orbittime.ui.screens.welcome

import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.hasim.orbittime.ui.theme.OrbitMotion
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * One drifting glow blob, specified entirely in fractions of the screen's own size rather
 * than fixed dp — a fixed 20-40dp drift on a 350dp-wide blob is proportionally tiny and reads
 * as "not moving" even though it technically is; a fraction of screen size guarantees a
 * visually obvious traversal on any device.
 */
private data class AtmosphereGlow(
    val color: Color,
    val radiusFraction: Float,
    val anchorXFraction: Float,
    val anchorYFraction: Float,
    val driftXFraction: Float,
    val driftYFraction: Float,
    val cyclePhaseDegrees: Float,
    val periodMs: Int,
    val morphPeriodMs: Int,
    val alpha: Float,
)

/** The X and Y drift phases run at different, non-integer-ratio periods, so each blob traces
 * its own slow Lissajous-like wander at its own speed — no two blobs move in sync or trace
 * the same path. */
private class GlowAnimState(val phaseX: State<Float>, val phaseY: State<Float>, val morph: State<Float>)

/** Gradient stop positions never change frame to frame, only the colors at each stop do. */
private val GlowGradientStops = floatArrayOf(0f, 0.45f, 0.75f, 1f)

private val glows = listOf(
    // Warm coral/orange-pink glow, upper-left.
    AtmosphereGlow(
        color = OrbitColors.coral500,
        radiusFraction = 0.46f,
        anchorXFraction = 0.10f,
        anchorYFraction = 0.16f,
        driftXFraction = 0.20f,
        driftYFraction = 0.16f,
        cyclePhaseDegrees = 0f,
        periodMs = OrbitMotion.ATMOSPHERE_DRIFT,
        morphPeriodMs = OrbitMotion.ATMOSPHERE_MORPH,
        alpha = 0.44f,
    ),
    // Violet glow, upper-right.
    AtmosphereGlow(
        color = OrbitColors.violet600,
        radiusFraction = 0.52f,
        anchorXFraction = 0.88f,
        anchorYFraction = 0.14f,
        driftXFraction = 0.22f,
        driftYFraction = 0.18f,
        cyclePhaseDegrees = 80f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 1.3f).toInt(),
        morphPeriodMs = (OrbitMotion.ATMOSPHERE_MORPH * 1.4f).toInt(),
        alpha = 0.40f,
    ),
    // Cool blue glow, left / mid.
    AtmosphereGlow(
        color = OrbitColors.blue500,
        radiusFraction = 0.44f,
        anchorXFraction = 0.06f,
        anchorYFraction = 0.55f,
        driftXFraction = 0.18f,
        driftYFraction = 0.22f,
        cyclePhaseDegrees = 165f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 0.8f).toInt(),
        morphPeriodMs = (OrbitMotion.ATMOSPHERE_MORPH * 0.75f).toInt(),
        alpha = 0.34f,
    ),
    // Cyan glow, lower-right.
    AtmosphereGlow(
        color = OrbitColors.cyan400,
        radiusFraction = 0.40f,
        anchorXFraction = 0.86f,
        anchorYFraction = 0.86f,
        driftXFraction = 0.20f,
        driftYFraction = 0.17f,
        cyclePhaseDegrees = 250f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 1.05f).toInt(),
        morphPeriodMs = (OrbitMotion.ATMOSPHERE_MORPH * 1.2f).toInt(),
        alpha = 0.36f,
    ),
    // Second, cooler purple glow, lower-left / centre — fills out the "4-6 blobs" look and
    // keeps the middle of the screen from ever looking empty as the others drift away from it.
    AtmosphereGlow(
        color = OrbitColors.purple500,
        radiusFraction = 0.38f,
        anchorXFraction = 0.30f,
        anchorYFraction = 0.92f,
        driftXFraction = 0.24f,
        driftYFraction = 0.15f,
        cyclePhaseDegrees = 310f,
        periodMs = (OrbitMotion.ATMOSPHERE_DRIFT * 0.9f).toInt(),
        morphPeriodMs = (OrbitMotion.ATMOSPHERE_MORPH * 1.6f).toInt(),
        alpha = 0.32f,
    ),
)

/**
 * The living Orbit Time atmosphere: a soft cream base wash with slow, obviously-drifting
 * blue, violet/purple, coral (orange-pink) and cyan glow blobs behind the foreground content.
 *
 * This is a full rewrite, not a patch of the previous version — that version animated
 * correctly in principle but was invisible in practice for two separate reasons, both fixed
 * here:
 *
 * 1. It used [androidx.compose.ui.draw.blur], which is backed by `RenderEffect` — an API that
 *    only exists from Android 12 (API 31) onward. This app's minSdk is 26, so on any older
 *    device that call silently does nothing: no exception, no blur, just a sharp-edged circle.
 *    `BlurMaskFilter` (the classic `android.graphics.Paint` blur) was **not** used to replace
 *    it either, for the same underlying reason: it is explicitly documented as unsupported on
 *    a hardware-accelerated canvas, which is how Compose draws by default on every version of
 *    Android — using it here would silently reproduce the exact same "looks static" bug through
 *    a different API. Forcing the whole window into software rendering to make it work would be
 *    an app-wide, performance-costly change for a decorative background, so instead this draws
 *    each blob through [drawIntoCanvas] onto the platform [android.graphics.Canvas] with a
 *    native [android.graphics.Paint] whose shader is a real [RadialGradient] fading to fully
 *    transparent — a soft glow with no blur API involved at all, so nothing here can silently
 *    no-op on any supported device.
 *
 * 2. Even where the old animation genuinely ran, its drift was only tens of dp against
 *    room-filling 300-400dp blobs — technically moving, imperceptibly so. Every blob here
 *    drifts by a fraction of the screen's own size (see [AtmosphereGlow.driftXFraction]),
 *    which guarantees a visually obvious traversal regardless of device size, plus a much
 *    wider breathing scale range.
 *
 * All motion is read from `State.value` inside the [Canvas] draw lambda (not via `by` in the
 * composable body), so every frame updates the draw phase only — no recomposition, no layout
 * pass. This composable's own size/position in the layout tree, and every call site's usage of
 * it, are unchanged from before; only how the glow itself is rendered changed.
 */
@Composable
fun OrbitAtmosphereBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "atmosphere")

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
                    val driftXPx = widthPx * glow.driftXFraction
                    val driftYPx = heightPx * glow.driftYFraction
                    val baseRadiusPx = shortSidePx * glow.radiusFraction

                    val radiansX = Math.toRadians(motion.phaseX.value.toDouble())
                    val radiansY = Math.toRadians(motion.phaseY.value.toDouble())
                    val centerX = anchorXPx + driftXPx * cos(radiansX).toFloat()
                    val centerY = anchorYPx + driftYPx * sin(radiansY).toFloat()
                    val radius = baseRadiusPx * (0.75f + motion.morph.value * 0.4f)
                    val alpha = glow.alpha * (0.78f + motion.morph.value * 0.22f)

                    val opaque = glow.color.copy(alpha = alpha).toArgb()
                    val mid = glow.color.copy(alpha = alpha * 0.45f).toArgb()
                    val fade = glow.color.copy(alpha = alpha * 0.12f).toArgb()
                    val clear = glow.color.copy(alpha = 0f).toArgb()

                    val paint = paints[index].apply {
                        shader = RadialGradient(
                            centerX,
                            centerY,
                            radius.coerceAtLeast(1f),
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
