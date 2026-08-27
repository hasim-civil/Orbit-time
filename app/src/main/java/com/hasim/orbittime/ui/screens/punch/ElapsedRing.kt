package com.hasim.orbittime.ui.screens.punch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

// Measured from the reference's live markup (238/218/196/172/160px nested rings at the
// reference's 393dp device width), not approximated from a still image.
private val HaloDiameter = 233.dp
private val ProgressRingDiameter = 192.dp
private val RippleDiameter = 168.dp
private val ButtonDiameter = 156.dp

private val ProgressEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val PressEasing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)

/**
 * The layered elapsed-time ring + tap target on the Punch hero card: three staggered
 * pulsing halo rings reading as one outward wave ("haloPulse"), the real shift-progress
 * ring (smoothly animated, not decorative), a soft pulsing edge glow ("edgeGlow"), a
 * counter-rotating conic glow behind the button ("revSpin"), and a one-shot expanding
 * ripple fired on tap ("punchGlow").
 */
@Composable
fun ElapsedRing(
    progress: Float,
    elapsedLabel: String,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "elapsedRing")

    // Three rings pulse on the same 3000ms cycle but staggered a third of a cycle apart, so
    // at any moment up to three concentric rings are expanding and fading at once — reads as
    // a continuous outward wave rather than one ring restarting abruptly.
    val haloRingCount = 3
    val halos = List(haloRingCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3000
                    0f at 0
                    1f at 2100
                    1f at 3000
                },
                initialStartOffset = StartOffset((3000 / haloRingCount) * index, StartOffsetType.Delay),
            ),
            label = "halo$index",
        )
    }
    val revRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(26000, easing = LinearEasing)),
        label = "revRotation",
    )
    val edgeGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(animation = tween(4500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "edgeGlow",
    )

    // The real shift-progress ring smoothly animates to a new value on change, matching
    // the reference's `transition: stroke-dashoffset 1.4s`, instead of snapping instantly.
    val animatedProgress = remember { Animatable(progress) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, tween(1400, easing = ProgressEasing))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(200, easing = PressEasing),
        label = "pressedScale",
    )

    // punchGlow: a one-shot expanding-and-fading ring fired on every tap.
    val ripple = remember { Animatable(0f) }
    var rippleKey by remember { mutableStateOf(0) }
    LaunchedEffect(rippleKey) {
        if (rippleKey > 0) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(700, easing = LinearEasing))
        }
    }

    Box(modifier = modifier.size(HaloDiameter), contentAlignment = Alignment.Center) {
        // 1. haloPulse — three staggered rings expanding and fading, reading as one wave.
        halos.forEach { halo ->
            Box(
                modifier = Modifier
                    .size(HaloDiameter)
                    .graphicsLayer {
                        val scale = 1f + halo.value * 0.28f
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - halo.value).coerceIn(0f, 1f) * 0.5f
                    }
                    .border(1.dp, OrbitColors.purple500.copy(alpha = 0.4f), CircleShape),
            )
        }

        // 2. The real shift-progress ring — data-driven, smoothly transitions on change.
        Canvas(modifier = Modifier.size(ProgressRingDiameter)) {
            val strokeWidth = size.minDimension * 0.036f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color.White.copy(alpha = 0.08f), radius = radius, center = center, style = Stroke(width = strokeWidth))

            val sweep = 360f * animatedProgress.value.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    color = OrbitColors.success,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        // 3. edgeGlow — soft pulsing outline around the progress ring.
        Box(
            modifier = Modifier
                .size(ProgressRingDiameter)
                .graphicsLayer { alpha = edgeGlow }
                .border(1.dp, OrbitColors.coral500.copy(alpha = 0.22f), CircleShape),
        )

        // 4. punchGlow — one-shot ripple, only visible while it's playing.
        if (ripple.value > 0f && ripple.value < 1f) {
            Box(
                modifier = Modifier
                    .size(RippleDiameter)
                    .graphicsLayer {
                        val scale = 1f + ripple.value * 0.5f
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - ripple.value) * 0.55f
                    }
                    .border(1.5.dp, OrbitColors.coral500.copy(alpha = 0.55f), CircleShape),
            )
        }

        // 5. revSpin — counter-rotating conic glow behind the button.
        Box(
            modifier = Modifier
                .size(ButtonDiameter)
                .graphicsLayer {
                    rotationZ = revRotation
                    alpha = 0.85f
                }
                .drawWithCache {
                    val brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent,
                            Color.Transparent,
                            OrbitColors.coral500.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.16f),
                        ),
                    )
                    onDrawBehind { drawCircle(brush = brush) }
                },
        )

        // 6. The center button — dark glossy sphere, doubling as a tap target.
        val clickModifier = if (onClick != null) {
            Modifier.clickable(interactionSource = interactionSource, indication = null) {
                rippleKey++
                onClick()
            }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .size(ButtonDiameter)
                .graphicsLayer { scaleX = pressedScale; scaleY = pressedScale }
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF42286E).copy(alpha = 0.85f), Color(0xFF100A1C).copy(alpha = 0.94f)),
                        center = Offset(size.width * 0.32f, size.height * 0.22f),
                        radius = size.minDimension * 0.85f,
                    )
                    onDrawBehind { drawCircle(brush = brush) }
                }
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .then(clickModifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ELAPSED",
                    style = OrbitTypography.label,
                    color = OrbitColors.slate300,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                Text(
                    text = elapsedLabel,
                    style = OrbitTypography.displayMedium,
                    color = OrbitColors.cream50,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(
                    text = statusLabel,
                    style = OrbitTypography.bodySmall,
                    color = statusColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
