package com.hasim.orbittime.ui.screens.comingsoon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.components.OrbitBottomNav
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.components.OrbitTab
import com.hasim.orbittime.ui.components.OrbitTopAppBar
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/**
 * Placeholder body for tabs whose real functionality is a later phase
 * (Timesheet, Reports). Keeps the same chrome (top bar, bottom nav) as the
 * built-out screens so switching tabs never feels like leaving the app.
 */
@Composable
fun ComingSoonScreen(
    title: String,
    userInitials: String,
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    modifier: Modifier = Modifier,
    signOutButton: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            OrbitTopAppBar(userInitials = userInitials)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = OrbitSpacing.screenHorizontal),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = OrbitTypography.headline, color = OrbitColors.ink900, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                Text(
                    "This part of Orbit Time is coming soon.",
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
                    textAlign = TextAlign.Center,
                )
                if (signOutButton != null) {
                    Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
                    signOutButton()
                }
            }

            Box(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 25.dp)) {
                OrbitBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }
        }
    }
}
