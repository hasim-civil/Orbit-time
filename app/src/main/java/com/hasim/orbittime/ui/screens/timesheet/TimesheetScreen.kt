package com.hasim.orbittime.ui.screens.timesheet

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitBottomNav
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceStats
import com.hasim.orbittime.util.AttendanceStatus
import com.hasim.orbittime.util.AttendanceTimeFormat
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Reference progress-bar denominator for a day's history row — no shift-schedule model exists yet. */
private val STANDARD_SHIFT = Duration.ofMinutes((8.5 * 60).toLong())
private val WEEKDAY_HEADERS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun TimesheetScreen(
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    viewModel: TimesheetViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    TimesheetContent(
        userInitials = userInitials,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
    )
}

@Composable
fun TimesheetContent(
    userInitials: String,
    uiState: TimesheetUiState,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
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
                if (uiState.errorMessage != null) {
                    InlineBanner(text = uiState.errorMessage, color = OrbitColors.danger, background = OrbitColors.dangerBg)
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))
                } else if (!uiState.isOnline) {
                    InlineBanner(
                        text = "You're offline. Showing the last synced attendance data.",
                        color = OrbitColors.warningDark,
                        background = OrbitColors.warningBg,
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrbitColors.violet600)
                    }
                } else {
                    MonthCalendarCard(
                        monthLabel = uiState.monthLabel,
                        days = uiState.days,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                    DailyHistoryCard(history = uiState.history)
                }

                Spacer(modifier = Modifier.height(OrbitSpacing.xl))
            }

            Box(modifier = Modifier.padding(horizontal = OrbitSpacing.lg, vertical = OrbitSpacing.sm)) {
                OrbitBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }
        }
    }
}

@Composable
private fun MonthCalendarCard(
    monthLabel: String,
    days: List<TimesheetDay>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val today = AttendanceTimeFormat.today()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.card)
            .padding(OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = monthLabel, style = OrbitTypography.headline, color = OrbitColors.ink900, modifier = Modifier.weight(1f))
            MonthNavButton(symbol = "‹", onClick = onPreviousMonth)
            Spacer(modifier = Modifier.width(OrbitSpacing.sm))
            MonthNavButton(symbol = "›", onClick = onNextMonth)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_HEADERS.forEach { label ->
                Text(
                    text = label,
                    style = OrbitTypography.label,
                    color = OrbitColors.slate400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.sm))

        val firstDate = days.firstOrNull()?.date
        val leadingBlanks = firstDate?.let { (it.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7 } ?: 0
        val cells: List<TimesheetDay?> = List(leadingBlanks) { null } + days

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(day = day, isToday = day?.date == today, modifier = Modifier.weight(1f))
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            LegendDot(color = OrbitColors.success, text = "Present")
            LegendDot(color = OrbitColors.warning, text = "Late")
            LegendDot(color = OrbitColors.danger, text = "Absent")
            LegendDot(color = OrbitColors.accent, text = "Leave")
        }
    }
}

@Composable
private fun MonthNavButton(symbol: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(OrbitColors.mist, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = OrbitTypography.titleMedium, color = OrbitColors.ink900)
    }
}

@Composable
private fun DayCell(day: TimesheetDay?, isToday: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = OrbitSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (day == null) {
            Spacer(modifier = Modifier.size(28.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(if (isToday) OrbitColors.ink900 else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = OrbitTypography.bodyMedium,
                    color = if (isToday) OrbitColors.cream50 else OrbitColors.ink900,
                )
            }
        }
        Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
        Box(modifier = Modifier.size(6.dp)) {
            val dotColor = day?.status?.let { statusDotColor(it) }
            if (dotColor != null) {
                Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
            }
        }
    }
}

private fun statusDotColor(status: AttendanceStatus): Color = when (status) {
    AttendanceStatus.PRESENT -> OrbitColors.success
    AttendanceStatus.LATE -> OrbitColors.warning
    AttendanceStatus.ABSENT -> OrbitColors.danger
    AttendanceStatus.LEAVE -> OrbitColors.accent
    AttendanceStatus.HOLIDAY -> OrbitColors.slate400
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(OrbitSpacing.xs))
        Text(text = text, style = OrbitTypography.bodySmall, color = OrbitColors.slate600)
    }
}

