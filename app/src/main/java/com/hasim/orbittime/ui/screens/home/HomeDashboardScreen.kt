package com.hasim.orbittime.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitBottomNav
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.screens.punch.AttendanceViewModel
import com.hasim.orbittime.ui.screens.punch.PunchUiState
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceRangeMode
import com.hasim.orbittime.util.AttendanceSummary
import com.hasim.orbittime.util.AttendanceTimeFormat
import java.time.Instant

@Composable
fun HomeDashboardScreen(
    userDisplayName: String,
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
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
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            OrbitTopAppBar(userInitials = userInitials, hasNotification = true)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrbitSpacing.screenHorizontal),
            ) {
                if (uiState.isLoading) {
                    LoadingBox(height = 200.dp)
                } else {
                    GreetingCard(userDisplayName, uiState)
                }

                Spacer(modifier = Modifier.height(OrbitSpacing.lg))

                if (uiState.summaryErrorMessage != null) {
                    InlineBanner(text = uiState.summaryErrorMessage, color = OrbitColors.danger, background = OrbitColors.dangerBg)
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))
                } else if (!uiState.isOnline) {
                    InlineBanner(
                        text = "You're offline. Showing the last synced attendance data.",
                        color = OrbitColors.warningDark,
                        background = OrbitColors.warningBg,
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))
                }

                if (uiState.isSummaryLoading) {
                    LoadingBox(height = 260.dp)
                } else {
                    MonthlyAttendanceCard(
                        summary = uiState.summary,
                        rangeMode = uiState.rangeMode,
                        onRangeModeSelected = onRangeModeSelected,
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                    AttendanceSummaryCard(summary = uiState.summary, rangeMode = uiState.rangeMode)
                }

                Spacer(modifier = Modifier.height(OrbitSpacing.xl))
            }

            Box(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 25.dp)) {
                OrbitBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
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
            .padding(OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${AttendanceTimeFormat.greeting()}${if (userDisplayName.isBlank()) "" else ", $userDisplayName"}",
                    style = OrbitTypography.headline,
                    color = OrbitColors.ink900,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(
                    text = "${AttendanceTimeFormat.dayOfWeekAndDate(today)} · Morning shift",
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AttendanceTimeFormat.clockTime(Instant.now()),
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.ink900,
                )
                Text(
                    text = "LOCAL",
                    style = OrbitTypography.label,
                    color = OrbitColors.slate500,
                )
            }
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.md))

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
                .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.xs),
        ) {
            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
            Spacer(modifier = Modifier.width(OrbitSpacing.xs))
            Text(text = statusText, style = OrbitTypography.bodySmall, color = statusColor)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            HomeStatCell(
                modifier = Modifier.weight(1f),
                label = "CHECK IN",
                value = uiState.checkInAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "—",
            )
            HomeStatCell(
                modifier = Modifier.weight(1f),
                label = "CHECK OUT",
                value = uiState.checkOutAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "—",
            )
            HomeStatCell(
                modifier = Modifier.weight(1f),
                label = "TOTAL",
                value = AttendanceTimeFormat.elapsedLabel(uiState.elapsed),
                emphasized = true,
            )
        }
    }
}

@Composable
private fun HomeStatCell(label: String, value: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Column(
        modifier = modifier
            .background(
                if (emphasized) OrbitColors.ink900 else OrbitColors.mist,
                OrbitShapes.medium,
            )
            .padding(OrbitSpacing.md),
    ) {
        Text(
            text = label,
            style = OrbitTypography.label,
            color = if (emphasized) OrbitColors.slate300 else OrbitColors.slate500,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
        Text(
            text = value,
            style = OrbitTypography.titleMedium,
            color = if (emphasized) OrbitColors.cream50 else OrbitColors.ink900,
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
            .padding(OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (rangeMode == AttendanceRangeMode.MONTH) "MONTHLY ATTENDANCE" else "WEEKLY ATTENDANCE",
                    style = OrbitTypography.label,
                    color = OrbitColors.slate300,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                Text(text = summary.rangeLabel, style = OrbitTypography.titleLarge, color = OrbitColors.cream50)
            }
            RangeModeToggle(selected = rangeMode, onSelected = onRangeModeSelected)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AttendanceRing(presentDays = summary.presentDays, ratePercent = summary.attendanceRatePercent)

            Spacer(modifier = Modifier.width(OrbitSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${summary.attendanceRatePercent}%", style = OrbitTypography.displayMedium, color = OrbitColors.cream50)
                Text(text = "attendance rate", style = OrbitTypography.bodyMedium, color = OrbitColors.slate300)
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
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
    diameter: Dp = 128.dp,
) {
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidth = size.minDimension * 0.11f
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
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = presentDays.toString(), style = OrbitTypography.titleLarge, color = OrbitColors.cream50)
            Text(
                text = "DAYS PRESENT",
                style = OrbitTypography.label,
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
            .background(Color.White.copy(alpha = 0.12f), OrbitShapes.pill)
            .padding(2.dp),
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
            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.xs),
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

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            SummaryCell(modifier = Modifier.weight(1f), dotColor = OrbitColors.success, background = OrbitColors.successBg, value = summary.presentDays.toString(), label = "Present")
            SummaryCell(modifier = Modifier.weight(1f), dotColor = OrbitColors.danger, background = OrbitColors.dangerBg, value = summary.absentDays.toString(), label = "Absent")
            SummaryCell(modifier = Modifier.weight(1f), dotColor = OrbitColors.warning, background = OrbitColors.warningBg, value = summary.lateDays.toString(), label = "Late")
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            SummaryCell(
                modifier = Modifier.weight(1f),
                dotColor = OrbitColors.accent,
                background = OrbitColors.accentBg,
                value = AttendanceTimeFormat.wholeHoursLabel(summary.worked),
                label = "Worked",
            )
            SummaryCell(modifier = Modifier.weight(1f), dotColor = OrbitColors.info, background = OrbitColors.infoBg, value = summary.leaveDays.toString(), label = "Leave")
            SummaryCell(
                modifier = Modifier.weight(1f),
                dotColor = OrbitColors.ink900,
                background = OrbitColors.mist,
                value = AttendanceTimeFormat.wholeHoursLabel(summary.overtime),
                label = "Overtime",
            )
        }
    }
}

@Composable
private fun SummaryCell(modifier: Modifier, dotColor: Color, background: Color, value: String, label: String) {
    Column(
        modifier = modifier
            .background(background, OrbitShapes.medium)
            .padding(OrbitSpacing.md),
    ) {
        Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Text(text = value, style = OrbitTypography.titleLarge, color = OrbitColors.ink900)
        Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
        Text(text = label, style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
    }
}
