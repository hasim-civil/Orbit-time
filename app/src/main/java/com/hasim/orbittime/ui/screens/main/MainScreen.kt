package com.hasim.orbittime.ui.screens.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.reminder.ShiftReminderScheduler
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.screens.home.HomeDashboardScreen
import com.hasim.orbittime.ui.screens.notifications.NotificationsScreen
import com.hasim.orbittime.ui.screens.profile.ProfileScreen
import com.hasim.orbittime.ui.screens.punch.AttendanceViewModel
import com.hasim.orbittime.ui.screens.punch.PunchScreen
import com.hasim.orbittime.ui.screens.reports.ReportsScreen
import com.hasim.orbittime.ui.screens.timesheet.TimesheetScreen
import com.hasim.orbittime.util.UserDisplay

/**
 * Owns which bottom-nav tab is showing. Home and Punch share one
 * [AttendanceViewModel] since they render the same underlying attendance
 * record; Reports owns its own [com.hasim.orbittime.ui.screens.reports.ReportsViewModel]
 * since it reads a much wider date range purely for analysis.
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

    // The shift reminder is a real system notification (Prompt 10), so on API 33+ it needs the
    // runtime POST_NOTIFICATIONS permission — requested once, here, rather than only at the
    // moment a notification is about to fire (which could be while the app isn't even open).
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
        OrbitTab.REPORTS -> ReportsScreen(
            userInitials = userInitials,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            photoBase64 = chromeState.photoBase64,
            hasNotification = chromeState.hasUnreadNotifications,
            onBellClick = { showNotifications = true },
        )
        OrbitTab.PROFILE -> ProfileScreen(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onSignOutClick = {
                ShiftReminderScheduler.cancel(context)
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
