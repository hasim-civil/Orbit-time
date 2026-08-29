package com.hasim.orbittime.ui.screens.home

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitFloatingNavContentClearance
import com.hasim.orbittime.ui.components.OrbitFloatingNavHost
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.components.cardRiseEntrance
import com.hasim.orbittime.ui.screens.punch.AttendanceViewModel
import com.hasim.orbittime.ui.screens.punch.PunchUiState
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.InstrumentSerif
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.PixelifySans
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceRangeMode
import com.hasim.orbittime.util.AttendanceSummary
import com.hasim.orbittime.util.AttendanceTimeFormat
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin

private val AttendanceRingSwayEasing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

/** Reference's dashboard tab content uses a tighter side margin (14px) than the auth flow's
 * own screens (~24-26px) — matched here rather than via the shared OrbitSpacing.screenHorizontal
 * token, which auth screens still rely on. */
private val DashboardHorizontalMargin = 14.dp
private val DashboardSectionGap = 13.dp

// Text styles below are measured directly from the reference's inline styles for this screen —
// the reference contrasts an editorial serif for headline moments (clock, month label, the two
// big ring numbers) against Manrope for everything else, which the shared OrbitTypography scale
// doesn't capture for these specific spots (it uses Manrope for them).
//
// The main greeting is a deliberate departure from the reference: a bold Pixelify Sans
// dot-matrix face for a premium, Nothing-Phone-style accent moment, scoped to only this one
// piece of text — every other heading keeps Instrument Serif. A small positive letter-spacing
// (rather than the editorial styles' tight/negative tracking) keeps the blocky glyphs legible.
private val GreetingHeadlineStyle = TextStyle(fontFamily = PixelifySans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp, letterSpacing = 0.3.sp)
private val ClockTimeStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 23.sp, lineHeight = 23.sp)
private val MonthHeadingStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 24.sp)
private val RingBigNumberStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 26.sp)
private val RingCaptionStyle = OrbitTypography.label.copy(fontWeight = FontWeight.Normal, fontSize = 8.sp, letterSpacing = 0.5.sp)

@Composable
fun HomeDashboardScreen(
    userDisplayName: String,
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    photoBase64: String = "",
    hasNotification: Boolean = false,
    onBellClick: () -> Unit = {},
    viewModel: AttendanceViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeDashboardContent(
        userDisplayName = userDisplayName,
        userInitials = userInitials,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        onRangeModeSelected = viewModel::setRangeMode,
        photoBase64 = photoBase64,
        hasNotification = hasNotification,
        onBellClick = onBellClick,
    )
}

@Composable
fun HomeDashboardContent(
    userDisplayName: String,
    userInitials: String,
    uiState: PunchUiState,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    onRangeModeSelected: (AttendanceRangeMode) -> Unit,
    photoBase64: String = "",
    hasNotification: Boolean = false,
    onBellClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            OrbitTopAppBar(
                userInitials = userInitials,
                photoBase64 = photoBase64,
                hasNotification = hasNotification,
                onBellClick = onBellClick,
                onAvatarClick = { onTabSelected(OrbitTab.PROFILE) },
            )

            OrbitFloatingNavHost(selectedTab = selectedTab, onTabSelected = onTabSelected, modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DashboardHorizontalMargin)
                        .cardRiseEntrance(),
                ) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.sm))

                    if (uiState.isLoading) {
                        LoadingBox(height = 140.dp)
                    } else {
                        GreetingCard(userDisplayName, uiState)
                    }

                    Spacer(modifier = Modifier.height(DashboardSectionGap))

                    if (uiState.summaryErrorMessage != null) {
                        InlineBanner(text = uiState.summaryErrorMessage, color = OrbitColors.danger, background = OrbitColors.dangerBg)
                        Spacer(modifier = Modifier.height(DashboardSectionGap))
                    } else if (!uiState.isOnline) {
                        InlineBanner(
                            text = "You're offline. Showing the last synced attendance data.",
                            color = OrbitColors.warningDark,
                            background = OrbitColors.warningBg,
                        )
                        Spacer(modifier = Modifier.height(DashboardSectionGap))
                    }

                    if (uiState.isSummaryLoading) {
                        LoadingBox(height = 180.dp)
                    } else {
                        MonthlyAttendanceCard(
                            summary = uiState.summary,
                            rangeMode = uiState.rangeMode,
                            onRangeModeSelected = onRangeModeSelected,
                        )
                        Spacer(modifier = Modifier.height(DashboardSectionGap))
                        AttendanceSummaryCard(summary = uiState.summary, rangeMode = uiState.rangeMode)
                    }

                    Spacer(modifier = Modifier.height(OrbitFloatingNavContentClearance))
                }
            }
        }
    }
}

@Composable
private fun LoadingBox(height: Dp) {
    Box(modifier = Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = OrbitColors.violet600)
    }
}

