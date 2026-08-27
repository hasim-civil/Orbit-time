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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.InlineBanner
import com.hasim.orbittime.ui.components.OrbitBottomNav
import com.hasim.orbittime.ui.components.OrbitGradientButton
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import com.hasim.orbittime.util.AttendanceTimeFormat
import java.time.Duration

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
                    Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                    CheckInOutMiniCards(uiState)
                    Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                    PunchTimeActionRow(
                        icon = "✎",
                        iconBackground = OrbitColors.violet600.copy(alpha = 0.12f),
                        iconColor = OrbitColors.violet600,
                        label = "Edit time",
                        labelColor = OrbitColors.ink900,
                        trailingText = "9:02 am – 5:48 pm",
                    )
                    Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                    PunchTimeActionRow(
                        icon = "+",
                        iconBackground = OrbitColors.mist,
                        iconColor = OrbitColors.slate500,
                        label = "Add past attendance",
                        labelColor = OrbitColors.slate500,
                        trailingText = null,
                    )
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
            .padding(vertical = OrbitSpacing.xxl, horizontal = OrbitSpacing.lg),
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
            caption = "Morning shift",
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            label = "CHECKED OUT",
            value = uiState.checkOutAt?.let { AttendanceTimeFormat.clockTime(it) } ?: "—",
            caption = "of 8.5h shift",
        )
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
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
        Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
        Text(text = caption, style = OrbitTypography.bodySmall, color = OrbitColors.slate300)
    }
}

/**
 * Reference "Edit time" / "Add past attendance" rows below the mini cards.
 * UI-only for this pass — no editable time picker or attendance-entry flow yet.
 */
@Composable
private fun PunchTimeActionRow(
    icon: String,
    iconBackground: Color,
    iconColor: Color,
    label: String,
    labelColor: Color,
    trailingText: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitColors.cream50, OrbitShapes.medium)
            .clickable { }
            .padding(OrbitSpacing.md),
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

