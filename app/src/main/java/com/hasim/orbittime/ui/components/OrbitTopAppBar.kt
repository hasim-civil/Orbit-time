package com.hasim.orbittime.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/** The dark tile's own two-stop gradient — measured from the reference, close to but not exactly [OrbitColors.void300]/[OrbitColors.void700]. */
private val LogoTileTop = Color(0xFF241542)

/** Reference wordmark: bolder and slightly larger than the shared [OrbitTypography.label]. */
private val WordmarkStyle = OrbitTypography.label.copy(fontSize = 12.sp, letterSpacing = 1.7.sp)

/** The logo + wordmark + bell + avatar bar shown atop every authenticated screen. */
@Composable
fun OrbitTopAppBar(
    userInitials: String,
    hasNotification: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // The reference's "logoGlowSm" keyframe on this tile runs at 9s (vs. 6s on the nav
    // button) — same two-state box-shadow breathe, reproduced as a lerp on a triangle wave.
    val infiniteTransition = rememberInfiniteTransition(label = "logoGlow")
    val glowT by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoGlowBreathe",
    )
    val glowColor = lerp(Color(0xFF6D3BF5).copy(alpha = 0.22f), OrbitColors.purple600.copy(alpha = 0.44f), glowT)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitSpacing.screenHorizontal, vertical = OrbitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .blur(12.dp)
                    .background(brush = Brush.radialGradient(colors = listOf(glowColor, Color.Transparent)), shape = CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        brush = Brush.linearGradient(colors = listOf(LogoTileTop, OrbitColors.void700)),
                        shape = OrbitShapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                OrbitMiniLogo()
            }
        }
        Spacer(modifier = Modifier.width(OrbitSpacing.sm))
        Text(
            text = "ORBIT TIME",
            style = WordmarkStyle,
            color = OrbitColors.ink900,
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(OrbitColors.cream50.copy(alpha = 0.96f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val bell = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.06f)
                    cubicTo(
                        size.width * 0.22f, size.height * 0.1f,
                        size.width * 0.2f, size.height * 0.4f,
                        size.width * 0.2f, size.height * 0.55f,
                    )
                    lineTo(size.width * 0.1f, size.height * 0.75f)
                    lineTo(size.width * 0.9f, size.height * 0.75f)
                    lineTo(size.width * 0.8f, size.height * 0.55f)
                    cubicTo(
                        size.width * 0.8f, size.height * 0.4f,
                        size.width * 0.78f, size.height * 0.1f,
                        size.width * 0.5f, size.height * 0.06f,
                    )
                    close()
                }
                drawPath(bell, color = OrbitColors.ink900, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.09f))
                drawArc(
                    color = OrbitColors.ink900,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.38f, size.height * 0.74f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.24f, size.height * 0.2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.09f),
                )
            }
            if (hasNotification) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .border(2.dp, OrbitColors.cream50, CircleShape)
                        .background(OrbitColors.danger, CircleShape)
                        .align(Alignment.TopEnd),
                )
            }
        }
        Spacer(modifier = Modifier.width(OrbitSpacing.sm))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    brush = Brush.linearGradient(colors = listOf(OrbitColors.purple500, OrbitColors.blue500)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = userInitials, style = OrbitTypography.bodySmall, color = OrbitColors.cream50)
        }
    }
}
