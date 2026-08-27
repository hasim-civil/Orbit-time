package com.hasim.orbittime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors

/**
 * The orbit mark, matching the app's own launcher icon exactly
 * (`ic_launcher_foreground.xml`, 108x108 viewport, values below scaled by 32/108): a
 * –28°-tilted ring stroked with the full cyan→blue→purple→coral gradient, a light
 * center dot, and a coral accent dot near the ring's upper edge. Used in the top bar.
 */
@Composable
fun OrbitMiniLogo(modifier: Modifier = Modifier, diameter: Dp = 20.dp, dotColor: Color = OrbitColors.lavenderWhite) {
    Canvas(modifier = modifier.size(diameter)) {
        val scale = size.width / 32f
        val center = Offset(16f * scale, 16f * scale)
        val rx = 8.3f * scale
        val ry = 5.04f * scale

        rotate(degrees = -28f, pivot = center) {
            drawOval(
                brush = Brush.linearGradient(
                    *arrayOf(
                        0f to OrbitColors.cyan400,
                        0.4f to OrbitColors.blue500,
                        0.72f to OrbitColors.purple500,
                        1f to OrbitColors.coral500,
                    ),
                    start = Offset(7.7f * scale, 21.04f * scale),
                    end = Offset(24.3f * scale, 10.96f * scale),
                ),
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1.6f * scale),
            )
        }
        drawCircle(color = dotColor, radius = 4.44f * scale, center = center)
        drawCircle(color = OrbitColors.coral500, radius = 2.22f * scale, center = Offset(24.3f * scale, 10.96f * scale))
    }
}
