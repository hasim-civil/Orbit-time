package com.hasim.orbittime.ui.screens.timesheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitFloatingNavContentClearance
import com.hasim.orbittime.ui.components.OrbitFloatingNavHost
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.data.attendance.AttendanceLocation
import com.hasim.orbittime.ui.components.cardRiseEntrance
import com.hasim.orbittime.ui.screens.punch.EditTimeModal
import com.hasim.orbittime.ui.screens.punch.ModalScrim
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
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val WEEKDAY_HEADERS = listOf("M", "T", "W", "T", "F", "S", "S")

// Measured directly from the reference design file (screen-edge to card-edge margin);
// tighter than the shared OrbitSpacing.screenHorizontal used elsewhere, matched here only.
private val TimesheetHorizontalMargin = 14.dp
private val CalendarRowHeight = 47.dp

// Text styles below are `.copy()` of the shared OrbitTypography tokens, adjusted only where
// this screen's measured reference values differ — no shared theme file is modified.
private val WeekdayHeaderStyle = OrbitTypography.label.copy(fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.4.sp)
private val DayNumberStyle = OrbitTypography.bodyMedium.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp)
private val DayNumberStyleToday = DayNumberStyle.copy(fontWeight = FontWeight.Bold)
private val WeekdayAbbrevStyle = OrbitTypography.label.copy(fontWeight = FontWeight.Normal, fontSize = 8.sp, letterSpacing = 0.5.sp)
private val TotalLabelStyle = OrbitTypography.titleMedium.copy(fontSize = 13.sp)
private val LegendTextStyle = OrbitTypography.label.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

@Composable
fun TimesheetScreen(
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    photoBase64: String = "",
    hasNotification: Boolean = false,
    onBellClick: () -> Unit = {},
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
        onEditDay = viewModel::editDay,
        onDeleteDay = viewModel::deleteDay,
        photoBase64 = photoBase64,
        hasNotification = hasNotification,
        onBellClick = onBellClick,
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
    onEditDay: (LocalDate, LocalTime, LocalTime?, AttendanceLocation?) -> Unit = { _, _, _, _ -> },
    onDeleteDay: (LocalDate) -> Unit = {},
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
                        .padding(horizontal = TimesheetHorizontalMargin)
                        .cardRiseEntrance(),
                ) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))

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
                        Spacer(modifier = Modifier.height(OrbitSpacing.md))
                        DailyHistoryCard(
                            history = uiState.history,
                            shiftDuration = uiState.shiftDuration,
                            onEditDay = onEditDay,
                            onDeleteDay = onDeleteDay,
                        )
                    }

                    Spacer(modifier = Modifier.height(OrbitFloatingNavContentClearance))
                }
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
            .padding(OrbitSpacing.xl),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = monthLabel, style = OrbitTypography.headline, color = OrbitColors.ink900, modifier = Modifier.weight(1f))
            MonthNavButton(symbol = "‹", onClick = onPreviousMonth)
            Spacer(modifier = Modifier.width(OrbitSpacing.sm))
            MonthNavButton(symbol = "›", onClick = onNextMonth)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_HEADERS.forEach { label ->
                Text(
                    text = label,
                    style = WeekdayHeaderStyle,
                    color = OrbitColors.slate500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.lg))

        val firstDate = days.firstOrNull()?.date
        val leadingBlanks = firstDate?.let { (it.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7 } ?: 0
        val cells: List<TimesheetDay?> = List(leadingBlanks) { null } + days

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().height(CalendarRowHeight)) {
                week.forEach { day ->
                    DayCell(day = day, isToday = day?.date == today, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.xxl))

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
            .border(1.dp, OrbitColors.slate200, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = OrbitTypography.bodyMedium, color = OrbitColors.slate600)
    }
}

