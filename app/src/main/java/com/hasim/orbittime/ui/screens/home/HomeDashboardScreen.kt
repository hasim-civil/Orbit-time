package com.hasim.orbittime.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.hasim.orbittime.util.AttendanceTimeFormat

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
    )
}

@Composable
fun HomeDashboardContent(
    userDisplayName: String,
    userInitials: String,
    uiState: PunchUiState,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
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
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrbitColors.violet600)
                    }
                } else {
                    GreetingCard(userDisplayName, uiState)
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
                    text = AttendanceTimeFormat.dayOfWeekAndDate(today),
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
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
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.xs),
        ) {
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
