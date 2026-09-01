package com.hasim.orbittime.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitFloatingNavContentClearance
import com.hasim.orbittime.ui.components.OrbitFloatingNavHost
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.components.cardRiseEntrance
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.InstrumentSerif
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceRangeMode
import com.hasim.orbittime.util.AttendanceTimeFormat
import com.hasim.orbittime.util.ReportsPerformance
import com.hasim.orbittime.util.ReportsPunctuality
import com.hasim.orbittime.util.ReportsTrendPoint
import com.hasim.orbittime.util.ReportsWorkHours
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Matches the Home dashboard's own tighter side margin (14dp) rather than the shared
// OrbitSpacing.screenHorizontal used by auth screens.
private val ReportsHorizontalMargin = 14.dp
private val ReportsSectionGap = OrbitSpacing.md

// A local "big number" style, same convention every other screen already follows (Home's
// RingBigNumberStyle, Timesheet's TotalLabelStyle) rather than adding a new shared token.
private val ReportsBigNumberStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 36.sp)

private val ReportsClockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private fun formatClock(time: LocalTime): String = ReportsClockFormatter.format(time).lowercase(Locale.getDefault())

@Composable
fun ReportsScreen(
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    photoBase64: String = "",
    hasNotification: Boolean = false,
    onBellClick: () -> Unit = {},
    viewModel: ReportsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ReportsContent(
        userInitials = userInitials,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        onTrendModeChanged = viewModel::setTrendMode,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onRetry = viewModel::retry,
        photoBase64 = photoBase64,
        hasNotification = hasNotification,
        onBellClick = onBellClick,
    )
}

@Composable
fun ReportsContent(
    userInitials: String,
    uiState: ReportsUiState,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    onTrendModeChanged: (AttendanceRangeMode) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
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
                        .padding(horizontal = ReportsHorizontalMargin)
                        .cardRiseEntrance(),
                ) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))

                    ReportsMonthSelector(
                        monthLabel = uiState.monthLabel,
                        canShowPrevious = uiState.canShowPreviousMonth,
                        canShowNext = uiState.canShowNextMonth,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                    )
                    Spacer(modifier = Modifier.height(ReportsSectionGap))

                    if (!uiState.isOnline) {
                        InlineBanner(
                            text = "You're offline. Showing the last synced attendance data.",
                            color = OrbitColors.warningDark,
                            background = OrbitColors.warningBg,
                        )
                        Spacer(modifier = Modifier.height(ReportsSectionGap))
                    }

                    when {
                        uiState.isLoading -> {
                            Box(modifier = Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = OrbitColors.violet600)
                            }
                        }
                        uiState.errorMessage != null -> {
                            InlineBanner(text = uiState.errorMessage, color = OrbitColors.danger, background = OrbitColors.dangerBg)
                            Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                            RetryLink(onClick = onRetry)
                        }
                        !uiState.hasEnoughData -> ReportsEmptyStateCard()
                        else -> {
                            AttendancePerformanceCard(uiState.performance)
                            Spacer(modifier = Modifier.height(ReportsSectionGap))
                            WorkHoursCard(uiState.workHours)
                            Spacer(modifier = Modifier.height(ReportsSectionGap))
                            PunctualityCard(uiState.punctuality)
                            Spacer(modifier = Modifier.height(ReportsSectionGap))
                            TrendCard(
                                trendMode = uiState.trendMode,
                                trendPoints = uiState.trendPoints,
                                onTrendModeChanged = onTrendModeChanged,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(OrbitFloatingNavContentClearance))
                }
            }
        }
    }
}

/** Month picker for the figures below — Previous/Next, matching Timesheet's own calendar-header
 * nav buttons, so switching months reads the same way it does everywhere else in the app. Both
 * arrows disable (rather than hide) past the already-fetched range, per [ReportsUiState]. */
@Composable
private fun ReportsMonthSelector(
    monthLabel: String,
    canShowPrevious: Boolean,
    canShowNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = monthLabel, style = OrbitTypography.headline, color = OrbitColors.ink900, modifier = Modifier.weight(1f))
        ReportsMonthNavButton(symbol = "‹", enabled = canShowPrevious, onClick = onPreviousMonth)
        Spacer(modifier = Modifier.width(OrbitSpacing.sm))
        ReportsMonthNavButton(symbol = "›", enabled = canShowNext, onClick = onNextMonth)
    }
}

@Composable
private fun ReportsMonthNavButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, OrbitColors.slate200, CircleShape)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = OrbitTypography.bodyMedium, color = if (enabled) OrbitColors.slate600 else OrbitColors.slate300)
    }
}

@Composable
private fun RetryLink(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = "Retry",
        style = OrbitTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = OrbitColors.violet600,
        modifier = Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    )
}

/** Shared dark/purple card shell — the same gradient Home's Monthly Attendance card uses, so
 * Reports reads as part of the existing visual language rather than a new design. */
@Composable
private fun ReportsDarkCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(colors = listOf(OrbitColors.void300, OrbitColors.void600, OrbitColors.void900)),
                shape = OrbitShapes.card,
            )
            .padding(horizontal = OrbitSpacing.xl, vertical = OrbitSpacing.lg),
        content = content,
    )
}

/** A small translucent glass tile for one labeled figure inside a dark card: a flat white tint,
 * a soft border, no blur/shadow APIs — the same non-directional-layer approach used for the six
 * Attendance Summary glass cards on Home, adapted for a dark backdrop instead of a light one. */
