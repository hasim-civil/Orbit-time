package com.hasim.orbittime.ui.screens.home

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.OrbitOutlineButton
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography
import androidx.compose.material3.Text

/**
 * Minimal authenticated landing spot after sign in / registration. The real
 * Home dashboard (attendance, hours, reports) is a later phase — this only
 * proves the auth flow lands somewhere real and offers a working Logout.
 */
@Composable
fun HomeScreen(
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    HomeContent(
        displayName = viewModel.displayName,
        email = viewModel.email,
        onLogoutClick = {
            viewModel.signOut()
            onLoggedOut()
        },
    )
}

@Composable
fun HomeContent(
    displayName: String?,
    email: String?,
    onLogoutClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = OrbitSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Good to see you${if (displayName.isNullOrBlank()) "" else ", $displayName"}",
                style = OrbitTypography.displayMedium,
                color = OrbitColors.ink900,
                textAlign = TextAlign.Center,
            )
            if (!email.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                Text(
                    text = email,
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(OrbitSpacing.sm))
            Text(
                text = "Your dashboard is being designed in a later phase.",
                style = OrbitTypography.bodyMedium,
                color = OrbitColors.slate600,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            OrbitOutlineButton(
                text = "Log out",
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
        }
    }
}
