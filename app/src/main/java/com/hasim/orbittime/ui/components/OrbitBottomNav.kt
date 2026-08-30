package com.hasim.orbittime.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import kotlin.math.cos
import kotlin.math.sin

enum class OrbitTab { HOME, TIMESHEET, PUNCH, REPORTS, PROFILE }

/** The button's own two-stop radial gradient — measured from the reference, no exact palette match. */
private val ButtonSphereHighlight = Color(0xFF5A2FB0)

/** The reference's press-release overshoot easing: cubic-bezier(0.3, 1.4, 0.5, 1). */
private val PressEasing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)

// Measured directly from the reference's live markup (60x60 button, margin-top: -28px, in a
// 66px pill) at the reference's 393dp device width — not an approximation from a still image.
private val PillHeight = 65.dp
private val PillShape = RoundedCornerShape(26.dp)
private val CenterSlotWidth = 68.dp
private val ButtonDiameter = 59.dp
private val ButtonGlowDiameter = 84.dp

// The button's top edge sits this far above the pill's top edge — the rest of its height
// (ButtonDiameter minus this) overlaps down into the pill. The component's own height must
// span from the button's top down to the pill's bottom, or the protrusion gets clipped.
private val ButtonAbovePill = 16.dp
private val NavComponentHeight = ButtonAbovePill + PillHeight

/** The reference's fixed side/bottom margins for the floating pill. */
private val NavHorizontalMargin = 14.dp
private val NavBottomMargin = 12.dp

/** The reference's bottom fade scrim height (`104px`), easing scrolled content into the nav. */
private val NavScrimHeight = 102.dp

/**
 * How much bottom breathing room scrollable screen content should reserve for itself inside
 * [OrbitFloatingNavHost] — matching the reference's own content `padding-bottom: 74px`. This is
 * deliberately less than the nav's full footprint: content is meant to keep scrolling in behind
 * the translucent pill and fade scrim, not stop dead clear of it.
 */
val OrbitFloatingNavContentClearance = 72.dp

/**
 * Hosts scrollable screen content with the custom bottom nav floating on top of it, exactly like
 * the reference: content scrolls in behind the translucent, shadowed pill (softened by a fade
 * scrim) instead of stopping in a reserved empty band beneath it. [content] is responsible for
 * its own trailing [OrbitFloatingNavContentClearance] spacer so its last item isn't fully hidden.
 */
@Composable
fun OrbitFloatingNavHost(
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    modifier: Modifier = Modifier,
    // Every screen but Home passes this implicitly and keeps the reference's original 12dp
    // margin. Home's content is a fixed, non-scrolling stack (see HomeDashboardContent), so on a
    // screen taller than that stack, the pill's position can't be reached at all by adjusting
    // Home's own layout — the pill floats independently of content height. Letting Home pass a
    // larger margin here is the only way to bring it closer to the Attendance Summary card
    // without touching this shared component's behavior anywhere else.
    pillBottomMargin: Dp = NavBottomMargin,
    content: @Composable () -> Unit,
) {
    // How much higher the pill sits than the reference's own 12dp — the fade scrim grows by the
    // same amount so the pill/button stay in the same relative position within it instead of the
    // button's top poking out above the scrim into the plain background behind it.
    val pillLift = (pillBottomMargin - NavBottomMargin).coerceAtLeast(0.dp)

    Box(modifier = modifier.fillMaxSize()) {
        content()

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(NavScrimHeight + pillLift)
                .background(
                    brush = Brush.verticalGradient(
                        0f to OrbitColors.cream100.copy(alpha = 0f),
                        0.46f to OrbitColors.cream100.copy(alpha = 0.82f),
                        1f to OrbitColors.cream100.copy(alpha = 0.96f),
                    ),
                ),
        )

        OrbitBottomNav(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = NavHorizontalMargin, end = NavHorizontalMargin, bottom = pillBottomMargin),
        )
    }
}

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
                .shadow(elevation = 10.dp, shape = PillShape, ambientColor = Color.Black.copy(alpha = 0.16f), spotColor = Color.Black.copy(alpha = 0.16f))
                .background(OrbitColors.cream50.copy(alpha = 0.92f), PillShape)
                .padding(horizontal = OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTabItem(NavGlyph.HOME, "Home", selectedTab == OrbitTab.HOME, Modifier.weight(1f)) { onTabSelected(OrbitTab.HOME) }
            NavTabItem(NavGlyph.TIMESHEET, "Timesheet", selectedTab == OrbitTab.TIMESHEET, Modifier.weight(1f)) { onTabSelected(OrbitTab.TIMESHEET) }
            Box(modifier = Modifier.width(CenterSlotWidth))
            NavTabItem(NavGlyph.REPORTS, "Reports", selectedTab == OrbitTab.REPORTS, Modifier.weight(1f)) { onTabSelected(OrbitTab.REPORTS) }
            NavTabItem(NavGlyph.PROFILE, "Profile", selectedTab == OrbitTab.PROFILE, Modifier.weight(1f)) { onTabSelected(OrbitTab.PROFILE) }
        }

        // The button's own bounding box top aligns with the component's top; the glow
        // wrapper is centered on the button, so it's offset up by half the extra diameter.
        OrbitCenterButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -(ButtonGlowDiameter - ButtonDiameter) / 2),
            onClick = { onTabSelected(OrbitTab.PUNCH) },
        )
    }
}

