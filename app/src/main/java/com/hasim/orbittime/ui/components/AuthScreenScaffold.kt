package com.hasim.orbittime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.screens.welcome.OrbitAtmosphereBackground
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/**
 * Shared chrome for the auth screens (Sign In, Create Account): the same
 * living atmosphere as Welcome, a fixed back button, a serif headline and
 * subtitle, a scrollable form region, and a footer link pinned near the
 * bottom — matching the approved Sign In reference's composition.
 */
@Composable
fun AuthScreenScaffold(
    headline: String,
    subtitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        OrbitAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = OrbitSpacing.lg, top = OrbitSpacing.md)
                    .size(40.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .background(OrbitColors.cream50, CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "‹", style = OrbitTypography.titleLarge, color = OrbitColors.ink900)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrbitSpacing.screenHorizontal),
            ) {
                Spacer(modifier = Modifier.height(OrbitSpacing.lg))
                Text(text = headline, style = OrbitTypography.displayLarge, color = OrbitColors.ink900)
                Spacer(modifier = Modifier.height(OrbitSpacing.xs))
                Text(text = subtitle, style = OrbitTypography.bodyMedium, color = OrbitColors.slate600)
                Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
                content()
                Spacer(modifier = Modifier.height(OrbitSpacing.xl))
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OrbitSpacing.screenHorizontal)) {
                footer()
                Spacer(modifier = Modifier.height(OrbitSpacing.xxl))
            }
        }
    }
}
