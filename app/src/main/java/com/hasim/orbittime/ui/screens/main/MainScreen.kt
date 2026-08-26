package com.hasim.orbittime.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.screens.comingsoon.ComingSoonScreen
import com.hasim.orbittime.ui.screens.home.HomeDashboardScreen
import com.hasim.orbittime.ui.screens.punch.AttendanceViewModel
import com.hasim.orbittime.ui.screens.punch.PunchScreen
import com.hasim.orbittime.ui.screens.timesheet.TimesheetScreen
import com.hasim.orbittime.util.UserDisplay

/**
 * Owns which bottom-nav tab is showing. Home and Punch share one
 * [AttendanceViewModel] since they render the same underlying attendance
 * record; Reports and Profile are out of scope for this phase.
 */
@Composable
fun MainScreen(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(OrbitTab.HOME) }
    val authRepository = remember { AuthRepository() }
    val currentUser = authRepository.currentUser
    val userDisplayName = remember(currentUser) { UserDisplay.firstName(currentUser) }
    val userInitials = remember(currentUser) { UserDisplay.initials(currentUser) }
    val attendanceViewModel: AttendanceViewModel = viewModel()

    when (selectedTab) {
        OrbitTab.HOME -> HomeDashboardScreen(
            userDisplayName = userDisplayName,
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            viewModel = attendanceViewModel,
        )
        OrbitTab.PUNCH -> PunchScreen(
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            viewModel = attendanceViewModel,
        )
        OrbitTab.TIMESHEET -> TimesheetScreen(
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
        )
        OrbitTab.REPORTS -> ComingSoonScreen(
            title = "Reports",
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = modifier,
        )
        OrbitTab.PROFILE -> ComingSoonScreen(
            title = "Profile",
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = modifier,
            signOutButton = {
                OrbitOutlineButton(
                    text = "Sign out",
                    onClick = {
                        authRepository.signOut()
                        onLoggedOut()
                    },
                )
            },
        )
    }
}