@Composable
private fun GreetingCard(userDisplayName: String, uiState: PunchUiState) {
    val today = AttendanceTimeFormat.today()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.card)
            .padding(horizontal = OrbitSpacing.xl, vertical = OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${AttendanceTimeFormat.greeting()}${if (userDisplayName.isBlank()) "" else ", $userDisplayName"}",
                    style = GreetingHeadlineStyle,
                    color = OrbitColors.ink900,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(
                    text = AttendanceTimeFormat.dayOfWeekAndDate(today),
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
                )
            }
            Text(
                text = AttendanceTimeFormat.clockTime(Instant.now()),
                style = ClockTimeStyle,
                color = OrbitColors.ink900,
            )
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.sm))

        val statusText = when {
            uiState.isCompleted -> "Checked out"
            uiState.isCheckedIn -> "Checked in"
            else -> "Not checked in"
        }
        val statusColor = if (uiState.isCheckedIn) OrbitColors.success else OrbitColors.slate500
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        ) {
            BreathingStatusDot(color = statusColor)
            Spacer(modifier = Modifier.width(OrbitSpacing.xs))
            Text(text = statusText, style = OrbitTypography.bodySmall, color = statusColor)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.sm))

        Row(
            modifier = Modifier.height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            HomeStatCell(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = "CHECK IN",
                value = uiState.checkInAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "—",
            )
            HomeStatCell(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = "CHECK OUT",
                value = uiState.checkOutAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "—",
            )
            HomeStatCell(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = "TOTAL",
                value = AttendanceTimeFormat.elapsedLabel(uiState.elapsed),
                emphasized = true,
            )
        }
    }
}

/** The reference's "dotBreathe": a small status dot that gently scales and dims, 3.6s ease-in-out infinite. */
@Composable
private fun BreathingStatusDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotBreathe")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dotBreatheT",
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer {
                scaleX = 1f + t * 0.4f
                scaleY = 1f + t * 0.4f
                alpha = 1f - t * 0.5f
            }
            .background(color, CircleShape),
    )
}

@Composable
private fun HomeStatCell(label: String, value: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Column(
        modifier = modifier
            .background(
                if (emphasized) OrbitColors.ink900 else OrbitColors.mist,
                OrbitShapes.small,
            )
            .padding(horizontal = 10.dp, vertical = OrbitSpacing.sm),
    ) {
        Text(
            text = label,
            style = OrbitTypography.label,
            color = if (emphasized) OrbitColors.slate300 else OrbitColors.slate500,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
        Text(
            text = value,
            style = OrbitTypography.titleMedium,
            color = if (emphasized) OrbitColors.cream50 else OrbitColors.ink900,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun MonthlyAttendanceCard(
    summary: AttendanceSummary,
    rangeMode: AttendanceRangeMode,
    onRangeModeSelected: (AttendanceRangeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(colors = listOf(OrbitColors.void300, OrbitColors.void600, OrbitColors.void900)),
                shape = OrbitShapes.card,
            )
            .padding(horizontal = OrbitSpacing.xl, vertical = OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (rangeMode == AttendanceRangeMode.MONTH) "MONTHLY ATTENDANCE" else "WEEKLY ATTENDANCE",
                    style = OrbitTypography.label,
                    color = OrbitColors.slate300,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(text = summary.rangeLabel, style = MonthHeadingStyle, color = OrbitColors.cream50)
            }
            RangeModeToggle(selected = rangeMode, onSelected = onRangeModeSelected)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AttendanceRing(presentDays = summary.presentDays, ratePercent = summary.attendanceRatePercent, diameter = 118.dp)

            Spacer(modifier = Modifier.width(OrbitSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${summary.attendanceRatePercent}%", style = RingBigNumberStyle, color = OrbitColors.cream50)
                Text(text = "attendance rate", style = OrbitTypography.bodyMedium, color = OrbitColors.slate300)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                LegendRow(color = OrbitColors.success, text = "${summary.presentDays} present")
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                LegendRow(color = OrbitColors.warning, text = "${summary.absentDays} absent · ${summary.lateDays} late")
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                LegendRow(color = OrbitColors.accent, text = "${AttendanceTimeFormat.wholeHoursLabel(summary.overtime)} overtime")
            }
        }
    }
}

@Composable
private fun AttendanceRing(
    presentDays: Int,
    ratePercent: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 118.dp,
) {
    // The outer glow sways back and forth like a wave rather than spinning all the way
    // around, plus the reference's "ringBreath" (5.5s arc breathe) and a small comet dot
    // that continuously circles the ring independent of the data value.
    val infiniteTransition = rememberInfiniteTransition(label = "attendanceRing")
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = -24f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(animation = tween(4400, easing = AttendanceRingSwayEasing), repeatMode = RepeatMode.Reverse),
        label = "glowRotation",
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2750, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "ringBreathe",
    )
    val cometAngle by infiniteTransition.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(animation = tween(9000, easing = LinearEasing)),
        label = "cometAngle",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(diameter + 24.dp)
                .graphicsLayer { rotationZ = glowRotation }
                .blur(10.dp)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            OrbitColors.purple600.copy(alpha = 0.34f),
                            Color.Transparent,
                            Color.Transparent,
                            OrbitColors.coral500.copy(alpha = 0.3f),
                            OrbitColors.purple600.copy(alpha = 0.34f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Canvas(modifier = Modifier.size(diameter)) {
            // Reference's ring is drawn at stroke-width 13 inside a 146-unit viewBox but
            // displayed at 118px — i.e. a stroke that's 13/146 ≈ 8.9% of the rendered
            // diameter, not the fraction of the box a "13" might suggest at face value.
            val strokeWidth = size.minDimension * 0.089f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )

            val sweep = 360f * ratePercent.coerceIn(0, 100) / 100f
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(OrbitColors.cyan400, OrbitColors.blue500, OrbitColors.purple600, OrbitColors.violet700, OrbitColors.cyan400),
                        center = center,
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    alpha = 0.9f + breathe * 0.1f,
                )
            }

            val cometRad = Math.toRadians(cometAngle.toDouble())
            val cometCenter = Offset(
                center.x + (radius * cos(cometRad)).toFloat(),
                center.y + (radius * sin(cometRad)).toFloat(),
            )
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = strokeWidth * 0.32f, center = cometCenter)
        }

        Column(
            modifier = Modifier.width(diameter * 0.62f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = presentDays.toString(), style = RingBigNumberStyle, color = OrbitColors.cream50)
            Text(
                text = "DAYS PRESENT",
                style = RingCaptionStyle,
                color = OrbitColors.slate300,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(OrbitSpacing.xs))
        Text(text = text, style = OrbitTypography.bodySmall, color = OrbitColors.slate200)
    }
}

