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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

enum class OrbitTab { HOME, TIMESHEET, PUNCH, REPORTS, PROFILE }

private val NavBarShape = OrbitShapes.card
private val CenterButtonDiameter = 64.dp
private val CenterButtonGlowDiameter = 92.dp

@Composable
fun OrbitBottomNav(
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = NavBarShape)
                .background(OrbitColors.cream50.copy(alpha = 0.8f), NavBarShape)
                .padding(horizontal = OrbitSpacing.sm, vertical = OrbitSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NavTabItem(NavGlyph.HOME, "Home", selectedTab == OrbitTab.HOME) { onTabSelected(OrbitTab.HOME) }
            NavTabItem(NavGlyph.TIMESHEET, "Timesheet", selectedTab == OrbitTab.TIMESHEET) { onTabSelected(OrbitTab.TIMESHEET) }
            Spacer(modifier = Modifier.size(CenterButtonDiameter))
            NavTabItem(NavGlyph.REPORTS, "Reports", selectedTab == OrbitTab.REPORTS) { onTabSelected(OrbitTab.REPORTS) }
            NavTabItem(NavGlyph.PROFILE, "Profile", selectedTab == OrbitTab.PROFILE) { onTabSelected(OrbitTab.PROFILE) }
        }

        OrbitCenterButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-26).dp),
            onClick = { onTabSelected(OrbitTab.PUNCH) },
        )
    }
}

/** The floating central action button — always navigates to Punch, position never shifts with tab selection. */
@Composable
private fun OrbitCenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "orbitButton")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(14000, easing = LinearEasing)),
        label = "ringRotation",
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "breathe",
    )
    val pressedScale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "pressedScale")
    val breatheScale = 1f + breathe * 0.035f
    val glowAlpha = 0.28f + breathe * 0.22f

    Box(modifier = modifier.size(CenterButtonGlowDiameter), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(CenterButtonGlowDiameter)
                .graphicsLayer { alpha = glowAlpha }
                .blur(18.dp)
                .background(
                    brush = Brush.radialGradient(colors = listOf(OrbitColors.violet600, Color.Transparent)),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .size(CenterButtonDiameter)
                .graphicsLayer {
                    scaleX = breatheScale * pressedScale
                    scaleY = breatheScale * pressedScale
                }
                .shadow(elevation = 14.dp, shape = CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(OrbitColors.purple600, OrbitColors.violet700, OrbitColors.void900),
                        center = Offset.Unspecified,
                    ),
                    shape = CircleShape,
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            OrbitNavRing(diameter = 32.dp, rotationDegrees = ringRotation)
        }
    }
}

/** A plain white ring + dot — the orbit mark rendered for the dark center button, no gradient stroke. */
@Composable
private fun OrbitNavRing(diameter: Dp, rotationDegrees: Float) {
    Canvas(modifier = Modifier.size(diameter).graphicsLayer { rotationZ = rotationDegrees }) {
        val strokeWidth = size.minDimension * 0.11f
        drawOval(
            color = Color.White,
            topLeft = Offset(size.width * 0.06f, size.height * 0.30f),
            size = Size(size.width * 0.88f, size.height * 0.40f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.10f,
            center = Offset(size.width * 0.86f, size.height * 0.62f),
        )
    }
}

@Composable
private fun NavTabItem(
    glyph: NavGlyph,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) OrbitColors.ink900 else OrbitColors.slate400
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = OrbitSpacing.sm, vertical = OrbitSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NavIcon(glyph = glyph, tint = tint)
        Text(text = label, style = OrbitTypography.bodySmall, color = tint)
    }
}
