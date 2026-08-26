package com.hasim.orbittime.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

enum class OrbitTab { HOME, TIMESHEET, PUNCH, REPORTS, PROFILE }

// Measured from the reference: pill height ~65dp, button ~68dp centered on the pill's
// top edge. Total component height (button-top to pill-bottom) comes out to ~110dp —
// that total must be the composable's own measured height, or the button's protrusion
// gets clipped/ignored by whatever lays this component out.
private val NavComponentHeight = 110.dp
private val PillHeight = 65.dp
private val PillShape = RoundedCornerShape(percent = 50)
private val ButtonDiameter = 70.dp
private val ButtonGlowDiameter = 96.dp

@Composable
fun OrbitBottomNav(
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().height(NavComponentHeight)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(PillHeight)
                .shadow(elevation = 10.dp, shape = PillShape)
                .background(OrbitColors.cream50.copy(alpha = 0.8f), PillShape)
                .padding(horizontal = OrbitSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTabItem(NavGlyph.HOME, "Home", selectedTab == OrbitTab.HOME, Modifier.weight(1f)) { onTabSelected(OrbitTab.HOME) }
            NavTabItem(NavGlyph.TIMESHEET, "Timesheet", selectedTab == OrbitTab.TIMESHEET, Modifier.weight(1f)) { onTabSelected(OrbitTab.TIMESHEET) }
            Box(modifier = Modifier.weight(1f))
            NavTabItem(NavGlyph.REPORTS, "Reports", selectedTab == OrbitTab.REPORTS, Modifier.weight(1f)) { onTabSelected(OrbitTab.REPORTS) }
            NavTabItem(NavGlyph.PROFILE, "Profile", selectedTab == OrbitTab.PROFILE, Modifier.weight(1f)) { onTabSelected(OrbitTab.PROFILE) }
        }

        // OrbitCenterButton is a ButtonGlowDiameter-tall box with the button centered inside it,
        // so its own visual center sits at ButtonGlowDiameter/2 from its top. Offsetting by
        // (pill-top minus that half-height) puts the button's actual center on the pill's top edge.
        val pillTopFromComponentTop = NavComponentHeight - PillHeight
        OrbitCenterButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = pillTopFromComponentTop - ButtonGlowDiameter / 2),
            onClick = { onTabSelected(OrbitTab.PUNCH) },
        )
    }
}

/** The floating central action button. Its center sits on the pill's top edge and never moves with tab selection. */
@Composable
private fun OrbitCenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "orbitButton")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(16000, easing = LinearEasing)),
        label = "ringRotation",
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "breathe",
    )
    val pressedScale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "pressedScale")
    val breatheScale = 1f + breathe * 0.02f
    val glowAlpha = 0.18f + breathe * 0.12f

    Box(modifier = modifier.size(ButtonGlowDiameter), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(ButtonGlowDiameter)
                .graphicsLayer { alpha = glowAlpha }
                .blur(16.dp)
                .background(
                    brush = Brush.radialGradient(colors = listOf(OrbitColors.violet600, Color.Transparent)),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .size(ButtonDiameter)
                .graphicsLayer {
                    scaleX = breatheScale * pressedScale
                    scaleY = breatheScale * pressedScale
                }
                .shadow(elevation = 12.dp, shape = CircleShape)
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        colors = listOf(OrbitColors.purple600, OrbitColors.void500, OrbitColors.void900),
                        center = Offset(size.width * 0.32f, size.height * 0.28f),
                        radius = size.minDimension * 0.85f,
                    )
                    onDrawBehind { drawCircle(brush = brush) }
                }
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // Gloss highlight — a small soft light patch near the upper-left, giving the sphere volume.
            Box(
                modifier = Modifier
                    .size(ButtonDiameter * 0.4f)
                    .offset(x = -ButtonDiameter * 0.14f, y = -ButtonDiameter * 0.16f)
                    .graphicsLayer { alpha = 0.22f }
                    .blur(6.dp)
                    .background(Color.White, CircleShape),
            )

            OrbitRingGlyph(diameter = ButtonDiameter * 0.5f, rotationDegrees = ringRotation)
        }
    }
}

/**
 * The orbit ring + dot, matching the mark used elsewhere in the app (Welcome screen, top bar) but
 * rendered in plain white for this dark sphere, and — unlike those static marks — rotating slowly.
 */
@Composable
private fun OrbitRingGlyph(diameter: Dp, rotationDegrees: Float) {
    Canvas(modifier = Modifier.size(diameter).graphicsLayer { rotationZ = rotationDegrees }) {
        val strokeWidth = size.minDimension * 0.12f
        drawOval(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = Offset(size.width * 0.02f, size.height * 0.32f),
            size = Size(size.width * 0.96f, size.height * 0.36f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.09f,
            center = Offset(size.width * 0.88f, size.height * 0.60f),
        )
    }
}

@Composable
private fun NavTabItem(
    glyph: NavGlyph,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) OrbitColors.ink900 else OrbitColors.slate400
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NavIcon(glyph = glyph, tint = tint)
        Text(text = label, style = OrbitTypography.bodySmall, color = tint)
    }
}
