package com.hasim.orbittime.ui.screens.punch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.data.attendance.AttendanceLocation
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitFloatingNavContentClearance
import com.hasim.orbittime.ui.components.OrbitFloatingNavHost
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.components.cardRiseEntrance
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceTimeFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Reference progress denominator for the elapsed ring — no shift-schedule model exists yet. */
private val STANDARD_SHIFT = Duration.ofMinutes((8.5 * 60).toLong())

@Composable
fun PunchScreen(
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    viewModel: AttendanceViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    PunchContent(
        userInitials = userInitials,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        onCheckInClick = viewModel::checkIn,
        onCheckOutClick = viewModel::checkOut,
        onEditTime = viewModel::editTodayTimes,
        onAddPastAttendance = viewModel::addPastAttendance,
    )
}

@Composable
fun PunchContent(
    userInitials: String,
    uiState: PunchUiState,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onEditTime: (LocalTime, LocalTime?, AttendanceLocation?) -> Unit = { _, _, _ -> },
    onAddPastAttendance: (LocalDate, LocalTime, LocalTime, AttendanceLocation?) -> Unit = { _, _, _, _ -> },
) {
    var showEditTimeDialog by remember { mutableStateOf(false) }
    var showPastDatePicker by remember { mutableStateOf(false) }
    var pastAttendanceDate by remember { mutableStateOf<LocalDate?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            OrbitTopAppBar(
                userInitials = userInitials,
                hasNotification = true,
                onAvatarClick = { onTabSelected(OrbitTab.PROFILE) },
            )

            OrbitFloatingNavHost(selectedTab = selectedTab, onTabSelected = onTabSelected, modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp)
                        .cardRiseEntrance(),
                ) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.md))

                    if (!uiState.isOnline) {
                        InlineBanner(text = "You're offline. Check-in/out needs a connection.", color = OrbitColors.warningDark, background = OrbitColors.warningBg)
                        Spacer(modifier = Modifier.height(OrbitSpacing.md))
                    }
                    if (uiState.errorMessage != null) {
                        InlineBanner(text = uiState.errorMessage, color = OrbitColors.danger, background = OrbitColors.dangerBg)
                        Spacer(modifier = Modifier.height(OrbitSpacing.md))
                    }

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(320.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = OrbitColors.violet600)
                        }
                    } else {
                        PunchHeroCard(uiState, onCheckInClick, onCheckOutClick)
                        Spacer(modifier = Modifier.height(OrbitSpacing.md))
                        CheckInOutMiniCards(uiState)
                        Spacer(modifier = Modifier.height(OrbitSpacing.md))
                        PunchTimeActionRow(
                            icon = "✎",
                            iconBackground = OrbitColors.violet600.copy(alpha = 0.12f),
                            iconColor = OrbitColors.violet600,
                            label = "Edit time",
                            labelColor = OrbitColors.ink900,
                            trailingText = if (uiState.checkInAt != null) {
                                "${AttendanceTimeFormat.clockTime(uiState.checkInAt)} – " +
                                    (uiState.checkOutAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "now")
                            } else {
                                null
                            },
                            enabled = uiState.checkInAt != null,
                            onClick = { showEditTimeDialog = true },
                        )
                        Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                        PunchTimeActionRow(
                            icon = "+",
                            iconBackground = OrbitColors.mist,
                            iconColor = OrbitColors.slate500,
                            label = "Add past attendance",
                            labelColor = OrbitColors.slate500,
                            trailingText = null,
                            onClick = { showPastDatePicker = true },
                        )
                    }

                    Spacer(modifier = Modifier.height(OrbitFloatingNavContentClearance))
                }
            }
        }

        if (showEditTimeDialog && uiState.checkInAt != null) {
            val zone = ZoneId.systemDefault()
            TimeRangeDialog(
                title = "Edit today's time",
                initialCheckIn = uiState.checkInAt.atZone(zone).toLocalTime(),
                initialCheckOut = (uiState.checkOutAt ?: Instant.now()).atZone(zone).toLocalTime(),
                confirmLabel = "Save",
                onConfirm = { checkIn, checkOut, location ->
                    onEditTime(checkIn, checkOut, location)
                    showEditTimeDialog = false
                },
                onDismiss = { showEditTimeDialog = false },
            )
        }

        if (showPastDatePicker) {
            PastDatePickerDialog(
                onDateSelected = { date ->
                    showPastDatePicker = false
                    pastAttendanceDate = date
                },
                onDismiss = { showPastDatePicker = false },
            )
        }

        val selectedPastDate = pastAttendanceDate
        if (selectedPastDate != null) {
            TimeRangeDialog(
                title = "Add attendance for ${AttendanceTimeFormat.dayLabel(selectedPastDate)}",
                initialCheckIn = LocalTime.of(9, 0),
                initialCheckOut = LocalTime.of(17, 30),
                confirmLabel = "Add",
                onConfirm = { checkIn, checkOut, location ->
                    onAddPastAttendance(selectedPastDate, checkIn, checkOut, location)
                    pastAttendanceDate = null
                },
                onDismiss = { pastAttendanceDate = null },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeDialog(
    title: String,
    initialCheckIn: LocalTime,
    initialCheckOut: LocalTime,
    confirmLabel: String,
    onConfirm: (LocalTime, LocalTime, AttendanceLocation?) -> Unit,
    onDismiss: () -> Unit,
) {
    val checkInState = rememberTimePickerState(initialHour = initialCheckIn.hour, initialMinute = initialCheckIn.minute, is24Hour = false)
    val checkOutState = rememberTimePickerState(initialHour = initialCheckOut.hour, initialMinute = initialCheckOut.minute, is24Hour = false)
    var location by remember { mutableStateOf<AttendanceLocation?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = OrbitTypography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                Text(text = "Check in", style = OrbitTypography.label, color = OrbitColors.slate500)
                TimeInput(state = checkInState)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                Text(text = "Check out", style = OrbitTypography.label, color = OrbitColors.slate500)
                TimeInput(state = checkOutState)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                Text(text = "Location", style = OrbitTypography.label, color = OrbitColors.slate500)
                Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
                LocationSelector(selected = location, onSelected = { location = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    LocalTime.of(checkInState.hour, checkInState.minute),
                    LocalTime.of(checkOutState.hour, checkOutState.minute),
                    location,
                )
            }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun LocationSelector(selected: AttendanceLocation?, onSelected: (AttendanceLocation) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        AttendanceLocation.entries.forEach { location ->
            val isSelected = location == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(OrbitShapes.small)
                    .background(if (isSelected) OrbitColors.violet600 else OrbitColors.mist)
                    .clickable { onSelected(location) }
                    .padding(vertical = OrbitSpacing.xs, horizontal = OrbitSpacing.xxs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = location.label,
                    style = OrbitTypography.bodySmall,
                    color = if (isSelected) OrbitColors.cream50 else OrbitColors.slate600,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastDatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = remember {
        AttendanceTimeFormat.today().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis < todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                }
            }) { Text("Next") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun PunchHeroCard(
    uiState: PunchUiState,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
) {
    val today = AttendanceTimeFormat.today()

    val headline = when {
        uiState.isCompleted -> "Nice work today"
        uiState.isCheckedIn && uiState.checkInAt != null -> "In orbit since ${AttendanceTimeFormat.clockTime(uiState.checkInAt)}"
        else -> "Ready when you are"
    }
    val statusLabel = when {
        uiState.isCompleted -> "Completed"
        uiState.isCheckedIn -> "Running"
        else -> "Not started"
    }
    val statusColor = if (uiState.isCheckedIn) OrbitColors.success else OrbitColors.slate300
    val progress = (uiState.elapsed.toMinutes().toFloat() / STANDARD_SHIFT.toMinutes().toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(OrbitColors.void300, OrbitColors.void600, OrbitColors.void900),
                ),
                shape = OrbitShapes.hero,
            )
            .padding(vertical = OrbitSpacing.xxl, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = AttendanceTimeFormat.dayLabel(today),
            style = OrbitTypography.label,
            color = OrbitColors.slate300,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Text(
            text = headline,
            style = OrbitTypography.headline,
            color = OrbitColors.cream50,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.xl))

        ElapsedRing(
            progress = progress,
            elapsedLabel = AttendanceTimeFormat.elapsedLabel(uiState.elapsed),
            statusLabel = statusLabel,
            statusColor = statusColor,
            onClick = when {
                uiState.isSubmitting || uiState.isCompleted -> null
                uiState.isCheckedIn -> onCheckOutClick
                else -> onCheckInClick
            },
        )

        Spacer(modifier = Modifier.height(OrbitSpacing.xl))

        when {
            uiState.isSubmitting -> {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrbitColors.cream50)
                }
            }
            uiState.isCompleted -> {
                Text(
                    text = "Done for today",
                    style = OrbitTypography.titleMedium,
                    color = OrbitColors.slate300,
                )
            }
            uiState.isCheckedIn -> {
                OrbitOutlineButton(text = "Punch out", onClick = onCheckOutClick)
            }
            else -> {
                OrbitGradientButton(text = "Punch in", onClick = onCheckInClick)
            }
        }
    }
}

@Composable
private fun CheckInOutMiniCards(uiState: PunchUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
        MiniStatCard(
            modifier = Modifier.weight(1f),
            label = "CHECKED IN",
            value = uiState.checkInAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "—",
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            label = "CHECKED OUT",
            value = uiState.checkOutAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "—",
        )
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(colors = listOf(OrbitColors.void300, OrbitColors.void900)),
                shape = OrbitShapes.card,
            )
            .padding(OrbitSpacing.lg),
    ) {
        Text(text = label, style = OrbitTypography.label, color = OrbitColors.slate300)
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        Text(text = value, style = OrbitTypography.titleLarge, color = OrbitColors.cream50)
    }
}

/** "Edit time" / "Add past attendance" rows below the mini cards. */
@Composable
private fun PunchTimeActionRow(
    icon: String,
    iconBackground: Color,
    iconColor: Color,
    label: String,
    labelColor: Color,
    trailingText: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(iconBackground, OrbitShapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, style = OrbitTypography.titleMedium, color = iconColor)
        }
        Spacer(modifier = Modifier.width(OrbitSpacing.md))
        Text(
            text = label,
            style = OrbitTypography.titleMedium,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(text = trailingText, style = OrbitTypography.bodyMedium, color = OrbitColors.slate500)
            Spacer(modifier = Modifier.width(OrbitSpacing.xs))
        }
        Text(text = "›", style = OrbitTypography.titleMedium, color = OrbitColors.slate500)
    }
}

