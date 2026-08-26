package com.hasim.orbittime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Small hand-drawn glyphs for the bottom nav and top bar — kept as plain Canvas
 * paths rather than pulling in an icon library for four simple silhouettes.
 */
enum class NavGlyph { HOME, TIMESHEET, REPORTS, PROFILE }

@Composable
fun NavIcon(glyph: NavGlyph, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)

        when (glyph) {
            NavGlyph.HOME -> {
                val path = Path().apply {
                    moveTo(w * 0.12f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.88f, h * 0.5f)
                    moveTo(w * 0.22f, h * 0.42f)
                    lineTo(w * 0.22f, h * 0.86f)
                    lineTo(w * 0.78f, h * 0.86f)
                    lineTo(w * 0.78f, h * 0.42f)
                }
                drawPath(path, color = tint, style = stroke)
            }
            NavGlyph.TIMESHEET -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.18f),
                    size = Size(w * 0.68f, h * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.16f, h * 0.4f), Offset(w * 0.84f, h * 0.4f), strokeWidth = stroke.width)
                drawLine(tint, Offset(w * 0.34f, h * 0.1f), Offset(w * 0.34f, h * 0.26f), strokeWidth = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(tint, Offset(w * 0.66f, h * 0.1f), Offset(w * 0.66f, h * 0.26f), strokeWidth = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            NavGlyph.REPORTS -> {
                val barWidth = w * 0.16f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.55f),
                    size = Size(barWidth, h * 0.32f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.3f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.42f, h * 0.32f),
                    size = Size(barWidth, h * 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.3f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.66f, h * 0.16f),
                    size = Size(barWidth, h * 0.71f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.3f),
                )
            }
            NavGlyph.PROFILE -> {
                drawCircle(color = tint, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.32f))
                val body = Path().apply {
                    moveTo(w * 0.2f, h * 0.86f)
                    quadraticTo(w * 0.5f, h * 0.55f, w * 0.8f, h * 0.86f)
                }
                drawPath(body, color = tint, style = stroke)
            }
        }
    }
}
