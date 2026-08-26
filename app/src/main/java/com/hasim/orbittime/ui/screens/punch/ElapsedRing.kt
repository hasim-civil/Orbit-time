package com.hasim.orbittime.ui.screens.punch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
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
                )

                val angleRad = Math.toRadians((-90.0 + sweep))
                val dotX = center.x + (radius * cos(angleRad)).toFloat()
                val dotY = center.y + (radius * sin(angleRad)).toFloat()
                drawCircle(color = OrbitColors.coral500, radius = strokeWidth * 0.55f, center = Offset(dotX, dotY))
            }
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
