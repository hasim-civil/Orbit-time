package com.hasim.orbittime.ui.screens.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/**
 * Stand-in destination for a screen that hasn't been designed yet (Sign In,
 * Create account). Keeps navigation real and testable now without pre-empting
 * the dedicated design work for that screen in a later phase.
 */
@Composable
fun OrbitPlaceholderScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = OrbitSpacing.screenHorizontal),
        ) {
            Spacer(modifier = Modifier.height(OrbitSpacing.sm))
            TextButton(onClick = onBackClick) {
                Text(
                    text = "‹ Back",
                    style = OrbitTypography.titleMedium,
                    color = OrbitColors.ink900,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(OrbitSpacing.giant))
                Text(
                    text = title,
                    style = OrbitTypography.displayMedium,
                    color = OrbitColors.ink900,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(OrbitSpacing.sm))
                Text(
                    text = "Designed in a later phase.",
                    style = OrbitTypography.bodyMedium,
                    color = OrbitColors.slate600,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
