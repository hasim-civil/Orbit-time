package com.hasim.orbittime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

enum class OrbitTab { HOME, TIMESHEET, PUNCH, REPORTS, PROFILE }

@Composable
fun OrbitBottomNav(
    selectedTab: OrbitTab,
    onTabSelected: (OrbitTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = OrbitShapes.hero)
                .background(OrbitColors.cream50, OrbitShapes.hero)
                .padding(horizontal = OrbitSpacing.sm, vertical = OrbitSpacing.sm),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            NavTabItem(NavGlyph.HOME, "Home", selectedTab == OrbitTab.HOME) { onTabSelected(OrbitTab.HOME) }
            NavTabItem(NavGlyph.TIMESHEET, "Timesheet", selectedTab == OrbitTab.TIMESHEET) { onTabSelected(OrbitTab.TIMESHEET) }
            Spacer(modifier = Modifier.size(64.dp))
            NavTabItem(NavGlyph.REPORTS, "Reports", selectedTab == OrbitTab.REPORTS) { onTabSelected(OrbitTab.REPORTS) }
            NavTabItem(NavGlyph.PROFILE, "Profile", selectedTab == OrbitTab.PROFILE) { onTabSelected(OrbitTab.PROFILE) }
        }

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .size(56.dp)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(OrbitColors.blue500, OrbitColors.purple500, OrbitColors.coral500),
                    ),
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onTabSelected(OrbitTab.PUNCH) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            OrbitMiniLogo(diameter = 26.dp)
        }
    }
}

@Composable
private fun NavTabItem(
    glyph: NavGlyph,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) OrbitColors.ink900 else OrbitColors.slate400
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = OrbitSpacing.sm, vertical = OrbitSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NavIcon(glyph = glyph, tint = tint)
        Text(text = label, style = OrbitTypography.bodySmall, color = tint)
    }
}