/** The floating central action button. Its position never moves with tab selection. */
@Composable
private fun OrbitCenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "orbitButton")
    // The reference's "logoGlowSm" keyframe: a 6s ease-in-out box-shadow breathe between two
    // states, reproduced here as a lerp between the two shadow colors on a triangle-wave float.
    val glowT by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowBreathe",
    )
    // The core dot's "cloudPulse" keyframe: 0%:0.5, 35%:0.95, 65%:0.72, 100%:0.5 over 4s.
    val coreAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0.5f at 0
                0.95f at 1400
                0.72f at 2600
                0.5f at 4000
            },
        ),
        label = "corePulse",
    )
    // The orbit dot's "animateMotion": one full lap around the tilted ring every 7s, linear.
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(7000, easing = LinearEasing)),
        label = "orbitAngle",
    )
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(250, easing = PressEasing),
        label = "pressedScale",
    )

    Box(modifier = modifier.size(ButtonGlowDiameter), contentAlignment = Alignment.Center) {
        // A soft multi-stop radial fade rather than Modifier.blur(): blur promotes this Box to
        // its own hardware layer and, on plenty of devices, that layer's square bounds show
        // through as a faint rectangular halo around the circular glow. A gradient that already
        // tapers to fully transparent needs no blur to look soft, and never has square edges.
        val glowColor = lerp(Color(0xFF6D3BF5).copy(alpha = 0.22f), OrbitColors.purple600.copy(alpha = 0.44f), glowT)
        Box(
            modifier = Modifier
                .size(ButtonGlowDiameter)
                .background(
                    brush = Brush.radialGradient(
                        0f to glowColor,
                        0.4f to glowColor.copy(alpha = glowColor.alpha * 0.5f),
                        0.7f to glowColor.copy(alpha = glowColor.alpha * 0.18f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .size(ButtonDiameter)
                .graphicsLayer { scaleX = pressedScale; scaleY = pressedScale }
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        0f to ButtonSphereHighlight,
                        0.78f to OrbitColors.void500,
                        center = Offset(size.width * 0.30f, size.height * 0.20f),
                        radius = size.minDimension * 0.95f,
                    )
                    onDrawBehind { drawCircle(brush = brush) }
                }
                .border(0.75.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            OrbitRingGlyph(diameter = ButtonDiameter - 16.dp, orbitAngleDegrees = orbitAngle)

            // The pulsing white "core" at the ring's center, with a soft lavender glow — a
            // multi-stop radial fade rather than Modifier.blur(), same reasoning as the button's
            // own outer glow: blur promotes this Box to its own hardware layer, and that layer's
            // square bounds can show through as a faint rectangle around the small glow.
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { alpha = coreAlpha * 0.6f }
                    .background(
                        brush = Brush.radialGradient(
                            0f to Color(0xFFD6C4FF),
                            0.5f to Color(0xFFD6C4FF).copy(alpha = 0.5f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .graphicsLayer { alpha = coreAlpha }
                    .background(Color.White, CircleShape),
            )
        }
    }
}

/**
 * The orbit glyph: a static tilted ring (–28°, matching the reference's fixed SVG transform —
 * the ring itself never rotates) with a small dot continuously travelling around it, reproducing
 * the reference's `<animateMotion>` orbit rather than spinning the whole ring.
 */
@Composable
private fun OrbitRingGlyph(diameter: Dp, orbitAngleDegrees: Float) {
    Canvas(modifier = Modifier.size(diameter)) {
        val rx = size.width * 0.40f
        val ry = size.height * 0.255f
        val center = Offset(size.width / 2f, size.height / 2f)
        val tiltRad = Math.toRadians(-28.0)
        val cosTilt = cos(tiltRad).toFloat()
        val sinTilt = sin(tiltRad).toFloat()

        rotate(degrees = -28f, pivot = center) {
            drawOval(
                color = OrbitColors.lavenderWhite.copy(alpha = 0.68f),
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = size.minDimension * 0.06f),
            )
        }

        val angleRad = Math.toRadians(orbitAngleDegrees.toDouble())
        val localX = (rx * cos(angleRad)).toFloat()
        val localY = (ry * sin(angleRad)).toFloat()
        val dotCenter = Offset(
            center.x + localX * cosTilt - localY * sinTilt,
            center.y + localX * sinTilt + localY * cosTilt,
        )
        drawCircle(color = OrbitColors.purple500, radius = size.minDimension * 0.075f, center = dotCenter)
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