@Composable
private fun DayCell(day: TimesheetDay?, isToday: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (day != null) {
            val baseDotColor = day.status?.let { statusDotColor(it) }
            val dotColor = if (isToday) baseDotColor?.let { OrbitColors.cream50 } else baseDotColor

            Column(
                modifier = Modifier
                    .size(42.dp)
                    .background(if (isToday) OrbitColors.ink900 else Color.Transparent, OrbitShapes.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = if (isToday) DayNumberStyleToday else DayNumberStyle,
                    color = if (isToday) OrbitColors.cream50 else OrbitColors.ink900,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(modifier = Modifier.size(5.dp)) {
                    if (dotColor != null) {
                        Box(modifier = Modifier.size(5.dp).background(dotColor, CircleShape))
                    }
                }
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
        Text(text = text, style = LegendTextStyle, color = OrbitColors.slate600)
    }
}

@Composable
private fun DailyHistoryCard(
    history: List<TimesheetDay>,
    shiftDuration: Duration,
    onEditDay: (LocalDate, LocalTime, LocalTime?, AttendanceLocation?) -> Unit,
    onDeleteDay: (LocalDate) -> Unit,
) {
    var editingDay by remember { mutableStateOf<TimesheetDay?>(null) }
    var pendingDeleteDay by remember { mutableStateOf<TimesheetDay?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.card)
            .padding(OrbitSpacing.xl),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Daily history", style = OrbitTypography.headline, color = OrbitColors.ink900, modifier = Modifier.weight(1f))
            Text(text = "in · out · total", style = OrbitTypography.label, color = OrbitColors.slate500)
        }

        Spacer(modifier = Modifier.height(OrbitSpacing.md))

        if (history.isEmpty()) {
            Text(
                text = "No attendance recorded yet this month.",
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = OrbitSpacing.lg),
            )
        } else {
            history.forEach { day ->
                SwipeableHistoryRow(
                    day = day,
                    shiftDuration = shiftDuration,
                    onEditClick = { editingDay = day },
                    onDeleteClick = { pendingDeleteDay = day },
                )
            }
        }
    }

    val dayBeingEdited = editingDay
    val editCheckIn = dayBeingEdited?.checkInAt
    if (dayBeingEdited != null && editCheckIn != null) {
        val zone = ZoneId.systemDefault()
        val today = AttendanceTimeFormat.today()
        val isOngoingToday = dayBeingEdited.date == today && dayBeingEdited.checkOutAt == null
        ModalScrim(onDismiss = { editingDay = null }) {
            EditTimeModal(
                title = "Edit ${AttendanceTimeFormat.dayLabel(dayBeingEdited.date)}",
                subtitle = "Update the check-in, check-out and location for this date.",
                initialCheckIn = editCheckIn.atZone(zone).toLocalTime(),
                initialCheckOut = dayBeingEdited.checkOutAt?.atZone(zone)?.toLocalTime(),
                initialLocation = dayBeingEdited.location?.let { runCatching { AttendanceLocation.valueOf(it) }.getOrNull() },
                forceShowCheckOut = !isOngoingToday,
                onConfirm = { checkIn, checkOut, location ->
                    onEditDay(dayBeingEdited.date, checkIn, checkOut, location)
                    editingDay = null
                },
                onDismiss = { editingDay = null },
            )
        }
    }

    val dayBeingDeleted = pendingDeleteDay
    if (dayBeingDeleted != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteDay = null },
            title = { Text(text = "Delete this record?", style = OrbitTypography.titleMedium) },
            text = {
                Text(
                    text = "This will permanently remove the attendance record for ${AttendanceTimeFormat.dayLabel(dayBeingDeleted.date)}.",
                    style = OrbitTypography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDay(dayBeingDeleted.date)
                    pendingDeleteDay = null
                }) { Text("Delete", color = OrbitColors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteDay = null }) { Text("Cancel") }
            },
        )
    }
}

private val SwipeActionWidth = 76.dp
private val SwipeRevealWidth = SwipeActionWidth * 2

/** Wraps the existing, unchanged [DailyHistoryRow] with a horizontal swipe-to-reveal: dragging
 * it left slides the row aside to expose Edit/Delete buttons underneath, snapping open or shut
 * based on how far it was dragged. The row's own visual design is untouched — only an opaque
 * background is added so the actions stay hidden until it's actually swiped open. */
