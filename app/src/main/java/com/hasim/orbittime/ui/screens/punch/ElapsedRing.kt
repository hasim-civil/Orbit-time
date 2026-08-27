package com.hasim.orbittime.ui.screens.punch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import kotlin.math.cos
import kotlin.math.sin

/** The circular elapsed-time indicator on the Punch hero card. */
@Composable
fun ElapsedRing(
    progress: Float,
    elapsedLabel: String,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp,
) {
    // The reference's "orbitSpin" (22s, outer conic glow), "ringBreath" (5.5s arc breathe),
    // and the small comet dot that continuously circles the ring independent of the progress.
    val infiniteTransition = rememberInfiniteTransition(label = "elapsedRing")
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(22000, easing = LinearEasing)),
        label = "glowRotation",
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2750, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "ringBreathe",
    )
    val cometAngle by infiniteTransition.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(animation = tween(9000, easing = LinearEasing)),
        label = "cometAngle",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(diameter + 32.dp)
                .graphicsLayer { rotationZ = glowRotation }
                .blur(14.dp)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            OrbitColors.success.copy(alpha = 0.28f),
                            Color.Transparent,
                            Color.Transparent,
                            OrbitColors.purple600.copy(alpha = 0.3f),
                            OrbitColors.success.copy(alpha = 0.28f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidth = size.minDimension * 0.055f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            drawCircle(
                color = OrbitColors.void300.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )

            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    color = OrbitColors.success,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    alpha = 0.9f + breathe * 0.1f,
                )

                val angleRad = Math.toRadians((-90.0 + sweep))
                val dotX = center.x + (radius * cos(angleRad)).toFloat()
                val dotY = center.y + (radius * sin(angleRad)).toFloat()
                drawCircle(color = OrbitColors.coral500, radius = strokeWidth * 0.55f, center = Offset(dotX, dotY))
            }

            val cometRad = Math.toRadians(cometAngle.toDouble())
            val cometCenter = Offset(
                center.x + (radius * cos(cometRad)).toFloat(),
                center.y + (radius * sin(cometRad)).toFloat(),
            )
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = strokeWidth * 0.3f, center = cometCenter)
        }

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