@Composable
private fun DailyHistoryCard(history: List<TimesheetDay>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.card)
            .padding(OrbitSpacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Daily history", style = OrbitTypography.headline, color = OrbitColors.ink900, modifier = Modifier.weight(1f))
            Text(text = "in · out · total", style = OrbitTypography.label, color = OrbitColors.slate500)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        if (history.isEmpty()) {
            Text(
                text = "No attendance recorded yet this month.",
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = OrbitSpacing.lg),
            )
        } else {
            history.forEachIndexed { index, day ->
                DailyHistoryRow(day = day)
                if (index != history.lastIndex) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))
                }
            }
        }
    }
}

@Composable
private fun DailyHistoryRow(day: TimesheetDay) {
    val today = AttendanceTimeFormat.today()
    val isOngoingToday = day.date == today && day.checkOutAt == null

    // A past day with no checkout (a forgotten punch-out) has no reliable end time —
    // never extend it to "now", or it would show an ever-growing, nonsensical total.
    val duration: Duration? = day.checkInAt?.let { checkIn ->
        val end = day.checkOutAt ?: if (isOngoingToday) Instant.now() else null
        end?.let { Duration.between(checkIn, it).let { d -> if (d.isNegative) Duration.ZERO else d } }
    }

    val (statusLabel, statusColor) = when {
        day.checkInAt != null && day.checkOutAt == null && !isOngoingToday -> "Incomplete" to OrbitColors.slate500
        day.status == AttendanceStatus.LATE -> "Late" to OrbitColors.warningDark
        duration != null && duration > AttendanceStats.OVERTIME_AFTER -> "Overtime" to OrbitColors.warningDark
        day.status == AttendanceStatus.ABSENT -> "Absent" to OrbitColors.danger
        day.status == AttendanceStatus.LEAVE -> "Leave" to OrbitColors.accent
        day.status == AttendanceStatus.HOLIDAY -> "Holiday" to OrbitColors.slate500
        else -> "On time" to OrbitColors.successDark
    }

    val timeRangeText = if (day.checkInAt != null) {
        val inText = AttendanceTimeFormat.clockTime(day.checkInAt)
        val outText = when {
            day.checkOutAt != null -> AttendanceTimeFormat.clockTime(day.checkOutAt)
            isOngoingToday -> "now"
            else -> "—"
        }
        "$inText → $outText"
    } else {
        "—"
    }

    val progress = duration?.let { (it.toMinutes().toFloat() / STANDARD_SHIFT.toMinutes().toFloat()).coerceIn(0f, 1f) } ?: 0f

    Row(verticalAlignment = Alignment.CenterVertically) {
        DateBadge(day = day, color = statusColor)

        Spacer(modifier = Modifier.width(OrbitSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = timeRangeText, style = OrbitTypography.bodyMedium, color = OrbitColors.ink900)
            Spacer(modifier = Modifier.height(OrbitSpacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(OrbitColors.mist, RoundedCornerShape(2.dp)),
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(statusColor, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(OrbitSpacing.md))

        Column(horizontalAlignment = Alignment.End) {
            Text(text = duration?.let { AttendanceTimeFormat.elapsedLabel(it) } ?: "—", style = OrbitTypography.titleMedium, color = OrbitColors.ink900)
            Text(text = statusLabel, style = OrbitTypography.bodySmall, color = statusColor)
        }
    }
}

@Composable
private fun DateBadge(day: TimesheetDay, color: Color) {
    Column(
        modifier = Modifier
            .size(44.dp)
            .background(color.copy(alpha = 0.12f), OrbitShapes.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = day.date.dayOfMonth.toString(), style = OrbitTypography.titleMedium, color = OrbitColors.ink900)
        Text(text = dayOfWeekAbbreviation(day.date), style = OrbitTypography.label, color = OrbitColors.slate500)
    }
}

private fun dayOfWeekAbbreviation(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> "MON"
    DayOfWeek.TUESDAY -> "TUE"
    DayOfWeek.WEDNESDAY -> "WED"
    DayOfWeek.THURSDAY -> "THU"
    DayOfWeek.FRIDAY -> "FRI"
    DayOfWeek.SATURDAY -> "SAT"
    DayOfWeek.SUNDAY -> "SUN"
}
