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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors

/**
 * A static, small-scale rendering of the orbit mark — the gradient ring + dot
 * glyph, without the Welcome screen's floating/orbiting animation. Used
 * anywhere the full [OrbitSymbol] would be too small or too busy: the top bar
 * and the bottom nav's raised centre button.
 */
@Composable
fun OrbitMiniLogo(modifier: Modifier = Modifier, diameter: Dp = 18.dp, dotColor: Color = OrbitColors.lavenderWhite) {
    Canvas(modifier = modifier.size(diameter)) {
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(OrbitColors.cyan400, OrbitColors.blue500, OrbitColors.purple500, OrbitColors.coral500),
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
            ),
            topLeft = Offset(size.width * 0.08f, size.height * 0.28f),
            size = Size(size.width * 0.84f, size.height * 0.44f),
            style = Stroke(width = size.minDimension * 0.14f),
        )
        drawCircle(color = dotColor, radius = size.minDimension * 0.18f)
    }
}