@Composable
private fun SwipeableHistoryRow(
    day: TimesheetDay,
    shiftDuration: Duration,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { SwipeRevealWidth.toPx() }
    val offsetX = remember(day.date) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun close() {
        scope.launch { offsetX.animateTo(0f, tween(200)) }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            SwipeActionButton(
                label = "Edit",
                background = OrbitColors.violet600,
                modifier = Modifier.width(SwipeActionWidth).fillMaxHeight(),
                onClick = { close(); onEditClick() },
            )
            SwipeActionButton(
                label = "Delete",
                background = OrbitColors.danger,
                modifier = Modifier.width(SwipeActionWidth).fillMaxHeight(),
                onClick = { close(); onDeleteClick() },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(OrbitColors.cream50)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        val target = if (offsetX.value < -revealPx / 2f || velocity < -800f) -revealPx else 0f
                        offsetX.animateTo(target, tween(200))
                    },
                ),
        ) {
            DailyHistoryRow(day = day, shiftDuration = shiftDuration)
        }
    }
}

@Composable
private fun SwipeActionButton(label: String, background: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = OrbitTypography.bodySmall.copy(fontWeight = FontWeight.Bold), color = OrbitColors.cream50)
    }
}

@Composable
private fun DailyHistoryRow(day: TimesheetDay, shiftDuration: Duration) {
    val today = AttendanceTimeFormat.today()
    val isOngoingToday = day.date == today && day.checkOutAt == null

    // A past day with no checkout (a forgotten punch-out) has no reliable end time —
    // never extend it to "now", or it would show an ever-growing, nonsensical total.
    val duration: Duration? = day.checkInAt?.let { checkIn ->
        val end = day.checkOutAt ?: if (isOngoingToday) Instant.now() else null
        end?.let { Duration.between(checkIn, it).let { d -> if (d.isNegative) Duration.ZERO else d } }
    }

    // A day worked from home or another site is still "on time" — but the location is more
    // useful to show than that generic label, since punctuality already has its own flags below.
    val locationLabel = day.location?.let { raw ->
        when (runCatching { AttendanceLocation.valueOf(raw) }.getOrNull()) {
            AttendanceLocation.WORK_FROM_HOME -> "WFH"
            AttendanceLocation.OUTSTATION -> "Outstation"
            else -> null
        }
    }

    val (statusLabel, statusColor) = when {
        day.checkInAt != null && day.checkOutAt == null && !isOngoingToday -> "Incomplete" to OrbitColors.slate500
        day.status == AttendanceStatus.LATE -> "Late" to OrbitColors.warningDark
        duration != null && duration > AttendanceStats.OVERTIME_AFTER -> "Overtime" to OrbitColors.warningDark
        day.status == AttendanceStatus.ABSENT -> "Absent" to OrbitColors.danger
        day.status == AttendanceStatus.LEAVE -> "Leave" to OrbitColors.accent
        day.status == AttendanceStatus.HOLIDAY -> "Holiday" to OrbitColors.slate500
        locationLabel != null -> locationLabel to OrbitColors.info
        else -> "On time" to OrbitColors.successDark
    }

    val timeRangeText = if (day.checkInAt != null) {
        val inText = AttendanceTimeFormat.clockTime(day.checkInAt)
        val outText = when {
            day.checkOutAt != null -> AttendanceTimeFormat.clockTime(day.checkOutAt)
            isOngoingToday -> "now"
            else -> "—"
        }
        val suffix = if (statusLabel == "Overtime") " · overtime" else ""
        "$inText → $outText$suffix"
    } else {
        "—"
    }

    val progress = duration?.let { (it.toMinutes().toFloat() / shiftDuration.toMinutes().toFloat()).coerceIn(0f, 1f) } ?: 0f
    // The reference's "barGrow": each row's progress bar grows in from empty when it first appears.
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, tween(700, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
    }

    Row(modifier = Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
        DateBadge(day = day, color = statusColor)

        Spacer(modifier = Modifier.width(OrbitSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = timeRangeText, style = OrbitTypography.bodyMedium, color = OrbitColors.ink900)
            Spacer(modifier = Modifier.height(OrbitSpacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(OrbitColors.fog, RoundedCornerShape(2.dp)),
            ) {
                if (animatedProgress.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.value)
                            .height(4.dp)
                            .background(statusColor, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(OrbitSpacing.md))

        Column(horizontalAlignment = Alignment.End) {
            Text(text = duration?.let { AttendanceTimeFormat.elapsedLabel(it) } ?: "—", style = TotalLabelStyle, color = OrbitColors.ink900)
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
        Text(text = dayOfWeekAbbreviation(day.date), style = WeekdayAbbrevStyle, color = color)
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