@Composable
private fun RangeModeToggle(selected: AttendanceRangeMode, onSelected: (AttendanceRangeMode) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), OrbitShapes.pill)
            .border(1.dp, Color.White.copy(alpha = 0.12f), OrbitShapes.pill)
            .padding(3.dp),
    ) {
        ToggleSegment(label = "Week", selected = selected == AttendanceRangeMode.WEEK) { onSelected(AttendanceRangeMode.WEEK) }
        ToggleSegment(label = "Month", selected = selected == AttendanceRangeMode.MONTH) { onSelected(AttendanceRangeMode.MONTH) }
    }
}

@Composable
private fun ToggleSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(OrbitShapes.pill)
            .background(if (selected) OrbitColors.cream50 else Color.Transparent, OrbitShapes.pill)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = OrbitSpacing.md, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = OrbitTypography.bodySmall,
            color = if (selected) OrbitColors.ink900 else OrbitColors.slate300,
        )
    }
}

@Composable
private fun AttendanceSummaryCard(summary: AttendanceSummary, rangeMode: AttendanceRangeMode) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.card)
            .padding(OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Attendance summary",
                style = OrbitTypography.headline,
                color = OrbitColors.ink900,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (rangeMode == AttendanceRangeMode.MONTH) "THIS MONTH" else "THIS WEEK",
                style = OrbitTypography.label,
                color = OrbitColors.slate500,
            )
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            SummaryCell(modifier = Modifier.weight(1f), accent = OrbitColors.success, value = summary.presentDays.toString(), label = "Present")
            SummaryCell(modifier = Modifier.weight(1f), accent = OrbitColors.danger, value = summary.absentDays.toString(), label = "Absent")
            SummaryCell(modifier = Modifier.weight(1f), accent = OrbitColors.warning, value = summary.lateDays.toString(), label = "Late")
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            SummaryCell(
                modifier = Modifier.weight(1f),
                accent = OrbitColors.accent,
                value = AttendanceTimeFormat.wholeHoursLabel(summary.worked),
                label = "Worked",
            )
            SummaryCell(modifier = Modifier.weight(1f), accent = OrbitColors.info, value = summary.leaveDays.toString(), label = "Leave")
            SummaryCell(
                modifier = Modifier.weight(1f),
                accent = OrbitColors.ink900,
                value = AttendanceTimeFormat.wholeHoursLabel(summary.overtime),
                label = "Overtime",
            )
        }
    }
}

private val SummaryCellChipShape = RoundedCornerShape(7.dp)

/**
 * Reference's per-stat "glass" formula, ported directly: a diagonal tint-to-white gradient
 * fill, a 1dp border and a small icon chip (a rounded tile in the same accent, holding a
 * solid dot of it) — all derived from one [accent] color rather than the flat pastel tokens
 * this cell used before, so each stat still reads as its own color without six near-identical
 * flat swatches.
 */
@Composable
private fun SummaryCell(modifier: Modifier, accent: Color, value: String, label: String) {
    Column(
        modifier = modifier
            .clip(OrbitShapes.small)
            .background(
                brush = Brush.linearGradient(
                    0f to accent.copy(alpha = 0.12f),
                    0.42f to accent.copy(alpha = 0.04f),
                    1f to Color.Transparent,
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.16f), OrbitShapes.small)
            .padding(horizontal = 10.dp, vertical = OrbitSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.1f)),
                    ),
                    shape = SummaryCellChipShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(7.dp).background(accent, CircleShape))
        }
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Text(text = value, style = OrbitTypography.titleLarge, color = OrbitColors.ink900)
        Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
        Text(text = label, style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
    }
}
