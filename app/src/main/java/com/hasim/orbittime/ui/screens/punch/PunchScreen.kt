package com.hasim.orbittime.ui.screens.punch

import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.hasim.orbittime.ui.theme.InstrumentSerif
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceTimeFormat
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Reference progress denominator for the elapsed ring — no shift-schedule model exists yet. */
private val STANDARD_SHIFT = Duration.ofMinutes((8.5 * 60).toLong())

/** The reference renders the Checked In/Out mini-card values in the editorial serif, not
 * Manrope — matches the "elegant serif for headline moments" contrast used throughout. */
private val MiniStatValueStyle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 27.sp, lineHeight = 27.sp)

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
        onSuccessMessageConsumed = viewModel::consumeSuccessMessage,
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
    onSuccessMessageConsumed: () -> Unit = {},
) {
    var showEditTimeDialog by remember { mutableStateOf(false) }
    var pastAttendanceDate by remember { mutableStateOf<LocalDate?>(AttendanceTimeFormat.today()) }
    var showAddPastModal by remember { mutableStateOf(false) }

    val modalOpen = showEditTimeDialog || showAddPastModal
    val blurBehind = modalOpen && Build.VERSION.SDK_INT >= 31

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurBehind) Modifier.blur(6.dp) else Modifier),
        ) {
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
                                onClick = {
                                    pastAttendanceDate = AttendanceTimeFormat.today().minusDays(1)
                                    showAddPastModal = true
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(OrbitFloatingNavContentClearance))
                    }
                }
            }
        }

        if (showEditTimeDialog && uiState.checkInAt != null) {
            val zone = ZoneId.systemDefault()
            ModalScrim(onDismiss = { showEditTimeDialog = false }) {
                EditTimeModal(
                    initialCheckIn = uiState.checkInAt.atZone(zone).toLocalTime(),
                    initialCheckOut = (uiState.checkOutAt ?: Instant.now()).atZone(zone).toLocalTime(),
                    onConfirm = { checkIn, checkOut, location ->
                        onEditTime(checkIn, checkOut, location)
                        showEditTimeDialog = false
                    },
                    onDismiss = { showEditTimeDialog = false },
                )
            }
        }

        val selectedPastDate = pastAttendanceDate
        if (showAddPastModal && selectedPastDate != null) {
            ModalScrim(onDismiss = { showAddPastModal = false }) {
                AddPastAttendanceModal(
                    initialDate = selectedPastDate,
                    onConfirm = { date, checkIn, checkOut, location ->
                        onAddPastAttendance(date, checkIn, checkOut, location)
                        showAddPastModal = false
                    },
                    onDismiss = { showAddPastModal = false },
                )
            }
        }

        PunchSuccessOverlay(kind = uiState.successMessage)
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(1100)
            onSuccessMessageConsumed()
        }
    }
}

/** Full-screen dark scrim behind a modal card — the blur applied to the real screen content
 * behind it (see [PunchContent]'s conditional Modifier.blur) rather than to this layer, since
 * blurring a solid-color box does nothing. Tapping the scrim dismisses, tapping the card itself
 * does not (consumed by the card's own background clickable-free surface). */
@Composable
private fun ModalScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0918).copy(alpha = 0.42f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
            content()
        }
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
        Text(text = value, style = MiniStatValueStyle, color = OrbitColors.cream50)
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

