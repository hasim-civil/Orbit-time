package com.hasim.orbittime.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.ImageCodec

/** The dark tile's own two-stop gradient — measured from the reference, close to but not exactly [OrbitColors.void300]/[OrbitColors.void700]. */
private val LogoTileTop = Color(0xFF241542)

/** Reference wordmark: bolder and slightly larger than the shared [OrbitTypography.label]. */
private val WordmarkStyle = OrbitTypography.label.copy(fontSize = 12.sp, letterSpacing = 1.7.sp)

/** The logo + wordmark + bell + avatar bar shown atop every authenticated screen. */
@Composable
fun OrbitTopAppBar(
    userInitials: String,
    hasNotification: Boolean = false,
    photoBase64: String = "",
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null,
    onBellClick: (() -> Unit)? = null,
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

    // One subtle pulse the moment a genuinely new notification arrives (false -> true), never
    // a continuous animation and never replayed just because the screen recomposes while it's
    // already true (e.g. re-entering a tab with an unread notification still pending).
    var wasNotified by remember { mutableStateOf(hasNotification) }
    val bellPulse = remember { Animatable(1f) }
    LaunchedEffect(hasNotification) {
        if (hasNotification && !wasNotified) {
            bellPulse.animateTo(1.22f, tween(140, easing = FastOutSlowInEasing))
            bellPulse.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        }
        wasNotified = hasNotification
    }

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

        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { scaleX = bellPulse.value; scaleY = bellPulse.value }
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(OrbitColors.cream50.copy(alpha = 0.96f), CircleShape)
                    .clickable(enabled = onBellClick != null) { onBellClick?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val scaleX = size.width / 24f
                    val scaleY = size.height / 24f
                    fun px(x: Float) = x * scaleX
                    fun py(y: Float) = y * scaleY

                    // A filled bell + clapper silhouette (proportioned on a 24x24 grid),
                    // reads more cleanly at this size than the earlier thin stroked outline.
                    val bell = Path().apply {
                        // Clapper knob.
                        moveTo(px(12f), py(22f))
                        cubicTo(px(13.1f), py(22f), px(14f), py(21.1f), px(14f), py(20f))
                        lineTo(px(10f), py(20f))
                        cubicTo(px(10f), py(21.1f), px(10.89f), py(22f), px(12f), py(22f))
                        close()

                        // Bell body.
                        moveTo(px(18f), py(16f))
                        lineTo(px(18f), py(11f))
                        cubicTo(px(18f), py(7.93f), px(16.36f), py(5.36f), px(13.5f), py(4.68f))
                        lineTo(px(13.5f), py(4f))
                        cubicTo(px(13.5f), py(3.17f), px(12.83f), py(2.5f), px(12f), py(2.5f))
                        cubicTo(px(11.17f), py(2.5f), px(10.5f), py(3.17f), px(10.5f), py(4f))
                        lineTo(px(10.5f), py(4.68f))
                        cubicTo(px(7.63f), py(5.36f), px(6f), py(7.92f), px(6f), py(11f))
                        lineTo(px(6f), py(16f))
                        lineTo(px(4f), py(18f))
                        lineTo(px(4f), py(19f))
                        lineTo(px(20f), py(19f))
                        lineTo(px(20f), py(18f))
                        close()
                    }
                    drawPath(bell, color = OrbitColors.ink900)
                }
            }
            if (hasNotification) {
                // Sits outside the button's own clipped Box — placed inside it, the CircleShape
                // clip cut off most of the dot since aligning to the bounding square's corner
                // lands mostly outside the inscribed circle.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                        .size(10.dp)
                        .border(1.5.dp, OrbitColors.cream50, CircleShape)
                        .background(OrbitColors.danger, CircleShape),
                )
            }
        }
        Spacer(modifier = Modifier.width(OrbitSpacing.sm))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(enabled = onAvatarClick != null) { onAvatarClick?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            val decodedPhoto = remember(photoBase64) {
                photoBase64.takeIf { it.isNotBlank() }?.let { ImageCodec.decodeToImageBitmap(it) }
            }
            if (decodedPhoto != null) {
                Image(
                    bitmap = decodedPhoto,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
            } else {
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
    }
}