@Composable
private fun ReportsStatTile(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(OrbitShapes.small)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), OrbitShapes.small)
            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
    ) {
        Text(text = value, style = OrbitTypography.titleLarge, color = OrbitColors.cream50)
        Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
        Text(text = label, style = OrbitTypography.label, color = OrbitColors.slate300)
    }
}

@Composable
private fun ReportsSectionLabel(text: String) {
    Text(text = text, style = OrbitTypography.label, color = OrbitColors.slate300)
}

@Composable
private fun AttendancePerformanceCard(performance: ReportsPerformance) {
    ReportsDarkCard {
        ReportsSectionLabel("ATTENDANCE PERFORMANCE")
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "${performance.attendanceRatePercent}%", style = ReportsBigNumberStyle, color = OrbitColors.cream50)
            val delta = performance.vsPreviousPeriodPercent
            if (delta != null) {
                Spacer(modifier = Modifier.width(OrbitSpacing.sm))
                val sign = if (delta >= 0) "+" else ""
                val color = if (delta >= 0) OrbitColors.success else OrbitColors.danger
                Text(
                    text = "$sign$delta% vs last month",
                    style = OrbitTypography.bodySmall,
                    color = color,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            ReportsStatTile(modifier = Modifier.weight(1f), label = "Present", value = performance.presentDays.toString())
            ReportsStatTile(modifier = Modifier.weight(1f), label = "Absent", value = performance.absentDays.toString())
        }
        Spacer(modifier = Modifier.height(OrbitSpacing.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            ReportsStatTile(modifier = Modifier.weight(1f), label = "Late", value = performance.lateDays.toString())
            ReportsStatTile(modifier = Modifier.weight(1f), label = "Leave", value = performance.leaveDays.toString())
        }
    }
}

@Composable
private fun WorkHoursCard(workHours: ReportsWorkHours) {
    ReportsDarkCard {
        ReportsSectionLabel("WORK HOURS")
        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            ReportsStatTile(
                modifier = Modifier.weight(1f),
                label = "Total Worked",
                value = AttendanceTimeFormat.elapsedLabel(workHours.totalWorked),
            )
            ReportsStatTile(
                modifier = Modifier.weight(1f),
                label = "Average / Day",
                value = AttendanceTimeFormat.elapsedLabel(workHours.averagePerDay),
            )
        }
        Spacer(modifier = Modifier.height(OrbitSpacing.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            ReportsStatTile(
                modifier = Modifier.weight(1f),
                label = "Average Check-in",
                value = workHours.averageCheckIn?.let { formatClock(it) } ?: "—",
            )
            ReportsStatTile(
                modifier = Modifier.weight(1f),
                label = "Average Check-out",
                value = workHours.averageCheckOut?.let { formatClock(it) } ?: "—",
            )
        }
    }
}

@Composable
private fun PunctualityCard(punctuality: ReportsPunctuality) {
    ReportsDarkCard {
        ReportsSectionLabel("PUNCTUALITY")
        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            ReportsStatTile(modifier = Modifier.weight(1f), label = "On-time", value = "${punctuality.onTimePercent}%")
            ReportsStatTile(modifier = Modifier.weight(1f), label = "Late Days", value = punctuality.lateDays.toString())
            ReportsStatTile(modifier = Modifier.weight(1f), label = "Average Late", value = "${punctuality.averageLateMinutes} min")
        }
    }
}

@Composable
private fun TrendCard(
    trendMode: AttendanceRangeMode,
    trendPoints: List<ReportsTrendPoint>,
    onTrendModeChanged: (AttendanceRangeMode) -> Unit,
) {
    ReportsDarkCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { ReportsSectionLabel("TREND") }
            TrendModeToggle(selected = trendMode, onSelected = onTrendModeChanged)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        if (trendPoints.all { it.attendanceRatePercent == 0 }) {
            Text(
                text = "Not enough data yet for a trend.",
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.slate300,
            )
        } else {
            TrendChart(points = trendPoints)
        }
    }
}

private val TrendBarMaxHeight = 96.dp
private val TrendBarWidth = 16.dp
private val TrendLabelStyle = OrbitTypography.label.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

@Composable
private fun TrendChart(points: List<ReportsTrendPoint>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(TrendBarMaxHeight + OrbitSpacing.lg),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(TrendBarWidth)
                        .height(TrendBarMaxHeight * (point.attendanceRatePercent.coerceIn(0, 100) / 100f))
                        .background(
                            brush = Brush.verticalGradient(colors = listOf(OrbitColors.cyan400, OrbitColors.violet600)),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        ),
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                Text(text = point.label, style = TrendLabelStyle, color = OrbitColors.slate300, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TrendModeToggle(selected: AttendanceRangeMode, onSelected: (AttendanceRangeMode) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), OrbitShapes.pill)
            .border(1.dp, Color.White.copy(alpha = 0.12f), OrbitShapes.pill)
            .padding(3.dp),
    ) {
        TrendToggleSegment(label = "Week", selected = selected == AttendanceRangeMode.WEEK) { onSelected(AttendanceRangeMode.WEEK) }
        TrendToggleSegment(label = "Month", selected = selected == AttendanceRangeMode.MONTH) { onSelected(AttendanceRangeMode.MONTH) }
    }
}

@Composable
private fun TrendToggleSegment(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun ReportsEmptyStateCard() {
    ReportsDarkCard {
        Text(
            text = "No attendance data yet",
            style = OrbitTypography.headline,
            color = OrbitColors.cream50,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Text(
            text = "Check in for a few days and your reports will show up here.",
            style = OrbitTypography.bodyMedium,
            color = OrbitColors.slate300,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
