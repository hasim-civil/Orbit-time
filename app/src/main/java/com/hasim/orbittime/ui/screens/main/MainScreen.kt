package com.hasim.orbittime.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.screens.comingsoon.ComingSoonScreen
import com.hasim.orbittime.ui.screens.home.HomeDashboardScreen
import com.hasim.orbittime.ui.screens.notifications.NotificationsScreen
import com.hasim.orbittime.ui.screens.profile.ProfileScreen
import com.hasim.orbittime.ui.screens.punch.AttendanceViewModel
import com.hasim.orbittime.ui.screens.punch.PunchScreen
import com.hasim.orbittime.ui.screens.timesheet.TimesheetScreen
import com.hasim.orbittime.util.UserDisplay

/**
 * Owns which bottom-nav tab is showing. Home and Punch share one
 * [AttendanceViewModel] since they render the same underlying attendance
 * record; Reports is still out of scope for this phase.
 *
 * Notifications is reached by tapping the bell from any tab (not a bottom-nav
 * destination itself), so it's tracked as its own overlay flag rather than an
 * [OrbitTab] value — the bottom nav (and whichever tab was active) stays put
 * underneath it, matching the reference where the alerts page sits alongside
 * the other page states rather than replacing the nav chrome.
 */
@Composable
fun MainScreen(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(OrbitTab.HOME) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    val authRepository = remember { AuthRepository() }
    val currentUser = authRepository.currentUser
    val userDisplayName = remember(currentUser) { UserDisplay.firstName(currentUser) }
    val userInitials = remember(currentUser) { UserDisplay.initials(currentUser) }
    val attendanceViewModel: AttendanceViewModel = viewModel()
    val chromeViewModel: MainChromeViewModel = viewModel()
    val chromeState by chromeViewModel.uiState.collectAsState()

    BackHandler(enabled = showNotifications) { showNotifications = false }

    if (showNotifications) {
        NotificationsScreen(
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = {
                selectedTab = it
                showNotifications = false
            },
            photoBase64 = chromeState.photoBase64,
        )
        return
    }

    when (selectedTab) {
        OrbitTab.HOME -> HomeDashboardScreen(
            userDisplayName = userDisplayName,
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            photoBase64 = chromeState.photoBase64,
            hasNotification = chromeState.hasUnreadNotifications,
            onBellClick = { showNotifications = true },
            viewModel = attendanceViewModel,
        )
        OrbitTab.PUNCH -> PunchScreen(
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            photoBase64 = chromeState.photoBase64,
            hasNotification = chromeState.hasUnreadNotifications,
            onBellClick = { showNotifications = true },
            viewModel = attendanceViewModel,
        )
        OrbitTab.TIMESHEET -> TimesheetScreen(
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            photoBase64 = chromeState.photoBase64,
            hasNotification = chromeState.hasUnreadNotifications,
            onBellClick = { showNotifications = true },
        )
        OrbitTab.REPORTS -> ComingSoonScreen(
            title = "Reports",
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = modifier,
            photoBase64 = chromeState.photoBase64,
            hasNotification = chromeState.hasUnreadNotifications,
            onBellClick = { showNotifications = true },
        )
        OrbitTab.PROFILE -> ProfileScreen(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onSignOutClick = {
                authRepository.signOut()
                onLoggedOut()
            },
            // Deletion itself (Firestore + Auth) already happened by the time this fires —
            // this just clears the session the same way signing out does.
            onAccountDeleted = onLoggedOut,
            photoBase64 = chromeState.photoBase64,
            hasNotification = chromeState.hasUnreadNotifications,
            onBellClick = { showNotifications = true },
        )
    }
}
