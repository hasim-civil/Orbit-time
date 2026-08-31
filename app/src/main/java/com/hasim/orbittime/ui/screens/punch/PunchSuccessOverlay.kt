package com.hasim.orbittime.ui.screens.punch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

private val CheckmarkDrawEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * The custom "Checked in"/"Checked out" confirmation the reference calls for in place of any
 * stock Toast/Snackbar/AlertDialog: a soft purple/blue/coral glow, a checkmark that draws itself
 * stroke-by-stroke, and a scale/fade entrance ending in one subtle settling pulse. Callers are
 * responsible for showing this only after the real Firestore write succeeds, and for hiding it
 * again (~1s later) — this composable only renders the visual, it owns no timer itself.
 */
@Composable
fun PunchSuccessOverlay(kind: PunchSuccessKind?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = kind != null,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.85f, animationSpec = tween(320, easing = CheckmarkDrawEasing)),
        exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.92f, animationSpec = tween(260)),
        modifier = modifier,
    ) {
        val title = when (kind) {
            PunchSuccessKind.CHECK_IN -> "Checked in"
            PunchSuccessKind.CHECK_OUT -> "Checked out"
            PunchSuccessKind.PAST_ATTENDANCE_ADDED, null -> "Attendance added"
        }
        val subtitle = when (kind) {
            PunchSuccessKind.CHECK_IN -> "Have a great shift"
            PunchSuccessKind.CHECK_OUT -> "Great work today"
            PunchSuccessKind.PAST_ATTENDANCE_ADDED, null -> "Your entry has been saved"
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0612).copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SuccessCheckmark()
                Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                Text(text = title, style = OrbitTypography.displayMedium, color = OrbitColors.cream50, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(text = subtitle, style = OrbitTypography.bodyMedium, color = OrbitColors.slate300, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SuccessCheckmark() {
    val infinite = rememberInfiniteTransition(label = "successGlow")
    val glowPulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "glowPulse",
    )

    // A one-shot expanding ripple, plus the checkmark's own stroke draw progress and a final
    // settling overshoot-then-rest pulse on the circular ring.
    val ripple = remember { Animatable(0f) }
    val strokeProgress = remember { Animatable(0f) }
    val ringSettle = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        strokeProgress.animateTo(1f, tween(420, delayMillis = 120, easing = CheckmarkDrawEasing))
        ringSettle.animateTo(1f, tween(260, easing = CheckmarkDrawEasing))
    }
    LaunchedEffect(Unit) {
        ripple.animateTo(1f, tween(700, delayMillis = 80, easing = LinearEasing))
    }

    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        // Soft circular glow, blending purple -> blue -> coral like the app's own accent gradient.
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { alpha = glowPulse }
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        colors = listOf(
                            OrbitColors.purple600.copy(alpha = 0.55f),
                            OrbitColors.blue500.copy(alpha = 0.32f),
                            OrbitColors.coral500.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    )
                    onDrawBehind { drawCircle(brush = brush) }
                },
        )

        // One-shot ripple expanding outward from the ring on entrance.
        if (ripple.value in 0f..1f && ripple.value > 0f) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        val scale = 1f + ripple.value * 0.6f
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - ripple.value) * 0.5f
                    }
                    .drawWithCache {
                        onDrawBehind {
                            drawCircle(color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
                        }
                    },
            )
        }

        Canvas(modifier = Modifier.size(80.dp)) {
            val ringScale = ringSettle.value.coerceIn(0f, 1f)
            val ringRadius = (size.minDimension / 2f) * (0.9f + ringScale * 0.1f)
            drawCircle(
                brush = Brush.linearGradient(colors = listOf(OrbitColors.purple500, OrbitColors.blue500, OrbitColors.success)),
                radius = ringRadius,
                center = center,
                style = Stroke(width = size.minDimension * 0.055f, cap = StrokeCap.Round),
            )
            drawCircle(color = Color(0xFF15102A).copy(alpha = 0.92f), radius = ringRadius - size.minDimension * 0.05f, center = center)

            // The checkmark itself, revealed stroke-by-stroke via PathMeasure.getSegment.
            val w = size.width
            val h = size.height
            val checkPath = Path().apply {
                moveTo(w * 0.28f, h * 0.53f)
                lineTo(w * 0.44f, h * 0.68f)
                lineTo(w * 0.74f, h * 0.34f)
            }
            val measure = PathMeasure().apply { setPath(checkPath, false) }
            val animatedPath = Path()
            measure.getSegment(0f, measure.length * strokeProgress.value.coerceIn(0f, 1f), animatedPath, true)
            drawPath(
                path = animatedPath,
                color = Color.White,
                style = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
