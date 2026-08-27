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
 * A static, small-scale rendering of the orbit mark — matching the reference's exact SVG
 * (viewBox 32x32: a –28°-tilted ring stroked with a blue-to-purple gradient, a light center
 * dot, and a small violet accent dot near the ring's upper edge). Used in the top bar.
 */
@Composable
fun OrbitMiniLogo(modifier: Modifier = Modifier, diameter: Dp = 20.dp, dotColor: Color = OrbitColors.lavenderWhite) {
    Canvas(modifier = modifier.size(diameter)) {
        val scale = size.width / 32f
        val center = Offset(16f * scale, 16f * scale)
        val rx = 13f * scale
        val ry = 8.4f * scale

        rotate(degrees = -28f, pivot = center) {
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(OrbitColors.blue500, OrbitColors.purple500),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                ),
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 2f * scale),
            )
        }
        drawCircle(color = dotColor, radius = 4.1f * scale, center = center)
        drawCircle(color = OrbitColors.purple500, radius = 2.6f * scale, center = Offset(26f * scale, 9.6f * scale))
    }
}
